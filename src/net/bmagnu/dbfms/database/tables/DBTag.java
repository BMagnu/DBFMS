package net.bmagnu.dbfms.database.tables;

import net.bmagnu.dbfms.database.DB;

public class DBTag extends DB{

	public DBTag(String collection) {
		super(collection, "tag");
	}

	@Override
	protected String getFieldsAndTypes() {
		return "tagID INT NOT NULL GENERATED ALWAYS AS IDENTITY, tagName VARCHAR(255) NOT NULL, tagDescURL VARCHAR(255), PRIMARY KEY (tagID)";
	}

}
