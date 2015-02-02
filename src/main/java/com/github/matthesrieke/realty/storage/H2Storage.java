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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.realty.Ad;
import com.github.matthesrieke.realty.Util;

public class H2Storage implements Storage {

	private static final Logger logger = LoggerFactory
			.getLogger(H2Storage.class);
	private static final String DB_FILE = "realty-ads";

	private static final String TABLE_NAME = "ADS";
	private static final String TIMESTAMP_COLUMN = "TIME";
	private static final String TIMESTAMP_COLUMN_TYPE = "TIMESTAMP";
	private static final String DATA_COLUMN = "DATA";
	private static final String DATA_COLUMN_TYPE = "BINARY(256000)";
	private static final String ID_COLUMN = "ID";

	private Connection connection;

	public H2Storage() {
		try {
			initialize(new File(getClass().getResource("/").getFile()));
		} catch (Exception e) {
			logger.error("Could not init storage database!", e);
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Map<String, Ad> storeItemsAndProvideNew(Map<String, Ad> items) {
		Map<String, Ad> result = new HashMap<>();
		
		for (String key : items.keySet()) {
			if (!checkIfExistingAndInsert(key, items.get(key))) {
				result.put(key, items.get(key));
			}
		}
		
		return result;
	}

	/**
	 * @param key
	 * @param ad
	 * @return if the item already existed
	 */
	private boolean checkIfExistingAndInsert(String key, Ad ad) {
		try {
			if (checkIfExisting(key)) {
				return true;
			}
			
			logger.debug("Persisting item: " + ad);
			
			PreparedStatement prep = this.connection
					.prepareStatement("insert into " + TABLE_NAME + " ("
							+ ID_COLUMN + ", " + TIMESTAMP_COLUMN
							+ ", " + DATA_COLUMN + ") values (?,?,?)");
			prep.setString(1, key);
			prep.setTimestamp(2, new Timestamp(ad.getDateTime().toDate().getTime()));
			prep.setBytes(3, Util.serialize(ad));
			prep.execute();
		} catch (SQLException e) {
			logger.warn("could not serialize ad " + ad, e);
		} catch (IOException e) {
			logger.warn("could not serialize ad " + ad, e);
		}
		return false;		
	}

	private boolean checkIfExisting(String key) throws SQLException {
		Statement stmt = this.connection.createStatement();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT count(*) from ");
		sb.append(TABLE_NAME);
		sb.append(" where ");
		sb.append(ID_COLUMN);
		sb.append(" = '");
		sb.append(key);
		sb.append("'");
		stmt.execute(sb.toString());
		
		ResultSet rs = stmt.getResultSet();
		
		boolean exists = false;
		if (rs.next()) {
			int count = rs.getInt(1);
			if (count > 0) {
				exists = true;;
			}
		}
		stmt.close();
		
		return exists;
	}

	protected void initialize(File baseLocation) throws Exception {
		Class.forName("org.h2.Driver");

		String connString = "jdbc:h2:" + baseLocation.getAbsolutePath() + "/"
				+ DB_FILE;

		logger.info("connecting to database: "+ connString);
		
		this.connection = DriverManager.getConnection(connString);
		this.connection.setAutoCommit(true);
		try {
			validateTable();
		} catch (IllegalStateException e) {
			logger.warn("database in an illegal state", e.getMessage());
			createTable();
		}
	}

	private void createTable() throws SQLException {
		Statement stmt = this.connection.createStatement();
		stmt.execute("CREATE TABLE " + TABLE_NAME + "(" + ID_COLUMN
				+ " VARCHAR(1024) PRIMARY KEY, " + TIMESTAMP_COLUMN + " "
				+ TIMESTAMP_COLUMN_TYPE + ", " + DATA_COLUMN + " "
				+ DATA_COLUMN_TYPE + ")");
	}

	private void validateTable() throws SQLException {
		DatabaseMetaData md = this.connection.getMetaData();
		ResultSet rs = md.getTables(null, null, TABLE_NAME, null);
		boolean valid = true;
		if (rs.next()) {
			rs.close();
			rs = md.getColumns(null, null, null, TIMESTAMP_COLUMN);
			if (rs.next()) {
				rs.close();
			} else {
				valid = false;
			}
			rs = md.getColumns(null, null, null, DATA_COLUMN);
			if (rs.next()) {
				rs.close();
			} else {
				valid = false;
			}
			rs = md.getColumns(null, null, null, ID_COLUMN);
			if (rs.next()) {
				rs.close();
			} else {
				valid = false;
			}
		} else {
			throw new IllegalStateException(TABLE_NAME + " not found.");
		}
		if (!valid) {
			Statement stmt = this.connection.createStatement();
			stmt.execute("DROP TABLE " + TABLE_NAME + " CASCADE");
			throw new IllegalStateException("Database malformed.");
		}
	}

	@Override
	public Map<String, Ad> getAllItems() throws IOException {
		try {
			Statement stmt = this.connection.createStatement();
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * from ");
			sb.append(TABLE_NAME);
			stmt.execute(sb.toString());
			
			ResultSet rs = stmt.getResultSet();
			
			Map<String, Ad> result = new HashMap<>();
			while (rs.next()) {
				String id = rs.getString(ID_COLUMN);
				InputStream data = rs.getBinaryStream(DATA_COLUMN);
				if (id != null && data != null) {
					result.put(id, Util.deserialize(data, Ad.class));
				}
			}
			stmt.close();
			
			return result;
		} catch (SQLException e) {
			logger.warn("Error retrieving database entries", e);
			throw new IOException(e);
		}
		
	}

	@Override
	public void shutdown() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			logger.warn("error on closing db connection", e);
		}
	}

}
