package com.github.matthesrieke.realty.storage;

import java.io.IOException;
import java.util.Map;

import com.github.matthesrieke.realty.Ad;

public interface Storage {

	Map<String, Ad> storeItemsAndProvideNew(Map<String, Ad> items);

	Map<String, Ad> getAllItems() throws IOException;

	void shutdown();
	
}
