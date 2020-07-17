package net.bmagnu.dbfms.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import net.bmagnu.dbfms.database.Collection;
import net.bmagnu.dbfms.database.SQLQueryHelper;
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

	@FXML
	private CheckBox checkCache;
	
	public List<String> tags = new ArrayList<>();

	public Map<String, String> fields = new HashMap<>();

	public Map<String, ComboBox<String>> types = new HashMap<>();

	public String thumbnailHash = "";

	private List<String> typeList = new ArrayList<>();

	public boolean fileExists = false;

	public void init(Collection collection, String filePath) {		
		String filePathNew = filePath.replaceAll("'", "''");
		
		labelFilename.setText(filePathNew);

		List<Map<String, Object>> typesSQL = executeSQL(
				"SELECT typeName FROM M_COLLECTIONTYPES WHERE collection = '" + collection.name + "'", "typeName");
		for (Map<String, Object> type : typesSQL)
			typeList.add((String) type.get("typeName"));

		for (String type : typeList) {
			ComboBox<String> typeField = new ComboBox<>();
			typeField.setEditable(true);

			typeField.getEditor().textProperty().addListener((obs, oldText, newText) -> {
				typeField.setValue(Collection.sanitize(newText));
			});
			typeField.setOnKeyPressed((event) -> {
			    if (event.getCode() == KeyCode.ENTER) { 
			        event.consume();
			    }
			});

			List<Map<String, Object>> typeValues = executeSQL(
					"SELECT typeValue FROM " + collection.typeValuesDB.globalName + " WHERE typeName = '" + type + "'",
					"typeValue");
			for (Map<String, Object> typeValue : typeValues)
				typeField.getItems().add((String) typeValue.get("typeValue"));

			HBox typeBox = new HBox(5, new Label(type + '.'), typeField);
			typeBox.setAlignment(Pos.CENTER_LEFT);

			listTypes.getChildren().add(typeBox);

			types.put(type, typeField);
		}

		List<Map<String, Object>> fileExists = executeSQL("SELECT fileID, fileThumb, rating FROM "
				+ collection.fileDB.globalName + " WHERE filePath = '" + filePathNew + "'", "fileID", "fileThumb",
				"rating");

		String thumbFile = "";

		if (!fileExists.isEmpty()) {
			//File exists, Pull from DB
			this.fileExists = true;
			thumbFile = thumbnailHash = (String) fileExists.get(0).get("fileThumb");

			textRating.setText(((Float)fileExists.get(0).get("rating")).toString());
			
			int fileID = ((Integer) fileExists.get(0).get("fileID"));
			
			List<Map<String, Object>> fileTags = executeSQL("SELECT " + collection.tagDB.globalName + ".tagName FROM "
					+ collection.tagDB.globalName + " NATURAL JOIN " + collection.fileTagsDB.globalName + " WHERE "
					+ collection.fileTagsDB.globalName + ".fileID = " + fileID, "tagName");
			
			for(Map<String, Object> tag : fileTags)
				addTagByName((String)tag.get("tagName"));
			
			List<Map<String, Object>> fileFields = executeSQL("SELECT fieldName, fieldContent FROM "
					+ collection.fieldDB.globalName + " WHERE fileID = " + fileID, "fieldName", "fieldContent");
			
			for(Map<String, Object> field : fileFields)
				addFieldByName((String)field.get("fieldName"), (String)field.get("fieldContent"));
			
			List<Map<String, Object>> fileTypes = executeSQL("SELECT " + collection.typeValuesDB.globalName + ".typeName, "
					+ collection.typeValuesDB.globalName + ".typeValue FROM "
					+ collection.typeValuesDB.globalName + " NATURAL JOIN " + collection.fileTypesDB.globalName + " WHERE "
					+ collection.fileTypesDB.globalName + ".fileID = " + fileID, "typeName", "typeValue");
			
			for(Map<String, Object> type : fileTypes)
				types.get((String)type.get("typeName")).setValue((String)type.get("typeValue"));
		}

		Thumbnail thumbnail = Thumbnail.getThumbnail(filePath, thumbFile);
		
		long time1 = System.nanoTime(), time2;
		Image image = thumbnail.loadImage();
		time2 = System.nanoTime();
		thumbnailView.setImage(image);
		
		if(!thumbFile.isEmpty()) {
			checkCache.setSelected(true);
			checkCache.setDisable(true);
		}
		else {
			checkCache.setSelected(time2 - time1 > 30000000); //Load longer than 30 ms
		}
		
		// TODO Tag Recommendations
	}

	@FXML
	public void addTag(ActionEvent e) {
		e.consume();
		
		addTagByName(Collection.sanitize(textTag.getText()));
	}

	private void addTagByName(String tagName) {
		if (tags.contains(tagName))
			return;

		tags.add(tagName);

		Label newTagLabel = new Label(tagName);

		newTagLabel.setOnMouseClicked((mouseEvent) -> {
			if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
				if (mouseEvent.getClickCount() == 2) {
					listTags.getChildren().remove(newTagLabel);
					tags.remove(tagName);
				}
			}
		});

		listTags.getChildren().add(newTagLabel);
		
		textTag.setText("");
	}
	
	@FXML
	public void addField(ActionEvent e) {
		e.consume();
		
		String fieldName = Collection.sanitize(textFieldName.getText());
		if (fields.get(fieldName) != null)
			return;

		addFieldByName(fieldName, Collection.sanitize(textFieldContent.getText()));
	}
	
	private void addFieldByName(String fieldName, String content) {
		fields.put(fieldName, content);

		Label newFieldLabel = new Label(fieldName + ':' + content);

		newFieldLabel.setOnMouseClicked((mouseEvent) -> {
			if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
				if (mouseEvent.getClickCount() == 2) {
					listFields.getChildren().remove(newFieldLabel);
					fields.remove(fieldName);
				}
			}
		});

		listFields.getChildren().add(newFieldLabel);

		textFieldName.setText("");
		textFieldContent.setText("");
	}

	@FXML
	public void setThumb(ActionEvent e) {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(thumbnailView.getScene().getWindow());

		if(selectedFile == null)
			return;
		
		Pair<String, Thumbnail> thumb = Thumbnail.emplaceThumbnailInCache(Thumbnail.getThumbnail(selectedFile.getAbsolutePath(), "").loadImage());
		thumbnailHash = thumb.getKey();
		thumbnailView.setImage(thumb.getValue().loadImage());
		
		checkCache.setSelected(true);
		checkCache.setDisable(true);
	}

	@FXML
	public void manageTypes(ActionEvent e) {
		// TODO Implement Type Management
		Logger.logWarning("Not Implemented");
	}

	public static Dialog<DialogAddFileResult> getDialog(Collection collection, String filePath) {

		return new Dialog<DialogAddFileResult>() {
			public Dialog<DialogAddFileResult> init() {
				String filePathNew = filePath.replaceAll("'", "''");
				
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
				((Stage) dialogPane.getScene().getWindow()).getIcons().add(DBFMS.icon);

				dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

				controller.init(collection, filePath);

				setResultConverter((dialogButton) -> {
					DialogAddFileResult data = new DialogAddFileResult();

					ButtonData button = dialogButton == null ? null : dialogButton.getButtonData();
					boolean buttonOK = button == ButtonData.OK_DONE;

					if (buttonOK) {
						Map<String, String> typeList = new HashMap<>();
						List<Pair<String, String>> typeList2 = new ArrayList<>();
						
						if(controller.thumbnailHash.isEmpty() && controller.checkCache.isSelected()) {
							Pair<String, Thumbnail> thumb = Thumbnail.emplaceThumbnailInCache(controller.thumbnailView.getImage());
							controller.thumbnailHash = thumb.getKey();
						}
						
						//TODO This throws a loooot of exceptions. Make methods check for existance?
						for (Entry<String, ComboBox<String>> types : controller.types.entrySet()) {
							collection.emplaceTypeValue(types.getKey(), types.getValue().getValue());
							typeList.put(types.getKey(), types.getValue().getValue());
							typeList2.add(new Pair<>(types.getKey(), types.getValue().getValue()));
						}
						
						String typeString = SQLQueryHelper.buildSQLTypeList(typeList2);
						
						collection.emplaceFile(controller.labelFilename.getText(), controller.thumbnailHash, Float.parseFloat(controller.textRating.getText()), typeList);

						int fileID = (Integer)executeSQL("SELECT fileID FROM "
								+ collection.fileDB.globalName + " WHERE filePath = '" + filePathNew + "'", "fileID").get(0).get("fileID");	
						
						if(controller.fileExists)
							executeSQL("UPDATE " + collection.fileDB.globalName + " SET rating = "+ Float.parseFloat(controller.textRating.getText()) + ", fileThumb = '" + controller.thumbnailHash + "' WHERE fileID = " + fileID, true);
			
						
						if(!typeString.isEmpty())
							executeSQL("DELETE FROM " + collection.fileTypesDB.globalName + " WHERE fileID = " + fileID
									+ " AND typeValueID NOT IN (SELECT typeValueID FROM " + collection.typeValuesDB.globalName + " WHERE " + typeString + ")", true);
						
						String tags = "";
						
						for (String tag : controller.tags) {
							collection.emplaceTag(tag, "");
							collection.connectTag(filePath, tag);
							
							tags += '\'' + tag + "', ";
						}
						if(!tags.isEmpty())
							tags = tags.substring(0, tags.length() - 2);
						
						StringBuilder findFields = new StringBuilder();
						
						{
							int i = 0;
							for(Entry<String, String> field : controller.fields.entrySet()) {
							
								if(i != 0) 
									findFields.append(" OR ");
							
								findFields.append("fieldName = '");
								findFields.append(field.getKey());
								findFields.append("' AND fieldContent = '");
								findFields.append(field.getValue());
								findFields.append('\'');
							
								i++;
							}
						}
						
						if(findFields.length() != 0)
							executeSQL("DELETE FROM " + collection.fieldDB.globalName + " WHERE fileID = " + fileID
									+ " AND NOT (" + findFields.toString() + ")", true);
						
						for (Entry<String, String> field : controller.fields.entrySet()) {
							collection.emplaceField(controller.labelFilename.getText(), field.getKey(), field.getValue());
						}		
						
						if(!tags.isEmpty())
							executeSQL("DELETE FROM " + collection.fileTagsDB.globalName + " WHERE fileID = " + fileID
									+ " AND tagID NOT IN (SELECT tagID FROM " + collection.tagDB.globalName + " WHERE tagName IN (" + tags + "))", true);
					}

					return data;
				});

				setTitle("Add File");
				return this;
			}
		}.init();

	}
}

class DialogAddFileResult {
	public boolean success = false;
}