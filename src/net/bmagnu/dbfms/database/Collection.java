package net.bmagnu.dbfms.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javafx.util.Pair;
import net.bmagnu.dbfms.database.tables.DBField;
import net.bmagnu.dbfms.database.tables.DBFile;
import net.bmagnu.dbfms.database.tables.DBFileTags;
import net.bmagnu.dbfms.database.tables.DBFileTypes;
import net.bmagnu.dbfms.database.tables.DBTag;
import net.bmagnu.dbfms.database.tables.DBTypeValues;
import net.bmagnu.dbfms.util.Logger;
import net.bmagnu.dbfms.util.Thumbnail;

import static net.bmagnu.dbfms.database.LocalDatabase.executeSQL;

public class Collection {

	public final String name;
	public final int typeCount;
	
	public final DBFile fileDB;
	public final DBTag tagDB;
	public final DBFileTags fileTagsDB;
	public final DBField fieldDB;
	public final DBTypeValues typeValuesDB;
	public final DBFileTypes fileTypesDB;
	
	public Collection(String name, int typeCount) {
		this.name = name;
		this.typeCount = typeCount;
		
		fileDB = new DBFile(name);
		tagDB = new DBTag(name);
		fileTagsDB = new DBFileTags(name);
		fieldDB = new DBField(name);
		typeValuesDB = new DBTypeValues(name);
		fileTypesDB = new DBFileTypes(name);
		
		List<Map<String, Object>> tables = executeSQL("SELECT TABLENAME FROM sys.systables", "TABLENAME");
		
		//Check if this Collection needs to be Created
		if(tables.stream().noneMatch((map) -> map.get("TABLENAME").equals(fileDB.globalName))) {
			for(DB db : new DB[]{fileDB, tagDB, fileTagsDB, fieldDB, typeValuesDB, fileTypesDB}) 
				db.create();
			
			executeSQL("CREATE INDEX C_" + name.toUpperCase(Locale.ENGLISH) + "_TAG_IDX ON " + tagDB.globalName + " (tagName)", true);
			executeSQL("CREATE INDEX C_" + name.toUpperCase(Locale.ENGLISH) + "_FIELD_IDX ON " + fieldDB.globalName + " (fieldName, fieldContent)", true);
			executeSQL("CREATE INDEX C_" + name.toUpperCase(Locale.ENGLISH) + "_FILEPATH_IDX ON " + fileDB.globalName + " (filePath)", true);
			executeSQL("CREATE INDEX C_" + name.toUpperCase(Locale.ENGLISH) + "_FILERATING_IDX ON " + fileDB.globalName + " (rating)", true);
			executeSQL("CREATE INDEX C_" + name.toUpperCase(Locale.ENGLISH) + "_TYPEVALUES_IDX ON " + typeValuesDB.globalName + " (typeName, typeValue)", true);
		}
	}
	
	/**
	 * 
	 * @param query The Search Query. Tags: tag (required) ~tag (one of tags) -tag (not tag) Types: type.t (type must be t) -type.t (type can't be t) Fields: field:f (field must be f) +field:f (field must contain f)
	 * @return Map of File ID + Thumbnail path
	 */
	public Map<Integer, Pair<String, Thumbnail>> queryFiles(String query){
		
		List<String> tagsAnd = new ArrayList<>();
		List<String> tagsOr = new ArrayList<>();
		List<String> tagsNot = new ArrayList<>();
		
		List<Pair<String, String>> types = new ArrayList<>();
		List<Pair<String, String>> typesNot = new ArrayList<>();
		
		List<Pair<String, String>> fields = new ArrayList<>();
		List<Pair<String, String>> fieldsLike = new ArrayList<>();
		
		boolean useRating = false;
		float rating = 0.0f;
		
		for(String part : query.split(" ")) {
			if (part.contains("rating>")) { //RATING
				try {
					float localRating = Float.parseFloat(part.split(">")[1]);
					
					useRating = true;
					rating = rating < localRating ? localRating : rating;
				} catch (NumberFormatException e) {
					Logger.logWarning("Rating not a Number, ignoring");
				}
			}
			else if(part.contains(".")) { //TYPE
				switch(part.charAt(0)) {
					case '-':
						typesNot.add(new Pair<String, String>(part.split("\\.")[0].substring(1), part.split("\\.")[1]));
						break;
					default:
						types.add(new Pair<String, String>(part.split("\\.")[0], part.split("\\.")[1]));
						break;
				}
			}
			else if (part.contains(":")) { //FIELD
				switch(part.charAt(0)) {
					case '+':
						fieldsLike.add(new Pair<String, String>(part.split(":")[0].substring(1), part.split(":")[1]));
						break;
					default:
						fields.add(new Pair<String, String>(part.split(":")[0], part.split(":")[1]));
						break;
				}
			}
			else { //TAG
				switch(part.charAt(0)) {
					case '~':
						tagsOr.add(part.substring(1));
						break;
					case '-':
						tagsNot.add(part.substring(1));
						break;
					default:
						tagsAnd.add(part);
						break;
				}
			}
		}
		
		List<String> queries = new ArrayList<>();

		if(!tagsAnd.isEmpty())
			queries.add(SQLQueryHelper.queryAndTags(this, tagsAnd.toArray(String[]::new)));
		if(!tagsOr.isEmpty())
			queries.add(SQLQueryHelper.queryOrTags(this, tagsOr.toArray(String[]::new)));
		if(!tagsNot.isEmpty())
			queries.add(SQLQueryHelper.queryNotTags(this, tagsNot.toArray(String[]::new)));
		if(!types.isEmpty())
			queries.add(SQLQueryHelper.queryTypes(this, types));
		if(!typesNot.isEmpty())
			queries.add(SQLQueryHelper.queryNotTypes(this, typesNot));
		if(!fields.isEmpty())
			queries.add(SQLQueryHelper.queryFields(this, fields));
		if(!fieldsLike.isEmpty())
			queries.add(SQLQueryHelper.queryLikeFields(this, fieldsLike));
		if(useRating)
			queries.add(SQLQueryHelper.queryRating(this, rating));
		
		StringBuilder finalQuery = new StringBuilder();
		
		for(int i = 0; i < queries.size(); i++) {
			
			if(i != 0) {
				finalQuery.append(" \r\nINTERSECT ");
			}
			finalQuery.append('(');
			finalQuery.append(queries.get(i));
			finalQuery.append(')');
			
		}
		
		Map<Integer, Pair<String, Thumbnail>> result = new HashMap<>();
		List<Map<String, Object>>tables = LocalDatabase.executeSQL("SELECT fileID, filePath, fileThumb FROM " + fileDB.globalName + " WHERE fileID IN \r\n("+finalQuery +")", "fileID", "filePath", "fileThumb");
		
		tables.stream().forEach((map) -> {
			String fileName = (String)map.get("filePath");
			result.put((Integer)map.get("fileID"), new Pair<>(fileName, Thumbnail.getThumbnail(fileName, (String)map.get("fileThumb"))));
		});
		
		return result;
	}
	
	public void emplaceFile(String filePath, String thumbnail, float rating, Map<String, String> types) {
		if(types.size() != this.typeCount) {
			Logger.logError("Specified types do not match Collection types");
			return;
		}
			
		List<Integer> typeIDs = new ArrayList<>();
		
		for(Entry<String, String> type : types.entrySet()) {
			executeSQL("SELECT typeValueID FROM " + typeValuesDB.globalName + " WHERE typeName = '" + type.getKey() + "' AND typeValue = '" + type.getValue() + "'", "typeValueID")
			.stream().findFirst().ifPresentOrElse((typeValueID) -> {
				
				typeIDs.add((Integer)typeValueID.get("typeValueID"));
				
			}, () -> { 
				Logger.logError("Specified Type/Value pair not in Database"); 
				return;
			});
		}
		
		String[] queries = new String[types.size() + 1];
		
		queries[0] = "INSERT INTO " + fileDB.globalName + " (filePath, fileThumb, rating) " +
					 "VALUES ('" + filePath + "', '" + thumbnail + "', " + rating + ")";
		
		for(int i = 0; i < typeIDs.size(); i++) {
			queries[i + 1] = "INSERT INTO " + fileTypesDB.globalName + " (typeValueID, fileID) " +
					 		 "SELECT " + typeIDs.get(i) + ", " + fileDB.globalName + ".fileID " +
					 		 "FROM " + fileDB.globalName + 
					 		 " WHERE " + fileDB.globalName + ".filePath = '" + filePath + "'";
		}
		
		for(String query : queries)
			executeSQL(query, true);
	}

	public void emplaceTag(String tag, String descriptionUrl) {
		executeSQL("INSERT INTO " + tagDB.globalName + " (tagName, tagDescURL) " +
				   "VALUES ('" + tag + "', '" + descriptionUrl + "')", true);
	}
	
	public void emplaceField(String filePath, String fieldName, String content) {
		executeSQL("SELECT fileID FROM " + fileDB.globalName + " WHERE filePath = '" + filePath + "'", "fileID")
			.stream().findFirst().ifPresentOrElse((fileID) -> {
				
				executeSQL("INSERT INTO " + fieldDB.globalName + " (fileID, fieldName, fieldContent) " +
						   "VALUES (" + ((Integer) fileID.get("fileID")) + ", '" + fieldName + "', '" + content + "')", true);
				
			}, () -> { Logger.logError("Specified Filepath not in Database"); });
	}
	
	public void emplaceTypeValue(String typeName, String typeValue) {
		executeSQL("INSERT INTO " + typeValuesDB.globalName + " (typeName, typeValue) " +
				   "VALUES ('" + typeName + "', '" + typeValue + "')", true);
	}
	
	public void connectTag(String filePath, String tag) {
		executeSQL("SELECT fileID FROM " + fileDB.globalName + " WHERE filePath = '" + filePath + "'", "fileID")
		.stream().findFirst().ifPresentOrElse((fileID) -> {
			
			executeSQL("SELECT tagID FROM " + tagDB.globalName + " WHERE tagName = '" + tag + "'", "tagID")
			.stream().findFirst().ifPresentOrElse((tagID) -> {
				
				executeSQL("INSERT INTO " + fileTagsDB.globalName + " (fileID, tagID) " +
						   "VALUES (" + ((Integer) fileID.get("fileID")) + ", " + ((Integer) tagID.get("tagID")) + ")", true);
				
			}, () -> { Logger.logError("Specified Tag not in Database"); });
			
		}, () -> { Logger.logError("Specified Filepath not in Database"); });
	}
}
