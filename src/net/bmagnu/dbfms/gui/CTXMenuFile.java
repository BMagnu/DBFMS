package net.bmagnu.dbfms.gui;

import static net.bmagnu.dbfms.database.LocalDatabase.executeSQL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;

import net.bmagnu.dbfms.database.DatabaseFileEntry;
import net.bmagnu.dbfms.database.LocalDatabase;
import net.bmagnu.dbfms.util.Logger;

public class CTXMenuFile {

	private GUIMainTab collectionTab;
	private DatabaseFileEntry file;
	
	public void init(GUIMainTab collection, DatabaseFileEntry filename) {
		this.collectionTab = collection;
		this.file = filename;
	}
	
	@FXML
	public void onEditFile() {
		Dialog<DialogAddFileResult> dialog = DialogAddFile.getDialog(collectionTab.collection, file.filename);

		dialog.showAndWait();
	}
	
	@FXML
	public void onDeleteFile() {
		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Delete File");
		confirm.setHeaderText("Delete file from database");
		confirm.setContentText("Are you sure you want to delete this file?");
		
		confirm.showAndWait().ifPresent((result) -> {
			if(result == ButtonType.OK) {
				if(!file.thumbHash.isBlank()) {
					try {
						Files.delete(Paths.get(LocalDatabase.thumbDBDir + file.thumbHash));
					} catch (IOException e) {
						Logger.logError(e);
					}
				}
				
				int fileID = (Integer)executeSQL("SELECT fileID FROM "
						+ collectionTab.collection.fileDB.globalName + " WHERE filePath = '" + file.filename + "'", "fileID").get(0).get("fileID");	
				
				executeSQL("DELETE FROM " + collectionTab.collection.fieldDB.globalName + " WHERE fileID = " + fileID, true);
				executeSQL("DELETE FROM " + collectionTab.collection.fileTagsDB.globalName + " WHERE fileID = " + fileID, true);
				executeSQL("DELETE FROM " + collectionTab.collection.fileTypesDB.globalName + " WHERE fileID = " + fileID, true);
				executeSQL("DELETE FROM " + collectionTab.collection.fileDB.globalName + " WHERE filePath = '" + file.filename + "'", true);
				
				collectionTab.doQuery();
			}
		});
	}
	
	@FXML
	public void onRunShell() {
		//TODO Implement
	}
}
