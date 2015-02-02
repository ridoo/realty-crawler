package com.github.matthesrieke.realty.crawler;

import java.io.IOException;
import java.util.Map;

import com.github.matthesrieke.realty.Ad;

public interface Crawler {

	StringBuilder preprocessContent(StringBuilder content);

	boolean supportsParsing(String url);

	Map<String, Ad> parseDom(StringBuilder content) throws IOException;

	int getFirstPageIndex();

	String prepareLinkForPage(String baseLink, int page);

}
