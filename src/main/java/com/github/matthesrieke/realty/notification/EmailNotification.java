package com.github.matthesrieke.realty.notification;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.Ad;

public class EmailNotification implements Notification {

	private static final Logger logger = LoggerFactory
			.getLogger(EmailNotification.class);
	
	@Override
	public void notifyOnNewItems(Map<String, Ad> newItems) {
		for (Ad a : newItems.values()) {
			logger.info(a.toString());
		}
	}

	@Override
	public void shutdown() {
		
	}

}
