package com.hitsuji.radio;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.net.CommunicationClient;
import com.net.DownloadManager;
import com.util.CipherUtil;
import com.util.Log;
import com.util.Result;
import com.util.Util;

public class Auth {
	private static final String TAG = Auth.class.getSimpleName();
	
	public static final String LASTFM_API_URL = "http://ws.audioscrobbler.com/2.0/";
	public static final String LASTFM_API_KEY = "a0732b3107348121362822265ee7e0dc";
	public static final String FACEBOOK_API_KEY = "350536824976290";
	public static final String FACEBOOK_SECRET_KEY = "";
	public static final String ECHONEST_API_URL = "http://developer.echonest.com/api/v4/";
	public static final String ECHONEST_API_KEY = "IKQFVCEHHKPLFDKAV";
	public static final String EHCONEST_SECRET_KEY = "";
	
	private static String LASTFM_API_SECRET = null;
	private static final String LASTFM_NAME = "Last.fm";
	private static final String LASTFM_AUTH_URL = "http://www.last.fm/api/auth/";
	private static final String LASTFM_SCROBBLER_URL = "http://post.audioscrobbler.com/";
	
	private static final String LASTFM_OLD_RADIO_API_URL = "http://ws.audioscrobbler.com/";
	private static final String LOCAL_AUTH_PATH = "auth";
	private static final String LOCAL_AUTH_TOKEN = "token";
	private static final String LOCAL_AUTH_SK = "sk";
	
	private static String ApiSignature = null;
	private static Map<String,String> RadioSignature = new ConcurrentHashMap<String, String>();
	private static String PlaylistSignature = null;
	
	private static String Name = null;
	private static String SK = null;
	
	private String mSigPath;
	private CommunicationClient mCommClient; 

	public Auth(String path){
        mCommClient = new CommunicationClient();
        mSigPath = path;
        File f = new File(mSigPath+"/"+LOCAL_AUTH_PATH);
        if(!f.exists()) f.mkdirs();
	}
	

	private static final String getSecret(){
		if (LASTFM_API_SECRET == null) LASTFM_API_SECRET = Jni.b();
		return LASTFM_API_SECRET;
	}
	private String getTokenUrl() {
		String sigArg = String.format("api_key%smethodauth.getToken%s",
                LASTFM_API_KEY,
                Auth.getSecret());
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		String url = String.format("%s?method=auth.getToken&api_key=%s&api_sig=%s&format=json",
				LASTFM_API_URL, LASTFM_API_KEY, sig);
		Log.d(TAG, "token url:"+url);
		return url;
	}
	
	public int fetchToken() throws IOException{
		String url = getTokenUrl();
		if (url == null) return -1;
		
		String tokenPath = mSigPath+"/"+LOCAL_AUTH_PATH+"/"+LOCAL_AUTH_TOKEN;
		DownloadManager dm = new DownloadManager();
		
		int ret = dm.download(url, tokenPath);
		if (ret < 0) return -1;
		
		String token = getToken(mSigPath);
		if (token==null) return -1;
		
		return 0;
	}
	
	public static String asHex(byte bytes[]) {
		StringBuffer strbuf = new StringBuffer(bytes.length * 2);
		for (int index = 0; index < bytes.length; index++) {
			int bt = bytes[index] & 0xff;
			if (bt < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Integer.toHexString(bt));
		}
		return strbuf.toString();
	}
	
	private synchronized static String getToken(String sigPath){
		String tokenPath = sigPath+"/"+LOCAL_AUTH_PATH+"/"+LOCAL_AUTH_TOKEN;
		String content = Util.readSmallFile(tokenPath);
		if (content == null) return null;
		Log.d(TAG, "token.json:"+content);
		//JSONObject jobj = Util.parseJsonobj(content.replace("\"", "\\\"").replace("{", "\\{").replace("}", "\\}"));
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null) return null;
		String token = null;
		try {
			token = jobj.getString("token");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return token;
	}

	public String getAuthUrl(){
		String token = getToken(mSigPath);
		if (token==null)return null;
		
		String url = String.format("%s?api_key=%s&token=%s",
				LASTFM_AUTH_URL, LASTFM_API_KEY, token);
		Log.d(TAG, "auth_url:"+url);
		return url;
	}
	
	
	public synchronized static String getApiSignature(String sigPath){
		if (ApiSignature!=null)return ApiSignature;
		
		String token = getToken(sigPath);
		if (token==null)return null;
		
		String sigArg = String.format("api_key%smethodauth.getSessiontoken%s%s",
                LASTFM_API_KEY,
                token,
                getSecret());
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		if (sig!=null) {
			Log.d(TAG, "create api signature");
			ApiSignature = sig;
		}
		return sig;
	}
	public synchronized static String getApiSignatureRadiotune(String sigPath, String station){
		
		if(RadioSignature.containsKey(station)) 
			return RadioSignature.get(station);
		
		String sk = Auth.getSessionkey(sigPath);
		if (sk==null)return null;
		
		String sigArg = String.format("api_key%smethodradio.tunesk%sstation%s%s",
                LASTFM_API_KEY,
                sk,
                station,
                getSecret());
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		if (sig!=null) {
			Log.d(TAG, "create api signature");
			RadioSignature.put(station, sig);
		}
		return sig;
	}

	
	private String getSessionkeyUrl(){
		String sig = getApiSignature(mSigPath);
		String token = getToken(mSigPath);
		String url = String.format("%s?method=auth.getSession&api_key=%s&token=%s&api_sig=%s&format=json",
				LASTFM_API_URL, LASTFM_API_KEY, token, sig);
		Log.d(TAG, "sk url:"+url);
		return url;
	}

	
	public int fetchSessionkey() throws IOException {
		// TODO Auto-generated method stub
		String url = getSessionkeyUrl();
		String skPath = mSigPath+"/"+LOCAL_AUTH_PATH+"/"+LOCAL_AUTH_SK;
		DownloadManager dm = new DownloadManager();
		
		int ret = dm.download(url, skPath);
		if (ret < 0) return -1;
		
		String sk = getSessionkey(mSigPath);
		if (sk==null) return -1;
		
		return 0;
	}


	public synchronized static String getSessionkey(String sigPath) {
		String path = sigPath+File.separator+LOCAL_AUTH_PATH+File.separator+"."+LOCAL_AUTH_SK;
		File sk = new File(path);
		if (!sk.exists()) return null;
		if (SK!=null) return SK;
		
		// TODO Auto-generated method stub

		Object obj = Util.read(path);
		if (obj==null || !(obj instanceof byte[])) return null;
		byte[] content = (byte[])obj;

		byte[] decsk = CipherUtil.decryptString(content);
		if (decsk==null) return null;
		SK = new String(decsk);
		return SK;
	}
	public synchronized static String getApiSignaturePlaylist(String path){
		if (PlaylistSignature!=null)return PlaylistSignature;
		
		String sigArg = String.format("api_key%smethodradio.getPlaylistrawtruesk%s%s",
                LASTFM_API_KEY,
                Auth.getSessionkey(path),
                getSecret());
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		if (sig!=null) {
			Log.d(TAG, "create playlist signature");
			PlaylistSignature = sig;
		}
		return sig;
	}
	
	public synchronized static String getApiSignatureNowplaying(
			String path, 
			String track, String artist, String album, String albumArtist, 
			String context, String trackNumber,	String mbid, String duration){
		if (artist==null || track==null) return null;
		
		String sigArg = 
				(Util.empty(album)?"":"album"+album)+
				(Util.empty(albumArtist)?"":"albumArtist"+albumArtist)+
				"api_key"+LASTFM_API_KEY+
				"artist"+artist+				
				(Util.empty(context)?"":"context"+context)+
				(Util.empty(duration)?"":"duration"+duration)+
				(Util.empty(mbid)?"":"mbid"+mbid)+
				"methodtrack.updateNowPlaying"+
				"sk"+Auth.getSessionkey(path)+
				"track"+track+
				(Util.empty(trackNumber)?"":"trackNumber"+trackNumber)+
				getSecret();
        Log.d(TAG, "nowplaying signature:"+sigArg);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		if (sig!=null) {
			Log.d(TAG, "create nowplaying signature");
		}
		return sig;
	}

	public synchronized static String getApiSignatureScrobble(
			String path, List<String> sigList){
		
		sigList.add("api_key"+LASTFM_API_KEY);
		sigList.add("sk"+Auth.getSessionkey(path));
		sigList.add("methodtrack.scrobble");
		Collections.sort(sigList);
		StringBuffer sigBuf = new StringBuffer();
		for (String s : sigList) {
			sigBuf.append(s);
		}
		sigBuf.append( getSecret() );
        Log.d(TAG, "scrobble signature:"+sigBuf.toString());
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigBuf.toString().getBytes());
		String sig = asHex(sigArgB);
		if (sig!=null) {
			Log.d(TAG, "create scrobble signature");
		}
		return sig;
	}
	
	public synchronized static String getLoveSignature(String path, 
			String artist, String track) {
		if (artist==null || track==null) return null;
		
		String sigArg = 
				"api_key"+LASTFM_API_KEY+
				"artist"+artist+
				"methodtrack.love"+
				"sk"+Auth.getSessionkey(path)+
				"track"+track+
				getSecret();
        Log.d(TAG, "get like signature:"+sigArg);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		if (sig!=null) {
			Log.d(TAG, "create get like signature");
		}
		return sig;
	}
	public synchronized static String getBanSignature(String path, 
			String artist, String track) {
		if (artist==null || track==null) return null;
		
		String sigArg = 
				"api_key"+LASTFM_API_KEY+
				"artist"+artist+
				"methodtrack.ban"+
				"sk"+Auth.getSessionkey(path)+
				"track"+track+
				getSecret();
        Log.d(TAG, "get unlike signature:"+sigArg);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		if (sig!=null) {
			Log.d(TAG, "create get unlike signature");
		}
		return sig;
	}


	public static Result loadMobileSession(String appPath, String user,
			String pass) throws IOException {
		// TODO Auto-generated method stub
		if (appPath == null || user == null || pass==null) return null;
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] passB = md.digest(pass.getBytes());
		String passSig = asHex(passB);
		byte[] tokenB = md.digest((user+passSig).getBytes());
		String token = asHex(tokenB);
		
		String sigArg = 
				"api_key"+LASTFM_API_KEY+
				"authToken"+token+
				"methodauth.getMobileSession"+
				"username"+user+
				getSecret();
        Log.d(TAG, "get mobile auth signature:"+sigArg);

		byte[] sigArgB = md.digest(sigArg.getBytes());
		String sig = asHex(sigArgB);
		if (sig == null) return null;
		
		File dir = new File(appPath+File.separator+LOCAL_AUTH_PATH);
		if (!dir.exists())dir.mkdirs();
		String response = appPath+File.separator+LOCAL_AUTH_PATH+File.separator+LOCAL_AUTH_SK;
		
		DownloadManager dm = new DownloadManager();
		StringBuffer get = new StringBuffer(""); 
		get.append("method=auth.getMobileSession&").
		     append("api_key=").append(Auth.LASTFM_API_KEY).append("&").
		     append("authToken=").append(token).append("&").
		     append("username=").append(user).append("&").
		     append("api_sig=").append(sig);

        Log.d(TAG, "auth get:"+get.toString());
		int ret = dm.download(LASTFM_API_URL+"?format=json&"+get.toString(), response);
		if (ret != 0) {
			return new Result(ret, "");
		}
		
		try {
			String content = Util.readSmallFile(response);
			if (content == null)return null;
			JSONObject jobj = Util.parseJsonobj(content);
			if(jobj == null) return null;
			
			try {
				if (jobj.has("error")) {
					String msg = jobj.getString("message");
					int result = jobj.getInt("error");
					return new Result(result, msg);
				}
				JSONObject s = jobj.getJSONObject("session");
				String value = s.getString("key");
				String name = s.getString("name");
				if (value != null && name != null) {
					Result result = new Result(0, "");
					result.retStr1 = value;
					result.retStr2 = name;
					return result;
				} else {
					return null;
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} finally {
			File skfile = new File(response);
			skfile.delete();
		}
		return null;
	
	}
	
	public synchronized static void clearAll(String appPath){
		if(appPath==null) return;
		String dirStr = appPath+File.separator+LOCAL_AUTH_PATH;
		File dir = new File(dirStr);
		if (!dir.exists()) return;
		
		for (File f : dir.listFiles()) {
			f.delete();
		}
	}


	public static int storeSk(String appPath, String sk) {
		// TODO Auto-generated method stub
		byte[] encsk = CipherUtil.encryptString(sk);
		byte[] decsk = null;
		if (encsk==null) return -1;
		decsk = CipherUtil.decryptString(encsk);
		
		if (decsk==null || !new String(decsk).equals(sk)) return -1;
		File f = new File(appPath+File.separator+
				LOCAL_AUTH_PATH+File.separator+
				"."+LOCAL_AUTH_SK);
		int ret = Util.write(f.getAbsolutePath(), encsk);
		if (ret!=0) f.delete();
		return ret;
	}

}
