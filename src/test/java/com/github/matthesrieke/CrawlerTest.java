package com.github.matthesrieke;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.CrawlerException;
import com.github.matthesrieke.realty.Util;
import com.github.matthesrieke.realty.crawler.Crawler;
import com.github.matthesrieke.realty.crawler.ImmomiaCrawler;

public class CrawlerTest {
	
	@Test
	public void testCrawling() throws CrawlerException, IOException {
		InputStream is = getClass().getResourceAsStream("/test2.html");
		Crawler crawler = new ImmomiaCrawler();
		Map<String, Ad> items = crawler.parseDom(crawler.preprocessContent(Util.parseStream(is)));
		
		Assert.assertTrue(items.size() == 2);
		
		Ad first = items.values().iterator().next();
		
		Assert.assertTrue(first.getProperties().get(Ad.PropertyKeys.IMAGE).equals("http://rub-media.westfaelische-nachrichten.de/media/18724/21531138954563/wna_7617893.jpg"));
	}

}
