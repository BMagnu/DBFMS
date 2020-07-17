package net.bmagnu.dbfms.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.bmagnu.dbfms.database.Collection;
import net.bmagnu.dbfms.util.Logger;

public class DialogAddCollection {

	@FXML
	private TextField collectionField;
	
	@FXML
	private VBox typeList;
	
	@FXML
	private TextField typeField;
	
	public List<String> types = new ArrayList<>();
	
	@FXML
	public void addType(ActionEvent e) {
		addTypeByName();
	}
	
	private void addTypeByName() {
		String typeName = Collection.sanitize(typeField.getText());
		
		typeField.setText("");
		
		if(types.contains(typeName))
			return;
		
		types.add(typeName);
		
		Label newTypeLabel = new Label(typeName);
		
		newTypeLabel.setOnMouseClicked((mouseEvent) -> {
			if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
	            if(mouseEvent.getClickCount() == 2){
	                typeList.getChildren().remove(newTypeLabel);
	                types.remove(typeName);
	            }
	        }
		});
		
		typeList.getChildren().add(newTypeLabel);
	}
	
	public String getCollectionName() {
		return collectionField.getText();
	}
	
	public static Dialog<DialogAddCollectionResult> getDialog() {

		return new Dialog<DialogAddCollectionResult>() {
			public Dialog<DialogAddCollectionResult> init() {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog_addCollection.fxml"));
				
				VBox content;
				DialogAddCollection controller;
				
				try {
					content = loader.load();
					controller = loader.getController();
				} catch (IOException e) {
					Logger.logError(e);
					return null;
				}
				
				final DialogPane dialogPane = getDialogPane();
				((Stage) dialogPane.getScene().getWindow()).getIcons().add(DBFMS.icon);
				dialogPane.setContent(content);
				
				dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
				
				controller.typeField.setOnAction((event) -> {
					controller.addTypeByName();
					event.consume();
				});
			
				setResultConverter((dialogButton) -> {
					DialogAddCollectionResult data = new DialogAddCollectionResult();
					
					data.name = controller.getCollectionName();
					data.types = controller.types;
					
					ButtonData button = dialogButton == null ? null : dialogButton.getButtonData();
					return button == ButtonData.OK_DONE ? data : null;
				});
				
				setTitle("Add Collection");
				return this;
			}	
		}.init();
		
	}
}

class DialogAddCollectionResult{
	public String name;
	public List<String> types;
}