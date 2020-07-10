package net.bmagnu.dbfms.database;

import java.util.Locale;

import static net.bmagnu.dbfms.database.LocalDatabase.executeSQL;

public abstract class DB {

	public final String globalName;
	
	public DB(String collection, String name) {
		this.globalName = ("c_" + collection + "_" + name).toUpperCase(Locale.ENGLISH);
	}
	
	public void create() {
		executeSQL("CREATE TABLE " + globalName + " (" + getFieldsAndTypes() + ")", true);
	}
	
	protected abstract String getFieldsAndTypes();
}
