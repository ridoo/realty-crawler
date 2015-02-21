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
import java.util.List;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.Ad.PropertyKeys;
import com.github.matthesrieke.realty.Util;

public class NadannCrawler implements Crawler {
	
	private static final Logger logger = LoggerFactory
			.getLogger(NadannCrawler.class);
	
	private static final String PROVIDER_NAME = "nadann";
	
	@Override
	public boolean supportsParsing(String url) {
		if (url != null && url.contains("nadann.de")) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public StringBuilder preprocessContent(StringBuilder sb) {
		/*
		 * trim to the important parts
		 */
		int start = sb.indexOf("class=\"klanz_list\"");
		int end = sb.indexOf("/Kleinanzeigen/Rubrik/Biete Wohnen.rss", start);
		end = sb.indexOf("</div>", end);
		end = sb.indexOf("</div>", end+6);
		sb.delete(end+6, sb.length());
		sb.delete(0, start-5);
		
		/*
		 * fix some markup
		 */
		String td = "</td>";
		String tr = "</tr>";
		String table = "</table>";
		int tdi = sb.indexOf(td) + td.length();
		
		/*
		 * insert missing </tr> closing tags
		 */
		while (tdi > 0) {
			int tri = sb.indexOf(tr, tdi);
			int tablei = sb.indexOf(table, tdi);
			if (tablei != -1) {
				if (tablei < tri || tri == -1) {
					sb.insert(tablei, tr);
					tdi = tablei;
					if (tdi > 0) {
						tdi += table.length();
					}
					continue;
				}
			}
			else {
				break;
			}
			tdi = sb.indexOf(td, tdi);
			if (tdi > 0) {
				tdi += td.length();
			}
		}
		
		
		sb = new StringBuilder(sb.toString().replaceAll("&(?!(?:apos|quot|[gl]t|amp);|#)", "&amp;"));
		return sb;
	}

	@Override
	public List<Ad> parseDom(StringBuilder is) throws IOException {
		List<Ad> result = new ArrayList<>();
		
		XmlObject xbean;
		try {
			xbean = XmlObject.Factory.parse(is.toString());
		} catch (XmlException e) {
			logger.warn("Error parsing XML!", e);
			throw new IOException(e);
		}
		
		XmlObject[] elems = Util.selectPath(".//div[@class=\"klanz_table_row\"]/div[@class=\"klanz klanz_table_cell\"]", xbean);
		
		DateTime crawlTime = new DateTime();
		
		for (int i = 0; i < elems.length; i++) {
			Ad entry = fromNode(elems[i]);
			
			if (entry == null) {
				continue;
			}
			
			entry.putProperty(PropertyKeys.PROVIDER, PROVIDER_NAME);
			entry.setDateTime(crawlTime);
			result.add(entry);
		}
		
		return result;
	}

	private Ad fromNode(XmlObject elems) {
		XmlObject[] divs = Util.selectPath("./div[@*]", elems);
		XmlObject[] divNoAtts = Util.selectPath("./div[not(@*)]", elems);
		
		String titleId = null;

		for (XmlObject div : divs) {
			XmlObject[] title = Util.selectPath("@id", div);
			if (title != null && title.length > 0) {
				XmlCursor cur = title[0].newCursor();
				titleId = cur.getTextValue();
			}
		}
		
		String content = null;
		for (XmlObject div : divNoAtts) {
			content = div.newCursor().getTextValue();
		}
		
		if (content != null) {
			content = content.trim();
		}
		
		if (content == null || content.isEmpty()) {
			logger.warn("Could not parse entry: "+elems);
			return null;
		}
		
		Ad result = null;
		if (titleId != null) {
			result = Ad.forId(titleId);	
		}
		else {
			result = Ad.forId("generated_".concat(Integer.toString(content.hashCode())));
		}
		
		result.putProperty(PropertyKeys.DESCRIPTION, content);
		
		return result;
	}

	@Override
	public int getFirstPageIndex() {
		return 1;
	}

	@Override
	public String prepareLinkForPage(String baseLink, int page) {
		return "http://www.nadann.de/Seiten/impressum";
	}
	
}
