package net.bmagnu.dbfms.database.tables;

import net.bmagnu.dbfms.database.DB;

public class DBFile extends DB {

	public DBFile(String collection) {
		super(collection, "files");
	}
	
	@Override
	protected String getFieldsAndTypes() {
		return "fileID INT NOT NULL GENERATED ALWAYS AS IDENTITY, filePath VARCHAR(255) NOT NULL, fileThumb VARCHAR(255), rating FLOAT(23), PRIMARY KEY (fileID)";
	}

}
