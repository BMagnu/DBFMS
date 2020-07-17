package net.bmagnu.dbfms.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;

import net.bmagnu.dbfms.util.Logger;

public class LocalDatabase {

	private static final String connectionUrl;
	
	public static final String programDataDir;
	public static final String thumbDBDir;
	public static final char systemDelim;
	
	private static final Connection connection;

	static {
		

		if ((System.getProperty("os.name")).toUpperCase(Locale.ENGLISH).contains("WIN")) {
			programDataDir = System.getenv("AppData") + "\\" + "DBFSM\\";
			systemDelim = '\\';
		} else {
			programDataDir = System.getProperty("user.home") + "/" + ".dbfsm/";
			systemDelim = '/';
		}

		connectionUrl = "jdbc:derby:" + programDataDir + "db" + ";create=true";
		thumbDBDir = programDataDir + "thumbCache" + systemDelim;

		Connection localdb = null;

		try {
			localdb = DriverManager.getConnection(connectionUrl);
			Logger.logInfo("Established Connection to DB at " + connectionUrl);
		} catch (SQLException e) {
			Logger.logError(e);
		}

		connection = localdb;
		
		File directory = new File(thumbDBDir);
		if (!directory.exists()) {
			directory.mkdir();
		}
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
			if (e instanceof DerbySQLIntegrityConstraintViolationException)
				Logger.logInfo("Constraint Violation: " + ((DerbySQLIntegrityConstraintViolationException) e).getTableName());
			else
				Logger.logError(e);
				
		}

		return results;
	}

	public static SQLException executeTransaction(String... queries) {
		try {
			connection.setAutoCommit(false);

			boolean success = true;
			SQLException sqlException = null;

			for (String query : queries) {
				try (Statement stmt = connection.createStatement()) {
					Logger.logInfo(query);
					stmt.executeUpdate(query);
				} catch (SQLException e) {
					success = false;
					sqlException = e;
					Logger.logError(e);
					break;
				}
			}

			if (success)
				connection.commit();
			else
				connection.rollback();

			connection.setAutoCommit(true);

			return sqlException;
		} catch (SQLException e) {
			Logger.logError(e);

			return e;
		}
	}
	
	public static void callSQL(String call, String... args) {
		try(CallableStatement cs = connection.prepareCall(call)) {
			
			for(int i = 0; i < args.length; i++) {
				cs.setString(i + 1, args[i]);
			}
			
			cs.execute(); 
		} catch (SQLException e) {
			Logger.logError(e);
		}
	}

}
