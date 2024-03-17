/*******************************************************************************
 * Copyright (c) 2022 - 2024 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters;

import org.json.JSONObject;

public class Constants {

	private Constants() {
		// do not instantiate this class
	}

	public static final String TOOLID = "OpenXLIFF";
	public static final String TOOLNAME = "OpenXLIFF Filters";
	public static final String VERSION = "3.20.0";
	public static final String BUILD = "20240316_1220";

	public static final String SUCCESS = "0";
	public static final String ERROR = "1";
	public static final String CANCELLED = "Cancelled";

	public static void main(String[] args) {
		JSONObject json = new JSONObject();
		json.put("toolId", TOOLID);
		json.put("toolName", TOOLNAME);
		json.put("version", VERSION);
		json.put("build", BUILD);
		json.put("java", System.getProperty("java.version"));
		json.put("javaVendor", System.getProperty("java.vendor"));
		json.put("xmlVersion", com.maxprograms.xml.Constants.VERSION);
		json.put("xmlBuild", com.maxprograms.xml.Constants.BUILD);
		System.out.println(json.toString(2));
	}
}
