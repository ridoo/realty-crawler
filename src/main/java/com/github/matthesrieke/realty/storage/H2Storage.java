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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
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
	
	private static final String META_TABLE_NAME = "METADATA";
	private static final String META_TIMESTAMP_COLUMN = "TIME";
	private static final String META_DATA_COLUMN = "DATA";
	private static final String META_DATA_COLUMN_TYPE = "VARCHAR(1024)";
	private static final String META_ID_COLUMN = "ID";

	private Connection connection;
	private String preferredDatabaseLocation;

	public H2Storage(String preferredDatabaseLocation) {
		this.preferredDatabaseLocation = preferredDatabaseLocation;
		try {
			initialize(new File(getClass().getResource("/").getFile()));
		} catch (Exception e) {
			logger.error("Could not init storage database!", e);
			throw new IllegalStateException(e);
		}
	}

	@Override
	public List<Ad> storeItemsAndProvideNew(List<Ad> items) {
		List<Ad> result = new ArrayList<>();
		
		for (Ad key : items) {
			if (!checkIfExistingAndInsert(key.getId(), key)) {
				result.add(key);
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

		String connString = "jdbc:h2:" + resolveFavoritePath(baseLocation) + "/"
				+ DB_FILE;

		logger.info("connecting to database: "+ connString);
		
		this.connection = DriverManager.getConnection(connString);
		this.connection.setAutoCommit(true);

		validateTables();
	}


	private void validateTables() throws SQLException {
		try {
			validateDataTable();
		} catch (IllegalStateException e) {
			logger.warn("database in an illegal state", e.getMessage());
			createDataTable();
		}
		
		try {
			validateMetadataTable();
		} catch (IllegalStateException e) {
			logger.warn("database in an illegal state", e.getMessage());
			createMetadataTable();
		}		
	}

	private void createMetadataTable() throws SQLException {
		Statement stmt = this.connection.createStatement();
		stmt.execute("CREATE TABLE " + META_TABLE_NAME + "(" + META_ID_COLUMN
				+ " bigint auto_increment PRIMARY KEY, " + META_TIMESTAMP_COLUMN + " "
				+ TIMESTAMP_COLUMN_TYPE + ", " + META_DATA_COLUMN + " "
				+ META_DATA_COLUMN_TYPE + ")");		
	}

	private void validateMetadataTable() throws SQLException {
		DatabaseMetaData md = this.connection.getMetaData();
		ResultSet rs = md.getTables(null, null, META_TABLE_NAME, null);
		boolean valid = true;
		if (rs.next()) {
			rs.close();
			rs = md.getColumns(null, null, null, META_TIMESTAMP_COLUMN);
			if (rs.next()) {
				rs.close();
			} else {
				valid = false;
			}
			rs = md.getColumns(null, null, null, META_DATA_COLUMN);
			if (rs.next()) {
				rs.close();
			} else {
				valid = false;
			}
			rs = md.getColumns(null, null, null, META_ID_COLUMN);
			if (rs.next()) {
				rs.close();
			} else {
				valid = false;
			}
		} else {
			throw new IllegalStateException(META_TABLE_NAME + " not found.");
		}
		if (!valid) {
			Statement stmt = this.connection.createStatement();
			stmt.execute("DROP TABLE " + META_TABLE_NAME + " CASCADE");
			throw new IllegalStateException("Meta Database malformed.");
		}		
	}

	private String resolveFavoritePath(File baseLocation) {
		if (preferredDatabaseLocation != null && !preferredDatabaseLocation.isEmpty()) {
			File preferred = new File(preferredDatabaseLocation);
			if (preferred.exists()) {
				if (preferred.isDirectory()) {
					return preferred.getAbsolutePath();
				}
			}
			else {
				if (preferred.mkdirs()) {
					return preferred.getAbsolutePath();
				}
				else {
					logger.warn("Could not create database directory: "+preferredDatabaseLocation);
				}
			}
		}
		
		String home = System.getProperty("user.home");
		File homeFile = new File(home);
		File realtyDir = new File(homeFile, ".realty-crawler");
		if (!realtyDir.exists()) {
			if (!realtyDir.mkdir()) {
				return baseLocation.getAbsolutePath();
			}
		}
		return realtyDir.getAbsolutePath();
	}

	private void createDataTable() throws SQLException {
		Statement stmt = this.connection.createStatement();
		stmt.execute("CREATE TABLE " + TABLE_NAME + "(" + ID_COLUMN
				+ " VARCHAR(1024) PRIMARY KEY, " + TIMESTAMP_COLUMN + " "
				+ TIMESTAMP_COLUMN_TYPE + ", " + DATA_COLUMN + " "
				+ DATA_COLUMN_TYPE + ")");
	}

	private void validateDataTable() throws SQLException {
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
	public List<Ad> getAllItems() throws IOException {
		try {
			Statement stmt = this.connection.createStatement();
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * from ");
			sb.append(TABLE_NAME);
			sb.append(" ORDER BY ");
			sb.append(TIMESTAMP_COLUMN);
			stmt.execute(sb.toString());
			
			ResultSet rs = stmt.getResultSet();
			
			List<Ad> result = new ArrayList<>();
			while (rs.next()) {
				String id = rs.getString(ID_COLUMN);
				InputStream data = rs.getBinaryStream(DATA_COLUMN);
				if (id != null && data != null) {
					result.add(Util.deserialize(data, Ad.class));
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
	public Map<DateTime, List<Ad>> getItemsGroupedByDate() throws IOException {
		Map<DateTime, List<Ad>> result = new HashMap<>();
		
		List<Ad> items = getAllItems();
		
		for (Ad ad : items) {
			DateTime ts = ad.getDateTime();
			if (!result.containsKey(ts)) {
				result.put(ts, new ArrayList<Ad>());
			}
			result.get(ts).add(ad);
		}
		
		return result;
	}

	@Override
	public void shutdown() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			logger.warn("error on closing db connection", e);
		}
	}

	@Override
	public void updateMetadata(Metadata md) {
		logger.debug("Updating metadata: " + md);
		
		PreparedStatement prep;
		try {
			prep = this.connection
					.prepareStatement("insert into " + META_TABLE_NAME + " ("
							+ META_TIMESTAMP_COLUMN
							+ ", " + META_DATA_COLUMN + ") values (?,?)");
			prep.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			prep.setString(2, md.getStatus());
			prep.execute();
		} catch (SQLException e) {
			logger.warn("Could not update metadata", e);
		}

	}

	@Override
	public List<Metadata> getLatestMetadata(int count) throws IOException {
		try {
			Statement stmt = this.connection.createStatement();
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * from ");
			sb.append(META_TABLE_NAME);
			sb.append(" ORDER BY ");
			sb.append(META_TIMESTAMP_COLUMN);
			if (count > 0) {
				sb.append(" LIMIT ");
				sb.append(count);
			}
			stmt.execute(sb.toString());
			
			ResultSet rs = stmt.getResultSet();
			
			List<Metadata> result = Metadata.fromResultSet(rs);
			
			return result;
		} catch (SQLException e) {
			logger.warn("Error retrieving meta-database entries", e);
			throw new IOException(e);
		}
	}

}
