package com.github.matthesrieke;

import java.io.InputStream;
import java.util.Scanner;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.store.Path;

public class Util {

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
	
}
