package net.bmagnu.dbfms.database.tables;

import net.bmagnu.dbfms.database.DB;

public class DBField extends DB {

	public DBField(String collection) {
		super(collection, "field");
	}
	
	@Override
	protected String getFieldsAndTypes() {
		return "fileID INT NOT NULL, fieldName VARCHAR(255) NOT NULL, fieldContent VARCHAR(255), PRIMARY KEY (fileID, fieldName), UNIQUE(fieldName, fieldContent)";
	}

}
