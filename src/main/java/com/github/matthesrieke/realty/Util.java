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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.store.Path;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

	private static final Logger logger = LoggerFactory.getLogger(Util.class);
	public static final DateTimeFormatter GER_DATE_FORMAT = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");
	
	/**
	 * This method allows XPath 2.0 expressions with XmlBeans 2.4.0+ It uses a
	 * wrapper to access Saxon-HE 9.4.0.6
	 *
	 * @param path
	 *            the XPath expression
	 * @param xo
	 *            the Xmlobject
	 * @param opts
	 *            XmlOptions
	 * @return the resulting XmlObject array
	 */
	public static XmlObject[] selectPath(String path, XmlObject xo,
			XmlOptions opts) {
		opts.put(Path.PATH_DELEGATE_INTERFACE,
				SaxonXPath.class.getCanonicalName());
		return xo.selectPath(path, opts);
	}
	
	public static XmlObject[] selectPath(String path, XmlObject xo) {
		return selectPath(path, xo, new XmlOptions());
	}

	public static StringBuilder parseStream(InputStream is) {
		Scanner sc = new Scanner(is);
		StringBuilder sb = new StringBuilder();
		while (sc.hasNext()) {
			sb.append(sc.nextLine());
			sb.append(System.getProperty("line.separator"));
		}
		sc.close();
		return sb;
	}

	public static void replaceAll(StringBuilder builder, String[][] replacements) {
		for (int i = 0; i < replacements.length; i++) {
			String from = replacements[i][0];
			String to = replacements[i][1];
			int index = builder.indexOf(from);
			while (index != -1) {
				builder.replace(index, index + from.length(), to);
				index += to.length();
				index = builder.indexOf(from, index);
			}
		}
		
	}
	
	public static String parseAttribute(XmlObject[] title, String attribute) {
		if (title != null && title.length > 0) {
			XmlCursor cur = title[0].newCursor();
			String result = cur.getAttributeText(new QName("", "href"));
			cur.dispose();
			return result;
		}
		return null;
	}
	
	public static <T> byte[] serialize(T object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(object);
		return bos.toByteArray();
	}

	public static <T> T deserialize(InputStream is, Class<T> clazz) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(is);
		Object o;
		try {
			o = ois.readObject();
			if (clazz.isInstance(o)) {
				return clazz.cast(o);
			}
			throw new IOException("Invalid resulting class object");
		} catch (ClassNotFoundException e) {
			logger.warn("Could not deserialize object", e);
			throw new IOException(e);
		}
	}
	
}
