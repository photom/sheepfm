package com.hitsuji.radio.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.LivingIntentService;
import com.android.MediaButtonHelper;
import com.android.RemoteControlClientCompat;
import com.android.RemoteControlHelper;
import com.hitsuji.radio.Auth;
import com.hitsuji.radio.PlayingActivity;
import com.hitsuji.radio.R;
import com.hitsuji.radio.RadioListActivity;
import com.hitsuji.radio.local.LocalAlbum;
import com.hitsuji.radio.local.LocalArtist;
import com.hitsuji.radio.local.LocalAudioInfo;
import com.hitsuji.radio.local.LocalPlaylist;
import com.hitsuji.radio.local.LocalTrack;
import com.hitsuji.radio.local.TrackInfo;
import com.hitsuji.radio.manager.IPlayManagerApi;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.radio.provider.RadioProvider;
import com.hitsuji.radio.provider.RadioResolver;
import com.hitsuji.radio.table.ImageItem;
import com.hitsuji.play.Track;
import com.hitsuji.radio.imp.Friend;
import com.hitsuji.radio.imp.Library;
import com.hitsuji.radio.imp.Local;
import com.hitsuji.radio.imp.Mix;
import com.hitsuji.radio.imp.Neighbour;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.imp.Recommend;
import com.hitsuji.radio.imp.Similar;
import com.util.Log;
import com.util.Result;
import com.util.Util;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.widget.Toast;

public class PlayManager 
extends LivingIntentService  
implements OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,OnPreparedListener
{

	private static final String TAG = PlayManager.class.getSimpleName();
	public static final String CREATE_PLAYLIST_ACTION = "com.histuji.manager.PlayManager.CREATE_PLAYLIST";
	public static final String PLAY_ACTION = "com.histuji.manager.PlayManager.PLAY";
	public static final String STOP_ACTION = "com.histuji.manager.PlayManager.STOP";
	public static final String NEXT_ACTION = "com.histuji.manager.PlayManager.NEXT";
	public static final String LIKE_ACTION = "com.histuji.manager.PlayManager.LOVE";
	public static final String DISLIKE_ACTION = "com.histuji.manager.PlayManager.BAN";
	public static final String FINISH_ACTION = "com.histuji.manager.PlayManager.FINISH";

	public static final String CLEAR_AND_FINISH_ACTION = "com.histuji.manager.PlayManager.CLEAR_AND_FINISH";	
	public static final String INIT_LOCAL_AUDIO_INFO = "com.histuji.manager.LastfmManager.INIT_LOCAL_AUDIO_INFO";

	private static final String MEDIA = "media";
	private static final int LOCAL_AUDIO = 1;
	private static final int STREAM_AUDIO = 2;
	private static final int RESOURCES_AUDIO = 3;

	private static final int LIKE_DISLIKE_TIME_THRESHOLD = 2;

	private static final String FILL_FLAG_KEY = "fill";
	private static final String RETRY_FLAG_KEY = "retry";

	public static final int IDLE = 1;
	public static final int INITIALIZED = 2;
	//public int PREPARING=1;
	public static final int PREPARED = 3;
	public static final int STARTED = 4;
	public static final int STOP = 5;
	public static final int PAUSED = 6;
	public static final int PLAY_COMPLETED = 7;

	public static final String LOCAL_ALBUM_KEY = "album";
	public static final String LOCAL_ARTIST_KEY = "artist";
	public static final String LOCAL_TITLE_KEY = "tracknum";
	public static final String LOCAL_PLAYLIST_KEY = "playlist_id";

	private Map<Integer, Radio> mRadioMap;
	private MediaPlayer mMediaPlayer;
	private int mCurrentRadioType = -1;

	private LocalAudioInfo mLocalAudioInfo = null;

	private int mState = 0;

	private String mPrevAction;
	private AudioManager mAudioManager;

	private LoadHandler mLHandler;
	private HandlerThread mHandlerThread;

	private TrackInfo mPlayingTrackInfo;

	private RemoteControlClientCompat mRemoteControlClientCompat;
	private ComponentName mMediaButtonReceiverComponent;

	public PlayManager() {
		super(PlayManager.class.getSimpleName());
	}
	public PlayManager(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate (){
		super.onCreate();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mHandlerThread = new HandlerThread("loadthread");
		mHandlerThread.start();
		mLHandler = new LoadHandler (mHandlerThread.getLooper());
		File fileDir = this.getExternalFilesDir(null);
		if (!fileDir.exists())
			fileDir.mkdirs();
		Radio l = new Library(Util.getName(this));
		Radio m = new Mix(Util.getName(this));
		Radio r = new Recommend(Util.getName(this));
		Radio n = new Neighbour(Util.getName(this));
		Radio s = new Similar(Util.getName(this));
		Radio f = new Friend(Util.getName(this));
		Radio local = new Local(Util.getName(this));
		mRadioMap = new ConcurrentHashMap<Integer, Radio>();
		mRadioMap.put(m.getType(), m);
		mRadioMap.put(r.getType(), r);
		mRadioMap.put(n.getType(), n);
		mRadioMap.put(l.getType(), l);
		mRadioMap.put(s.getType(), s);
		mRadioMap.put(f.getType(), f);
		mRadioMap.put(local.getType(), local);

		// Create a new media player and set the listeners
		mMediaPlayer = new MediaPlayer();
		mState = 0;
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mMediaPlayer.setOnCompletionListener(this);
		mMediaPlayer.setOnPreparedListener(this);
		mMediaPlayer.setOnErrorListener(this);
		mMediaPlayer.setLooping(false);
		mMediaPlayer.reset();
		mState = IDLE;
		mMediaButtonReceiverComponent = MediaButtonHelper.getComponentName(this, RemoteControlEventReceiver.class); 
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBindService;
	}
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "onUnbind");
		broadcastUnbind();

		super.onUnbind(intent);
		return true;
	}

	private synchronized void setCurrentRadio(int type){
		mCurrentRadioType = type;
	}

	@Override
	public void onDestroy() {
		releaseMediaPlayer();
		doCleanUp();
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
		Log.d(TAG, "onDestroied");
		super.onDestroy();
	}

	private RemoteCallbackList<IPlayManagerCallback> mListeners = new RemoteCallbackList<IPlayManagerCallback>();

	private final IPlayManagerApi.Stub mBindService = new IPlayManagerApi.Stub() {

		public void register(IPlayManagerCallback listener, String cookie)
				throws RemoteException {
			boolean ret = mListeners.register(listener, cookie);
			Log.d(TAG, "setObserver called by " + Thread.currentThread().getName() + " cookie:"+cookie + " listener:"+listener.hashCode() + " ret:"+ret);
		}


		public void unregister(IPlayManagerCallback listener)
				throws RemoteException {
			Log.d(TAG, "removeObserver called by " + Thread.currentThread().getName() + " listener:"+listener.hashCode());
			boolean ret = mListeners.unregister(listener);
			Log.d(TAG, "removeObserver called by " + Thread.currentThread().getName() + " listener:"+listener.hashCode() + "ret:"+ret);
		}
		public int getCurrentRadio(){
			return PlayManager.this.getCurrentRadio();
		}
		public int getCurrentRadioState() throws RemoteException {
			synchronized ( PlayManager.this ) {
				return mState;
			}
		}

		int mCount = 0;
		@Override
		public CharSequence getCurrentPosition() throws RemoteException {
			// TODO Auto-generated method stub
			Track track = getTopTrack();
			String time = "";

			if (getCurrentRadioState() == PlayManager.INITIALIZED || 
					getCurrentRadioState() == PlayManager.PREPARED) {

				for (int i=0; i<mCount; i++) {
					time += ".";
				}
				for (int i=mCount; i<3; i++) {
					time = "&nbsp;" + time;
				}		
				time = "<font face='monospace'>"+time+"</font>";
				CharSequence source = Html.fromHtml(time);
				mCount = (mCount + 1) % 4;
				return source;
			} else if (mMediaPlayer.isPlaying() && track != null){
				int pos = mMediaPlayer.getCurrentPosition() / 1000;
				int d  = track.getDuration() / 1000;
				int dhour = d / 3600;
				int dm = d % 3600;
				int dmin = dm / 60;
				int dsec = dm % 60;
				String dstr = (dhour>0 ? String.format("%02d:", dhour):"") +
						String.format("%02d:", dmin) +
						String.format("%02d", dsec);

				int phour = pos / 3600;
				int pm = pos % 3600;
				int pmin = pm / 60;
				int psec = pm % 60;			
				String pstr = (dhour>0 ? String.format("%02d:", phour):"") +
						String.format("%02d:", pmin) +
						String.format("%02d", psec);
				time = pstr + "/" + dstr;
			}
			return  time;
		}


		@Override
		public String getCurrentArtist() throws RemoteException {
			// TODO Auto-generated method stub
			Track track = getTopTrack();
			if (track != null) return track.getArtist();
			else return null;
		}


		@Override
		public String getCurrentTitle() throws RemoteException {
			// TODO Auto-generated method stub
			Track track = getTopTrack();
			if (track!=null) return track.getTitle();
			else return null;
		}


		@Override
		public String loadCurrentContent() throws RemoteException {
			// TODO Auto-generated method stub
			Track track = getTopTrack();
			if (track==null) return "";
			if (track.getContent()!=null) return track.getContent();

			track.loadContent(PlayManager.this.getExternalFilesDir(null).getAbsolutePath());
			return track.getContent();
		}


		@Override
		public Bundle getSimilarArtistList() throws RemoteException {
			// TODO Auto-generated method stub
			Track track = getTopTrack();
			if (track==null) return null;
			else return track.getSimilarArtists();
		}


		@Override
		public ImageItem getImageUrl(int idx) throws RemoteException {
			// TODO Auto-generated method stub
			Track track = getTopTrack();
			if (track==null) return null;
			else return track.getImageUrl(idx);
		}


		@Override
		public String getSimilarRadioArtist() throws RemoteException {
			// TODO Auto-generated method stub
			return PlayManager.this.getSimilarRadioArtist();
		}


		@Override
		public Bundle getPlayingTrackInfo() throws RemoteException {
			// TODO Auto-generated method stub
			if (mPlayingTrackInfo == null) return null;
			Bundle b = new Bundle();
			synchronized (this) {
				b.putString(LOCAL_TITLE_KEY, mPlayingTrackInfo.getLTitle());
				b.putString(LOCAL_ALBUM_KEY, mPlayingTrackInfo.getLAlbumId());
				b.putString(LOCAL_ARTIST_KEY, mPlayingTrackInfo.getLArtistId());
				b.putString(LOCAL_PLAYLIST_KEY, mPlayingTrackInfo.getLPlaylistId());
			}
			return b;
		}
	};


	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		if (intent==null) return;

		String action = intent.getAction();
		Log.d(TAG, "action:"+action);

		try {
			if (action.equals(CREATE_PLAYLIST_ACTION)) {
				Bundle ext = intent.getExtras();
				String similar = null;
				int type;
				boolean fill = false;

				if (ext == null) type = this.getCurrentRadio();
				else {
					type = ext.getInt(Radio.TYPE);
					if (type == Radio.KIND.LOCAL.Type) {
						this.setIntentRedelivery(false);
						synchronized(this) {
							this.createLocalPlayList(ext);
						}
						return;
					} else {
						synchronized (this) {
							mPlayingTrackInfo = null;
						}
					}
					if (type == Radio.KIND.SIMILAR.Type) {
						similar = ext.getString(Radio.SIMILAR_ARTIST_KEY);
					}
					fill = ext.getBoolean(FILL_FLAG_KEY, false);
				}

				synchronized(this) {
					createPlayList(type, similar, fill);
				}
				this.setIntentRedelivery(false);
				return;
			} else if (action.equals(PLAY_ACTION)) {
				int type;
				Bundle ext = intent.getExtras();
				if (ext != null)
					type = ext.getInt(Radio.TYPE);
				else 
					type = getCurrentRadio();
				if (type != this.getCurrentRadio() || !mMediaPlayer.isPlaying()){
					boolean ret;
					if (type == Radio.KIND.LOCAL.Type) {
						ret = playAudio(LOCAL_AUDIO);
					} else {
						ret = playAudio(STREAM_AUDIO);
					}
					if (ret) {
						doNotification();
					}
				}
				return;
			}  else if (action.equals(STOP_ACTION)) {
				stop();
				return;
			}  else if (action.equals(NEXT_ACTION)) {
				boolean retry = intent.getBooleanExtra(RETRY_FLAG_KEY, false);

				if (!action.equals(mPrevAction)){
					synchronized ( PlayManager.this ) {
						if (mState == STOP && retry ) return;

						mMediaPlayer.stop();
						pollTrack();
						mState = STOP;
					}
					//playAudio(STREAM_AUDIO);
					intent = new Intent(this, PlayManager.class);
					intent.setAction(PlayManager.PLAY_ACTION);
					intent.putExtra(Radio.TYPE, this.getCurrentRadio());
					startService(intent);

					fillTracks();    
					return;
				}
			} else if (action.equals(LIKE_ACTION)) {
				if (!action.equals(mPrevAction) &&
						mMediaPlayer.isPlaying() && 
						mMediaPlayer.getCurrentPosition() / 1000>LIKE_DISLIKE_TIME_THRESHOLD) {
					Track track = getTopTrack();
					if (track==null) return;
					if (Auth.getSessionkey(this.getFilesDir().getAbsolutePath()) == null)
						return;

					int ret;
					try {
						ret = track.postLike(this.getExternalFilesDir(null).getAbsolutePath());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						doToasting(e.toString());
						return;
					}
					if (ret != 0) {
						doToasting("Internl Error: Fail to post Love");
						return;
					}
					Result result = checkResponse(this.getExternalFilesDir(null).getAbsolutePath()+
							File.separator+Radio.LOCAL_DIR+File.separator+Track.LOVE_RESPONSE);

					if (result == null) {
						doToasting("Internal Error: Fail to post 'Love'");
					} else if (result.ret == 0) {
						doToasting("posted 'Love'");
					} else {
						doToasting("Fail to post 'Love': "+result.msg);
					}
				}
				return;
			} else if (action.equals(DISLIKE_ACTION)) {
				if (!action.equals(mPrevAction) &&
						mMediaPlayer.isPlaying() && 
						mMediaPlayer.getCurrentPosition() / 1000>LIKE_DISLIKE_TIME_THRESHOLD) {			
					Track track = getTopTrack();
					if (track==null) return;
					if (Auth.getSessionkey(this.getFilesDir().getAbsolutePath()) == null)
						return;

					int ret;
					try {
						ret = track.postBan(this.getExternalFilesDir(null).getAbsolutePath());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						doToasting(e.toString());
						return;
					}
					if (ret != 0) {
						doToasting("Internal Error: Fail to post 'Ban'");
						return;
					}
					Result result = checkResponse(this.getExternalFilesDir(null).getAbsolutePath()+
							File.separator+Radio.LOCAL_DIR+File.separator+Track.BAN_RESPONSE);

					if (result == null) {
						doToasting("Internal Error: Fail to post 'Ban'");
					} else if (result.ret == 0) {
						doToasting("posted 'Ban'");
					} else {
						doToasting("Fail to post 'Ban': "+result.msg);
					}				
					intent = new Intent(this, PlayManager.class);
					intent.setAction(NEXT_ACTION);
					intent.putExtra(Radio.TYPE, this.getCurrentRadio());
					startService(intent);
				}
			} else if (action.equals(FINISH_ACTION)) {
				this.setSelfDestruction();
			} else if (action.equals(CLEAR_AND_FINISH_ACTION)) {
				this.setSelfDestruction();
				stop();
				clearAll();
			} else if (action.equals(INIT_LOCAL_AUDIO_INFO)) {
				synchronized(this) {
					if (mLocalAudioInfo==null){
						mLocalAudioInfo = new LocalAudioInfo();
						mLocalAudioInfo.init(this);
					}
				}
			}
		} finally {
			mPrevAction = action;
		}
	}


	private static final int NOTIFY_ID = 1;
	private void doNotification() {
		// TODO Auto-generated method stub

		NotificationManager mNM;
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		long code = System.currentTimeMillis();
		Notification notification;
		notification = new Notification(
				R.drawable.notification,  this.getApplicationInfo().name, code);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, PlayingActivity.class),
				Intent.FLAG_ACTIVITY_NEW_TASK);
		String artist = null, title = null;
		try {
			artist = mBindService.getCurrentArtist();
			title = mBindService.getCurrentTitle();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		if (artist == null || title == null) return;
		notification.setLatestEventInfo(this,
				artist, 
				title, contentIntent);

		notification.flags = Notification.FLAG_INSISTENT;
		mNM.notify(NOTIFY_ID, notification);
	}
	private void removeNotification(){
		NotificationManager mNM;
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(NOTIFY_ID);
	}

	private Result checkResponse(String path) {
		// TODO Auto-generated method stub
		String content = Util.readSmallFile(path);
		if (content == null) return null;
		Log.d(TAG, "token.json:"+content);
		JSONObject jobj = Util.parseJsonobj(content);
		if(jobj == null) return null;
		String body = null;
		try {
			if (jobj.has("error")){
				String msg = jobj.getString("message");
				int code = jobj.getInt("error");
				return new Result(code, msg);
			} else {
				return new Result (0, "");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private void stop(){
		if (mMediaPlayer.isPlaying()){
			mMediaPlayer.stop();
			//pollTrack();
			setCurrentState(STOP);
			mMediaPlayer.reset();
			if (mRemoteControlClientCompat != null) {
				mRemoteControlClientCompat
				.setPlaybackState(RemoteControlClientCompat.PLAYSTATE_STOPPED);
			}
			removeNotification();
		}
	}
	private void clearAll() {
		// TODO Auto-generated method stub

		Intent intent = new Intent(this, ScrobbleManager.class);
		intent.putExtra(ScrobbleManager.KEY, (Parcelable)this.getTopTrack());
		intent.setAction(ScrobbleManager.CLEAR_ACTION);
		startService(intent);

		Track.clearAll(this.getExternalFilesDir(null).getAbsolutePath());    	
		Auth.clearAll(this.getFilesDir().getAbsolutePath());
		Radio.clearAll(this.getExternalFilesDir(null).getAbsolutePath());

		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.clear();
		editor.commit();
	}
	private void fillTracks() {
		// TODO Auto-generated method stub

		Radio radio = mRadioMap.get(getCurrentRadio());
		if (radio != null && radio.poverty() && !(radio instanceof Local)){
			Intent intent = new Intent(this, PlayManager.class);
			intent.setAction(CREATE_PLAYLIST_ACTION);
			int type = this.getCurrentRadio();
			intent.putExtra(Radio.TYPE, type);
			if (type == Radio.KIND.SIMILAR.Type) {
				intent.putExtra(Radio.SIMILAR_ARTIST_KEY, getSimilarRadioArtist());
			}
			intent.putExtra(FILL_FLAG_KEY, true);
			startService(intent);
		}
	}
	public int getCurrentRadio(){
		synchronized(PlayManager.this) {
			return mCurrentRadioType;
		}
	}
	private void createPlayList(int type, String similar, boolean fill){
		String appPath = this.getExternalFilesDir(null).getAbsolutePath();
		String sigPath = this.getFilesDir().getAbsolutePath();
		boolean tune = true;
		Radio radio = mRadioMap.get(type);
		if (radio != null && similar != null && type == Radio.KIND.SIMILAR.Type && radio instanceof Similar) {
			Similar similarRadio = (Similar) radio;
			similarRadio.setArtist(similar);
		} else if ((type == Radio.KIND.SIMILAR.Type) && 
				(similar==null || !(radio instanceof Similar))){
			doToasting("Internal Error: Invalid Radio State.");
			OnFinishedCreateTrackList(-1, type, fill);
			return;
		} 

		if (type == this.getCurrentRadio()) tune = false;

		int ret = 0;
		if (tune) {
			try {
				ret = radio.tune(appPath, sigPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				ret = -1;
				e.printStackTrace();
				doToasting(e.toString());
				OnFinishedCreateTrackList(ret, type, fill);
				return;

			}
			if (ret != 0) {
				OnFinishedCreateTrackList(ret, type, fill);
				Result result = checkResponse(this.getExternalFilesDir(null).getAbsolutePath()+
						File.separator+Radio.LOCAL_DIR+File.separator+Radio.RESPONSE);

				if (result == null) {
					doToasting("Internal Error: Fail to tune radio");
				} else if (result.ret == 4) {
					doToasting("Fail to tune radio: Your account might not have the permission.");
				} else if (result.ret != 0) {
					doToasting("Fail to tune radio: "+result.msg);
				}		        		
				return;
			}
		}
		try {
			ret = radio.fetchList(appPath, sigPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			doToasting(e.toString());
			ret = -1;
			OnFinishedCreateTrackList(ret, type, fill);
			return;
		}
		if (ret!=0) {
			Result result = checkResponse(this.getExternalFilesDir(null).getAbsolutePath()+
					File.separator+Radio.LOCAL_DIR+File.separator+Radio.RESPONSE);

			if (result == null) {
				doToasting("Internal Error: Fail to load playlist");
			} else if (result.ret != 0) {
				doToasting("Fail to load playlist: "+result.msg);
			}	         	
			OnFinishedCreateTrackList(ret, type, fill);
			return;
		}
		if (radio instanceof Similar) {
			ret = ((Similar)radio).updateTrackList(appPath, null, fill);
		} else {
			ret = radio.updateTrackList(appPath, null);
		}
		if (ret!=0) {
			doToasting("Internal Error: Fail to create playlist");
			OnFinishedCreateTrackList(ret, type, fill);
			return;
		}
		setCurrentRadio(type);        
		OnFinishedCreateTrackList(0, type, fill);
	}
	private int createLocalPlayList(Bundle bundle){
		Local radio =(Local) mRadioMap.get(Radio.KIND.LOCAL.Type);
		int type = Radio.KIND.LOCAL.Type;
		LocalAudioInfo lai = new LocalAudioInfo();

		radio.updateTrackList(
				lai.createTrackList(this, 
						bundle.getString(LOCAL_ARTIST_KEY), 
						bundle.getString(LOCAL_ALBUM_KEY), 
						bundle.getString(LOCAL_TITLE_KEY),
						bundle.getString(LOCAL_PLAYLIST_KEY)));
		setCurrentRadio(type);
		synchronized (this) {
			if (bundle.get(LOCAL_TITLE_KEY)!=null) {
				mPlayingTrackInfo = new LocalTrack(
						bundle.getString(LOCAL_TITLE_KEY), 
						bundle.getString(LOCAL_ALBUM_KEY),
						bundle.getString(LOCAL_ARTIST_KEY));
			} else if (bundle.get(LOCAL_ALBUM_KEY)!=null) {
				mPlayingTrackInfo = new LocalAlbum(
						bundle.getString(LOCAL_ALBUM_KEY),
						bundle.getString(LOCAL_ARTIST_KEY));

			} else if (bundle.get(LOCAL_PLAYLIST_KEY)!=null) {
				mPlayingTrackInfo = new LocalPlaylist(
						bundle.getString(LOCAL_PLAYLIST_KEY));

			} else if (bundle.get(LOCAL_ARTIST_KEY)!=null) {
				mPlayingTrackInfo = new LocalArtist(
						bundle.getString(LOCAL_ARTIST_KEY));

			} else {
				mPlayingTrackInfo = null;
			}
		}
		OnFinishedCreateTrackList(0, type, false);
		return 0;
	}
	private void OnFinishedCreateTrackList(int ret, int type, boolean fill) {
		int observerNum = mListeners.beginBroadcast();
		for(int i = 0; i < observerNum; i++){
			try {
				mListeners.getBroadcastItem(i).onFinishedCreatePlayList(ret, type, fill);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mListeners.finishBroadcast();
	}


	private void doCleanUp() {

	}
	private boolean playAudio(Integer Media) {
		doCleanUp();
		boolean ret = false;
		try {
			Track track = null;
			String path = null;
			String title = null;
			String album = null;
			String artist = null;
			Radio r = mRadioMap.get(getCurrentRadio());
			switch (Media) {
			case LOCAL_AUDIO:

				if (r!=null && r.getTopTrack()!=null) {
					track = r.getTopTrack();

					path = track.getLocation();
					title = track.getTitle();
					album = track.getAlbum();
					artist = track.getArtist();

				} else 
					path = null;
				break;
			case STREAM_AUDIO:
				if (r!=null && r.getTopTrack()!=null) {
					track = r.getTopTrack();
					if (track.timeover()) {
						Intent intent = new Intent(this, PlayManager.class);
						intent.setAction(NEXT_ACTION);
						intent.putExtra(RETRY_FLAG_KEY, true);
						startService(intent);
						return ret;
					}
					path = track.getLocation();
					title = track.getTitle();
					album = track.getAlbum();
					artist = track.getArtist();

				} else 
					path = null;
				break;
			}
			if (path != null && track!=null) {
				mMediaPlayer.reset();
				setCurrentState(IDLE);
				this.broadcastStarting();
				mMediaPlayer.setDataSource(path);
				setCurrentState(INITIALIZED);
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.prepare();
				setCurrentState(PREPARED);
				focus();

				mMediaPlayer.start();
				Log.d(TAG, "start playing... title:"+ title +" album:"+album+" artist:"+artist);
				track.setStartTime(Util.getUtc()/1000);
				setCurrentState(STARTED);

				onStarted(track);
				postActionToFacebook();
				mLHandler.sendMessage(mLHandler.obtainMessage(LOAD, track));           		
				ret = true;
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Intent intent = new Intent(this, PlayManager.class);
			intent.setAction(NEXT_ACTION);
			intent.putExtra(RETRY_FLAG_KEY, true);
			startService(intent);
		}
		return ret;

	}

	private void focus() {
		// TODO Auto-generated method stub
		mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager, 
				MediaButtonHelper.getComponentName(this, RemoteControlEventReceiver.class));

		if (mRemoteControlClientCompat == null) {
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.setComponent(mMediaButtonReceiverComponent);
			mRemoteControlClientCompat = new RemoteControlClientCompat(
					PendingIntent.getBroadcast(this /*context*/,
							0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
			RemoteControlHelper.registerRemoteControlClient(mAudioManager,
					mRemoteControlClientCompat);
		}
		mRemoteControlClientCompat.setPlaybackState(
				RemoteControlClientCompat.PLAYSTATE_PLAYING);

		mRemoteControlClientCompat.setTransportControlFlags(
				RemoteControlClientCompat.FLAG_KEY_MEDIA_NEXT |
				RemoteControlClientCompat.FLAG_KEY_MEDIA_STOP);

		Track t = getTopTrack();
		if (t!=null) { 
			mRemoteControlClientCompat.editMetadata(true)
			.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, t.getArtist())
			.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, t.getAlbum())
			.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, t.getTitle())
			.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, t.getDuration())
			.apply();
		}
	}

	private void onStarted(Track track) {
		// TODO Auto-generated method stub
		if (mListeners==null) return;

		int observerNum = mListeners.beginBroadcast();
		for(int i = 0; i < observerNum; i++){
			try {
				if (mListeners.getBroadcastItem(i)!=null)
					mListeners.getBroadcastItem(i).onStarted(track);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mListeners.finishBroadcast();
	}
	private void onLoadedTrackInfo(Track track) {
		// TODO Auto-generated method stub
		if (mListeners==null) return;

		int observerNum = mListeners.beginBroadcast();
		Log.d(TAG, "onloadedtrackinfo listener num:"+observerNum);
		for(int i = 0; i < observerNum; i++){
			try {
				if (mListeners.getBroadcastItem(i)!=null){
					mListeners.getBroadcastItem(i).onLoadedTrackInfo(track);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mListeners.finishBroadcast();
	}
	public void onBufferingUpdate(MediaPlayer arg0, int percent) {
		Log.d(TAG, "onBufferingUpdate percent:" + percent);
	}

	public void onCompletion(MediaPlayer arg0) {
		Log.d(TAG, "onCompletion called. pos:"+arg0.getCurrentPosition() + " dur:"+arg0.getDuration());
		setCurrentState(PLAY_COMPLETED);
		removeNotification();

		Intent intent;
		Track track = getTopTrack();

		if (track!=null) {
			intent = new Intent(this, ScrobbleManager.class);
			intent.putExtra(ScrobbleManager.KEY, (Parcelable)this.getTopTrack());
			intent.setAction(ScrobbleManager.SCROBBLE_ACTION);
			startService(intent);
		}
		boolean next = false;
		if (this.getCurrentRadio() == Radio.KIND.LOCAL.Type){
			Radio r = mRadioMap.get(getCurrentRadio());
			if (r.trackSize() >1){
				next = true;
			}
		} else {
			next = true;
		}
		if (next) {
			intent = new Intent(this, PlayManager.class);
			intent.setAction(NEXT_ACTION);
			startService(intent);
		}
	}

	public void postActionToFacebook(){
		Intent intent = new Intent(this, ScrobbleManager.class);
		intent.putExtra(ScrobbleManager.KEY, (Parcelable)this.getTopTrack());
		intent.setAction(ScrobbleManager.POST_ACTION);
		startService(intent);
	}

	public void onPrepared(MediaPlayer mediaplayer) {
		Log.d(TAG, "onPrepared called");
	}

	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	public synchronized Track getTopTrack(){
		if (mRadioMap==null) return null;
		Radio r = mRadioMap.get(getCurrentRadio());
		if (r!=null) {
			return r.getTopTrack();
		}
		return null;
	}
	public synchronized Track pollTrack(){
		Radio r = mRadioMap.get(getCurrentRadio());
		if (r!=null) {
			return r.pollTrack();
		}
		return null;
	}
	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		Log.d(TAG, "some error occur. 1:"+arg1 + " 2:"+arg2);
		arg0.reset();
		return false;
	}

	private synchronized void setCurrentState(int state){
		mState = state;
	}

	private void doToasting(String msg) {
		int observerNum = mListeners.beginBroadcast();
		for(int i = 0; i < observerNum; i++){
			try {
				mListeners.getBroadcastItem(i).toast(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mListeners.finishBroadcast();
	}

	private void broadcastStarting(){
		int observerNum = mListeners.beginBroadcast();
		for(int i = 0; i < observerNum; i++){
			try {
				mListeners.getBroadcastItem(i).onStarting();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mListeners.finishBroadcast();
	}

	private void broadcastUnbind() {
		// TODO Auto-generated method stub
		int observerNum = mListeners.beginBroadcast();
		for(int i = 0; i < observerNum; i++){
			try {
				mListeners.getBroadcastItem(i).onUnbind();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mListeners.finishBroadcast();
	}

	private String getSimilarRadioArtist(){
		int type = getCurrentRadio();
		Radio radio = mRadioMap.get(type);
		if (radio != null && radio instanceof Similar) {
			Similar s = (Similar)radio;
			return s.getArtist();
		} else {
			return null;
		}
	}

	private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			// AudioFocus is a new feature: focus updates are made verbose on purpose
			switch (focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS:
				Log.v(TAG, "AudioFocus: received AUDIOFOCUS_LOSS");
				if(mMediaPlayer.isPlaying()) {
					Intent intent = new Intent(PlayManager.this, PlayManager.class);
					intent.setAction(PlayManager.STOP_ACTION);
					startService(intent);
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				Log.v(TAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
				if(mMediaPlayer.isPlaying()) {
					Intent intent = new Intent(PlayManager.this, PlayManager.class);
					intent.setAction(PlayManager.STOP_ACTION);
					startService(intent);
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				Log.v(TAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
				if(mMediaPlayer.isPlaying()) {
					Intent intent = new Intent(PlayManager.this, PlayManager.class);
					intent.setAction(PlayManager.STOP_ACTION);
					startService(intent);
				}
				break;
			case AudioManager.AUDIOFOCUS_GAIN:
				Log.v(TAG, "AudioFocus: received AUDIOFOCUS_GAIN");

				break;
			default:
				Log.e(TAG, "Unknown audio focus change code");
			}
		}
	};

	private static final int LOAD = 0;
	private class LoadHandler extends Handler {

		public LoadHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg){
			if (msg.what == LOAD && msg.obj!=null){
				Track track = (Track)msg.obj;
				List<ImageItem> list = RadioResolver.loadImages(PlayManager.this, track);
				if (list!=null && list.size()>0)
					track.setImageList(list);
				else {
					track.fetchImageUrls(PlayManager.this.getExternalFilesDir(null).getAbsolutePath());
					RadioResolver.addCreator(PlayManager.this, track.getArtist());
					RadioResolver.addImages(PlayManager.this, track.getImageList());
				}
				
				track.loadArtistInfo(PlayManager.this.getExternalFilesDir(null).getAbsolutePath(), 
						Util.getName(PlayManager.this));
				track.loadSimilarArtists(PlayManager.this.getExternalFilesDir(null).getAbsolutePath());

				if (Auth.getSessionkey(PlayManager.this.getFilesDir().getAbsolutePath()) != null)
					track.notifyNowPlaying(PlayManager.this.getExternalFilesDir(null).getAbsolutePath());
				PlayManager.this.onLoadedTrackInfo(track);
			}
		}
	}
}
