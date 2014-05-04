package com.hitsuji.radio.manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.hitsuji.radio.Auth;
import com.hitsuji.play.Track;
import com.hitsuji.radio.imp.Radio;
import com.net.DownloadManager;
import com.util.Log;
import com.util.Util;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class ScrobbleManager extends IntentService {
	private static final String TAG = ScrobbleManager.class.getSimpleName();
	public static final String KEY = "scrobble";
	public static final String RESPONSE = "response_scrobble";
	public static final String SCROBBLE_ACTION = "com.hitsuji.manager.ScrobbleManager.scrobble";
	public static final String CLEAR_ACTION = "com.hitsuji.manager.ScrobbleManager.clear";
	public static final String POST_ACTION = "com.hitsuji.manager.ScrobbleManager.post_action";

	private Facebook facebook = new Facebook(Auth.FACEBOOK_API_KEY);
	private SharedPreferences mPrefs;
	private boolean mIsValidFacebook = false;
	private AsyncFacebookRunner mFRunner = null;
	private Object state = new Object();

	public ScrobbleManager() {
		super(ScrobbleManager.class.getSimpleName());
	}

	public ScrobbleManager(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(){
		super.onCreate();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);
		if(access_token != null) {
			facebook.setAccessToken(access_token);
		}
		if(expires != 0) {
			facebook.setAccessExpires(expires);
		}
		if(access_token!=null && facebook.isSessionValid()) {
			mIsValidFacebook = true;
			mFRunner = new AsyncFacebookRunner(facebook);
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		if (intent != null && intent.getAction()!=null &&
				intent.getAction().equals(CLEAR_ACTION)) {
			clear();
		} else if (intent != null && intent.getAction()!=null &&
				intent.getAction().equals(SCROBBLE_ACTION)) {
			scrobble(intent);
		} else if (intent != null && intent.getAction()!=null &&
				intent.getAction().equals(POST_ACTION)) {
			postAction(intent);
		} 
	}

	private void postAction(Intent intent) {
		// TODO Auto-generated method stub
		Track track = intent.getParcelableExtra(KEY);
		if (track == null) return;
		if (this.mIsValidFacebook) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Bundle params = new Bundle();
			String title = URLEncoder.encode(track.getTitle());
			String artist = URLEncoder.encode(track.getArtist());
			params.putString(URLEncoder.encode("song"), 
					URLEncoder.encode("http://www.facebook.com/pages/Yesterday/103948619642527"));

			Date s = new Date();
			s.setTime(track.getStartTime()*1000);
			params.putString(URLEncoder.encode("start_time"), URLEncoder.encode(sdf.format(s)));
			params.putString(URLEncoder.encode("expires_in"), ""+track.getDuration()/1000);

			String furl = "me/music.listens";
			Log.d(TAG, "url:"+furl + " p:"+params.toString());
			mFRunner.post(furl, params, new FListener());
		}
	}

	private void clear(){
		String appPath = this.getExternalFilesDir(null).getAbsolutePath();
		String trackStr = appPath + File.separator+Track.DIR;
		File track = new File(trackStr);
		if (!track.exists()) return;

		for (File f : track.listFiles()) {
			f.delete();
		}
		String response = appPath+File.separator+Radio.LOCAL_DIR+File.separator+RESPONSE;
		File f = new File(response);
		f.delete();
	}

	private void scrobble(Intent intent){
		String sigPath = this.getFilesDir().getAbsolutePath();
		String appPath = this.getExternalFilesDir(null).getAbsolutePath();
		
		if (Auth.getSessionkey(sigPath) == null)
			return;

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean scrobble = sharedPreferences.getBoolean("scrobble", false);
		if (!scrobble) return;



		String url = Radio.getRadioUrl(appPath);
		if (url==null) return;

		Track track = intent.getParcelableExtra(KEY);
		if (track == null) return;

		File radio = new File(appPath+File.separator+Radio.LOCAL_DIR);
		if (!radio.exists()) radio.mkdirs();
		String response = appPath+File.separator+Radio.LOCAL_DIR+File.separator+RESPONSE;

		track.toFile(appPath);
		StringBuffer post = new StringBuffer(); 
		List<String> sigArr = new ArrayList<String>();

		File dir = new File(appPath + File.separator + Track.DIR);
		File[] files = dir.listFiles();
		for (int i=0; i<50 && i<files.length; i++){
			Object obj = Util.read(files[i].getAbsolutePath());
			if (obj!=null && obj instanceof Track) {
				Track t = (Track)obj;
				t.addParams(sigArr, post, i);
			}
		}

		String sig = Auth.getApiSignatureScrobble(sigPath, sigArr);

		post.append("&api_key=").append(Auth.LASTFM_API_KEY).
		append("&api_sig=").append(sig).
		append("&sk=").append(Auth.getSessionkey(sigPath)).
		append("&method=track.scrobble");
		Log.d(TAG, "post:"+post.toString());
		Log.d(TAG, "sig:"+sig.toString());


		DownloadManager dm = new DownloadManager();
		int ret;
		try {
			ret = dm.download(url+"?format=json", post.toString(), response);
			if (ret == 0 && checkResponse()) {
				for (int i=0; i<50 && i<files.length; i++){
					files[i].delete();
				}
			} else {
				Log.e(TAG, "fail to post scrobble");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}

	private boolean checkResponse() {
		// TODO Auto-generated method stub
		String appPath = this.getExternalFilesDir(null).getAbsolutePath();
		String response = appPath+File.separator+Radio.LOCAL_DIR+File.separator+RESPONSE;
		File file = new File (response);

		String content = Util.readSmallFile(response);
		if (content == null) {
			file.delete();
			return false;
		}
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null) {
			file.delete();
			return false;
		}
		String value = null;

		try {
			if (jobj.has("error") ){
				int code = jobj.getInt("error");
				if (code == 11 || code ==16) {
					return false;
				} else {
					String msg = jobj.getString("message");
					Log.e(TAG, "fail to scrobble:"+ msg);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			file.delete();
			return false;
		}
		return true;
	}
	private class FListener implements AsyncFacebookRunner.RequestListener {

		@Override
		public void onComplete(String response, Object state) {
			// TODO Auto-generated method stub
			Log.d(TAG, "oncomplete:"+response);
		}

		@Override
		public void onIOException(IOException e, Object state) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onioexceotion:"+e.getMessage());
			e.printStackTrace();
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onioexceotion:"+e.getMessage());
			e.printStackTrace();
		}

		@Override
		public void onMalformedURLException(MalformedURLException e,
				Object state) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onioexceotion:"+e.getMessage());
			e.printStackTrace();
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onioexceotion:"+e.getMessage());
			e.printStackTrace();
		}

	}
}
