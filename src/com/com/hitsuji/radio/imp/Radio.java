package com.hitsuji.radio.imp;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import com.hitsuji.play.Track;
import com.hitsuji.play.TrackList;
import com.hitsuji.radio.Auth;
import com.net.DownloadManager;
import com.util.Log;
import com.util.Util;

public abstract class Radio {
	private static final String TAG = Radio.class.getSimpleName();
	public static final String LOCAL_DIR = "radio";
	public static final String RESPONSE = "response_tune";
	public static final String TYPE = "type";
	public static final String SIMILAR_ARTIST_KEY = "similar";
	
	private static Boolean EXIT = false;
	
	public static enum KIND{
		RECOMMEND(1, "Recommendation"),
		MIX(2, "Mix"),
		LIBRARY(3, "Library"),
		NEIGHTBOUR(4, "Neightbours"),
		SIMILAR(5, "Similar"),
		FRIEND(6, "Friends"),
		LOCAL(7, "Local");
		
		public int Type;
		public String Name;
		KIND(int type, String name){
			Type = type;
			Name = name;
		}
		
		
	};
	
	protected KIND mKind;
	protected String mUser;
	protected String mName;
	protected String mStation;
	protected String mLocalFile;
	protected TrackList mTrackList;
	
	public Radio(String user){
		mUser = user;
		mTrackList = new TrackList();
	}
	
	public static void setExit(){
		synchronized (EXIT) {
			EXIT = true;
		}
	}
	public static void unsetExit(){
		synchronized (EXIT) {
			EXIT = false;
		}
	}
	public static boolean exit(){
		synchronized (EXIT) {
			return EXIT;
		}
	}
	
	public static KIND getKind(int type) {
		KIND ret;
		switch (type) {
		default:
			ret = null;
			break;
		case 1:
			ret = KIND.RECOMMEND;
			break;
		case 2:
			ret = KIND.MIX;
			break;
		case 3:
			ret = KIND.LIBRARY;
			break;
		case 4:
			ret = KIND.NEIGHTBOUR;
			break;
		case 5:
			ret = KIND.SIMILAR;
			break;		
		case 6:
			ret = KIND.FRIEND;
			break;			
		case 7:
			ret = KIND.LOCAL;
			break;			
		}
		return ret;
	}
	
	public String getName() {
		return mName;
	}
	public int getType(){
		return mKind.Type;
	}
	
	public Track getTopTrack(){
		return mTrackList.getTop();
	}
	public synchronized Track pollTrack(){
		return mTrackList.poll();
	}
	public synchronized int trackSize(){
		return mTrackList.size();
	}
	public synchronized boolean poverty(){
		return mTrackList.poverty();
	}
	
	public String getStation(){
		return String.format(mStation, mUser);
	}
	
	public static String getRadioUrl(String appPath) {
		String url = Auth.LASTFM_API_URL;
		return url;
	}
	public static String getEchonestUrl(String appPath) {
		String url = Auth.ECHONEST_API_URL;
		return url;
	}
	public int tune(String appPath, String sigPath) throws IOException{
		String url = getRadioUrl(appPath);
		if (url==null) return -1;
		
		File dir = new File(appPath+File.separator+Radio.LOCAL_DIR);
		if (!dir.exists())dir.mkdirs();
		String radioPath = appPath+File.separator+Radio.LOCAL_DIR+File.separator+RESPONSE;
		DownloadManager dm = new DownloadManager();
		StringBuffer post = new StringBuffer(""); 
		post.append("method=radio.tune&").
			 append("station=").append(URLEncoder.encode(getStation())).append("&").
		     append("api_key=").append(Auth.LASTFM_API_KEY).append("&").
		     append("api_sig=").append(Auth.getApiSignatureRadiotune(sigPath, getStation())).append("&").
		     append("sk=").append( Auth.getSessionkey(sigPath));
        Log.d(TAG, "radio tune post:"+post);
		int ret = dm.download(url+"?format=json", post.toString(), radioPath);
		if (ret < 0) return -1;
		return checkResponse(appPath);
	}
	
	/**
	 * <lfm status="ok">
	<station>
		<type>artist</type>
		<name>Cher Similar Artists</name>
		<url>http://www.last.fm/listen/artist/Cher/similarartists</url>
		<supportsdiscovery>1</supportsdiscovery>
	</station>
	</lfm>
	 */
	public int checkResponse(String appPath) {
		String path = appPath+File.separator+Radio.LOCAL_DIR+File.separator+RESPONSE;
		String content = Util.readSmallFile(path);
		if (content==null) return -1;
		
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null) return -1;
		try {
			jobj.getJSONObject("station");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	public synchronized int updateTrackList(String appPath, TrackList old){
		String path = appPath+File.separator+Radio.LOCAL_DIR+File.separator+this.mLocalFile;
		String content = Util.readSmallFile(path);
		if (content==null) return -1;
		TrackList ret = TrackList.fill(content, mTrackList);
		if (ret== null) return -1;
		return 0;
	}


	public int fetchList(String appPath, String sigPath) throws IOException {
		// TODO Auto-generated method stub
		String url = getRadioUrl(appPath);
		if (url==null) return -1;
		
		File dir = new File(appPath+File.separator+Radio.LOCAL_DIR);
		if (!dir.exists())dir.mkdirs();
		String radioPath = appPath+File.separator+Radio.LOCAL_DIR+File.separator+this.mLocalFile;
		DownloadManager dm = new DownloadManager();
		StringBuffer post = new StringBuffer(""); 
		post.append("method=radio.getPlaylist&").
		     append("api_key=").append(Auth.LASTFM_API_KEY).append("&").
		     append("api_sig=").append(Auth.getApiSignaturePlaylist(sigPath)).append("&").
		     append("sk=").append( Auth.getSessionkey(sigPath)).append("&").
		     append("raw=true");
        Log.d(TAG, "playlist post:"+post);
		int ret = dm.download(url+"?format=json", post.toString(), radioPath);
		if (ret < 0) return -1;
		return 0;
	}

	public synchronized static void clearAll(String appPath){
		String dirStr = appPath+File.separator+LOCAL_DIR;
		File radio = new File(dirStr);
		if (!radio.exists()) return;
		
		for (File f : radio.listFiles()) {
			f.delete();
		}
	}
}
