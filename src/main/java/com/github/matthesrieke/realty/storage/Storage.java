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
package com.github.matthesrieke.realty.storage;

import java.io.IOException;
import java.util.Map;

import com.github.matthesrieke.realty.Ad;

public interface Storage {

	Map<String, Ad> storeItemsAndProvideNew(Map<String, Ad> items);

	Map<String, Ad> getAllItems() throws IOException;

	void shutdown();
	
}
