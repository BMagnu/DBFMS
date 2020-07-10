package net.bmagnu.dbfms.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;

import net.bmagnu.dbfms.database.Collection;

public class GUIMainTab {

	@FXML 
	private TextField searchQueryField;
	
	@FXML
	private TilePane filePane;
	
	public Collection collection;
	
	public void init(Collection collection) {
		this.collection = collection;
	}
	
	@FXML
	public void search_onSearch(ActionEvent event) {
        
    }
	
}
