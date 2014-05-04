package com.hitsuji.radio.imp;

import java.io.IOException;
import java.util.List;

import com.hitsuji.play.Track;
import com.hitsuji.play.TrackList;

public class Local extends Radio {

	public Local(String user) {
		super(user);
		// TODO Auto-generated constructor stub
		mName = "Local Music Radio";
		mLocalFile = "local";
		mKind = Radio.KIND.LOCAL;
	}
	@Override
	public String getStation(){
		return null;
	}
	@Override
	public int tune(String appPath, String sigPath) throws IOException {
		return -1;
	}
	@Override
	public int checkResponse(String appPath) {
		return -1;
	}
	
	public synchronized int updateTrackList(List<Track> items){
		mTrackList.clear();
		TrackList.fill(items, mTrackList);
		return 0;
	}
	@Override
	public synchronized int updateTrackList(String appPath, TrackList old){
		return -1;
	}
	
	@Override
	public int fetchList(String appPath, String sigPath) throws IOException {
		return -1;
	}
	
	
	
}
