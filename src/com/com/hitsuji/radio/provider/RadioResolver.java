package com.hitsuji.radio.provider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.hitsuji.play.Track;
import com.hitsuji.radio.table.CreatorItem;
import com.hitsuji.radio.table.ImageItem;
import com.util.Log;
import com.util.Util;

public class RadioResolver {
	private static final String TAG = RadioResolver.class.getSimpleName();
	private static final int QUERY_LIMIT = 1000;
	
	public static List<ImageItem> loadImages(Context context, Track track) {
		boolean onlyLoaded = Util.isImageCached(context) && Util.isPoorNetwork(context);
		
		List<ImageItem> list = new ArrayList<ImageItem>();
		String[] projection = new String[]{
				ImageItem.TABLE+"."+ImageItem.COLUMN_ID,
				ImageItem.COLUMN_NO, 
				ImageItem.COLUMN_URL, 
				ImageItem.COLUMN_FNAME, 
				ImageItem.COLUMN_LOADED,
				CreatorItem.COLUMN_NAME};
		String selection = CreatorItem.COLUMN_NAME + " = ?" + 
				(onlyLoaded ? " and "+ImageItem.COLUMN_LOADED + "=?" : "");
		String[] selectionArgs = (onlyLoaded ? 
				new String[] {track.getArtist(), ""+1} : 
				new String[] {track.getArtist()});
		
		Cursor cursor = context.getContentResolver().query(
				RadioProvider.IMGCR_CONTENT_URI, 
				projection, 
				selection, 
				selectionArgs, 
				ImageItem.COLUMN_NO +" ASC LIMIT "+QUERY_LIMIT);
		
		if (cursor==null)
			return list;
		
		try {
			cursor.moveToFirst();
			for (int i=0; i<cursor.getCount(); i++) {
				ImageItem item = new ImageItem();
				int idx = cursor.getColumnIndex(ImageItem.COLUMN_ID);
				if (!cursor.isNull(idx))
					item.id = cursor.getInt(idx);
				else
					continue;
				
				idx = cursor.getColumnIndex(CreatorItem.COLUMN_NAME);
				if (!cursor.isNull(idx))
					item.creator = cursor.getString(idx);
				else
					continue;
				
				idx = cursor.getColumnIndex(ImageItem.COLUMN_NO);
				if (!cursor.isNull(idx))
					item.no = cursor.getInt(idx);
				else
					continue;
	
				idx = cursor.getColumnIndex(ImageItem.COLUMN_URL);
				if (!cursor.isNull(idx))
					item.url = cursor.getString(idx);
				else
					continue;
				
				idx = cursor.getColumnIndex(ImageItem.COLUMN_FNAME);
				if (!cursor.isNull(idx))
					item.fname = cursor.getString(idx);
	
				idx = cursor.getColumnIndex(ImageItem.COLUMN_LOADED);
				if (!cursor.isNull(idx))
					item.loaded = cursor.getInt(idx);
				else
					continue;
				
				list.add(item);
				cursor.moveToNext();
			}
		} catch (IllegalArgumentException e){
			Log.e(TAG, e.getMessage());
		} finally {
			cursor.close();
		}
		return list;
	}

	public static int markLoaded(Context context, ImageItem item) {
		Cursor cursor = null;
		int creatorId;
		try {
			String[] projection = new String[]{CreatorItem.COLUMN_ID, CreatorItem.COLUMN_NAME};
			String selection = "name = ? ";
			String[] selectionArgs = new String[]{item.creator}; 
			cursor = context.getContentResolver().query(
					RadioProvider.CREATOR_CONTENT_URI, 
					projection,
					selection,
					selectionArgs,
					null);
			if (cursor==null || cursor.getCount()<1)
				return -1;
			cursor.moveToFirst();
			int idx = cursor.getColumnIndex(CreatorItem.COLUMN_ID);
			creatorId = cursor.getInt(idx);
			Log.d(TAG, "markloaded creatorid:"+creatorId);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
			return -1;
		} finally {
			if (cursor!=null)
				cursor.close();
		}

		ContentValues values = new ContentValues();
		values.put(ImageItem.COLUMN_LOADED, 1);
		try {
			String where = ImageItem.COLUMN_CREATOR_ID + "=? and "+ImageItem.COLUMN_NO+"=?";
			String[] whereArgs = new String[] {""+creatorId, ""+item.no};
			context.getContentResolver().update(RadioProvider.IMAGE_CONTENT_URI, values, where, whereArgs);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
			return -1;
		}
		return 0;
	}
	public synchronized static void addImages(Context context, List<ImageItem> list){
		if (list.size()==0)
			return;
		Cursor cursor = null;
		int creatorId;
		try {
			String[] projection = new String[]{CreatorItem.COLUMN_ID, CreatorItem.COLUMN_NAME};
			String selection = "name = ? ";
			String[] selectionArgs = new String[]{list.get(0).creator}; 
			cursor = context.getContentResolver().query(
					RadioProvider.CREATOR_CONTENT_URI, 
					projection,
					selection,
					selectionArgs,
					null);
			if (cursor==null || cursor.getCount()<1)
				return;
			cursor.moveToFirst();
			int idx = cursor.getColumnIndex(CreatorItem.COLUMN_ID);
			creatorId = cursor.getInt(idx);
			Log.d(TAG, "addImages creatorid:"+creatorId + " creator:"+list.get(0).creator);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
			return;
		} finally {
			if (cursor!=null)
				cursor.close();
		}
		
		Iterator<ImageItem> it = list.iterator();
		while(it.hasNext()) { 
			ImageItem item = it.next();
			ContentValues values = new ContentValues();
			values.put(ImageItem.COLUMN_CREATOR_ID, creatorId);
			values.put(ImageItem.COLUMN_NO, item.no);
			values.put(ImageItem.COLUMN_URL, item.url);
			values.put(ImageItem.COLUMN_FNAME, item.fname);
			values.put(ImageItem.COLUMN_LOADED, item.loaded);
			try {
				context.getContentResolver().insert(RadioProvider.IMAGE_CONTENT_URI, values);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, e.getMessage());
				it.remove();
			}
		}
	}
	
	public synchronized static void addCreator(Context context, String creator){
		Cursor cursor = null;
		boolean found = false;
		try {
			String[] projection = new String[]{CreatorItem.COLUMN_ID, CreatorItem.COLUMN_NAME};
			String selection = "name = ? ";
			String[] selectionArgs = new String[]{creator}; 
			cursor = context.getContentResolver().query(
					RadioProvider.CREATOR_CONTENT_URI, 
					projection,
					selection,
					selectionArgs,
					null);
			Log.d(TAG, "addCreator creator:"+creator + " count:"+cursor.getCount());
			found = cursor.getCount()>0 ? true : false;
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
			return;
		} finally {
			if (cursor!=null)
				cursor.close();
		}
		if (found)
			return;
		
		ContentValues values = new ContentValues();
		values.put(CreatorItem.COLUMN_NAME, creator);
		try {
			context.getContentResolver().insert(RadioProvider.CREATOR_CONTENT_URI, values);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
		}
	}
}
