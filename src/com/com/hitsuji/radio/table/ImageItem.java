package com.hitsuji.radio.table;

import java.io.Serializable;

import com.util.Log;

import android.os.Parcel;
import android.os.Parcelable;

public class ImageItem implements Parcelable, Serializable {

	private static final long serialVersionUID = -8411054935182340089L;
	
	public static final String TAG = ImageItem.class.getSimpleName();
	public static final String TABLE = "images";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_CREATOR_ID= "creator_id";
	public static final String COLUMN_NO = "no";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_FNAME = "fname";
	public static final String COLUMN_LOADED = "loaded";
	
	public static final String CREATE = 
			"create table " +
			TABLE + "(" + 
			COLUMN_ID + " integer primary key autoincrement, " + 
			COLUMN_CREATOR_ID + " integer not null, " + 
			COLUMN_NO + " integer not null, " + 
			COLUMN_URL + " text not null, " +
			COLUMN_FNAME + " text, " + 
			COLUMN_LOADED + " integer not null default 0" + 
			");";
	public static final String IDX_CREATE = 
			"create index if not exists images_idx on " +
			TABLE + "(" + 
			COLUMN_CREATOR_ID + ", " +
			COLUMN_NO + ");";
	
	public int id=0, no=0, loaded = 0;
	public String creator, url, fname;
	
    public static final Parcelable.Creator<ImageItem> CREATOR = new Parcelable.Creator<ImageItem>() {
        public ImageItem createFromParcel(Parcel in) {
            return new ImageItem(in);
        }

        public ImageItem[] newArray(int size) {
            return new ImageItem[size];
        }
    };
    
	public ImageItem(Parcel in) {
		readFromParcel(in);
	}
	public ImageItem() {
	}
	
	public String getFileName(){
		Log.d(TAG, "no:"+no);
		return new StringBuilder().append(no).append("_").append(fname).toString();
	}
	
	private void readFromParcel(Parcel in) {
		id = in.readInt();
		no = in.readInt();
		loaded = in.readInt();
		creator = in.readString();
		url = in.readString();
		fname = in.readString();
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeInt(no);
		dest.writeInt(loaded);
		dest.writeString(creator);
		dest.writeString(url);
		dest.writeString(fname);
	}

}
