package com.github.matthesrieke;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2548657318153366650L;
	private static final Logger logger = LoggerFactory
			.getLogger(CrawlerServlet.class);
	private Timer timer;
	private Properties properties;

	@Override
	public void init() throws ServletException {
		super.init();

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
		
		this.timer = new Timer();
		
		this.timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				logger.info("Starting to parse ad entries...");
				try {
					Crawler crawler = new Crawler();
					String link = properties.getProperty("baselink");
					logger.info("Baselink: "+link);
					
					int page = 1;
					while (true) {
						logger.info("Parsing page "+page);
						
						link = properties.getProperty("baselink");
						if (page != 1) {
							link = link.concat("&page="+page);
						}
						
						HttpGet get = new HttpGet(link);
						CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(20000).build()).build();
						try {
							CloseableHttpResponse resp = client.execute(get);
							if (resp.getStatusLine().getStatusCode() < HttpStatus.SC_MULTIPLE_CHOICES) {
								InputStream content = resp.getEntity().getContent();
								
								Map<String, Ad> items = crawler.parse(content);
								if (items == null || items.size() == 0) {
									break;
								}
								
								compareAndStoreItems(items);
								page++;
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
		}, 0, 1000 * 60 * 60 * 12);
	}
	
	@Override
	public void destroy() {
		super.destroy();
		
		this.timer.cancel();
	}

	protected void compareAndStoreItems(Map<String, Ad> items) {
		for (Ad a : items.values()) {
			logger.info(a.toString());
		}
	}
	
}
