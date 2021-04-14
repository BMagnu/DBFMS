package net.bmagnu.dbfms.database;

import java.util.Comparator;

public class DatabaseFileEntryComparator implements Comparator<DatabaseFileEntry> {

	public SortMode mode = SortMode.SORT_ARBITRARY;
	
	@Override
	public int compare(DatabaseFileEntry o1, DatabaseFileEntry o2) {
		switch(mode) {
		case SORT_CREATED:
			return o2.getCreated().compareTo(o1.getCreated());
		case SORT_CREATED_INV:
			return o1.getCreated().compareTo(o2.getCreated());
		case SORT_MODIFIED:
			return o2.getLastModified().compareTo(o1.getLastModified());
		case SORT_MODIFIED_INV:
			return o1.getLastModified().compareTo(o2.getLastModified());
		case SORT_RATING:
			return Float.compare(o2.rating, o1.rating);
		case SORT_RATING_INV:
			return Float.compare(o1.rating, o2.rating);
		case SORT_ARBITRARY:
		default:
			return 0;
		}
	}
	
	public enum SortMode{
		SORT_ARBITRARY("Arbitrary"),
		SORT_CREATED("Creation Date (Desc.)"),
		SORT_CREATED_INV("Creation Date (Asc.)"),
		SORT_MODIFIED("Last Modified Date (Desc.)"),
		SORT_MODIFIED_INV("Last Modified Date (Asc.)"),
		SORT_RATING("Rating (Desc.)"),
		SORT_RATING_INV("Rating (Asc.)");
		
		private final String displayName;
		
		SortMode(String displayName){
			this.displayName = displayName;
		}
		
		@Override
		public String toString() {
			return displayName;
		}
	}

}
