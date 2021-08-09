package net.bmagnu.dbfms;

import net.bmagnu.dbfms.gui.DBFMS;
import net.bmagnu.dbfms.util.Logger;

import java.io.IOException;
import java.util.Properties;

import static  javafx.application.Application.launch;

public class Main {
	
	final static public Properties properties = new Properties();
	
	static {
		System.setProperty("sun.jnu.encoding", "UTF-8");
	}

	public static void main(String[] args) throws IOException {
		properties.load(Main.class.getResourceAsStream("dbfms.properties"));
		
		Logger.logInfo("Version: " + properties.getProperty("version"));
		
		launch(DBFMS.class, args);
	}
}
