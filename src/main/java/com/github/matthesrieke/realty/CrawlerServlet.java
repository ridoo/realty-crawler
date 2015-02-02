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
package com.github.matthesrieke.realty;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.crawler.Crawler;
import com.github.matthesrieke.realty.notification.EmailNotification;
import com.github.matthesrieke.realty.notification.Notification;
import com.github.matthesrieke.realty.storage.H2Storage;
import com.github.matthesrieke.realty.storage.Storage;

public class CrawlerServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2548657318153366650L;
	private static final Logger logger = LoggerFactory
			.getLogger(CrawlerServlet.class);
	private final Notification notification = new EmailNotification();
	private Timer timer;
	private Properties properties;
	private ArrayList<Crawler> crawlers;
	private Storage storage = new H2Storage();
	private StringBuilder listTemplate;

	@Override
	public void init() throws ServletException {
		super.init();

		this.listTemplate = Util.parseStream(getClass().getResourceAsStream("list-template.html"));
		
		this.properties = new Properties();
		InputStream is = getClass().getResourceAsStream("/config.properties");
		if (is == null) {
			is = getClass().getResourceAsStream("/config.properties.default");
		}
		try {
			this.properties.load(is);
		} catch (IOException e1) {
			logger.warn(e1.getMessage(), e1);
			throw new ServletException("Could not init properties!", e1);
		}
		
		initializeCrawlers();
		
		this.timer = new Timer();
		
		this.timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				logger.info("Starting to parse ad entries...");
				try {
					String baseLink = properties.getProperty("baselink");
					logger.info("Baselink = "+properties.getProperty("baselink"));
					
					Crawler crawler;
					try {
						crawler = resolveCrawler(baseLink);
					} catch (UnsupportedBaseLinkException e1) {
						logger.warn(e1.getMessage(), e1);
						return;
					}					
					
					String link;
					int page = crawler.getFirstPageIndex();
					while (true) {
						logger.info("Parsing page "+page);
						
						link = crawler.prepareLinkForPage(baseLink, page);
						HttpGet get = new HttpGet(link);
						page++;
						
						CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(20000).build()).build();
						try {
							CloseableHttpResponse resp = client.execute(get);
							if (resp.getStatusLine().getStatusCode() < HttpStatus.SC_MULTIPLE_CHOICES) {
								InputStream content = resp.getEntity().getContent();
								
								Map<String, Ad> items = parse(content, crawler);
								if (items == null || items.size() == 0) {
									break;
								}
								
								compareAndStoreItems(items);
								
							}
						} catch (IOException | CrawlerException e) {
							logger.warn(e.getMessage(), e);
						}
	
					}
										
					logger.info("finished parsing ad entries!");
				}
				catch (RuntimeException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}, 0, 1000 * 60 * 60 * 4);
	}
	
	private void initializeCrawlers() {
		ServiceLoader<Crawler> loader = ServiceLoader.load(Crawler.class);
		
		this.crawlers = new ArrayList<>();
		for (Crawler crawler : loader) {
			this.crawlers.add(crawler);
		}
	}

	private Map<String, Ad> parse(InputStream is, Crawler crawler) throws CrawlerException {
		try {
			StringBuilder sb = Util.parseStream(is);
			sb = crawler.preprocessContent(sb);

			logger.debug("Parsing content: " + sb.toString());
			return crawler.parseDom(sb);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			throw new CrawlerException(e);
		}
	}
	
	protected Crawler resolveCrawler(String baseLink) throws UnsupportedBaseLinkException {
		for (Crawler crawler : crawlers) {
			if (crawler.supportsParsing(baseLink)) {
				return crawler;
			}
		}
		
		throw new UnsupportedBaseLinkException("No crawler available for baselink: "+baseLink);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		
		String itemsMarkup = createItemsMarkup();
		String content = this.listTemplate.toString().replace("${entries}",
				itemsMarkup);
		
		byte[] bytes = content.getBytes("UTF-8");
		
		resp.setContentLength(bytes.length);
		resp.setCharacterEncoding("UTF-8");
		resp.setStatus(HttpStatus.SC_OK);
		
		resp.getOutputStream().write(bytes);
		resp.getOutputStream().flush();
	}

	private String createItemsMarkup() {
		Map<String, Ad> items;
		StringBuilder sb = new StringBuilder();
		try {
			items = this.storage.getAllItems();
		} catch (IOException e) {
			logger.warn("Retrieval of items failed.", e);
			sb.append("Retrieval of items failed.");
			sb.append(e);
			return sb.toString();
		}
		
		for (Ad a : items.values()) {
			sb.append(a.toHTML());
		}
		
		return sb.toString();
	}

	@Override
	public void destroy() {
		super.destroy();
		
		this.timer.cancel();
		this.storage.shutdown();
		this.notification.shutdown();
	}
	

	protected void compareAndStoreItems(Map<String, Ad> items) {
		Map<String, Ad> newItems = this.storage.storeItemsAndProvideNew(items);
		if (newItems != null && newItems.size() > 0) {
			this.notification.notifyOnNewItems(newItems);
		}
	}
	
}
