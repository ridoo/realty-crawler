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
package com.github.matthesrieke.realty.crawler;

import java.io.IOException;
import java.util.List;

import com.github.matthesrieke.realty.Ad;

public interface Crawler {

	StringBuilder preprocessContent(StringBuilder content);

	boolean supportsParsing(String url);

	List<Ad> parseDom(StringBuilder content) throws IOException;

	int getFirstPageIndex();

	String prepareLinkForPage(String baseLink, int page);

}
