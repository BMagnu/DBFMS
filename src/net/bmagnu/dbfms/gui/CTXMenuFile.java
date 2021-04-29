package net.bmagnu.dbfms.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Dialog;
import net.bmagnu.dbfms.database.Collection;

public class CTXMenuFile {

	private Collection collection;
	private String filename;
	
	public void init(Collection collection, String filename) {
		this.collection = collection;
		this.filename = filename;
	}
	
	@FXML
	public void onEditFile() {
		Dialog<DialogAddFileResult> dialog = DialogAddFile.getDialog(collection, filename);

		dialog.showAndWait();
	}
	
	@FXML
	public void onDeleteFile() {
		//TODO Implement
	}
	
	@FXML
	public void onRunShell() {
		//TODO Implement
	}
}
