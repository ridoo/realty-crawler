package com.github.matthesrieke.realty.notification;

import java.util.Map;

import com.github.matthesrieke.realty.Ad;

public interface Notification {

	void notifyOnNewItems(Map<String, Ad> newItems);

	void shutdown();

}
