package com.hitsuji.play;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.util.Log;
import com.util.Util;

public class TrackList {
	private static final String TAG = TrackList.class.getSimpleName();
	private Queue<Track> mTracks;
	private String mCreator, mTitle;
	
	public static int TRACK_SIZE_UNDERLIMIT = 2;
	
	//private int mExpiry = -1;
	
	//parse
	public static TrackList fill(String content, TrackList list) {
		// TODO Auto-generated method stub
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null || list==null) return null;
		
		try {
			list.mTitle = jobj.getString("title");
			list.mCreator = jobj.getString("creator");
			String date = jobj.getString("date");
			long time = Util.convertLocaltime(date);
			JSONObject link = jobj.getJSONObject("link");
			int expiry = link.getInt("#text");
			
			Log.d(TAG, "time:"+time + " expiry:"+expiry);
			
			
			JSONObject tlist = jobj.getJSONObject("trackList");
			JSONArray tracks = tlist.getJSONArray("track");
			for (int i=0; i<tracks.length(); i++){
				JSONObject track = tracks.getJSONObject(i);
				Track trackobj = Track.create(track, list, time+expiry*1000);
				if (trackobj!=null)list.mTracks.add(trackobj);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return list;
	}
	public static TrackList fill(List<Track>items, TrackList list) {
		// TODO Auto-generated method stub
		for(Track t : items) {
			list.mTracks.add(t);
		}
		return list;
	}
	public TrackList(){
		mTracks= new LinkedBlockingQueue<Track>();
	}
	
	public Track getTop(){
		return mTracks.peek();
	}
	public Track poll(){
		Track t =  mTracks.poll();
		return t;
	}
	
	public int size(){
		return mTracks.size();
	}
	public boolean poverty(){
		return size() <= TRACK_SIZE_UNDERLIMIT;
	}
	
	public void clear(){
		mTracks.clear();
	}
}
