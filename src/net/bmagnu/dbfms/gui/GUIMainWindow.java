package net.bmagnu.dbfms.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import net.bmagnu.dbfms.Main;
import net.bmagnu.dbfms.database.Collection;
import net.bmagnu.dbfms.database.LocalDatabase;
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
			else if(event.getGestureSource() != collectionTabs && event.getDragboard().hasUrl()) {
				String url = event.getDragboard().getUrl();
				if(url.startsWith("http://") || url.startsWith("https://"))
					event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			event.consume();
        });
	}
	
	public GUIMainTab getCollectionTab() {
		return tabs.get(collectionTabs.getSelectionModel().getSelectedItem().getText());
	}
	
	private Collection getCollection() {
		return getCollectionTab().collection;
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
		FileChooser fileChooser = new FileChooser();
		
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("ZIP file (*.zip)", "*.zip");
        fileChooser.getExtensionFilters().add(extFilter);

		File selectedFile = fileChooser.showSaveDialog(collectionTabs.getScene().getWindow());
		
		if(selectedFile == null)
			return;
		
		Path backupDB = Paths.get(LocalDatabase.programDataDir + "backup");
		
		try {
			if(Files.exists(backupDB))
				Files.walk(backupDB)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} catch (IOException e) {
			Logger.logError(e);
			return;
		}

		LocalDatabase.callSQL("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)", backupDB.toAbsolutePath().toString().replace('\\', '/'));
		
		try {
			Files.write(backupDB.resolve("version.txt"), Main.properties.getProperty("version").getBytes());
		} catch (IOException e) {
			Logger.logError(e);
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(selectedFile);
	        ZipOutputStream zipOut = new ZipOutputStream(fos);
			
	        int depth = backupDB.getNameCount();
	        
			Files.walk(backupDB)
				.forEach((file) -> {
					try {
						if (file.toFile().isDirectory() && file.compareTo(backupDB) != 0) {
							zipOut.putNextEntry(new ZipEntry(file.subpath(depth,file.getNameCount()).toString().replace('\\', '/') + '/'));
							zipOut.closeEntry();
						}
						else if (!file.toFile().isDirectory()) {
							FileInputStream fis = new FileInputStream(file.toFile());
					        ZipEntry zipEntry = new ZipEntry(file.subpath(depth,file.getNameCount()).toString().replace('\\', '/'));
					        zipOut.putNextEntry(zipEntry);
					        byte[] bytes = new byte[1024];
					        int length;
					        while ((length = fis.read(bytes)) >= 0) {
					            zipOut.write(bytes, 0, length);
					        }
					        fis.close();
						}
					} catch (IOException e) {
						Logger.logError(e);
					}
				});
			
			Path thumbCache = Paths.get(LocalDatabase.programDataDir + "thumbCache");
			int depthCache = thumbCache.getNameCount();
			zipOut.putNextEntry(new ZipEntry("thumbs/"));
			Files.walk(thumbCache)
			.forEach((file) -> {
				try {
					if (file.toFile().isDirectory() && file.compareTo(thumbCache) != 0) {
						zipOut.putNextEntry(new ZipEntry("thumbs/" + file.subpath(depthCache,file.getNameCount()).toString().replace('\\', '/') + '/'));
						zipOut.closeEntry();
					}
					else if (!file.toFile().isDirectory()) {
						FileInputStream fis = new FileInputStream(file.toFile());
				        ZipEntry zipEntry = new ZipEntry("thumbs/" + file.subpath(depthCache,file.getNameCount()).toString().replace('\\', '/'));
				        zipOut.putNextEntry(zipEntry);
				        byte[] bytes = new byte[1024];
				        int length;
				        while ((length = fis.read(bytes)) >= 0) {
				            zipOut.write(bytes, 0, length);
				        }
				        fis.close();
					}
				} catch (IOException e) {
					Logger.logError(e);
				}
			});
			
			
			zipOut.close();
			fos.close();
		} catch (IOException e) {
			Logger.logError(e);
		}
		
    }
	
	@FXML
	public void menuDB_onRestore(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("ZIP file (*.zip)", "*.zip");
        fileChooser.getExtensionFilters().add(extFilter);

		File selectedFile = fileChooser.showOpenDialog(collectionTabs.getScene().getWindow());
		
		if(selectedFile == null)
			return;
		
		LocalDatabase.restore(selectedFile);
	}
	
	@FXML
	public void menuCollection_onManageTags(ActionEvent event) {
		//TODO Implement Tag Management
		Logger.logWarning("To Be Implemented");
    }
	
	private void addFile(String filePath) {
		Dialog<DialogAddFileResult> dialog = DialogAddFile.getDialog(getCollection(), filePath);

		dialog.showAndWait();
		
		getCollectionTab().doQuery();
	}
	
	@FXML
	public void menuCollection_onAddFile(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(collectionTabs.getScene().getWindow());
		
		if(selectedFile == null)
			return;
		
		addFile(selectedFile.getAbsolutePath());
    }
	
	@FXML
	public void menuCollection_onAddURL(ActionEvent event) {
		TextInputDialog td = new TextInputDialog("");
		td.getDialogPane().setMinWidth(400);
		td.setTitle("Add URL");
		td.setHeaderText("Enter URL");
		td.showAndWait().ifPresent((url) -> {
			if (!(url.startsWith("http://")  || url.startsWith("https://"))) {
				url = "https://" + url;
			}
			
			try {
				new URL(url).toURI();
			} catch (MalformedURLException | URISyntaxException e) {
				return;
			}
			
			addFile(url);
		});
    }
	
	@FXML
	public void menuCollection_onAddDir(ActionEvent event) {
		DirectoryChooser fileChooser = new DirectoryChooser();
		File selectedFile = fileChooser.showDialog(collectionTabs.getScene().getWindow());
		
		if(selectedFile == null)
			return;
		
		addFile(selectedFile.getAbsolutePath());
    }
	
	@FXML
	public void menuCollection_delete(ActionEvent event) {
		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Delete Collection");
		confirm.setHeaderText("Delete collection and all files in this collections from database");
		confirm.setContentText("Are you sure you want to delete this collections and remove all of it's files from the database?");
		
		confirm.showAndWait().ifPresent((result) -> {
			if(result == ButtonType.OK) {
				executeSQL("DROP TABLE " + getCollection().fileDB.globalName, true);
				executeSQL("DROP TABLE " + getCollection().tagDB.globalName, true);
				executeSQL("DROP TABLE " + getCollection().fileTagsDB.globalName, true);
				executeSQL("DROP TABLE " + getCollection().fieldDB.globalName, true);
				executeSQL("DROP TABLE " + getCollection().typeValuesDB.globalName, true);
				executeSQL("DROP TABLE " + getCollection().fileTypesDB.globalName, true);
				
				executeSQL("DELETE FROM M_COLLECTIONS WHERE collection = '" + getCollection().name + "'", true);
				executeSQL("DELETE FROM M_COLLECTIONTYPES WHERE collection = '" + getCollection().name + "'", true);
				
				collectionTabs.getTabs().remove(collectionTabs.getSelectionModel().getSelectedItem());
			}
		});
    }
	
	@FXML
	public void tab_droppedFile(DragEvent event) {
		List<String> files = new ArrayList<String>();
		if (event.getDragboard().hasFiles())
			event.getDragboard().getFiles().stream().map(File::getAbsolutePath).forEachOrdered(files::add);
		else if (event.getDragboard().hasUrl()) {
			files.add(event.getDragboard().getUrl());
		}
		
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
