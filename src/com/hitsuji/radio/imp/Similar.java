package com.hitsuji.radio.imp;

import java.io.File;
import java.net.URLEncoder;

import android.util.Log;

import com.hitsuji.play.TrackList;
import com.hitsuji.radio.Auth;
import com.hitsuji.radio.imp.Radio;
import com.net.DownloadManager;

public class Similar extends Radio {
	private static final String TAG = Similar.class.getSimpleName();
	public static final int FOR_PLAY = 0;
	public static final int CURRENT_RADIO = 1;
	public static final int CURRENT_TRACK = 2;

	private String mArtist;
	private int mType;
	
	public Similar(String user) {
		super(user);
		// TODO Auto-generated constructor stub
		mArtist = null;
		mName = "Similar Artists Radio";
		mLocalFile = "similar";
		mKind = Radio.KIND.SIMILAR;
		mStation = "lastfm://artist/%s/similarartists";
		mType = FOR_PLAY;
	}
	public Similar(String user, int type) {
		super(user);
		// TODO Auto-generated constructor stub
		mArtist = null;
		mName = "Similar Artists Radio";
		mLocalFile = "similar";
		mKind = Radio.KIND.SIMILAR;
		mStation = "lastfm://artist/%s/similarartists";
		mType = type;
	}
	
	public int getSimilarRadioType(){
		return mType;
	}
	
	public void setArtist(String artist){
		mArtist = artist;
	}
	
	public String getArtist() {
		return mArtist;
	}
	
	@Override
	public String getStation(){
		if(mArtist == null) return null;
		else return String.format(mStation, mArtist);
	}
	@Override
	public int updateTrackList(String appPath, TrackList old){
		return updateTrackList(appPath, old, true);
	}
	public int updateTrackList(String appPath, TrackList old, boolean fill){
		if (fill)this.mTrackList.clear();
		return super.updateTrackList(appPath, old);
	}
}
