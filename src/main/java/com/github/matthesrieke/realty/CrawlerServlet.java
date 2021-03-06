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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.crawler.Crawler;
import com.github.matthesrieke.realty.notification.BasicNotification;
import com.github.matthesrieke.realty.notification.Notification;
import com.github.matthesrieke.realty.storage.H2Storage;
import com.github.matthesrieke.realty.storage.Metadata;
import com.github.matthesrieke.realty.storage.Storage;

public class CrawlerServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2548657318153366650L;
	private static final Logger logger = LoggerFactory
			.getLogger(CrawlerServlet.class);
	private final Notification notification = new BasicNotification();
	private Timer timer;
	private Properties properties;
	private ArrayList<Crawler> crawlers;
	private Storage storage;
	private StringBuilder listTemplate;
	private StringBuilder groupTemplate;
	private List<String> crawlLinks = new ArrayList<>();

	@Override
	public void init() throws ServletException {
		super.init();

		this.listTemplate = Util.parseStream(getClass().getResourceAsStream(
				"index-template.html"));
		this.groupTemplate = Util.parseStream(getClass().getResourceAsStream(
				"group-template.html"));

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

		readCrawlingLinks();
		
		String preferredDatabaseLocation = properties.getProperty("DATABASE_DIR");
		storage = new H2Storage(preferredDatabaseLocation);

		this.timer = new Timer();
		
		Integer crawlPeriod = Util.getIntegerProperty(this.properties, "crawlPeriodHours", 6);
		logger.info(String.format("Scheduling crawl every %s hours.", crawlPeriod));
		
		this.timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				logger.info("Starting to parse ad entries...");
				DateTime now = new DateTime();

				int insertedCount = 0;
				int crawlerCount = 0;
				
				try {
					for (String baseLink : crawlLinks) {
						logger.info("Baselink = " + baseLink);

						Crawler crawler;
						try {
							crawler = resolveCrawler(baseLink);
						} catch (UnsupportedBaseLinkException e1) {
							logger.warn(e1.getMessage(), e1);
							break;
						}
						
						crawlerCount++;

						String link;
						int page = crawler.getFirstPageIndex();
						while (true) {
							Thread.sleep(1000);
							logger.info("Parsing page " + page);

							link = crawler.prepareLinkForPage(baseLink, page);
							HttpGet get = new HttpGet(link);
							page++;

							CloseableHttpClient client = HttpClientBuilder
									.create()
									.setDefaultRequestConfig(
											RequestConfig.custom()
													.setConnectTimeout(20000)
													.build()).build();
							try {
								CloseableHttpResponse resp = client
										.execute(get);
								if (resp.getStatusLine().getStatusCode() < HttpStatus.SC_MULTIPLE_CHOICES) {
									InputStream content = resp.getEntity()
											.getContent();

									List<Ad> items = parse(content, crawler);
									if (items == null || items.size() == 0) {
										break;
									}

									insertedCount += compareAndStoreItems(items, now);

								} else {
									break;
								}
							} catch (IOException | CrawlerException e) {
								logger.warn(e.getMessage(), e);
								Metadata md = new Metadata("Exception during crawl: "+e.getMessage());
								storage.updateMetadata(md);
								break;
							}

						}

						logger.info("finished parsing ad entries!");
					}
					
					Metadata md = new Metadata(String.format(
							"Added %s new entries. %s crawlers have been used", insertedCount, crawlerCount));
					storage.updateMetadata(md);
				} catch (RuntimeException | InterruptedException e) {
					logger.warn(e.getMessage(), e);
					
					Metadata md = new Metadata("Exception during crawl: "+e.getMessage());
					storage.updateMetadata(md);
				}
				
			}
		}, 0, 1000 * 60 * 60 * crawlPeriod);
	}

	private void readCrawlingLinks() {
		InputStream is = getClass().getResourceAsStream("/links.txt");
		if (is != null) {
			Scanner sc = new Scanner(is);
			String l;
			while (sc.hasNext()) {
				l = sc.nextLine().trim();
				if (!l.startsWith("#")) {
					this.crawlLinks.add(l);
				}
			}
			sc.close();
		}
	}

	private void initializeCrawlers() {
		ServiceLoader<Crawler> loader = ServiceLoader.load(Crawler.class);

		this.crawlers = new ArrayList<>();
		for (Crawler crawler : loader) {
			this.crawlers.add(crawler);
		}
	}

	private List<Ad> parse(InputStream is, Crawler crawler)
			throws CrawlerException {
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

	protected Crawler resolveCrawler(String baseLink)
			throws UnsupportedBaseLinkException {
		for (Crawler crawler : crawlers) {
			if (crawler.supportsParsing(baseLink)) {
				return crawler;
			}
		}

		throw new UnsupportedBaseLinkException(
				"No crawler available for baselink: " + baseLink);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");

		String itemsMarkup = createGroupedItemsMarkup();
		String content = this.listTemplate.toString().replace("${entries}",
				itemsMarkup).replace("${meta}", createMetaMarkup());

		byte[] bytes = content.getBytes("UTF-8");

		resp.setContentLength(bytes.length);
		resp.setCharacterEncoding("UTF-8");
		resp.setStatus(HttpStatus.SC_OK);

		resp.getOutputStream().write(bytes);
		resp.getOutputStream().flush();
	}

	private String createMetaMarkup() {
		try {
			List<Metadata> mds = this.storage.getLatestMetadata(10);
			StringBuilder sb = new StringBuilder();
			for (Metadata md : mds) {
				sb.append("<li>[");
				sb.append(md.getDateTime().toString(Util.GER_DATE_FORMAT));
				sb.append("] ");
				sb.append(md.getStatus());
				sb.append("</li>");
			}
			return sb.toString();
		} catch (IOException e) {
			logger.warn("Error receiving storage metadata", e);
			return "";
		}
	}

	private String createGroupedItemsMarkup() {
		Map<DateTime, List<Ad>> items;
		StringBuilder sb = new StringBuilder();
		try {
			items = this.storage.getItemsGroupedByDate();
		} catch (IOException e) {
			logger.warn("Retrieval of items failed.", e);
			sb.append("Retrieval of items failed.");
			sb.append(e);
			return sb.toString();
		}

		List<DateTime> sortedKeys = new ArrayList<DateTime>(items.keySet());
		Collections.sort(sortedKeys);
		Collections.reverse(sortedKeys);
		for (DateTime a : sortedKeys) {
			StringBuilder adsBuilder = new StringBuilder();
			List<Ad> ads = items.get(a);
			for (Ad ad : ads) {
				adsBuilder.append(ad.toHTML());
			}
			sb.append(this.groupTemplate.toString()
					.replace("${GROUP_DATE}", a.toString(Util.GER_DATE_FORMAT))
					.replace("${entries}", adsBuilder.toString()));
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

	protected int compareAndStoreItems(List<Ad> items, DateTime now) {
		for (Ad ad : items) {
			ad.setDateTime(now);
		}

		List<Ad> newItems = this.storage.storeItemsAndProvideNew(items);
		if (newItems != null && newItems.size() > 0) {
			this.notification.notifyOnNewItems(newItems);
		}
		
		return newItems == null ? 0 : newItems.size();
	}

}
