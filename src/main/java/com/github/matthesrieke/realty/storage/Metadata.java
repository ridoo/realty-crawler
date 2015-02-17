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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

public class Metadata {

	private String status;
	private DateTime dateTime;

	public Metadata(String s) {
		this.status = s;
	}

	private Metadata() {
	}

	public String getStatus() {
		return this.status;
	}
	
	public DateTime getDateTime() {
		return dateTime;
	}

	public static List<Metadata> fromResultSet(ResultSet rs) throws SQLException {
		if (rs == null) {
			return null;
		}
		
		List<Metadata> result = new ArrayList<>();
		while (rs.next()) {
			Metadata md = new Metadata();
			md.status = rs.getString("DATA");
			md.dateTime = new DateTime(rs.getTimestamp("TIME"));
			result.add(md);
		}
		
		return result;
	}


}
