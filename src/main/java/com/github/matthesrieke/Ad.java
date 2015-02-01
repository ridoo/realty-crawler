package com.github.matthesrieke;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor.TokenType;
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
	private List<String> featureList;
	private Map<PropertyKeys, String> properties = new HashMap<>();
	private DateTime dateTime;

	public enum PropertyKeys {
		LOCATION,
		SPACE,
		ROOMS,
		PRICE,
		FEATURES,
		AVAILABLE_FROM,
		SELLER_TYPE,
		IMAGE,
		DATETIME,
		ID
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
			sb.delete(sb.length()-2, sb.length());
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
		
		for (PropertyKeys key : properties.keySet()) {
			result = result.replace("${"+key.toString()+"}", properties.get(key));
		}
		
		return result;
	}
	
	public static Ad fromNode(XmlObject elems) {
		Ad result = new Ad();
		result.setNode(elems);
		XmlObject[] title = Util.selectPath(".//div[@class=\"title-holder\"]/a", elems);
		result.id = parseResults(title, "href");
		result.properties.put(PropertyKeys.ID, result.id);
		
		XmlObject[] features = Util.selectPath(".//div[@class=\"feature-tags\"]/span", elems);
		result.featureList = parseFeatures(features);
		
		StringBuilder sb = new StringBuilder();
		for (String f : result.featureList) {
			sb.append(f);
			sb.append(", ");
		}
		if (result.featureList != null && result.featureList.size() > 0) {
			sb.delete(sb.length()-2, sb.length());
		}
		result.properties.put(PropertyKeys.FEATURES, sb.toString());
		
		XmlObject[] specs = Util.selectPath(".//div[@class=\"spec-table-wrapper\"]//tr", elems);
		parseSpecs(specs, result);
		
		XmlObject[] imgs = Util.selectPath(".//div[@class=\"image-wrapper\"]//a", elems);
		result.properties.put(PropertyKeys.IMAGE, parseImage(imgs));
		
		XmlObject[] seller = Util.selectPath(".//td[@class=\"sellername-wrapper\"]//div", elems);
		result.properties.put(PropertyKeys.SELLER_TYPE, parseSeller(seller));
		
		result.dateTime = new DateTime();
		result.properties.put(PropertyKeys.DATETIME, result.dateTime.toString());
		
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
			result.properties.put(PropertyKeys.PRICE, value);
		}
		else if (key.contains("Zimmer")) {
			result.properties.put(PropertyKeys.ROOMS, value);
		}
		else if (key.contains("fläche")) {
			result.properties.put(PropertyKeys.SPACE, value);
		}
		else if (key.contains("Ort")) {
			result.properties.put(PropertyKeys.LOCATION, value);
		}
		else if (key.contains("Verfügbar")) {
			result.properties.put(PropertyKeys.AVAILABLE_FROM, value);
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
	

}
