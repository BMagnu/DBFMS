package net.bmagnu.dbfms.database.tables;

import net.bmagnu.dbfms.database.DB;

public class DBFileTags extends DB {

	public DBFileTags(String collection) {
		super(collection, "fileTags");
	}
	
	@Override
	protected String getFieldsAndTypes() {
		return "tagID INT NOT NULL, fileID INT NOT NULL, PRIMARY KEY (tagID, fileID)";
	}

}
