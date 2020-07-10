package net.bmagnu.dbfms.database;

import java.util.List;

import javafx.util.Pair;

public class SQLQueryHelper {

	private static String buildSQLTagList(String... items) {
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < items.length; i++) {
			if(i != 0)
				builder.append(", ");
				
			builder.append('\'');
			builder.append(items[i]);
			builder.append('\'');
		}
		
		return builder.toString();
	}
	
	private static String buildSQLTypeList(List<Pair<String, String>> types) {
		StringBuilder findTypeIDs = new StringBuilder();
		
		for(int i = 0; i < types.size(); i++) {
			
			if(i != 0) 
				findTypeIDs.append(" OR ");
			
			findTypeIDs.append("typeName = '");
			findTypeIDs.append(types.get(i).getKey());
			findTypeIDs.append("' AND typeValue = '");
			findTypeIDs.append(types.get(i).getValue());
			findTypeIDs.append('\'');
		}
		
		return findTypeIDs.toString();
	}
	
	public static String queryAndTags(Collection collection, String... tags) {
		String tagCollection = buildSQLTagList(tags);
		
		//TODO Optimize Having?
		
		String query = "SELECT fileID FROM (SELECT fileID, COUNT(fileID) cnt FROM ("
		+ "SELECT DISTINCT fileID, tagID FROM " + collection.fileTagsDB.globalName + " WHERE tagID IN ("
		+ "SELECT tagID FROM " + collection.tagDB.globalName + " WHERE tagName IN (" + tagCollection + ")"
		+ ")) T_TAGS GROUP BY fileID) T_TAGCNT WHERE "
		+ "T_TAGCNT.cnt = " + tags.length;

		return query;
	}

	public static String queryOrTags(Collection collection, String... tags) {
		String tagCollection = buildSQLTagList(tags);
		
		String query = "SELECT DISTINCT fileID FROM " + collection.fileTagsDB.globalName + " WHERE tagID IN ("
		+ "SELECT tagID FROM " + collection.tagDB.globalName + " WHERE tagName IN (" + tagCollection + "))";
		
		return query;
	}

	public static String queryNotTags(Collection collection, String... tags) {
		String tagCollection = buildSQLTagList(tags);
		
		String query = "SELECT fileID FROM " + collection.fileDB.globalName + " WHERE fileID NOT IN ("
		+ "SELECT DISTINCT fileID FROM " + collection.fileTagsDB.globalName + " WHERE tagID IN ("
		+ "SELECT tagID FROM " + collection.tagDB.globalName + " WHERE tagName IN (" + tagCollection + ")))";
		
		return query;
	}
	
	public static String queryTypes(Collection collection, List<Pair<String, String>> types) {
		String findTypeIDs = buildSQLTypeList(types);
		
		String query = "SELECT fileID FROM (SELECT fileID, COUNT(typeValueID) cnt FROM ("
		+ "SELECT DISTINCT fileID, typeValueID FROM " + collection.fileTypesDB.globalName + " WHERE typeValueID IN ("
		+ "SELECT typeValueID FROM " + collection.typeValuesDB.globalName + " WHERE " + findTypeIDs 
		+ ")) T_FILETYPES GROUP BY fileID) T_TYPECNT WHERE "
		+ "T_TYPECNT.cnt = " + types.size();
		
		return query;
	}
	
	public static String queryNotTypes(Collection collection, List<Pair<String, String>> types) {
		String findTypeIDs = buildSQLTypeList(types);
		
		String query = "SELECT fileID FROM " + collection.fileDB.globalName + " WHERE fileID NOT IN ("
		+ "SELECT DISTINCT fileID FROM " + collection.fileTypesDB.globalName + " WHERE typeValueID IN ("
		+ "SELECT typeValueID FROM " + collection.typeValuesDB.globalName + " WHERE " + findTypeIDs + "))";
		
		return query;
	}
	
	public static String queryFields(Collection collection, List<Pair<String, String>> fields) {
		StringBuilder findFields = new StringBuilder();
		
		for(int i = 0; i < fields.size(); i++) {
			
			if(i != 0) 
				findFields.append(" OR ");
			
			findFields.append("fieldName = '");
			findFields.append(fields.get(i).getKey());
			findFields.append("' AND fieldContent = '");
			findFields.append(fields.get(i).getValue());
			findFields.append('\'');
		}
		
		String query = "SELECT fileID FROM (SELECT fileID, COUNT(fileID) cnt FROM ("
		+ "SELECT fileID FROM " + collection.fieldDB.globalName + " WHERE " + findFields.toString() 
		+ ") T_FILEFIELDS GROUP BY fileID) T_FIELDCNT WHERE "
		+ "T_FIELDCNT.cnt = " + fields.size();
		
		return query;
	}
	
	public static String queryLikeFields(Collection collection, List<Pair<String, String>> fields) {
		StringBuilder findFields = new StringBuilder();
		
		for(int i = 0; i < fields.size(); i++) {
			
			if(i != 0) 
				findFields.append(" OR ");
			
			findFields.append("fieldName = '");
			findFields.append(fields.get(i).getKey());
			findFields.append("' AND fieldContent LIKE '%");
			findFields.append(fields.get(i).getValue());
			findFields.append("%'");
		}
		
		String query = "SELECT fileID FROM (SELECT fileID, COUNT(fileID) cnt FROM ("
		+ "SELECT fileID FROM " + collection.fieldDB.globalName + " WHERE " + findFields.toString() 
		+ ") T_FILEFIELDSLIKE GROUP BY fileID) T_FIELDCNTLIKE WHERE "
		+ "T_FIELDCNTLIKE.cnt = " + fields.size();
		
		return query;
	}
	
	public static String queryRating(Collection collection, float rating) {
		String query = "SELECT fileID FROM " + collection.fileDB.globalName + " WHERE rating > " + rating;
		
		return query;
	}

}
