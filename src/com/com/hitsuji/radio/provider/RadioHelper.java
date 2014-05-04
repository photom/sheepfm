package com.hitsuji.radio.provider;

import com.hitsuji.radio.table.CreatorItem;
import com.hitsuji.radio.table.ImageItem;
import com.util.Log;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RadioHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "radio.db";
	private static final int DATABASE_VERSION = 1;

	public RadioHelper(Context context) {
		//super(context, "/mnt/sdcard/"+DATABASE_NAME, null, DATABASE_VERSION);
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(ImageItem.CREATE);
		database.execSQL(ImageItem.IDX_CREATE);
		database.execSQL(CreatorItem.CREATE);
		database.execSQL(CreatorItem.IDX_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(RadioHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		if (oldVersion == 0 && newVersion == 1) {
			db.execSQL("DROP TABLE IF EXISTS " + ImageItem.TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + CreatorItem.TABLE);
			onCreate(db);
		}
	}
}
