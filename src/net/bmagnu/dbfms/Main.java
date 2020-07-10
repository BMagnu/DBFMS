package net.bmagnu.dbfms;

import net.bmagnu.dbfms.database.Collection;
import net.bmagnu.dbfms.database.LocalDatabase;
import net.bmagnu.dbfms.database.SQLQueryHelper;
import net.bmagnu.dbfms.gui.DBFMS;

import static  javafx.application.Application.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.util.Pair;

public class Main {
	public static void main(String[] args) {
		//launch(DBFMS.class, args);
		
		/*List<Map<String, Object>> tables = LocalDatabase.executeSQL("SELECT * FROM sys.systables", "TABLENAME");
		for(Map<String, Object> table : tables) {
			for(Entry<String, Object> value : table.entrySet())
				System.out.println(value.getKey() + ": " + (String)value.getValue());
		}*/
		
		//LocalDatabase.executeSQL("CREATE TABLE test(Test1 INT, Test2 VARCHAR(255))", true);
		Collection collection = new Collection("test", "a", "b", "c");
		
		/*tables = LocalDatabase.executeSQL("SELECT * FROM sys.systables", "TABLENAME");
		for(Map<String, Object> table : tables) {
			for(Entry<String, Object> value : table.entrySet())
				System.out.println(value.getKey() + ": " + (String)value.getValue());
		}*/
		
		collection.emplaceTag("1", "");
		collection.emplaceTag("2", "");
		collection.emplaceTag("3", "");
		
		collection.emplaceTypeValue("a", "1");
		collection.emplaceTypeValue("a", "2");
		collection.emplaceTypeValue("b", "1");
		collection.emplaceTypeValue("b", "2");
		collection.emplaceTypeValue("c", "1");
		collection.emplaceTypeValue("c", "2");
		
		Map<String, String> types1 = new HashMap<>();
		types1.put("a", "1");
		types1.put("b", "1");
		types1.put("c", "1");
		
		Map<String, String> types2 = new HashMap<>();
		types2.put("a", "1");
		types2.put("b", "2");
		types2.put("c", "2");
		
		collection.emplaceFile("a", "", 0, types1);
		collection.emplaceFile("b", "", 0, types1);
		collection.emplaceFile("c", "", 0, types2);
		
		collection.connectTag("a", "1");
		collection.connectTag("a", "2");
		
		collection.connectTag("b", "2");
		collection.connectTag("b", "3");
		
		collection.connectTag("c", "3");
		collection.connectTag("c", "1");
		
		collection.emplaceField("a", "f", "te");
		collection.emplaceField("b", "f", "ttes");
		collection.emplaceField("c", "f", "banane");
		collection.emplaceField("c", "g", "obst");
		
		List<Pair<String, String>> types = new ArrayList<>();
		types.add(new Pair<>("a","2"));
		
		String query = SQLQueryHelper.queryTypes(collection, types) ;
		
		List<Map<String, Object>>tables = LocalDatabase.executeSQL(query, "fileID");
		
		
		
		for(Map<String, Object> table : tables) {
			for(Entry<String, Object> value : table.entrySet())
				System.out.println(value.getKey() + ": " + (Integer)value.getValue());
		}
		
		System.out.println();
		
		Map<Integer, String> test = collection.queryFiles("+f:nane +g:o");
		for(Integer value : test.keySet())
			System.out.println(value);
		
		System.out.println();
		
		/*test = collection.queryFiles("1 ~2");
		for(Integer value : test.keySet())
			System.out.println(value);*/
	}
}
