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

	requires org.bytedeco.javacv;
	
	requires org.bytedeco.ffmpeg;
	
	requires org.apache.derby.commons;
	
	requires org.apache.derby.engine;
	
	opens net.bmagnu.dbfms.gui to javafx.fxml, javafx.graphics;
}