package com.hitsuji.radio.local;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.hitsuji.play.Track;
import com.hitsuji.radio.Auth;
import com.hitsuji.radio.HitsujiRadioMobileActivity;
import com.hitsuji.radio.PlayingActivity;
import com.hitsuji.radio.R;
import com.hitsuji.radio.imp.Local;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.manager.ConnectionListener;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.net.DownloadManager;
import com.util.Log;
import com.util.Util;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.CallException;
import de.umass.lastfm.ImageSize;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LocalAudioListActivity extends Activity{
	private static final String TAG = LocalAudioListActivity.class.getSimpleName();
	private static final int INIT = 0;
	private static final int ADD_ALBUMS = 1;
	private static final int DEL_ALBUMS = 2;
	private static final int ADD_TRACKS = 3;
	private static final int DEL_TRACKS = 4;
	private static final int DL_COVER = 5;
	private static final int REFRESH = 6;
	private static final int TOAST = 7;

	private static final String COVER_DIR = "localcover";
	private static final int PADDING_SIZE = 1;
	private static final int MARGIN_SIZE = 3;
	private static int TOGGLE_WIDTH = 0;
	
	private ListView mListView;
	private ArrayAdapter<TrackInfo> mAdapter;
	private List<TrackInfo> mLocalTrackList = new ArrayList<TrackInfo>();
	private ExecHandler mEHandler;
	private HandlerThread mEHandlerThread;
	private DisplayHandler mDHandler;
	private LocalAudioInfo mAudioInfo;

	private PlayManagerServiceConnection mConnection;
	private IPlayManagerCallback mCallback;
	private ConnectDialog mProgressDialog; 
	private String mPlayingTitle;
	private String mPlayingAlbumId;
	private String mPlayingArtistId;
	private String mPlayingPlaylistId;
	
	private SharedPreferences mPrefs;


	class OnTouchViewListner implements OnTouchListener{
		private GestureDetector mDetector;
		
		public OnTouchViewListner(GestureDetector g){
			mDetector = g;
		}
		@Override
		public boolean onTouch(View v, MotionEvent me) {
	        boolean ret = mDetector.onTouchEvent(me);
	        return ret;
		}
		
	}
	
	private class TrackArrayAdapter extends ArrayAdapter<TrackInfo> {
		private int resourceId;
		private LayoutInflater inflater;

		public TrackArrayAdapter(Context context, int resourceId) {
			super(context, resourceId);
			this.resourceId = resourceId;
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final TrackInfo item = (TrackInfo) getItem(position);
			if (convertView == null) {
				convertView = inflater.inflate(resourceId, null);
			}

			RelativeLayout layout = (RelativeLayout)convertView.findViewById(R.id.track_layout);
			ImageView toggle = (ImageView)convertView.findViewById(R.id.track_toggle);
			ImageView playing = (ImageView)convertView.findViewById(R.id.track_playing);
			ImageView cover = (ImageView)convertView.findViewById(R.id.track_cover);
			TextView nameView = (TextView)convertView.findViewById(R.id.track_title);

			File dir = new File(LocalAudioListActivity.this.getExternalFilesDir(null).getAbsolutePath()+File.separator+COVER_DIR);
			if (!dir.exists()) dir.mkdirs();

			if (item instanceof LocalArtist) {
				layout.setBackgroundColor(Color.parseColor("#000a14"));
				LocalArtist la = (LocalArtist)item;
				if (TOGGLE_WIDTH==0 && toggle.getWidth()>0)
					TOGGLE_WIDTH = toggle.getWidth();
				
				String name = la.getCoverImageName();
				File file = new File(dir.getAbsolutePath()+File.separator+name);
				if (file.exists()) {
					if ( file.length()>0 ) {
						Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
						cover.setImageBitmap(b);
					} else {
						cover.setImageBitmap(null);
					}
				} else {
					//mEHandler.sendMessage(mEHandler.obtainMessage(DL_COVER, la));
					cover.setImageBitmap(null);
				}

				toggle.setOnClickListener(new ArtistOrPlaylistToggleClickListener((LocalArtist)item, position));
				toggle.setImageBitmap(null);
				if (item.isFolded())
					toggle.setImageResource(R.xml.ic_btn_track_toggleoff);
				else
					toggle.setImageResource(R.xml.ic_btn_track_toggleon);
				nameView.setTextColor(Color.WHITE);
				nameView.setText(la.getName());
				nameView.setPadding(0, 0, 0, PADDING_SIZE);
				///toggle.setImageBitmap(null);
				if (mPlayingTitle==null &&
						mPlayingAlbumId==null &&
						mPlayingArtistId!=null &&
						mPlayingArtistId.equals(la.getLArtistId())) {
					la.setPlaying(true);
					playing.setImageResource(R.drawable.playing);
				} else {
					la.setPlaying(false);
					playing.setImageBitmap(null);
				}

			} else if (item instanceof LocalPlaylist) {
				layout.setBackgroundColor(Color.parseColor("#000a14"));
				LocalPlaylist lp = (LocalPlaylist)item;

				String name = lp.getCoverImageName();
				File file = new File(dir.getAbsolutePath()+File.separator+name);
				if (file.exists()) {
					if ( file.length()>0 ) {
						Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
						cover.setImageBitmap(b);
					} else {
						cover.setImageBitmap(null);
					}
				} else {
					//mEHandler.sendMessage(mEHandler.obtainMessage(DL_COVER, la));
					cover.setImageBitmap(null);
				}

				toggle.setOnClickListener(new ArtistOrPlaylistToggleClickListener((LocalPlaylist)item, position));
				toggle.setImageBitmap(null);
				if (item.isFolded())
					toggle.setImageResource(R.xml.ic_btn_track_toggleoff);
				else
					toggle.setImageResource(R.xml.ic_btn_track_toggleon);
				nameView.setTextColor(Color.WHITE);
				nameView.setText(lp.getName());
				nameView.setPadding(0, 0, 0, PADDING_SIZE);
				///toggle.setImageBitmap(null);
				if (mPlayingTitle==null &&
						mPlayingAlbumId==null &&
						mPlayingPlaylistId!=null &&
						mPlayingPlaylistId.equals(lp.getLPlaylistId())) {
					lp.setPlaying(true);
					playing.setImageResource(R.drawable.playing);
				} else {
					lp.setPlaying(false);
					playing.setImageBitmap(null);
				}

			} else if (item instanceof LocalAlbum) {
				layout.setBackgroundColor(Color.parseColor("#001428"));
				LocalAlbum la = (LocalAlbum)item;
				toggle.setOnClickListener(new AlbumToggleClickListener((LocalAlbum)item, position) );
				nameView.setText(la.getName());
				nameView.setPadding(0, 0, 0, PADDING_SIZE);
				nameView.setTextColor(Color.WHITE);
				String name = la.getCoverImageName();
				File file = new File(dir.getAbsolutePath()+File.separator+name);
				if (file.exists()) {
					if ( file.length()>0 ) {
						Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
						cover.setImageBitmap(b);
					} else {
						cover.setImageResource(R.drawable.cd_case);
					}
				} else {
					mEHandler.sendMessage(mEHandler.obtainMessage(DL_COVER, la));
					cover.setImageResource(R.drawable.cd_case);
				}
				if (mPlayingTitle==null &&
						mPlayingAlbumId!=null &&
						mPlayingAlbumId.equals(la.getLAlbumId()) &&
						mPlayingArtistId!=null &&
						mPlayingArtistId.equals(la.getLArtistId())) {
					la.setPlaying(true);
					playing.setImageResource(R.drawable.playing);
				} else {
					la.setPlaying(false);
					playing.setImageBitmap(null);
				}
				if (item.isFolded())
					toggle.setImageResource(R.xml.ic_btn_track_toggle_album_off);
				else
					toggle.setImageResource(R.xml.ic_btn_track_toggle_album_on);
			} else if (item instanceof LocalTrack) {
				LocalTrack lt = (LocalTrack)item;
				layout.setBackgroundColor(Color.parseColor("#002851"));
				nameView.setText(lt.getTitle());
				nameView.setTextColor(Color.WHITE);
				nameView.setPadding(TOGGLE_WIDTH, 0, 0, PADDING_SIZE);
				toggle.setOnClickListener(null);
				toggle.setImageBitmap(null);
				if (mPlayingTitle!=null &&
						mPlayingTitle.equals(lt.getLTitle()) &&
						mPlayingAlbumId!=null &&
						mPlayingAlbumId.equals(lt.getLAlbumId()) &&
						mPlayingArtistId!=null &&
						mPlayingArtistId.equals(lt.getLArtistId())) {
					lt.setPlaying(true);
					playing.setImageResource(R.drawable.playing);
				} else {
					lt.setPlaying(false);
					playing.setImageBitmap(null);
				}
				cover.setImageBitmap(null);
			}
			return convertView;
		}
		
	}

	private class ArtistOrPlaylistToggleClickListener implements OnClickListener{
		private TrackInfo mInfo;
		private int mPos;

		private ArtistOrPlaylistToggleClickListener(TrackInfo ti, int pos){
			mInfo = ti;
			mPos = pos;
		}
		@Override
		public void onClick(View arg0) {
			if (mInfo instanceof LocalArtist) {
				onClickArtist(arg0);
			} else if (mInfo instanceof LocalPlaylist) {
				onClickPlaylist(arg0);
			}
		}
		
		public void onClickArtist(View arg0) {
			// TODO Auto-generated method stub
			synchronized(mInfo) {
				LocalArtist lar = (LocalArtist)mInfo;
				if (lar.isFolded()) {
					lar.unfold();
					mDHandler.sendMessage(mDHandler.obtainMessage(ADD_ALBUMS, mPos, 0, mInfo));

				} else {
					lar.fold();
					for (LocalAlbum la : lar.getAlbums() ){
						la.fold();
					}
					mDHandler.sendMessage(mDHandler.obtainMessage(DEL_ALBUMS, mPos, 0, mInfo));
				}
			}
		}
		
		public void onClickPlaylist(View arg0) {
			// TODO Auto-generated method stub
			synchronized(mInfo) {
				LocalPlaylist lp = (LocalPlaylist)mInfo;
				if (lp.isFolded()) {
					lp.unfold();
					mDHandler.sendMessage(mDHandler.obtainMessage(ADD_ALBUMS, mPos, 0, lp));

				} else {
					lp.fold();
					for (LocalAlbum la : lp.getAlbums() ){
						la.fold();
					}
					mDHandler.sendMessage(mDHandler.obtainMessage(DEL_ALBUMS, mPos, 0, lp));
				}
			}
		}
	}


	private class AlbumToggleClickListener implements OnClickListener{
		private LocalAlbum mAlbum;
		private int mPos;

		private AlbumToggleClickListener(LocalAlbum la, int pos){
			mAlbum = la;
			mPos = pos;
		}

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if (mAlbum.isFolded()) {
				mAlbum.unfold();
				mDHandler.sendMessage(mDHandler.obtainMessage(ADD_TRACKS, mPos, 0, mAlbum));
			} else {
				mAlbum.fold();
				mDHandler.sendMessage(mDHandler.obtainMessage(DEL_TRACKS, mPos, 0, mAlbum));
			}
			//mAdapter.notifyDataSetChanged();
		}

	}
	
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.d(TAG, "oncreate");
		setTheme(R.style.Theme_Sherlock);
		setContentView(R.layout.radio_list);

		mEHandlerThread = new HandlerThread("ExecHandlerThread");
		mEHandlerThread.start();
		mEHandler = new ExecHandler(mEHandlerThread.getLooper());
		mDHandler = new DisplayHandler();
		mAudioInfo = new LocalAudioInfo();
		
		RelativeLayout.LayoutParams params = 
				new RelativeLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, 
						LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ABOVE, R.id.layout_logo);
		params.setMargins(0, 0, 0, MARGIN_SIZE);
		RelativeLayout layout = (RelativeLayout)this.findViewById(R.id.radio_list);
		layout.setBackgroundColor(Color.BLACK);
		mListView = new ListView(this);
		layout.addView(mListView, params);
		mAdapter = new TrackArrayAdapter(this,  R.layout.local_track_list_raw);
		mListView.setAdapter(mAdapter);
		mListView.setOnTouchListener(
				new OnTouchViewListner(
						new GestureDetector(this, this.onGestureListener)));
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onitemclick parent:"+parent + " view:"+view.toString() + " id:"+id);
				ListView listView = (ListView) parent;
				TrackInfo ti = (TrackInfo) listView.getItemAtPosition(position);
				if (ti.isPlaying()) {
					goNext();
				} else {
					createPlayList(ti, position);
				}
			}    
			public void createPlayList(TrackInfo ti, int position){

				synchronized (LocalAudioListActivity.this){
					mProgressDialog = new ConnectDialog(LocalAudioListActivity.this, new Local(""));
					mProgressDialog.setOnDismissListener(new OnDismissListener(){

						@Override
						public void onDismiss(DialogInterface dialog) {
							// TODO Auto-generated method stub
							mProgressDialog.unsetShowFlag();
						}

					});
					mProgressDialog.show();
				}


				Intent intent = new Intent(LocalAudioListActivity.this, PlayManager.class);
				intent.setAction(PlayManager.STOP_ACTION);
				LocalAudioListActivity.this.startService(intent);

				intent = new Intent(LocalAudioListActivity.this, PlayManager.class);
				intent.setAction(PlayManager.CREATE_PLAYLIST_ACTION);
				intent.putExtra(Radio.TYPE, Radio.KIND.LOCAL.Type);
				if (ti instanceof LocalTrack) {
					intent.putExtra(PlayManager.LOCAL_PLAYLIST_KEY, ((LocalTrack)ti).getLPlaylistId());					
					intent.putExtra(PlayManager.LOCAL_ARTIST_KEY, ((LocalTrack)ti).getArtistId());
					intent.putExtra(PlayManager.LOCAL_ALBUM_KEY, ((LocalTrack)ti).getAlbumId());
					intent.putExtra(PlayManager.LOCAL_TITLE_KEY, ((LocalTrack) ti).getTitle());
				} else if (ti instanceof LocalAlbum) {
					intent.putExtra(PlayManager.LOCAL_PLAYLIST_KEY, ((LocalAlbum)ti).getLPlaylistId());
					intent.putExtra(PlayManager.LOCAL_ARTIST_KEY, ((LocalAlbum)ti).getArtistId());
					intent.putExtra(PlayManager.LOCAL_ALBUM_KEY, ((LocalAlbum)ti).getId());
				} else if (ti instanceof LocalArtist) {
					intent.putExtra(PlayManager.LOCAL_ARTIST_KEY, ((LocalArtist)ti).getId());
				} else if (ti instanceof LocalPlaylist) {
					intent.putExtra(PlayManager.LOCAL_PLAYLIST_KEY, ((LocalPlaylist)ti).getId());
				}
				LocalAudioListActivity.this.startService(intent);

				intent = new Intent(LocalAudioListActivity.this, PlayManager.class);
				intent.setAction(PlayManager.PLAY_ACTION);
				intent.putExtra(Radio.TYPE, Radio.KIND.LOCAL.Type);
				startService(intent);
			}


		});

		ImageView logo = (ImageView)this.findViewById(R.id.logo2);
		logo.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String name = Util.getName(LocalAudioListActivity.this);
				String url = "http://m.last.fm/"+(name!=null? "user/"+URLEncoder.encode(name) : "");
				Log.d(TAG, "onartworkclicklistener url:" + url);
				Uri uri = Uri.parse(url);
				Intent i = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(i); 
			}

		});

		mEHandler.sendMessage(mEHandler.obtainMessage(INIT, this));
		setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));


		mCallback = new IPlayManagerCallback.Stub() {

			@Override
			public void onFinishedCreatePlayList(int ret, int type, boolean fill) throws RemoteException {
				// TODO Auto-generated method stub
				Log.d(TAG, "onfinishedcreateplaylist");
				if (!fill) {
					if (ret == 0) {
						Intent intent = new Intent( LocalAudioListActivity.this, PlayingActivity.class );
						intent.putExtra(Radio.TYPE, type);
						startActivity(intent);
					}
					if (mProgressDialog != null) mProgressDialog.dismiss();
				}
				updatePlayingTrackInfo();
			}

			@Override
			public void onStarted(Track track) throws RemoteException {
				// TODO Auto-generated method stub
				mDHandler.sendMessage(mDHandler.obtainMessage(REFRESH));
			}

			@Override
			public void toast(String msg) throws RemoteException {
				// TODO Auto-generated method stub
				doToasting(msg);
			}

			@Override
			public void onStarting() throws RemoteException {
				// TODO Auto-generated method stub

			}

			@Override
			public void onUnbind() throws RemoteException {
				// TODO Auto-generated method stub
				mConnection.unsetBind();
			}

			@Override
			public void onLoadedTrackInfo(Track track) throws RemoteException {
				// TODO Auto-generated method stub

			}
		};
		mConnection = new PlayManagerServiceConnection(mCallback, this, 
				new ConnectionListener(){

			@Override
			public void onConnected() {
				// TODO Auto-generated method stub
				updatePlayingTrackInfo();
			}

			@Override
			public void onDisconnected() {
				// TODO Auto-generated method stub

			}

		});
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	}
	private void goNext(){
		Intent intent = new Intent( LocalAudioListActivity.this, PlayingActivity.class );
		intent.putExtra(Radio.TYPE, Radio.KIND.LOCAL.Type);
		startActivity(intent);
	}	
	@Override
	public void onStart(){
		super.onStart();
		Log.d(TAG, "onstart");

		if (Radio.exit()) {
			Radio.unsetExit();
			mConnection.onServiceDisconnected(null);
			finish();
			return;
		}

		Intent intent = new Intent(this, PlayManager.class);
		bindService(intent, mConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onresume");
		mDHandler.sendMessage(mDHandler.obtainMessage(REFRESH));
		synchronized (this){
			if (mProgressDialog!=null && mProgressDialog.getShowFlag())mProgressDialog.dismiss();
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();
		Log.d(TAG, "onPause");
	}
	
	@Override
	public void onStop(){
		try {
			if (mConnection.getService()!=null) {
				mConnection.getService().unregister(mCallback);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (mConnection.isBind()){
			unbindService(mConnection);
		}

		super.onStop();
		Log.d(TAG, "onStoped");
	}
	@Override
	public void onDestroy(){
		//mEHandlerThread.stop();
		Log.d(TAG, "ondestroy");
		super.onDestroy();
	}

	private void updatePlayingTrackInfo(){
		if (mConnection.getService() !=null){
			Bundle b;
			try {
				b = mConnection.getService().getPlayingTrackInfo();
				if (b!=null){
					synchronized (this) {
						mPlayingTitle = b.getString(PlayManager.LOCAL_TITLE_KEY);
						mPlayingAlbumId = b.getString(PlayManager.LOCAL_ALBUM_KEY);
						mPlayingArtistId = b.getString(PlayManager.LOCAL_ARTIST_KEY);
						mPlayingPlaylistId = b.getString(PlayManager.LOCAL_PLAYLIST_KEY);
					}
					mDHandler.sendMessage(mDHandler.obtainMessage(REFRESH));
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private class ExecHandler extends Handler {
		public ExecHandler(Looper l){
			super(l);
		}
		private void failProc(String path){
			File f = new File(path);
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == INIT) {
				mAudioInfo.init((Context)msg.obj);
				mDHandler.sendMessage(mDHandler.obtainMessage(INIT));
			} else if (msg.what == DL_COVER){
				if (msg.obj instanceof LocalAlbum) {
					LocalAlbum la = (LocalAlbum)msg.obj;
					String fileName = LocalAudioListActivity.this.getExternalFilesDir(null).getAbsolutePath()+File.separator+
							COVER_DIR +File.separator+
							la.getCoverImageName();
					try {
						Album lastAlbum = Album.getInfo(la.getArtist(), la.getName(), Auth.LASTFM_API_KEY);
						if (lastAlbum==null) {
							return;
						}
						String url = lastAlbum.getImageURL(ImageSize.MEDIUM);
						if (url==null || url.length()==0) {
							failProc(fileName);
							return;
						}
						DownloadManager dm = new DownloadManager();
						int ret;
						try {
							ret = dm.download(url, fileName);
							if (ret != 0){
								return;
							} else {
								mDHandler.sendMessage(mDHandler.obtainMessage(REFRESH));
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (CallException e) {
						e.printStackTrace();
						return;
					}
				} else if (msg.obj instanceof LocalArtist) {
					LocalArtist la = (LocalArtist)msg.obj;
					String fileName = LocalAudioListActivity.this.getExternalFilesDir(null).getAbsolutePath()+File.separator+
							COVER_DIR +File.separator+
							la.getCoverImageName();
					try {
						Artist lastArtist = Artist.getInfo(la.getName(), null, Auth.LASTFM_API_KEY);
						if (lastArtist==null) {
							return;
						}
						String url = lastArtist.getImageURL(ImageSize.MEDIUM);
						if (url==null || url.length()==0) {
							failProc(fileName);
							return;
						}
						DownloadManager dm = new DownloadManager();
						int ret;
						try {
							ret = dm.download(url, fileName);
							if (ret != 0){
								return;
							} else {
								mDHandler.sendMessage(mDHandler.obtainMessage(REFRESH));
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (CallException e) {
						e.printStackTrace();
						return;
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void doToasting(String msg) {
		mDHandler.sendMessage(mDHandler.obtainMessage(TOAST, msg));
	}
	private class DisplayHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == INIT) {
				mAdapter.clear();
				for(LocalArtist la : mAudioInfo.getArtists()) {
					mAdapter.add(la);
				}
				for(LocalPlaylist lp : mAudioInfo.getPlaylists()) {
					mAdapter.add(lp);
				}				
				mAdapter.notifyDataSetChanged();
			} else if (msg.what == ADD_ALBUMS) {
				List<LocalAlbum> lalist;
				if (msg.obj instanceof LocalArtist){
					lalist = ((LocalArtist)(msg.obj)).getAlbums();
				} else {
					lalist = ((LocalPlaylist)msg.obj).getAlbums();
				}
				int pos = msg.arg1+1;
				for (LocalAlbum album : lalist) {
					mAdapter.insert(album, pos);
					pos++;
				}
				mAdapter.notifyDataSetChanged();
			} else if (msg.what == DEL_ALBUMS) {
				int pos = msg.arg1;
				List<TrackInfo> list = new ArrayList<TrackInfo>();

				for (int i=pos+1; i<mAdapter.getCount(); i++){
					TrackInfo ti = mAdapter.getItem(i);
					if (ti instanceof LocalArtist ||
							ti instanceof LocalPlaylist) break;
					list.add(ti);
				}
				for (TrackInfo ti : list) {
					mAdapter.remove(ti);
				}
				mAdapter.notifyDataSetChanged();
			} else if (msg.what == ADD_TRACKS) {
				LocalAlbum la = (LocalAlbum)msg.obj;
				int pos = msg.arg1+1;
				for (LocalTrack track : la.getTracks()){
					mAdapter.insert(track, pos);
					pos++;
				}
				mAdapter.notifyDataSetChanged();
			} else if (msg.what == DEL_TRACKS) {
				LocalAlbum la = (LocalAlbum)msg.obj;
				int pos = msg.arg1;
				List<TrackInfo> list = new ArrayList<TrackInfo>();

				for (int i=pos+1; i<mAdapter.getCount(); i++){
					TrackInfo ti = mAdapter.getItem(i);
					if (!(ti instanceof LocalTrack)) break;
					list.add(ti);
				}
				for (TrackInfo ti : list) {
					mAdapter.remove(ti);
				}
				mAdapter.notifyDataSetChanged();
			} else if (msg.what == REFRESH) {
				mAdapter.notifyDataSetChanged();	
			} else if (msg.what == TOAST) {
				Toast.makeText(LocalAudioListActivity.this, (String)msg.obj, 1).show();

			}

		}
	}

	private class ConnectDialog extends ProgressDialog {
		private Radio mRadio;
		private boolean mShowed = true;
		private ConnectDialog(Context context, Radio radio) {
			super(context);
			mRadio = radio;
			// TODO Auto-generated constructor stub
			setProgressStyle(ProgressDialog.STYLE_SPINNER);
			setMessage("fetching list...");
			setTitle(radio.getName());
			setCancelable(true);
		}
		@Override
		public void onBackPressed () {
			//do nothing
		}
		@Override
		public boolean onSearchRequested (){
			return false;
		}
		public void unsetShowFlag(){
			mShowed = false;
		}
		public boolean getShowFlag(){
			return mShowed;
		}
	}



	private static final int MENU_ID_PREFERENCE = (Menu.FIRST + 1);
	private static final int MENU_ID_DONATE = (Menu.FIRST + 2);
	private static final int MENU_ID_EXIT = (Menu.FIRST + 3);


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ID_PREFERENCE, Menu.NONE, "Preferences");
		//menu.add(Menu.NONE, MENU_ID_DONATE, Menu.NONE, "Donate");
		menu.add(Menu.NONE, MENU_ID_EXIT, Menu.NONE, "Exit");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = true;
		Intent intent;

		switch (item.getItemId()) {
		default:
			ret = super.onOptionsItemSelected(item);
			break;
		case MENU_ID_PREFERENCE:
			ret = true;
			intent = new Intent(this, com.hitsuji.radio.pref.Settings.class);
			this.startActivity(intent);
			break;
		case MENU_ID_DONATE:
			ret = true;
			break;   
		case MENU_ID_EXIT:
			ret = true;
			intent = new Intent(this, PlayManager.class);
			intent.setAction(PlayManager.FINISH_ACTION);
			this.startService(intent);
			this.finish();
			break;            
		}
		return ret;
	}

	private final SimpleOnGestureListener onGestureListener = new SimpleOnGestureListener() {
		boolean moveEnable = false;
		float move = 0;
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, 
				float vx, float vy) {
			Log.d(TAG, "onFling vx:"+vx + " vy:"+vy +" move:"+move + " enable:"+moveEnable);
			float m = move;
			boolean enable = moveEnable;
			move = 0;
			moveEnable = true;

			if (enable && m>280) {
				if( mPlayingTitle!=null || mPlayingAlbumId != null ||
					mPlayingArtistId!=null || mPlayingPlaylistId!=null) {
					goNext();
					return true;
				}
			}
			return false;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
			Log.d(TAG, "onScroll dx:"+dx + " dy:"+dy);
			if (dx<=0)
				moveEnable = false;
			move += dx;
			return false;			
		}
		@Override
		public boolean onDown(MotionEvent e){
			move = 0;
			moveEnable = true;
			return super.onDown(e);
		}
	};



}
