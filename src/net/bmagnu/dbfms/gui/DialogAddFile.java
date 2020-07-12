package net.bmagnu.dbfms.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import net.bmagnu.dbfms.database.Collection;
import net.bmagnu.dbfms.util.Logger;
import net.bmagnu.dbfms.util.Thumbnail;

import static net.bmagnu.dbfms.database.LocalDatabase.executeSQL;

public class DialogAddFile {

	@FXML
	private Label labelFilename;
	
	@FXML
	private ImageView thumbnailView;
	
	@FXML
	private VBox listTags;
	
	@FXML
	private VBox listFields;
	
	@FXML
	private VBox listTypes;
	
	@FXML
	private TextField textTag;
	
	@FXML
	private TextField textFieldName;
	
	@FXML
	private TextField textFieldContent;
	
	@FXML
	private TextField textRating;
	
	public List<String> tags = new ArrayList<>();
	
	public Map<String, String> fields = new HashMap<>();
	
	public Map<String, ComboBox<String>> types = new HashMap<>();
	
	public String thumbnailHash = "";
	
	private List<String> typeList = new ArrayList<>();
	
	public void init(Collection collection, String filePath) {
		labelFilename.setText(filePath);
		
		List<Map<String, Object>> typesSQL = executeSQL("SELECT typeName FROM M_COLLECTIONTYPES WHERE collection = '" + collection.name + "'", "typeName");
		for(Map<String, Object> type : typesSQL)
			typeList.add((String) type.get("typeName")); 
		
		for(String type : typeList) {
			ComboBox<String> typeField = new ComboBox<>();
			typeField.setEditable(true);
			
			typeField.getEditor().textProperty().addListener((obs, oldText, newText) -> {
				typeField.setValue(newText);
		    });
			
			List<Map<String, Object>> typeValues = executeSQL("SELECT typeValue FROM " + collection.typeValuesDB.globalName + " WHERE typeName = '" + type + "'", "typeValue");
			for(Map<String, Object> typeValue : typeValues)
				typeField.getItems().add((String)typeValue.get("typeValue"));			
			
			HBox typeBox = new HBox(5, new Label(type + '.'), typeField);
			typeBox.setAlignment(Pos.CENTER_LEFT);
			
			listTypes.getChildren().add(typeBox);
			
			types.put(type, typeField);
		}
		
		List<Map<String, Object>> fileExists = executeSQL("SELECT fileID, fileThumb, rating FROM " + collection.fileDB.globalName + " WHERE filePath = '" + filePath + "'", "fileID", "fileThumb", "rating");

		String thumbFile = "";
		
		if(!fileExists.isEmpty()) {
			thumbnailHash = (String)fileExists.get(0).get("fileThumb");
			thumbFile = "";
			//TODO
		}
		
		Thumbnail thumbnail = Thumbnail.getThumbnail(filePath, thumbFile);
		thumbnailView.setImage(thumbnail.loadImage());
		
		//TODO Tag Recommendations
	}
	
	@FXML
	public void addTag(ActionEvent e) {
		String tagName = textTag.getText();
		if(tags.contains(tagName))
			return;
		
		tags.add(tagName);
		
		Label newTagLabel = new Label(tagName);
		
		newTagLabel.setOnMouseClicked((mouseEvent) -> {
			if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
	            if(mouseEvent.getClickCount() == 2){
	            	listTags.getChildren().remove(newTagLabel);
	                types.remove(tagName);
	            }
	        }
		});
		
		listTags.getChildren().add(newTagLabel);
	}
	
	@FXML
	public void addField(ActionEvent e) {
		String fieldName = textFieldName.getText();
		if(fields.get(fieldName) != null)
			return;
		
		fields.put(fieldName, textFieldContent.getText());
		
		Label newFieldLabel = new Label(fieldName + ':' + textFieldContent.getText());
		
		newFieldLabel.setOnMouseClicked((mouseEvent) -> {
			if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
	            if(mouseEvent.getClickCount() == 2){
	            	listFields.getChildren().remove(newFieldLabel);
	                fields.remove(fieldName);
	            }
	        }
		});
		
		listFields.getChildren().add(newFieldLabel);
	}
	
	@FXML
	public void setThumb(ActionEvent e) {
		//TODO
	}
	
	@FXML
	public void manageTypes(ActionEvent e) {
		//TODO
		Logger.logWarning("Not Implemented");
	}
	
	public static Dialog<DialogAddFileResult> getDialog(Collection collection, String filePath) {

		return new Dialog<DialogAddFileResult>() {
			public Dialog<DialogAddFileResult> init() {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("dialog_addFile.fxml"));
				
				VBox content;
				DialogAddFile controller;
				
				try {
					content = loader.load();
					controller = loader.getController();
				} catch (IOException e) {
					Logger.logError(e);
					return null;
				}
				
				final DialogPane dialogPane = getDialogPane();
				dialogPane.setContent(content);
				
				dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
				
				controller.init(collection, filePath);
			
				setResultConverter((dialogButton) -> {
					DialogAddFileResult data = new DialogAddFileResult();
					
					ButtonData button = dialogButton == null ? null : dialogButton.getButtonData();
					boolean buttonOK = button == ButtonData.OK_DONE;
					
					if(buttonOK) {
						//TODO Submit to DB
						
						
					}
					
					return data;
				});
				
				setTitle("Add File");
				return this;
			}	
		}.init();
		
	}
}

class DialogAddFileResult{
	public boolean success = false;
}