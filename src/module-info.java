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
	
	requires java.sql;
	
	requires java.desktop;

	requires org.bytedeco.javacv;
	
	requires org.bytedeco.ffmpeg;
	
	opens net.bmagnu.dbfms.gui to javafx.fxml, javafx.graphics;
}