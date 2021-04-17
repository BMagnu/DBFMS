/**
 * @author Birk
 *
 */
module dbfms {

	exports net.bmagnu.dbfms;
	
	requires javafx.controls;
	
	requires javafx.fxml;
	
	requires javafx.media;
	
	requires javafx.graphics;
	
	requires javafx.base;
	
	requires javafx.swing;
	
	requires java.sql;
	
	requires java.desktop;
	
	requires java.net.http;

	requires org.bytedeco.javacv;
	
	requires org.bytedeco.ffmpeg;
	
    requires org.bytedeco.ffmpeg.windows.x86_64; //TODO Only pick relevant
	
    requires org.bytedeco.ffmpeg.linux.x86_64;
    
	requires org.apache.derby.commons;
	
	requires org.apache.derby.engine;
	
	opens net.bmagnu.dbfms.gui to javafx.fxml, javafx.graphics;
}