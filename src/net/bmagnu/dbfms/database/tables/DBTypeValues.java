package net.bmagnu.dbfms.database.tables;

import net.bmagnu.dbfms.database.DB;

public class DBTypeValues extends DB {

	public DBTypeValues(String collection) {
		super(collection, "typeValues");
	}
	
	@Override
	protected String getFieldsAndTypes() {
		return "typeValueID INT NOT NULL GENERATED ALWAYS AS IDENTITY, typeName VARCHAR(255) NOT NULL, typeValue VARCHAR(255), PRIMARY KEY (typeValueID)";
	}

}
