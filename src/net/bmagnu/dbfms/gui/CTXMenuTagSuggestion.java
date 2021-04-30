package net.bmagnu.dbfms.gui;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.Pair;
import net.bmagnu.dbfms.database.Collection;

public class CTXMenuTagSuggestion extends ContextMenu {

	public ObservableList<Pair<String, Integer>> entries = FXCollections.observableArrayList();

	public static void register(TextField textField, Collection collection) {
		new CTXMenuTagSuggestion(textField, collection);
	}
	
	private CTXMenuTagSuggestion(TextField textField, Collection collection) {
		setAutoFix(false);
	
		entries.addListener((ListChangeListener.Change<? extends Pair<String,Integer>> change) -> {
			getItems().clear();
			
			for(Pair<String, Integer> entry : entries) {
				StackPane pane = new StackPane();
				pane.setPrefSize(100, 20);
				pane.prefWidthProperty().bind(textField.widthProperty().subtract(10));
				Label tag = new Label(entry.getKey());
				Label count = new Label(entry.getValue().toString());
				StackPane.setAlignment(tag, Pos.CENTER_LEFT);
				StackPane.setAlignment(count, Pos.CENTER_RIGHT);
				pane.getChildren().addAll(tag, count);
				pane.setOnMouseEntered(e -> {
					tag.setStyle("-fx-underline: true;");
				});
				pane.setOnMouseExited(e -> {
					tag.setStyle("-fx-underline: false;");
				});
				
				CustomMenuItem item = new CustomMenuItem(pane);	
				item.setOnAction(e -> {
					String text = textField.getText();
					int caret = textField.getCaretPosition();
					int start = text.lastIndexOf(" ", caret - 1) + 1;
					int stop = text.indexOf(" ", caret);
					
					if (text.charAt(start) == '~' || text.charAt(start) == '-')
						start++;
					
					stop = stop == -1 ? text.length() : stop;
					String textNew = text.substring(0, start) + entry.getKey() + text.substring(stop) + ' ';
					textField.setText(textNew);
					textField.positionCaret(start + entry.getKey().length() + 1);
				});
				
				getItems().add(item);
			}
		});
		
		textField.textProperty().addListener((obs, oldV, newV) -> {
			int caret = textField.getCaretPosition() + (oldV.length() > newV.length() ? -1 : 1);
			int start = newV.lastIndexOf(" ", caret - 1) + 1;
			int stop = newV.indexOf(" ", caret);
			stop = stop == -1 ? newV.length() : stop;
			String currentTag = newV.substring(start, stop);
			
			if (currentTag.startsWith("~") || currentTag.startsWith("-"))
				currentTag = currentTag.substring(1);
			
			if (currentTag.length() < 3) {
				if (isShowing())
					hide();
				return;
			}
			List<Pair<String, Integer>> suggestions = collection.recommendTags(currentTag);
			
			if(entries.size() == suggestions.size()) {
				boolean different = false;
				for(int i = 0; i < suggestions.size(); i++) {
					if (!entries.get(i).equals(suggestions.get(i))) {
						different = true;
					}
				}
				
				if(!different)
					return;
			}
			
			entries.setAll(suggestions);	
			
			if(suggestions.isEmpty()) {
				if (isShowing())
					hide();
				return;
			}
			
			Bounds bounds = textField.localToScreen(textField.getBoundsInLocal());
			if (!isShowing())
				show(textField, bounds.getMinX(), bounds.getMaxY());
			getSkin().getNode().requestFocus();
		});
	}
	
}
