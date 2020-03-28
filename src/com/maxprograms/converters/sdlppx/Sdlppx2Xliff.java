/*******************************************************************************
 * Copyright (c) 2003-2020 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.sdlppx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Join;
import com.maxprograms.converters.Utils;
import com.maxprograms.converters.sdlxliff.Sdl2Xliff;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLOutputter;

import org.json.JSONObject;
import org.xml.sax.SAXException;

public class Sdlppx2Xliff {

	static List<String> srcLangs;
	static List<String> tgtLangs;
	private static String inputFile;
	private static String skeleton;
	private static ZipInputStream in;
	private static ZipOutputStream out;

	private Sdlppx2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();

		inputFile = params.get("source");
		String xliff = params.get("xliff");
		skeleton = params.get("skeleton");
		String sourceLanguage = params.get("srcLang");
		String targetLanguage = params.get("tgtLang");

		try {
			getPackageLanguages(inputFile);

			if (!tgtLangs.contains(targetLanguage)) {
				for (int i = 0; i < tgtLangs.size(); i++) {
					String lang = tgtLangs.get(i);
					if (lang.equalsIgnoreCase(targetLanguage)) {
						targetLanguage = lang;
						break;
					}
				}
			}
			if (!tgtLangs.contains(targetLanguage)) {
				result.add(Constants.ERROR);
				StringBuilder string = new StringBuilder("Incorrect target language. Valid options:");
				for (int i = 0; i < tgtLangs.size(); i++) {
					string.append(' ');
					string.append(tgtLangs.get(i));
				}
				result.add(string.toString());
				return result;
			}

			if (!srcLangs.contains(sourceLanguage)) {
				for (int i = 0; i < srcLangs.size(); i++) {
					String lang = srcLangs.get(i);
					if (lang.equalsIgnoreCase(sourceLanguage)) {
						sourceLanguage = lang;
						break;
					}
				}
			}
			if (!srcLangs.contains(sourceLanguage)) {
				result.add(Constants.ERROR);
				StringBuilder string = new StringBuilder("Incorrect source language. Valid options:");
				for (int i = 0; i < srcLangs.size(); i++) {
					string.append(' ');
					string.append(srcLangs.get(i));
				}
				result.add(string.toString());
				return result;
			}

			out = new ZipOutputStream(new FileOutputStream(skeleton));
			in = new ZipInputStream(new FileInputStream(inputFile));

			List<String> xliffList = new ArrayList<>();

			ZipEntry entry = null;
			while ((entry = in.getNextEntry()) != null) {
				File f = new File(entry.getName());
				if (targetLanguage.equals(f.getParent()) && f.getName().endsWith(".sdlxliff")) {
					// it is sdlxliff from target folder
					String name = f.getName();
					File tmp = File.createTempFile(name.substring(0, name.lastIndexOf('.')), ".sdlxliff");
					try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
						byte[] buf = new byte[1024];
						int len;
						while ((len = in.read(buf)) > 0) {
							output.write(buf, 0, len);
						}
					}

					Map<String, String> table = new HashMap<>();
					table.put("source", tmp.getAbsolutePath());
					table.put("xliff", tmp.getAbsolutePath() + ".xlf");
					table.put("skeleton", tmp.getAbsolutePath() + ".skl");
					table.put("catalog", params.get("catalog"));
					table.put("srcLang", params.get("srcLang"));
					String tgtLang = params.get("tgtLang");
					if (tgtLang != null) {
						table.put("tgtLang", tgtLang);
					}
					table.put("srcEncoding", params.get("srcEncoding"));
					table.put("paragraph", params.get("paragraph"));
					table.put("srxFile", params.get("srxFile"));
					table.put("format", params.get("format"));
					List<String> res = Sdl2Xliff.run(table);
					if (Constants.SUCCESS.equals(res.get(0))) {
						updateXliff(tmp.getAbsolutePath() + ".xlf", entry.getName());
						ZipEntry content = new ZipEntry(entry.getName() + ".skl");
						content.setMethod(ZipEntry.DEFLATED);
						out.putNextEntry(content);
						try (FileInputStream input = new FileInputStream(tmp.getAbsolutePath() + ".skl")) {
							byte[] array = new byte[1024];
							int len;
							while ((len = input.read(array)) > 0) {
								out.write(array, 0, len);
							}
							out.closeEntry();
						}
						File skl = new File(tmp.getAbsolutePath() + ".skl");
						Files.delete(skl.toPath());
						File xlf = new File(tmp.getAbsolutePath() + ".xlf");
						xliffList.add(xlf.getAbsolutePath());
					} else {
						saveEntry(entry, tmp.getAbsolutePath());
					}
					Files.delete(tmp.toPath());
				} else if (sourceLanguage.equals(f.getParent()) || f.getName().endsWith(".sdlproj")) {
					// preserve source files and project
					File tmp = File.createTempFile("zip", ".tmp");
					try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
						byte[] buf = new byte[1024];
						int len;
						while ((len = in.read(buf)) > 0) {
							output.write(buf, 0, len);
						}
					}
					saveEntry(entry, tmp.getAbsolutePath());
					Files.delete(tmp.toPath());
				}
			}

			in.close();
			out.close();

			// generate final XLIFF

			Join.join(xliffList, xliff);

			for (int i = 0; i < xliffList.size(); i++) {
				File xlf = new File(xliffList.get(i));
				Files.delete(Paths.get(xlf.toURI()));
			}

			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Sdlppx2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting SDL package", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void updateXliff(String xliff, String original)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		Element file = root.getChild("file");
		file.setAttribute("datatype", "x-sdlpackage");
		file.setAttribute("original", Utils.cleanString(inputFile));
		Element header = file.getChild("header");
		Element propGroup = new Element("prop-group");
		propGroup.setAttribute("name", "document");
		Element prop = new Element("prop");
		prop.setAttribute("prop-type", "original");
		prop.setText(original);
		propGroup.addContent(prop);
		header.addContent(propGroup);

		Element ext = header.getChild("skl").getChild("external-file");
		ext.setAttribute("href", Utils.cleanString(skeleton));

		XMLOutputter outputter = new XMLOutputter();
		Indenter.indent(root, 2);
		try (FileOutputStream output = new FileOutputStream(xliff)) {
			outputter.output(doc, output);
		}
	}

	private static void saveEntry(ZipEntry entry, String name) throws IOException {
		ZipEntry content = new ZipEntry(entry.getName());
		content.setMethod(ZipEntry.DEFLATED);
		out.putNextEntry(content);
		try (FileInputStream input = new FileInputStream(name)) {
			byte[] array = new byte[1024];
			int len;
			while ((len = input.read(array)) > 0) {
				out.write(array, 0, len);
			}
			out.closeEntry();
		}
	}

	public static JSONObject getPackageLanguages(String packageFile)
			throws IOException, SAXException, ParserConfigurationException {
		JSONObject result = new JSONObject();
		File project = null;
		srcLangs = new ArrayList<>();
		tgtLangs = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(new FileInputStream(packageFile))) {
			ZipEntry entry = null;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.getName().endsWith(".sdlproj")) {
					File f = new File(entry.getName());
					String name = f.getName();
					File tempFolder = new File(System.getProperty("java.io.tmpdir"));
					project = new File(tempFolder, name);
					try (FileOutputStream output = new FileOutputStream(project)) {
						byte[] buf = new byte[2048];
						int len;
						while ((len = zip.read(buf)) > 0) {
							output.write(buf, 0, len);
						}
					}
					break;
				}
			}
		}
		if (project == null) {
			result.put("result", "Failed");
			result.put("reason", "Project file not found");
			return result;
		}
		SAXBuilder builder = new SAXBuilder();
		Document proj = builder.build(project);
		Element projectRoot = proj.getRootElement();
		Element directions = projectRoot.getChild("LanguageDirections");
		List<Element> directionsList = directions.getChildren("LanguageDirection");
		Iterator<Element> it = directionsList.iterator();
		while (it.hasNext()) {
			Element direction = it.next();
			String sourceLanguageCode = direction.getAttributeValue("SourceLanguageCode");
			if (!srcLangs.contains(sourceLanguageCode)) {
				srcLangs.add(sourceLanguageCode);
			}
			String targetLanguageCode = direction.getAttributeValue("TargetLanguageCode");
			if (!tgtLangs.contains(targetLanguageCode)) {
				tgtLangs.add(targetLanguageCode);
			}
		}
		Files.delete(project.toPath());
		result.put("srcLangs", srcLangs);
		result.put("tgtLangs", tgtLangs);
		return result;
	}
}