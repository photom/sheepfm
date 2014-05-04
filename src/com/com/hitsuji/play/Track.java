package com.hitsuji.play;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hitsuji.radio.R;
import com.hitsuji.radio.Auth;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.tab.JacketFragment;
import com.hitsuji.radio.table.ImageItem;
import com.net.DownloadManager;
import com.net.HttpLoadManager;
import com.util.Log;
import com.util.Util;

import de.umass.lastfm.Artist;
import de.umass.lastfm.CallException;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.User;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.RelativeLayout;

public class Track implements Parcelable, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1129208950563437490L;

	private static final String TAG = Track.class.getSimpleName();
	
	private static final int THRESHOLD_TIME = 5000;
	private static final String NOW_PLAYING_RESPONSE = "response_nowplaying";
	public static final String DIR = "track";
	public static final String JACKET_DIR = "image";
	public static final String ARTIST_RESPONSE = "response_artist";
	public static final String SIMILAR_IMG_DIR = "similar";
	public static final String SIMILAR_ARTISTS_RESPONSE = "response_similar";
	public static final String LOVE_RESPONSE = "response_love";
	public static final String BAN_RESPONSE = "response_ban";
	public static final int SIMILAR_ARTISTS_LIMIT = 5;

	public static final String SIMILAR_NAME_KEY = "name";
	public static final String SIMILAR_MATCH_KEY = "match";
	public static final String SIMILAR_INDEX_KEY = "index";
	
	
	protected String mTitle,mAlbum,mCreator;
	private String mIdentifier;
	private String mImageUrl;
	protected String mLocation;
	private String mBuyTrackURL, mBuyAlbumURL, mFreeTrackURL;
	private String mArtistPage,mAlbumPage,mTrackPage;
	private String mTrackAuth;
	protected String mAlbumId, mArtistId;
	private String mRecording;
	private String mStreamid;
	private int mExplicit, mLoved;
	protected int mDuration;
	private long mLimitTime;	
	private String mAlbumArtist, mContext;
	protected int mTrackNumber = -1;
	private String mMbid;
	private long mStartTime;
	private int mChosenByUser;
	private String mContent = null;
	protected String mPlaylistName;
	protected String mPlaylistId;
	protected int mPlayOrder;
	
	private TrackList mParent;
	private List<ImageItem> mImageList = Collections.synchronizedList(new ArrayList<ImageItem>());
	private ArrayList<String> mSimilarArtistNameList = new ArrayList<String>();
	private ArrayList<Integer> mSimilarArtistMatchList = new ArrayList<Integer>();


	public static Track create(JSONObject obj, TrackList list, long time) {
		// TODO Auto-generated method stub
		Track track = new Track();
		
		track.mLimitTime = time;
		try {
			track.mLocation = obj.getString("location");
			track.mTitle = obj.getString("title");
			track.mIdentifier = obj.getString("identifier");
			track.mAlbum = obj.getString("album");
			track.mCreator = obj.getString("creator");
			track.mDuration = obj.getInt("duration");
			track.mImageUrl = obj.getString("image");
			
			JSONObject ext = obj.getJSONObject("extension");
			track.mTrackAuth = ext.getString("trackauth");
			track.mAlbumId = ext.getString("albumid");
			track.mArtistId = ext.getString("artistid");
			track.mRecording = ext.getString("recording");
			track.mArtistPage = ext.getString("artistpage");
			track.mAlbumPage = ext.getString("albumpage");
			track.mTrackPage = ext.getString("trackpage");
			track.mBuyTrackURL = ext.getString("buyTrackURL");
			track.mBuyAlbumURL = ext.getString("buyAlbumURL");
			track.mFreeTrackURL = ext.getString("freeTrackURL");
			track.mExplicit = ext.getInt("explicit");
			track.mLoved = ext.getInt("loved");
			track.mStreamid = ext.getString("streamid");
			track.mChosenByUser = 0;
			track.mParent = list;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return track;
	}
	
	
	
    public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };
    
    public Track(){}
	public Track(Parcel in) {
		readFromParcel(in);
	}
	
	public String getLocation(){
		return this.mLocation;
	}
	public String getTitle(){
		return mTitle;
	}
	public String getArtist(){
		return mCreator;
	}
	public String getAlbum(){
		return mAlbum;
	}
	public long getLimit(){
		return mLimitTime;
	}
	public synchronized long getStartTime(){
		return mStartTime;
	}
	public synchronized void setStartTime(long time){
		mStartTime = time;
	}
	
	public int getDuration(){
		return this.mDuration;
	}
	public String getContent(){
		return mContent;
	}
	
	public String getArtistId(){
		return mArtistId;
	}
	public String getAlbumId(){
		return mAlbumId;
	}
	public int getTrackNum(){
		return mTrackNumber;
	}
	public List<ImageItem> getImageList(){
		return this.mImageList;
	}
	public void setImageList(List<ImageItem> list){
		long seed = System.nanoTime();
		Collections.shuffle(list, new Random(seed));
		this.mImageList = list;
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	public boolean timeover(){
		return (getLimit() - System.currentTimeMillis() < THRESHOLD_TIME); 
	}

	public Bundle getSimilarArtists(){
		Bundle b = new Bundle();
		b.putStringArrayList(SIMILAR_NAME_KEY, this.mSimilarArtistNameList);
		b.putIntegerArrayList(SIMILAR_MATCH_KEY, this.mSimilarArtistMatchList);
		return b;
	}
	public ImageItem getImageUrl(int idx){
		if (mImageList.size() == 0) 
			return null;
		if (idx < mImageList.size()) 
				return mImageList.get(idx);
		else return null;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mTitle);
		dest.writeString(mIdentifier);
		dest.writeString(mAlbum);
		dest.writeString(mCreator);
		dest.writeString(mImageUrl);
		dest.writeString(mLocation);
		dest.writeString(mBuyTrackURL);
		dest.writeString(mBuyAlbumURL);
		dest.writeString(mFreeTrackURL);
		dest.writeString(mArtistPage);
		dest.writeString(mAlbumPage);
		dest.writeString(mTrackPage);
		dest.writeString(mTrackAuth);
		dest.writeString(mAlbumId);
		dest.writeString(mArtistId);
		dest.writeString(mRecording);
		dest.writeString(mStreamid);
		dest.writeInt(mExplicit);
		dest.writeInt(mLoved);
		dest.writeInt(mDuration);
		dest.writeLong(mLimitTime);
		dest.writeString(mAlbumArtist);
		dest.writeString(mContext);
		dest.writeInt(mTrackNumber);
		dest.writeString(mMbid);
		dest.writeLong(mStartTime);
		dest.writeInt(mChosenByUser);
		dest.writeString(mContent);
		dest.writeString(mPlaylistName);
		dest.writeString(mPlaylistId);
		dest.writeInt(mPlayOrder);
	}
	
    public void readFromParcel(Parcel in) {
    	mTitle = in.readString();
    	mIdentifier = in.readString();
    	mAlbum = in.readString();
    	mCreator = in.readString();
    	mImageUrl = in.readString();
    	mLocation = in.readString();
    	mBuyTrackURL = in.readString();
    	mBuyAlbumURL = in.readString();
    	mFreeTrackURL = in.readString();
    	mArtistPage = in.readString();
    	mAlbumPage = in.readString();
    	mTrackPage = in.readString();
    	mTrackAuth = in.readString();
    	mAlbumId = in.readString();
    	mArtistId = in.readString();
    	mRecording = in.readString();
    	mStreamid = in.readString();
    	mExplicit = in.readInt();
    	mLoved = in.readInt();
    	mDuration = in.readInt();
    	mLimitTime = in.readLong();
		mAlbumArtist = in.readString();
		mContext = in.readString();
		mTrackNumber = in.readInt();
		mMbid = in.readString();
		mStartTime = in.readLong();
		mChosenByUser = in.readInt();
		mContent = in.readString();
		mPlaylistName = in.readString();
		mPlaylistId = in.readString();
		mPlayOrder = in.readInt();
    }
    
	public int notifyNowPlaying(String appPath) {
		// TODO Auto-generated method stub
		String url = Radio.getRadioUrl(appPath);
		if (url==null) return -1;
		
		File dir = new File(appPath+File.separator+Radio.LOCAL_DIR);
		if (!dir.exists())dir.mkdirs();
		String radioPath = appPath+File.separator+Radio.LOCAL_DIR+File.separator+NOW_PLAYING_RESPONSE;
		
		String sig = Auth.getApiSignatureNowplaying(appPath, 
				mTitle, mCreator, mAlbum, null, null, null, null, 
				String.format("%d", mDuration/1000));
		
		DownloadManager dm = new DownloadManager();
		StringBuffer post = new StringBuffer(""); 
		post.append("method=track.updateNowPlaying&").
		     append("api_key=").append(Auth.LASTFM_API_KEY).append("&").
		     append("api_sig=").append(sig).append("&").
		     append("sk=").append( Auth.getSessionkey(appPath)).append("&").
		     append("track=").append(URLEncoder.encode(mTitle)).append("&").
		     append("artist=").append(URLEncoder.encode(mCreator));
		String postStr = post.toString();
		if (!Util.empty(mAlbum)) {
			postStr += "&album=" + URLEncoder.encode(mAlbum);
		}
		if (!Util.empty(String.format("%d", mDuration/1000))){
		    postStr += "&duration="+ String.format("%d", mDuration/1000);
		}
        Log.d(TAG, "nowplaying post:"+postStr);
		int ret;
		try {
			ret = dm.download(url+"?format=json", postStr, radioPath);
			if (ret < 0) return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		return 0;
	}
	
	public void toFile(String appPath) {
		// TODO Auto-generated method stub
		Long time = System.currentTimeMillis();
		/*
		StringBuffer content = new StringBuffer();
		content*/
		File file = new File(appPath+File.separator+DIR);
		if(!file.exists()) file.mkdirs();
		Util.write(appPath+File.separator+DIR+File.separator+String.format("%d", time), this);
	}
	
	public void addParams(List<String> sig, StringBuffer post, int idx) {
		// TODO Auto-generated method stub
		if (mTitle==null || mCreator == null) return;
		if (post.length()>0) post.append("&");
		
		post.append(String.format(
				"timestamp[%d]=%d", idx, mStartTime)).append("&").
		append(String.format(
				"album[%d]=%s", idx, URLEncoder.encode(mAlbum==null?"":mAlbum))).append("&").
		append(String.format(
				"track[%d]=%s", idx, URLEncoder.encode(mTitle))).append("&").
		append(String.format(
				"artist[%d]=%s", idx, URLEncoder.encode(mCreator))).append("&").
		append(String.format(
				"albumArtist[%d]=%s", idx, URLEncoder.encode(mAlbumArtist==null?"":mAlbumArtist))).append("&").
		append(String.format(
				"duration[%d]=%d", idx, mDuration)).append("&").
		append(String.format(
				"streamid[%d]=%s", idx, mStreamid==null?"":mStreamid)).append("&").
		append(String.format(
				"chosenByUser[%d]=%d", idx, mChosenByUser)).append("&").
		append(String.format(
				"context[%d]=%s", idx, mContext==null?"":mContext)).append("&").
		append(String.format(
				"trackNumber[%d]=%s", idx, (mTrackNumber<0?"":String.format("%d", mTrackNumber)))).append("&").
		append(String.format(
				"mbid[%d]=%s", idx, mMbid==null?"":mMbid));
		
		
		sig.add(String.format("timestamp[%d]%s", idx, mStartTime));
		sig.add(String.format("album[%d]%s", idx, mAlbum==null?"":mAlbum));
		sig.add(String.format("track[%d]%s", idx, mTitle));
		sig.add(String.format("artist[%d]%s", idx, mCreator));
		sig.add(String.format("albumArtist[%d]%s", idx, mAlbumArtist==null?"": mAlbumArtist));
		sig.add(String.format("duration[%d]%d", idx, mDuration));
		sig.add(String.format("streamid[%d]%s", idx, mStreamid==null? "" : mStreamid));
		sig.add(String.format("chosenByUser[%d]%d", idx, mChosenByUser));
		sig.add(String.format("context[%d]%s", idx, mContext==null ? "" : mContext));
		if (mTrackNumber>-1) sig.add(String.format("trackNumber[%d]%d", idx, mTrackNumber));
		else sig.add(String.format("trackNumber[%d]", idx));
		sig.add(String.format("mbid[%d]%s", idx, mMbid==null? "" : mMbid));
		
	}
	public int fetchImageUrls(String appPath) {
		return fetchImageUrls(appPath, 0);
	}
	public int fetchImageUrls(String appPath, int offset) {
		String url = Radio.getEchonestUrl(appPath);
		if (url==null) 
			return -1;
		
		File dir = new File(appPath+File.separator+
				Radio.LOCAL_DIR+File.separator+
				mCreator);
		if (!dir.exists())
			dir.mkdirs();
		
		HttpLoadManager dm = new HttpLoadManager();
		StringBuffer query = new StringBuffer(""); 
		query.append("api_key=").append(Auth.ECHONEST_API_KEY).append("&").
		     append("results=").append(100).append("&").
		     append("start=").append(offset).append("&").
		     append("format=").append("json").append("&").
		     append("name=").append(URLEncoder.encode(mCreator));

        Log.d(TAG, "image list query:"+query.toString());
		String ret;
		try {
			ret = dm.execEchonest(url+"artist/images?" + query.toString());
			if (ret != null) 
				return loadImages(ret, appPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		return -1;
	}
	
	
	private synchronized int loadImages(String response, String appPath) {
		// TODO Auto-generated method stub
		Log.d(TAG, "appmsg:"+appPath);
		String content = response;
		if (content == null)return -1;
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null) return -1;
		List<ImageItem> list = new ArrayList<ImageItem>();
		
		try {
			int total = 0;
			JSONObject s = jobj.getJSONObject("response");
			if (!s.has("status") || s.getJSONObject("status").getInt("code")!=0) 
				return -1;
			if (s.has("total")) {
				total = s.getInt("total");
				if (total==0) {
					Log.d(TAG, mCreator + " has not album arts");
					return -1;
				}
			}

			if(!s.has("images")) 
				return -1;
			try {
				JSONArray arr = s.getJSONArray("images");
				int len = arr.length();
				List<Integer> shfarr = shuffleArray(len);
				for (int i=0; i<len; i++) {
					if (shfarr.get(i)==null)
						continue;
					JSONObject item = arr.getJSONObject(i);
					ImageItem ii = new ImageItem();
					String url = item.getString("url");
					Uri uri = Uri.parse(url);
					String fname = uri.getLastPathSegment();
					ii.fname = fname;
					ii.creator = mCreator;
					ii.no = i;
					ii.loaded = 0;
					ii.url = url;
					if (url!=null)
						list.add(ii);
				}
			} catch (JSONException e) {
				return -1;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		this.setImageList(list);
		return 0;
	}
	
	private List<Integer> shuffleArray(int len) {
		// TODO Auto-generated method stub
		List<Integer> list = new ArrayList<Integer>();
		for (int i=0; i<len; i++) {
			list.add(i);
		}
		Collections.shuffle(list);
		return list;
	}

	
	public static File getArtistImageDir(File base, String artist) {
		File f = new File(base, artist);
		if (!f.exists() && !f.mkdirs()) 
			return null;
		else 
			return f; 
	}
	
	public static int loadImage(String artistPath, String name, int maxWidth, int maxHeight, String url){
		Log.d(TAG, "path:"+artistPath + " name:"+name + " maxw:"+maxWidth + " maxh:"+maxHeight + " url:"+url);
		int ret = 0;
		if (artistPath==null)
			return -1;
		
		File dir = new File(artistPath);
		if (!dir.exists() && !dir.mkdirs()) return -1;
		
		String imagePath = artistPath + File.separator+name;
		Log.d(TAG, "imagepath:"+imagePath);
		File img = new File(imagePath);
		if (!img.exists()) {
			DownloadManager dm = new DownloadManager();
			Log.d(TAG, "doanload jacket:"+url);
			try {
				ret = dm.download(url, imagePath+".tmp");
				if (ret!=0) {
					Log.e(TAG, "fail to load thumbnail:"+url);
				} else {
					ret = scaleImage(imagePath, maxWidth, maxHeight);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}

		}
		return ret;
	}

	private static Bitmap resize(Bitmap bitmap, float resizeWidth, float resizeHeight){
		float resizeScaleWidth;
		float resizeScaleHeight;

		Matrix matrix = new Matrix();        
		resizeScaleWidth = resizeWidth / bitmap.getWidth();
		resizeScaleHeight = resizeHeight / bitmap.getHeight();
		matrix.postScale(resizeScaleWidth, resizeScaleHeight);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}
	
	private static int scaleImage(String imagePath, int maxWidth, int maxHeight) {
		// TODO Auto-generated method stub
		int width, height, result=0;
		String path = imagePath + ".tmp";
		BitmapFactory.Options option = new BitmapFactory.Options();
		option.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, option);
		double rate1 = 1, rate2=1;
		
		if (option.outHeight > maxHeight) {
			rate1 = ((double)option.outHeight) / ((double)maxHeight);
			width = (int)((double)option.outWidth * ((double)maxHeight / (double) option.outHeight));
			height = maxHeight;
		} else {
			width = option.outWidth;
			height = option.outHeight;
		}
		
		if(width > maxWidth){
			rate2 = width / maxWidth;
			height = (int)((double)height * ((double)maxWidth / (double) width));
			width = maxWidth;
		}
		option.inSampleSize = (int)(rate1*rate2+0.5);
		option.inJustDecodeBounds = false;
		
		Bitmap tmp = BitmapFactory.decodeFile(path, option);
		
		if (tmp!=null && (rate1==1 && rate2==1)) {
			File file = new File(path);
			boolean ret = file.renameTo(new File(imagePath));
			if (!ret){
				Log.e(TAG, "fail to rename jacket image.");
				result = -1;
			}
		} else if (tmp != null) {
			Bitmap bmp = resize(tmp, (float)width, (float)height);
			if (bmp!=null) {
				File file = new File(imagePath);
				try {
					file.createNewFile();
					FileOutputStream fos = new FileOutputStream(file);
					boolean ret = bmp.compress (Bitmap.CompressFormat.PNG, 100, fos);
					if (!ret) {
						Log.e(TAG, "fail to scale jacket image.");
						result = -1;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					result = -1;
				}
			}
		}
		File tmpF = new File(path);
		tmpF.delete();
		return result;
	}

	/**
	 * 
	 * @param appPath
	 * @param name
	 * @return
	 */
	public synchronized int loadArtistInfo(String appPath, String name) {
		// TODO Auto-generated method stub
		String url = Radio.getRadioUrl(appPath);
		if (url==null) return -1;
		url += "?format=json&method=artist.getInfo&api_key=" + Auth.LASTFM_API_KEY + 
				"&artist="+URLEncoder.encode(mCreator) + "&username="+name; 
		String dirStr = appPath+File.separator+Radio.LOCAL_DIR;
		File dir = new File(dirStr);
		if (!dir.exists())
			dir.mkdirs();
		String path = dir.getAbsolutePath()+File.separator+ARTIST_RESPONSE;
		DownloadManager dm = new DownloadManager();
		int ret = -1;
		try {
			ret = dm.download(url, path);
			if (ret!=0) Log.e(TAG, "fail to load thumbnail path:"+url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return ret;
	}
	public int loadContent(String appPath) {
		Log.d(TAG, "loadContent path:"+appPath);
		String dirStr = appPath+File.separator+Radio.LOCAL_DIR;
		File dir = new File(dirStr);
		if (!dir.exists())
			dir.mkdirs();
		String path = dir.getAbsolutePath()+File.separator+ARTIST_RESPONSE;
		String content = Util.readSmallFile(path);
		Log.d(TAG, "loadContent content:"+content);
		if (content == null)return -1;
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null) return -1;
		
		
		try {
			if (jobj.has("error")) return -1;
			JSONObject artist = jobj.getJSONObject("artist");
			String name = artist.getString("name");
			if (!name.equalsIgnoreCase(this.mCreator)) return -1;
			
			JSONObject bio = artist.getJSONObject("bio");
			String con = bio.getString("content");
			
			mContent = con;
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	
	public int loadSimilarArtists(String appPath) throws CallException {
		File dir = new File(appPath+File.separator + SIMILAR_IMG_DIR);
		if (!dir.exists()) dir.mkdirs();
		for (File f : dir.listFiles()){
			f.delete();
		}
		
		DownloadManager dm = new DownloadManager();
		Collection<Artist> artists = null;
		try {
			artists = Artist.getSimilar(mCreator, SIMILAR_ARTISTS_LIMIT, Auth.LASTFM_API_KEY);
		}  catch(CallException e) {
			e.printStackTrace();
			Log.e(TAG, "fail to load similar artists info");
			return -1;
		}
		mSimilarArtistMatchList.clear();
		mSimilarArtistNameList.clear();
		Log.d(TAG, "loadSimilarArtists similar artists:"+artists.size());
		for (Artist a : artists) {
			//if (!a.isStreamable()) continue;
			String url = a.getImageURL(ImageSize.LARGE);
			if (url==null) continue;
			
			int num = dir.listFiles() == null ? 0 : dir.listFiles().length;
			int ret;
			try {
				ret = dm.download(url, 
						dir.getAbsolutePath()+ File.separator+num);
				if (ret != 0) continue;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			
			mSimilarArtistNameList.add(a.getName());
			mSimilarArtistMatchList.add((int)(a.getSimilarityMatch()*100));
		}
		return 0;
	}
	
	/**
	 * @deprecated
	 * @param appPath
	 * @return
	 * @throws IOException
	 */
	public int loadSimilarArtistsOld(String appPath) throws IOException {
		// TODO Auto-generated method stub
		String url = Radio.getRadioUrl(appPath);
		if (url==null) return -1;
		String dirStr = appPath+File.separator+Radio.LOCAL_DIR;
		File dir = new File(dirStr);
		if (!dir.exists())
			dir.mkdirs();
		String path = dir.getAbsolutePath()+File.separator+SIMILAR_ARTISTS_RESPONSE;
		
		url += "?format=json&method=artist.getSimilar&api_key=" + Auth.LASTFM_API_KEY + 
				"&artist="+URLEncoder.encode(mCreator) + "&limit="+SIMILAR_ARTISTS_LIMIT; 

		
		DownloadManager dm = new DownloadManager();
		int ret = dm.download(url, path);
		if (ret!=0) {
			Log.e(TAG, "fail to load similar artists list:"+url);
			return ret;
		}
		return loadSimilarArtistImages(appPath);
	}
	/**
	 * @deprecated
	 * @param appPath
	 * @return
	 */
	private int loadSimilarArtistImages(String appPath) {
		// TODO Auto-generated method stub
		File dir = new File(appPath+File.separator + SIMILAR_IMG_DIR);
		if (!dir.exists()) dir.mkdirs();
		for (File f : dir.listFiles()){
			f.delete();
		}
		DownloadManager dm = new DownloadManager();

		mSimilarArtistMatchList.clear();
		mSimilarArtistNameList.clear();
		String dirStr = appPath+File.separator+Radio.LOCAL_DIR;
		dir = new File(dirStr);
		if (!dir.exists())
			dir.mkdirs();
		String path = dir.getAbsolutePath()+File.separator+SIMILAR_ARTISTS_RESPONSE;

		String content = Util.readSmallFile(path);
		if (content == null)return -1;
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null) return -1;
		
		try {
			if (jobj.has("error")) return -1;
			JSONObject sim = jobj.getJSONObject("similarartists");
			if (!sim.has("@attr"))return -1;
			JSONObject att = sim.getJSONObject("@attr");
			if (att==null) return -1;
			String name = att.getString("artist");
			if (!name.equalsIgnoreCase(this.mCreator)) return -1;
			
			JSONArray list = sim.getJSONArray("artist");
			int len = list.length();
			for (int i=0; i<len; i++){
				JSONObject artist = list.getJSONObject(i);
				int streamable = artist.getInt("streamable");
				if (streamable!=1) continue;
				String simName = artist.getString("name");
				int simMatch = (int)(artist.getDouble("match") * 100.0);
				
				JSONArray imgSize = artist.getJSONArray("image");
				String url = null;
				int sizeLen = imgSize.length();
				for (int j=0; j<sizeLen; j++) {
					JSONObject sizeObj = imgSize.getJSONObject(j);
					String sizeStr = sizeObj.getString("size");
					if (sizeStr != null && sizeStr.equals("large")) {
						url = sizeObj.getString("#text");
					}
				}
				if (url == null) continue;
				
				int num = dir.listFiles() == null ? 0 : dir.listFiles().length;
				int ret;
				try {
					ret = dm.download(url, 
							dir.getAbsolutePath()+ File.separator+num);
					if (ret != 0) continue;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				
				mSimilarArtistNameList.add(simName);
				mSimilarArtistMatchList.add(simMatch);
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} 
		
		return 0;
	}
	
	public int postLike(String appPath) throws IOException {
		// TODO Auto-generated method stub
		String url = Radio.getRadioUrl(appPath);
		if (url==null) return -1;
		
		File dir = new File(appPath+File.separator+Radio.LOCAL_DIR);
		if (!dir.exists())dir.mkdirs();
		String radioPath = appPath+File.separator+Radio.LOCAL_DIR+File.separator+LOVE_RESPONSE;
		
		DownloadManager dm = new DownloadManager();
		StringBuffer post = new StringBuffer(""); 
		post.append("method=track.love&").
		     append("api_key=").append(Auth.LASTFM_API_KEY).append("&").
		     append("artist=").append(URLEncoder.encode(mCreator)).append("&").
		     append("track=").append(URLEncoder.encode(mTitle)).append("&").
		     append("sk=").append(Auth.getSessionkey(appPath)).append("&").
		     append("api_sig=").append(Auth.getLoveSignature(appPath, mCreator, mTitle));

        Log.d(TAG, "love post:"+post.toString());
		int ret = dm.download(url+"?format=json", post.toString(), radioPath);
		return ret;
	}
	
	public int postBan(String appPath) throws IOException {
		// TODO Auto-generated method stub
		String url = Radio.getRadioUrl(appPath);
		if (url==null) return -1;
		
		File dir = new File(appPath+File.separator+Radio.LOCAL_DIR);
		if (!dir.exists())dir.mkdirs();
		String radioPath = appPath+File.separator+Radio.LOCAL_DIR+File.separator+BAN_RESPONSE;
		
		DownloadManager dm = new DownloadManager();
		StringBuffer post = new StringBuffer(""); 
		post.append("method=track.ban&").
		     append("api_key=").append(Auth.LASTFM_API_KEY).append("&").
		     append("artist=").append(URLEncoder.encode(mCreator)).append("&").
		     append("track=").append(URLEncoder.encode(mTitle)).append("&").
		     append("sk=").append(Auth.getSessionkey(appPath)).append("&").
		     append("api_sig=").append(Auth.getBanSignature(appPath, mCreator, mTitle));

        Log.d(TAG, "ban post:"+post.toString());
		int ret = dm.download(url+"?format=json", post.toString(), radioPath);
		return ret;
	}
	
	public synchronized static void clearAll(String appPath) {

		String imageStr = appPath + File.separator+JACKET_DIR;
		File image = new File(imageStr);
		if (!image.exists()) return;
		
		for (File f : image.listFiles()) {
			f.delete();
		}
		
		String simStr = appPath + File.separator+ SIMILAR_IMG_DIR;
		File sim = new File(simStr);
		if (!sim.exists()) return;
		for (File f : sim.listFiles()) {
			f.delete();
		}
	}
	public static void removeOldArtistDir(File filesDir) {
		// TODO Auto-generated method stub
		File image = new File(filesDir, JACKET_DIR);
		if (!image.exists()) return;
		
		for (File f : image.listFiles()) {
			f.delete();
		}
		image.delete();
	}

}
