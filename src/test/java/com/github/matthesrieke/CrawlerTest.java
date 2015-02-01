package com.github.matthesrieke;

import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class CrawlerTest {
	
	@Test
	public void testCrawling() throws CrawlerException {
		InputStream is = getClass().getResourceAsStream("/test2.html");
		Crawler crawler = new Crawler();
		Map<String, Ad> items = crawler.parse(is);
		
		Assert.assertTrue(items.size() == 2);
		
		Ad first = items.values().iterator().next();
		
		Assert.assertTrue(first.getProperties().get(Ad.PropertyKeys.IMAGE).equals("http://rub-media.westfaelische-nachrichten.de/media/18724/21531138954563/wna_7617893.jpg"));
	}

}
