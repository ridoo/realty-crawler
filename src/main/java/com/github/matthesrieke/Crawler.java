package com.github.matthesrieke;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crawler {

	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	
	public Map<String, Ad> parse(InputStream is) throws CrawlerException {
		try {
			String preparedContent = preprocessContent(is);
			preparedContent = preparedContent.replace("<!-------", "<!--").replace("------>", "-->").replace("&", "&amp;");
			
			logger.info("Parsing content: "+preparedContent);
			return parseDom(new ByteArrayInputStream(preparedContent.getBytes()));
		} catch (XPathExpressionException | IOException | XmlException e) {
			logger.warn(e.getMessage(), e);
			throw new CrawlerException(e);
		}
	}

	private String preprocessContent(InputStream is) {
		StringBuilder sb = Util.parseStream(is);
		
		int sli = sb.lastIndexOf("class=\"searchresults-list\"");
		int tablei = sb.indexOf("<table", sli - 70);
		
		sb.delete(0, tablei);
		sb.delete(sb.lastIndexOf("</body"), sb.length());
		
		int endlii = sb.lastIndexOf("ende listEntry");
		int endi = sb.indexOf("</table>", endlii);
		sb.delete(endi + "</table>".length(), sb.length());
		
		return sb.toString();
	}

	private Map<String, Ad> parseDom(InputStream is) throws XPathExpressionException, XmlException, IOException {
		Map<String, Ad> result = new HashMap<>();
		
		XmlObject xbean = XmlObject.Factory.parse(is);
		XmlCursor cur = xbean.newCursor();
		cur.toFirstChild();
		cur.toFirstChild();
		xbean = cur.getObject();
		cur.dispose();
		
		XmlObject[] elems = xbean.selectChildren(new QName("tr"));
		
		for (int i = 0; i < elems.length; i++) {
			Ad entry = Ad.fromNode(elems[i]);
			result.put(entry.getId(), entry);
		}
		
		return result;
	}

}
