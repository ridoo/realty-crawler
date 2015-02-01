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
		
		for (String key : items.keySet()) {
			System.out.println(key +"="+items.get(key));
		}
	}

}
