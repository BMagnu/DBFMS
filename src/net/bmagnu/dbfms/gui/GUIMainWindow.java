package net.bmagnu.dbfms.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import net.bmagnu.dbfms.database.Collection;
import net.bmagnu.dbfms.util.Logger;

import static net.bmagnu.dbfms.database.LocalDatabase.executeSQL;

public class GUIMainWindow {

	@FXML 
	private Menu collectionMenu;
	
	@FXML
	private TabPane collectionTabs;
	
	private Map<String, GUIMainTab> tabs = new HashMap<>();
	
	public void init() {
		collectionMenu.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			return collectionTabs.getTabs().isEmpty();
		}, collectionTabs.getTabs()));
		
		List<Map<String, Object>> tables = executeSQL("SELECT TABLENAME FROM sys.systables", "TABLENAME");
		if(tables.stream().noneMatch((map) -> map.get("TABLENAME").equals("M_COLLECTIONS"))) {
			executeSQL("CREATE TABLE M_COLLECTIONS (collection VARCHAR(255) NOT NULL, typeCount INT, PRIMARY KEY(collection))", true);
			executeSQL("CREATE TABLE M_COLLECTIONTYPES (collection VARCHAR(255) NOT NULL, typeName VARCHAR(255), PRIMARY KEY(collection, typeName))", true);
		}
		
		List<Map<String, Object>> collections = executeSQL("SELECT collection, typeCount FROM M_COLLECTIONS", "collection", "typeCount");
		for(Map<String, Object> collectionData : collections) {
		
			Collection collection = new Collection((String)collectionData.get("collection"), (Integer)collectionData.get("typeCount"));
			
			addCollectionTab(collection);
		}
		
		collectionTabs.setOnDragOver((event) -> {
			if (event.getGestureSource() != collectionTabs && event.getDragboard().hasFiles()) {
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			event.consume();
        });
	}
	
	private Collection getCollection() {
		return tabs.get(collectionTabs.getSelectionModel().getSelectedItem().getText()).collection;
	}
	
	@FXML
	public void menuFile_onCreateCollection(ActionEvent event) {
		Dialog<DialogAddCollectionResult> dialog = DialogAddCollection.getDialog();

		Optional<DialogAddCollectionResult> result = dialog.showAndWait();
		result.ifPresentOrElse((collection) -> {
			
			executeSQL("INSERT INTO M_COLLECTIONS (collection, typeCount) VALUES ('" + collection.name + "', " + collection.types.size() + ")", true);
			
			for(String type : collection.types)
				executeSQL("INSERT INTO M_COLLECTIONTYPES (collection, typeName) VALUES ('" + collection.name + "', '" + type + "')", true);
			
			Collection newCollection = new Collection(collection.name, collection.types.size());
			
			addCollectionTab(newCollection);
			
		}, () -> {
			Logger.logWarning("Aborted collection creation");
		});
		
    }
	
	@FXML
	public void menuDB_onBackup(ActionEvent event) {
        //TODO
		Logger.logWarning("To Be Implemented");
    }
	
	@FXML
	public void menuCollection_onManageTags(ActionEvent event) {
		//TODO
		Logger.logWarning("To Be Implemented");
    }
	
	private void addFile(String filePath) {
		Dialog<DialogAddFileResult> dialog = DialogAddFile.getDialog(getCollection(), filePath);

		dialog.showAndWait();
	}
	
	@FXML
	public void menuCollection_onAddFile(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(collectionTabs.getScene().getWindow());
		
		addFile(selectedFile.getAbsolutePath());
    }
	
	@FXML
	public void tab_droppedFile(DragEvent event) {
		List<String> files = event.getDragboard().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toList());
		
		event.setDropCompleted(true);

        event.consume();
        
        Platform.runLater(() -> {
        	for(String file : files)
				addFile(file);
        });

    }
	
	private void addCollectionTab(Collection collection) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("main_tab.fxml"));
		GUIMainTab tabController = null;
		try {
			HBox tabBox = loader.load();
			tabController = loader.getController();
			tabController.init(collection);
			
			Tab tab = new Tab();
			tab.setContent(tabBox);
			tab.setText(collection.name);
			
			collectionTabs.getTabs().add(tab);
			
		} catch (IOException e) {
			Logger.logError(e);
			return;
		}
		
		tabs.put(collection.name, tabController);
	}
	
}
