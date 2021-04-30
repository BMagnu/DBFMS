package net.bmagnu.dbfms.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.bmagnu.dbfms.Main;
import net.bmagnu.dbfms.util.Logger;

public class LocalDatabase {

	private static final String connectionUrl;
	
	public static final String programDataDir;
	public static final String thumbDBDir;
	public static final char systemDelim;
	
	private static final Connection connection;

	static {
		

		if ((System.getProperty("os.name")).toUpperCase(Locale.ENGLISH).contains("WIN")) {
			programDataDir = System.getenv("AppData") + "\\" + "DBFMS\\";
			systemDelim = '\\';
		} else {
			programDataDir = System.getProperty("user.home") + "/" + ".dbfms/";
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
	
	public static void restore(File selectedFile) {
		Path backupDB = Paths.get(LocalDatabase.programDataDir + "backup");
		try {
			if(Files.exists(backupDB))
				Files.walk(backupDB)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} catch (IOException e) {
			Logger.logError(e);
			return;
		}

		try {
			FileInputStream fis = new FileInputStream(selectedFile);
			ZipInputStream zipIn = new ZipInputStream(fis);
			
	        ZipEntry entry;
	        while((entry = zipIn.getNextEntry()) != null) {
	        	if(entry.isDirectory()) {
	        		Files.createDirectories(backupDB.resolve("./" + entry.getName()));
	        	}
	        	else {
	        		FileOutputStream fos = new FileOutputStream(backupDB.resolve("./" + entry.getName()).toFile());
	        		byte[] buf = new byte[2048];
	        		int length;
	        		
	        		while((length = zipIn.read(buf)) > 0) {
	        			fos.write(buf, 0, length);
	        		}
	        		
	        		fos.close();
	        	}
	        }
	        
	        zipIn.close();
		} catch (IOException e) {
			Logger.logError(e);
			return;
		}
		
		try {
			if(!Files.readString(backupDB.resolve("version.txt")).equals(Main.properties.getProperty("version"))) {
				Logger.logWarning("Old DB Version detected for restoring backup!");
				
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Restoring Database");
				alert.setHeaderText("The database selected to restore was backed up with an old version of DBFMS");
				alert.setContentText("Are you sure you want to continue? It is advisable to create a backup of the current database before you proceed.");

				Optional<ButtonType> result = alert.showAndWait();
				if (result.isEmpty() || result.get() != ButtonType.OK){
					return;
				} 
			}
		} catch (IOException e) {
			Logger.logError(e);
			return;
		}
		
		try {
			connection.close();
			try {
				DriverManager.getConnection(connectionUrl.split(";")[0] + ";shutdown=true");
			} catch (SQLException e) {
				//Expect 08006 from a Shutdown
				if(!e.getSQLState().equals("08006")) {
					Logger.logError(e);
					return;
				}
			}
			
			String backupURL = connectionUrl.split(";")[0] + ";restoreFrom=" + programDataDir + "backup\\db";
			Connection newConn = DriverManager.getConnection(backupURL);
			newConn.commit();
			
			Logger.logInfo("Restored DB Backup with " + backupURL);
			
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Restored Database");
			alert.setHeaderText(null);
			alert.setContentText("The database was successfully restored. Please restart the application.");
			alert.showAndWait();
			
			System.exit(0);
		} catch (SQLException e) {
			Logger.logError(e);
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
