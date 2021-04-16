package net.bmagnu.dbfms.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;

import net.bmagnu.dbfms.database.Collection;
import net.bmagnu.dbfms.database.DatabaseFileEntry;
import net.bmagnu.dbfms.database.DatabaseFileEntryComparator;
import net.bmagnu.dbfms.util.Logger;

public class GUIMainTab {

	@FXML 
	private TextField searchQueryField;
	
	@FXML
	private TilePane filePane;
	
	@FXML
	private ScrollPane fileScrollPane;
	
	@FXML
	private ComboBox<DatabaseFileEntryComparator.SortMode> sortModeBox;
	
	@FXML
	private Label labelPerformance;
	
	public Collection collection;
	
	private LinkedList<DatabaseFileEntry> files;
	private DatabaseFileEntryComparator sorter = new DatabaseFileEntryComparator();
	
	public void init(Collection collection) {
		this.collection = collection;
		
		filePane.setOnScroll((event) -> {
	            double deltaY = event.getDeltaY() * 2;
	            double width = fileScrollPane.getContent().getBoundsInLocal().getWidth();
	            double vvalue = fileScrollPane.getVvalue();
	            fileScrollPane.setVvalue(vvalue + -deltaY / width);
	    });
		
		sortModeBox.getItems().addAll(DatabaseFileEntryComparator.SortMode.values());
		sortModeBox.setValue(DatabaseFileEntryComparator.SortMode.SORT_ARBITRARY);
		sortModeBox.valueProperty().addListener((obs, oldV, newV) -> {
			sorter.mode = newV;
			displayFiles();
		});
		
		searchFiles("");
		displayFiles();
	}
	
	@FXML
	public void search_onSearch(ActionEvent event) {
		long time1 = System.nanoTime(), time2, time3;
		searchFiles(searchQueryField.getText());
		
		time2 = System.nanoTime();
       
		displayFiles();
		
        time3 = System.nanoTime();
        
        String query = "Query Time: " + ((time2 - time1) / 1000000) + "ms, Display Time: " + ((time3 - time2) / 1000000) + "ms";
        labelPerformance.setText(query);
        Logger.logInfo(query);
    }
	
	public void searchFiles(String queryString) {
        files = new LinkedList<>(collection.queryFiles(queryString).values());
	}
	
	public void displayFiles() {
		files.sort(sorter);
		
        filePane.getChildren().clear();
        
        for(DatabaseFileEntry file : files) {
        	StackPane filePaneLocal = new StackPane();
        	ImageView fileThumb = new ImageView();
   
        	fileThumb.setImage(file.thumbnail.getImage());
        	fileThumb.setPreserveRatio(true);
        	fileThumb.setFitWidth(300);
        	fileThumb.setFitHeight(300);
        	
        	filePaneLocal.setAlignment(Pos.CENTER);
        	filePaneLocal.getChildren().add(fileThumb);  	
        	
        	filePaneLocal.setMinHeight(300);
        	filePaneLocal.setMinWidth(300);
        	
        	filePaneLocal.setOnMouseClicked((mouseEvent) -> {
        		if(mouseEvent.getButton() == MouseButton.PRIMARY) {
        			try {
        				if (file.filename.startsWith("http://") || file.filename.startsWith("https://"))
        					Desktop.getDesktop().browse(new URI(file.filename));
        				else
        					Desktop.getDesktop().open(new File(file.filename));
					} catch (IOException | URISyntaxException e) {
						Logger.logError(e);
					}
        		}
        		else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
        			Dialog<DialogAddFileResult> dialog = DialogAddFile.getDialog(collection, file.filename);

        			dialog.showAndWait();
        		}
        	});
        	
        	filePaneLocal.setStyle("-fx-border-color: black");
        	
        	filePane.getChildren().add(filePaneLocal);
        }
	}
	
}
