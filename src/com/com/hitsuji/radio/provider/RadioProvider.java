package com.hitsuji.radio.provider;

import java.util.Arrays;
import java.util.HashSet;

import com.hitsuji.radio.table.CreatorItem;
import com.hitsuji.radio.table.ImageItem;
import com.util.Log;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class RadioProvider extends ContentProvider {
	private static final String TAG = RadioProvider.class.getSimpleName();
	
	// database
	private RadioHelper database;
	private static final int IMAGES = 10;
	private static final int IMAGE_ID = 20;
	private static final int CREATORS = 30;
	private static final int CREATOR_ID = 40;
	private static final int IMGCRS = 50;
	
	private static final String AUTHORITY = "com.hitsuji.radio.provider";

	private static final String IMAGE_PATH = "images";
	private static final String CREATOR_PATH = "creators";
	private static final String IMGCR_PATH = "imgcrs";
	
	public static final Uri IMAGE_CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + IMAGE_PATH);
	public static final String IMAGE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/images";
	public static final String IMAGE_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/image";
	
	public static final Uri CREATOR_CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + CREATOR_PATH);
	public static final String CREATOR_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/creators";
	public static final String CREATOR_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/creator";
	
	public static final Uri IMGCR_CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + IMGCR_PATH);
	public static final String IMGCR_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/imgcr";
	
	
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, IMAGE_PATH, IMAGES);
		sURIMatcher.addURI(AUTHORITY, IMAGE_PATH + "/#", IMAGE_ID);
		sURIMatcher.addURI(AUTHORITY, CREATOR_PATH, CREATORS);
		sURIMatcher.addURI(AUTHORITY, CREATOR_PATH + "/#", CREATOR_ID);
		sURIMatcher.addURI(AUTHORITY, IMGCR_PATH, IMGCRS);
	}

	@Override
	public boolean onCreate() {
		database = new RadioHelper(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		// Uisng SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case IMAGES:
			queryBuilder.setTables(ImageItem.TABLE);
			break;
		case IMAGE_ID:
			queryBuilder.setTables(ImageItem.TABLE);
			// adding the ID to the original query
			queryBuilder.appendWhere(ImageItem.COLUMN_ID + "="
					+ uri.getLastPathSegment());
			break;
		case CREATORS:
			queryBuilder.setTables(CreatorItem.TABLE);
			break;
		case CREATOR_ID:
			queryBuilder.setTables(CreatorItem.TABLE);
			// adding the ID to the original query
			queryBuilder.appendWhere(CreatorItem.COLUMN_ID + "="
					+ uri.getLastPathSegment());
			break;
		case IMGCRS:
			queryBuilder.setTables(ImageItem.TABLE + " inner join "+ CreatorItem.TABLE +
					" on " + ImageItem.TABLE + "."+ImageItem.COLUMN_CREATOR_ID+"="+
					CreatorItem.TABLE + "."+CreatorItem.COLUMN_ID);
			break;	
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		Cursor cursor;
		SQLiteDatabase db = database.getWritableDatabase();
		cursor = queryBuilder.query(db, projection, selection,
			selectionArgs, null, null, sortOrder);

		// make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		long id = 0;
		switch (uriType) {
		case IMAGES:
			id = sqlDB.insert(ImageItem.TABLE, null, values);
			break;
		case CREATORS:
			id = sqlDB.insert(CreatorItem.TABLE, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		if (uriType == IMAGES)
			return Uri.parse(IMAGE_PATH + "/" + id);
		else if (uriType == CREATORS)
			return Uri.parse(CREATOR_PATH + "/" + id);
		else
			return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case IMAGES:
			rowsDeleted = sqlDB.delete(ImageItem.TABLE, selection,
					selectionArgs);
			break;
		case IMAGE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(ImageItem.TABLE,
						ImageItem.COLUMN_ID + "=" + id, 
						null);
			} else {
				rowsDeleted = sqlDB.delete(ImageItem.TABLE,
						ImageItem.COLUMN_ID + "=" + id 
						+ " and " + selection,
						selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		Log.d(TAG, "update uritype:"+uriType);
		switch (uriType) {
		case IMAGES:
			rowsUpdated = sqlDB.update(ImageItem.TABLE,
					values, 
					selection,
					selectionArgs);
			break;
		case IMAGE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(ImageItem.TABLE,
						values,
						ImageItem.COLUMN_ID + "=" + id, 
						null);
			} else {
				rowsUpdated = sqlDB.update(ImageItem.TABLE,
						values,
						ImageItem.COLUMN_ID + "=" + id 
						+ " and " 
						+ selection,
						selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}
	private static String[] available = {
		ImageItem.COLUMN_ID,
		ImageItem.COLUMN_CREATOR_ID,
		ImageItem.COLUMN_NO, ImageItem.COLUMN_URL,
		ImageItem.COLUMN_FNAME, ImageItem.COLUMN_LOADED,
		CreatorItem.COLUMN_NAME,
		ImageItem.TABLE+"."+ImageItem.COLUMN_ID,
		};
	private void checkColumns(String[] projection) {
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			// check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}

}
