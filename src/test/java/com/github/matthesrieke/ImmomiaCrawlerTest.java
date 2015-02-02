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
package com.github.matthesrieke;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.CrawlerException;
import com.github.matthesrieke.realty.Util;
import com.github.matthesrieke.realty.crawler.Crawler;
import com.github.matthesrieke.realty.crawler.ImmomiaCrawler;

public class ImmomiaCrawlerTest {
	
	@Test
	public void testCrawling() throws CrawlerException, IOException {
		InputStream is = getClass().getResourceAsStream("/immomia.html");
		Crawler crawler = new ImmomiaCrawler();
		List<Ad> items = crawler.parseDom(crawler.preprocessContent(Util.parseStream(is)));
		
		Assert.assertTrue(items.size() == 2);
		
		Ad first = items.get(0);

		String img = first.getProperties().get(Ad.PropertyKeys.IMAGE);
		
		Assert.assertTrue(img.equals("http://rub-media.westfaelische-nachrichten.de/media/18724/21531138954563/wna_7617893.jpg") ||
				img.equals("http://rub-media.westfaelische-nachrichten.de/media/18719/88286312783062/wna_7616825.jpg"));

	}

}
