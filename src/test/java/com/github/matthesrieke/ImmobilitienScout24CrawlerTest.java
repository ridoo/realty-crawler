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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.matthesrieke;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.CrawlerException;
import com.github.matthesrieke.realty.Util;
import com.github.matthesrieke.realty.crawler.Crawler;
import com.github.matthesrieke.realty.crawler.ImmobilienScout24Crawler;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author Henning Bredel <h.bredel@52north.org>
 */
public class ImmobilitienScout24CrawlerTest {

    @Test
	public void testPreprocessing() throws CrawlerException, IOException {
		InputStream is = getClass().getResourceAsStream("/immobilienscout24.html");
		Crawler crawler = new ImmobilienScout24Crawler();
        StringBuilder data = crawler.preprocessContent(Util.parseStream(is));

        ObjectMapper om = new ObjectMapper();
        om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        Map<String,Object> map = om.readValue(data.toString(), Map.class);
        assertThat(map.containsKey("model"), CoreMatchers.is(true));
    }

    @Test
	public void testCrawling() throws CrawlerException, IOException {
		InputStream is = getClass().getResourceAsStream("/immobilienscout24.html");
		Crawler crawler = new ImmobilienScout24Crawler();
		List<Ad> items = crawler.parseDom(crawler.preprocessContent(Util.parseStream(is)));

        MatcherAssert.assertThat(20, CoreMatchers.is(items.size()));

		Ad first = items.get(0);

		String img = first.getProperties().get(Ad.PropertyKeys.IMAGE);

		assertThat(img, CoreMatchers.is("http://picture8is24-a.akamaihd.net/pic/orig02/N/355/851/533/355851533-0.jpg/ORIG/legacy_thumbnail/300x225/format/jpg/quality/80?642789033"));

	}
}
