package net.bmagnu.dbfms.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

	public enum LogLevel{
		INFO,
		WARNING,
		ERROR
	}
	
	public static synchronized void log(String msg, LogLevel level) {
		//TODO Fancier Logging to File maybe
		
		switch(level) {
		case ERROR:
			System.err.println("[ERROR] " + msg);
			break;
		case INFO:
			System.out.println("[INFO] " + msg);
			break;
		case WARNING:
			System.out.println("[WARNING] " + msg);
			break;
		}
		
	}
	
	public static void logInfo(String msg) {
		log(msg, LogLevel.INFO);
	}

	public static void logWarning(String msg) {
		log(msg, LogLevel.WARNING);
	}

	public static void logError(String msg) {
		log(msg, LogLevel.ERROR);
	}
	
	public static void logError(Exception e) {
		StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
		log(sw.toString(), LogLevel.ERROR);
	}
}
