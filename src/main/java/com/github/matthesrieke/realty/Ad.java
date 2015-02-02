/**
 * Copyright (C) 2015 Matthes Rieke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.matthesrieke.realty;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.joda.time.DateTime;

public class Ad implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6513599382861284635L;

	private static final String SEP = "; ";

	private static String htmlTemplate = null;

	static {
		InputStream is = Ad.class.getResourceAsStream("ad-template.html");
		htmlTemplate = Util.parseStream(is).toString();
	}

	private String id;
	private List<String> featureList = new ArrayList<>();
	private Map<PropertyKeys, String> properties = new HashMap<>();
	private DateTime dateTime;

	public enum PropertyKeys {
		LOCATION, SPACE, ROOMS, PRICE, FEATURES, AVAILABLE_FROM, SELLER_TYPE, IMAGE, DATETIME, ID, DESCRIPTION
	}

	private Ad() {
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(properties.get(PropertyKeys.LOCATION));
		sb.append(SEP);

		sb.append("Größe: ");
		sb.append(properties.get(PropertyKeys.SPACE));
		sb.append(SEP);

		sb.append("Zimmer: ");
		sb.append(properties.get(PropertyKeys.ROOMS));
		sb.append(SEP);

		sb.append("Miete: ");
		sb.append(properties.get(PropertyKeys.PRICE));
		sb.append(SEP);

		sb.append("Ausstattung: ");
		for (String f : featureList) {
			sb.append(f);
			sb.append(", ");
		}
		if (featureList != null && featureList.size() > 0) {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(SEP);

		sb.append("Bezug ab: ");
		sb.append(properties.get(PropertyKeys.AVAILABLE_FROM));
		sb.append(SEP);

		sb.append("Anbieter: ");
		sb.append(properties.get(PropertyKeys.SELLER_TYPE));
		sb.append(SEP);

		sb.append("Original: ");
		sb.append(properties.get(PropertyKeys.IMAGE));

		return sb.toString();
	}

	public String toHTML() {
		String result = htmlTemplate;

		StringBuilder tmpsb = new StringBuilder("${");
		for (PropertyKeys key : properties.keySet()) {
			tmpsb.append(key.toString());
			tmpsb.append("}");
			String value = properties.get(key);
			if (value == null) {
				value = "";
			}
			result = result.replace(tmpsb.toString(), value);
			tmpsb.delete(2, tmpsb.length());
		}

		return result;
	}

	private XmlObject node;

	public void setNode(XmlObject elems) {
		this.node = elems;
	}

	public XmlObject getNode() {
		return node;
	}

	public String getId() {
		return this.id;
	}

	public List<String> getFeatureList() {
		return featureList;
	}

	public Map<PropertyKeys, String> getProperties() {
		return properties;
	}

	public DateTime getDateTime() {
		return dateTime;
	}

	public static Ad forId(String theId) {
		Ad a = new Ad();
		a.id = theId;
		a.properties.put(PropertyKeys.ID, a.id);
		a.properties.put(PropertyKeys.AVAILABLE_FROM, null);
		a.setDateTime(new DateTime());
		return a;
	}

	public void setFeatureList(List<String> parseFeatures) {
		featureList = parseFeatures;

		StringBuilder sb = new StringBuilder();
		for (String f : featureList) {
			sb.append(f);
			sb.append(", ");
		}
		if (featureList != null && featureList.size() > 0) {
			sb.delete(sb.length() - 2, sb.length());
		}

		properties.put(PropertyKeys.FEATURES, sb.toString());
	}

	public void putProperty(PropertyKeys key, String value) {
		properties.put(key, value);
	}

	public void setDateTime(DateTime crawlTime) {
		dateTime = crawlTime;
		properties.put(PropertyKeys.DATETIME, dateTime.toString());
	}

}
