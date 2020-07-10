/**
 * @author Birk
 *
 */
module dbfms {

	exports net.bmagnu.dbfms;
	
	requires javafx.controls;
	
	requires javafx.fxml;
	
	requires javafx.media;
	
	requires transitive javafx.graphics;
	
	requires javafx.base;
	requires java.sql;
	
	opens net.bmagnu.dbfms.gui to javafx.fxml, javafx.graphics;
}