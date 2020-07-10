package net.bmagnu.dbfms.database.tables;

import net.bmagnu.dbfms.database.DB;

public class DBFileTypes extends DB {

	public DBFileTypes(String collection) {
		super(collection, "fileTypes");
	}
	
	@Override
	protected String getFieldsAndTypes() {
		return "typeValueID INT NOT NULL, fileID INT NOT NULL, PRIMARY KEY (typeValueID, fileID)";
	}

}
