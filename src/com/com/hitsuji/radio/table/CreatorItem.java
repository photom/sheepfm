package com.hitsuji.radio.table;

public class CreatorItem {
	public static final String TABLE = "creators";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_NAME = "name";
	
	public static final String CREATE = 
			"create table " +
			TABLE + "(" + 
			COLUMN_ID + " integer primary key autoincrement, " + 
			COLUMN_NAME + " text not null" + 
			");";
	public static final String IDX_CREATE = 
			"create index if not exists creators_idx on " +
			TABLE + "(" + 
			COLUMN_NAME + 
			");";
			
	int id =0 ;
	String name;
}
