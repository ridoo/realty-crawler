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
package com.github.matthesrieke.realty.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.Ad.PropertyKeys;
import com.github.matthesrieke.realty.Util;

public class WohnungenMsCrawler implements Crawler {

	private static final Logger logger = LoggerFactory
			.getLogger(WohnungenMsCrawler.class);
	
	private static final String PROVIDER_NAME = "wohnungen.ms";
	
	@Override
	public StringBuilder preprocessContent(StringBuilder content) {
		int bodyIdx = content.indexOf("<body");
		content.delete(0, bodyIdx);
		bodyIdx = content.lastIndexOf("</body>");
		content.delete(bodyIdx + "</body>".length(), content.length());
		
		Util.replaceAll(content, new String[][] {{"&hellip;", ""}, {"&nbsp;", ""}, {"&bdquo;", ""}, {"&ldquo;", ""}});
		fixUnclosedTags(content);
		
		return content;
	}
	
	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder("<a><img src='' > </a>");
		fixUnclosedTags(sb);
	}

	private static void fixUnclosedTags(StringBuilder content) {
		List<String> candidates = new ArrayList<>();
		candidates.add("img");
		candidates.add("br");
		candidates.add("hr");
		
		for (String c : candidates) {
			String start = "<".concat(c);
			String end = "</".concat(c);
			int index = content.indexOf(start, 0);
			while (index < content.length() && index != -1) {
				int validClose = content.indexOf("/>", index);
				int justClose = content.indexOf(">", index);
				int elemClose = content.indexOf(end, index);
				
				if (validClose == -1 && elemClose == -1) {
					content.insert(justClose, "/");
				}
				else if (validClose > justClose && validClose > elemClose) {
					content.insert(justClose, "/");
				}
				index = content.indexOf(start, index+1);
			}
		}
	}

	@Override
	public boolean supportsParsing(String url) {
		if (url != null && url.contains("wohnungen.ms")) {
			return true;
		}
		return false;
	}

	@Override
	public List<Ad> parseDom(StringBuilder content) throws IOException {
		try {
			List<Ad> result = new ArrayList<>();
			XmlObject xo = XmlObject.Factory.parse(content.toString());
			
			XmlObject[] elems = Util.selectPath(".//article", xo);
			
			for (XmlObject elem : elems) {
				result.add(parseElement(elem));
			}
			
			return result;
		} catch (XmlException e) {
			logger.warn("could not parse dom", e);
			throw new IOException(e);
		}
	}

	private Ad parseElement(XmlObject elems) {
		//class="tile-title tile-title-list-seb"
		XmlObject[] title = Util.selectPath(".//h3[@class=\"tile-title tile-title-list-seb\"]/a", elems);
		Ad result = Ad.forId(Util.parseAttribute(title, "href"));
		result.putProperty(PropertyKeys.PROVIDER, PROVIDER_NAME);
		
		XmlObject[] content = Util.selectPath(".//div[@class=\"row\"]//div[@class=\"seb-content\"]/p", elems);
		result.putProperty(PropertyKeys.DESCRIPTION, parseContent(content));
		
		XmlObject[] props = Util.selectPath(".//div[@class=\"row\"]//table[@class=\"table-seb\"]", elems);
		parseProperties(props, result);
		
		return result;
	}

	private void parseProperties(XmlObject[] props, Ad result) {
		for (XmlObject xo : props) {
			XmlObject[] tdl = Util.selectPath(".//td[@class=\"tdl\"]//span[@class=\"hidden-xs\"]", xo);
			XmlObject[] tdr = Util.selectPath(".//td[@class=\"tdr\"]", xo);

			if (tdl != null && tdl.length > 0 && tdr != null && tdr.length > 0) {
				String key = null;
				String value = null;
				XmlCursor cur = tdl[0].newCursor();
				if (cur.toFirstContentToken() == TokenType.TEXT) {
					key = cur.getChars();
				}
				
				cur = tdr[0].newCursor();
				value = cur.getTextValue();
				
				if (key != null && value != null) {
					processProperty(key, value, result);
				}
			}
			
		}
	}

	private void processProperty(String key, String value, Ad result) {
		List<String> values = preprocessValues(value);
		if (key.contains("Größe")) {
			if (values.size() > 0) {
				String[] rooms = values.get(0).split(" ");
				result.putProperty(PropertyKeys.ROOMS, rooms[0]);	
			}
			if (values.size() > 1) {
				result.putProperty(PropertyKeys.SPACE, values.get(1));
			}
		}
		else if (key.contains("Stadtteil")) {
			result.putProperty(PropertyKeys.LOCATION, value);
		}
		else if (key.contains("Mietpreis")) {
			if (values.size() > 0) {
				result.putProperty(PropertyKeys.PRICE, values.get(0));	
			}
		}
		else if (key.contains("Ausstattungen")) {
			result.setFeatureList(values);
		}
		else if (key.contains("Eigenschaften")) {
			String content = result.getProperties().get(PropertyKeys.DESCRIPTION);
			if (content != null) {
				content = content.concat(value.trim().replaceAll("\\s+", " "));
			}
			else {
				content = value;
			}
			result.putProperty(PropertyKeys.DESCRIPTION, content);
		}
	}

	private List<String> preprocessValues(String value) {
		if (value == null) {
			return Collections.emptyList();
		}
		
		String[] values = value.split(",");
		List<String> result = new ArrayList<>();
		for (String v : values) {
			result.add(v.trim().replaceAll("\\s+", " "));
		}
		
		return result;
	}

	private String parseContent(XmlObject[] content) {
		if (content != null && content.length > 0) {
			XmlCursor cur = content[0].newCursor();
			if (cur.toFirstContentToken() == TokenType.TEXT) {
				return cur.getChars();
			}
		}
		return null;
	}

	@Override
	public int getFirstPageIndex() {
		return 1;
	}

	@Override
	public String prepareLinkForPage(String baseLink, int page) {
		if (page != 1) {
			return baseLink.concat("/seite/"+page);
		}
		return baseLink.trim();
	}

}
