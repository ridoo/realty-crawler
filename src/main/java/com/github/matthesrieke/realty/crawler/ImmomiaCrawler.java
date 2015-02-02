package com.github.matthesrieke.realty.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.Util;
import com.github.matthesrieke.realty.Ad.PropertyKeys;

public class ImmomiaCrawler implements Crawler {
	
	private static final Logger logger = LoggerFactory
			.getLogger(ImmomiaCrawler.class);
	
	@Override
	public boolean supportsParsing(String url) {
		if (url != null && url.contains("wn-immo.de")) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public StringBuilder preprocessContent(StringBuilder sb) {
		int sli = sb.lastIndexOf("class=\"searchresults-list\"");
		int tablei = sb.indexOf("<table", sli - 70);
		
		sb.delete(0, tablei);
		sb.delete(sb.lastIndexOf("</body"), sb.length());
		
		int endlii = sb.lastIndexOf("ende listEntry");
		int endi = sb.indexOf("</table>", endlii);
		sb.delete(endi + "</table>".length(), sb.length());
		
		Util.replaceAll(sb, new String[][] {{"<!-------", "<!--"},{"------>", "-->"},{"&", "&amp;"}});
		
		return sb;
	}

	@Override
	public Map<String, Ad> parseDom(StringBuilder is) throws IOException {
		Map<String, Ad> result = new HashMap<>();
		
		XmlObject xbean;
		try {
			xbean = XmlObject.Factory.parse(is.toString());
		} catch (XmlException e) {
			logger.warn("Error parsing XML!", e);
			throw new IOException(e);
		}
		
		XmlCursor cur = xbean.newCursor();
		cur.toFirstChild();
		cur.toFirstChild();
		xbean = cur.getObject();
		cur.dispose();
		
		XmlObject[] elems = xbean.selectChildren(new QName("tr"));
		
		DateTime crawlTime = new DateTime();
		
		for (int i = 0; i < elems.length; i++) {
			Ad entry = fromNode(elems[i]);
			entry.setDateTime(crawlTime);
			result.put(entry.getId(), entry);
		}
		
		return result;
	}

	private Ad fromNode(XmlObject elems) {
		XmlObject[] title = Util.selectPath(".//div[@class=\"title-holder\"]/a", elems);
		Ad result = Ad.forId(parseResults(title, "href"));
		result.setNode(elems);
		
		XmlObject[] features = Util.selectPath(".//div[@class=\"feature-tags\"]/span", elems);
		result.setFeatureList(parseFeatures(features));
		
		XmlObject[] specs = Util.selectPath(".//div[@class=\"spec-table-wrapper\"]//tr", elems);
		parseSpecs(specs, result);
		
		XmlObject[] imgs = Util.selectPath(".//div[@class=\"image-wrapper\"]//a", elems);
		result.putProperty(PropertyKeys.IMAGE, parseImage(imgs));
		
		XmlObject[] seller = Util.selectPath(".//td[@class=\"sellername-wrapper\"]//div", elems);
		result.putProperty(PropertyKeys.SELLER_TYPE, parseSeller(seller));
		
		return result;
	}

	private static String parseSeller(XmlObject[] seller) {
		if (seller != null && seller.length > 0) {
			XmlCursor cur = seller[0].newCursor();
			if (cur.toFirstContentToken() == TokenType.TEXT) {
				return cur.getChars();
			}
		}
		return null;
	}

	private static String parseImage(XmlObject[] imgs) {
		for (XmlObject img : imgs) {
			XmlObject[] imgSrc = Util.selectPath(".//img/@src", img);
			if (imgSrc != null && imgSrc.length > 0) {
				XmlCursor cur = imgSrc[0].newCursor();
				cur.toFirstContentToken();
				String result = cur.getTextValue();
				if (result != null && result.length() > 0) {
					return result;
				}
			}
		}
		return null;
	}

	private static void parseSpecs(XmlObject[] specs, Ad result) {
		for (XmlObject xo : specs) {
			XmlObject[] parameters = Util.selectPath(".//td", xo);
			String value = null;
			String key = null;
			for (XmlObject param : parameters) {
				XmlObject att = param.selectAttribute(new QName("", "class"));
				if (att != null && att.xmlText().contains("spec-value-small")) {
					XmlCursor cur = param.newCursor();
					cur.toFirstContentToken();
					value = cur.getChars();
				}
				else {
					XmlCursor cur = param.newCursor();
					cur.toFirstContentToken();
					key = cur.getChars();
				}
				
				if (key != null && value != null) {
					setKey(key, value, result);
				}
			}
		}
	}

	private static void setKey(String key, String value, Ad result) {
		if (key.equals("Kaltmiete")) {
			result.putProperty(PropertyKeys.PRICE, value);
		}
		else if (key.contains("Zimmer")) {
			result.putProperty(PropertyKeys.ROOMS, value);
		}
		else if (key.contains("fläche")) {
			result.putProperty(PropertyKeys.SPACE, value);
		}
		else if (key.contains("Ort")) {
			result.putProperty(PropertyKeys.LOCATION, value);
		}
		else if (key.contains("Verfügbar")) {
			result.putProperty(PropertyKeys.AVAILABLE_FROM, value);
		}
	}

	private static List<String> parseFeatures(XmlObject[] features) {
		List<String> result = new ArrayList<>();
		for (XmlObject xo : features) {
			XmlCursor cur = xo.newCursor();
			if (cur.toFirstContentToken() == TokenType.TEXT) {
				result.add(cur.getChars());
			}
		}
		return result;
	}

	private static String parseResults(XmlObject[] title, String attribute) {
		if (title != null && title.length > 0) {
			XmlCursor cur = title[0].newCursor();
			String result = cur.getAttributeText(new QName("", "href"));
			cur.dispose();
			return result;
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
			return baseLink.concat("&page="+page);
		}
		return baseLink.trim();
	}
	
}
