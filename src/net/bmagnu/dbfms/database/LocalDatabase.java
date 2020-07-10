package net.bmagnu.dbfms.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bmagnu.dbfms.util.Logger;

public class LocalDatabase {

	private static final String connectionUrl;
	public static final String programDataDir;

	private static final Connection connection;

	static {
		if ((System.getProperty("os.name")).toUpperCase().contains("WIN")) {
			programDataDir = System.getenv("AppData") + "\\" + "DBFSM\\";
		} else {
			programDataDir = System.getProperty("user.home") + "/" + ".dbfsm/";
		}

		connectionUrl = "jdbc:derby:" + programDataDir + "db" + ";create=true";

		Connection localdb = null;

		try {
			localdb = DriverManager.getConnection(connectionUrl);
			Logger.logInfo("Established Connection to DB at " + connectionUrl);
		} catch (SQLException e) {
			Logger.logError(e);
		}

		connection = localdb;
	}

	public static List<Map<String, Object>> executeSQL(String query, String... columns) {
		return executeSQL(query, false, columns);
	}

	public static List<Map<String, Object>> executeSQL(String query, boolean update, String... columns) {
		List<Map<String, Object>> results = new ArrayList<>();

		try (Statement stmt = connection.createStatement()) {

			Logger.logInfo(query);
			
			if (update) {
				stmt.executeUpdate(query);
				return null;
			}
		
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {

				Map<String, Object> currentRow = new HashMap<>(columns.length, 0.99f);

				for (String column : columns)
					currentRow.put(column, rs.getObject(column));

				results.add(currentRow);
			}
		} catch (SQLException e) {
			Logger.logError(e);
		}

		return results;
	}

	public static SQLException executeTransaction(String... queries) {
		try {
			connection.setAutoCommit(false);
//TODO actually commit
			SQLException sqlException = null;
			
			for (String query : queries) {
				try (Statement stmt = connection.createStatement()) {
					Logger.logInfo(query);
					stmt.executeUpdate(query);
				} catch (SQLException e) {
					sqlException = e;
					Logger.logError(e);
					break;
				}
			}

			connection.setAutoCommit(true);
			
			return sqlException;
		} catch (SQLException e) {
			Logger.logError(e);
			
			return e;
		}
	}

}
