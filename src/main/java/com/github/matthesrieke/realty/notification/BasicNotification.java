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
package com.github.matthesrieke.realty.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.storage.Storage;

public class BasicNotification implements Notification {

	private static final Logger logger = LoggerFactory
			.getLogger(BasicNotification.class);
	
	@Override
	public void notifyOnNewItems(List<Ad> newItems) {
		for (Ad a : newItems) {
			logger.info(a.toString());
		}
	}

	@Override
	public void init(Storage st) {
	}
	
	@Override
	public void shutdown() {
		
	}

}
