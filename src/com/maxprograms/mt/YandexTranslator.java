/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

package com.maxprograms.mt;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

public class YandexTranslator implements MTEngine {

    private String apiKey;
    private String srcLang;
    private String tgtLang;
    private List<String> directions;

    public YandexTranslator(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getName() {
        return "Yandex Translate API";
    }

    @Override
    public String getShortName() {
        return "Yandex";
    }

    @Override
    public List<Language> getSourceLanguages() throws IOException, InterruptedException {
        if (directions == null) {
            getDirections();
        }
        List<Language> result = new ArrayList<>();
        for (int i = 0; i < directions.size(); i++) {
            String dir = directions.get(i);
            Language lang = LanguageUtils.getLanguage(dir.substring(0, dir.indexOf("-")));
            if (!result.contains(lang)) {
                result.add(lang);
            }
        }
        Collections.sort(result, (o1, o2) -> o1.getCode().compareTo(o2.getCode()));
        return result;
    }

    @Override
    public List<Language> getTargetLanguages() throws IOException, InterruptedException {
        if (directions == null) {
            getDirections();
        }
        List<Language> result = new ArrayList<>();
        for (int i = 0; i < directions.size(); i++) {
            String dir = directions.get(i);
            Language lang = LanguageUtils.getLanguage(dir.substring(dir.indexOf("-") + 1));
            if (!result.contains(lang)) {
                result.add(lang);
            }
        }
        Collections.sort(result, (o1, o2) -> o1.getCode().compareTo(o2.getCode()));
        return result;
    }

    private void getDirections() throws IOException, InterruptedException {
        HttpClient httpclient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://translate.yandex.net/api/v1.5/tr.json/getLangs?key=" + this.apiKey + "&ui=en"))
                .build();
        HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            String body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body);
                directions = new ArrayList<>();
                JSONArray dirs = json.getJSONArray("dirs");
                for (int i = 0; i < dirs.length(); i++) {
                    directions.add(dirs.getString(i));
                }
                return;
            }
            throw new IOException(Messages.getString("YandexTranslator.6"));
        }
        MessageFormat mf = new MessageFormat(Messages.getString("YandexTranslator.7"));
        throw new IOException(mf.format(new String[] { "" + response.statusCode() }));
    }

    public List<String> getTranslationDirections() throws IOException, InterruptedException {
        if (directions == null) {
            getDirections();
        }
        Collections.sort(directions);
        return directions;
    }

    @Override
    public void setSourceLanguage(String lang) {
        srcLang = lang;
    }

    @Override
    public void setTargetLanguage(String lang) {
        tgtLang = lang;
    }

    @Override
    public String translate(String source) throws IOException, InterruptedException {
        HttpClient httpclient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + apiKey + "&lang="
                        + srcLang + "-" + tgtLang + "&text=" + URLEncoder.encode(source, StandardCharsets.UTF_8)))
                .build();

        HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body);
                int code = json.getInt("code");
                if (code == 200) {
                    JSONArray array = json.getJSONArray("text");
                    return array.getString(0);
                }
                if (code == 413) {
                    throw new IOException(Messages.getString("YandexTranslator.2"));
                }
                if (code == 422) {
                    throw new IOException(Messages.getString("YandexTranslator.3"));
                }
                if (code == 501) {
                    if (directions == null) {
                        getDirections();
                    }
                    if (!directions.contains(srcLang + "-" + tgtLang)) {
                        throw new IOException(Messages.getString("YandexTranslator.4"));
                    }
                }
                MessageFormat mf = new MessageFormat(Messages.getString("YandexTranslator.5"));
                throw new IOException(mf.format(new String[] { "" + code }));
            }
            throw new IOException(Messages.getString("YandexTranslator.6"));
        }
        MessageFormat mf = new MessageFormat(Messages.getString("YandexTranslator.7"));
        throw new IOException(mf.format(new String[] { "" + response.statusCode() }));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof YandexTranslator yt) {
            return srcLang.equals(yt.getSourceLanguage()) && tgtLang.equals(yt.getTargetLanguage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return YandexTranslator.class.getName().hashCode();
    }

    @Override
    public String getSourceLanguage() {
        return srcLang;
    }

    @Override
    public String getTargetLanguage() {
        return tgtLang;
    }
}