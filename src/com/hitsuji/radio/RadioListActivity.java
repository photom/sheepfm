package com.hitsuji.radio;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hitsuji.radio.R;
import com.hitsuji.radio.manager.IPlayManagerApi;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.play.Track;
import com.hitsuji.radio.imp.Friend;
import com.hitsuji.radio.imp.Library;
import com.hitsuji.radio.imp.Mix;
import com.hitsuji.radio.imp.Neighbour;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.imp.Recommend;
import com.hitsuji.radio.imp.Similar;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.hitsuji.radio.pref.AccountDialogPreference;
import com.util.Log;
import com.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class RadioListActivity extends Activity {
	private static final String TAG = RadioListActivity.class.getSimpleName();

	private ConnectDialog mProgressd; 
	private Context mContext;

	private PlayManagerServiceConnection mConnection;
	private IPlayManagerCallback mCallback;
	private ListView mListView;

	private Handler mHandler;
	private ArrayAdapter<Radio> mAdapter;

	private class IntentItemArrayAdapter extends ArrayAdapter<Radio> {
		private int resourceId;

		public IntentItemArrayAdapter(Context context, int resourceId) {
			super(context, resourceId);
			this.resourceId = resourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Radio radio = (Radio) getItem(position);
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(resourceId, null);
			}

			int type = getCurrentRadio();
			TextView tv = (TextView) convertView.findViewById(R.id.title);
			ImageView iv = (ImageView)convertView.findViewById(R.id.playing);

			if (radio instanceof Similar) {
				Similar s = (Similar)radio;
				String playingArtist = getCurrentArtist();
				String radioArtist = getSimilarRadioArtist();

				if (s.getSimilarRadioType() == Similar.CURRENT_TRACK) {
					if (playingArtist == null || playingArtist.length() == 0) {
						convertView.setVisibility(View.INVISIBLE);
						return convertView;
					}

					((Similar) radio).setArtist(playingArtist);
					convertView.setVisibility(View.VISIBLE);
					tv.setText(playingArtist+" Similar Radio");            			

					if (radioArtist!=null && playingArtist.equals(radioArtist)) {
						Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.radio);
						iv.setImageBitmap(bitmap);
					} else {
						iv.setImageDrawable(null);
					}
					return convertView;
				} else if (s.getSimilarRadioType() == Similar.CURRENT_RADIO) {
					if (type != Radio.KIND.SIMILAR.Type || 
							radioArtist==null || radioArtist.length()==0){
						convertView.setVisibility(View.INVISIBLE);
						return convertView;
					}
					if (playingArtist!=null && radioArtist.equals(playingArtist)){
						convertView.setVisibility(View.INVISIBLE);
						return convertView;
					}
					Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.radio);
					iv.setImageBitmap(bitmap);            		
					((Similar) radio).setArtist(radioArtist);
					convertView.setVisibility(View.VISIBLE);
					tv.setText(radioArtist+" Similar Radio");   
				} else {
					Log.e(TAG, "internal error. invalid similar radio type:" + s.getSimilarRadioType());
					return convertView;
				}

			} else {
				tv.setText(radio.getName());
				if (type == radio.getType()) {
					Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.radio);
					iv.setImageBitmap(bitmap);
				} else {
					iv.setImageDrawable(null);
				}
				convertView.setVisibility(View.VISIBLE);
			}
			return convertView;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		mListView = new ListView(this);
		setContentView(R.layout.radio_list);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		RelativeLayout layout = (RelativeLayout)this.findViewById(R.id.radio_list);
		layout.addView(mListView, params);

		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListView listView = (ListView) parent;
				Radio radio = (Radio) listView.getItemAtPosition(position);
				int current = getCurrentRadio();

				if (radio instanceof Similar) {
					Similar s = (Similar)radio;
					String artist = s.getArtist();
					if (artist == null || artist.length()==0) return;

					if (current != Radio.KIND.SIMILAR.Type) {
						createPlayList(radio, artist);
						return;
					}

					if (s.getSimilarRadioType() == Similar.CURRENT_TRACK) {
						String radioArtist = getSimilarRadioArtist();
						String playingArtist = getCurrentArtist();
						if (playingArtist!=null && radioArtist!=null && 
								playingArtist.length()>0 && radioArtist.length()>0&&
								playingArtist.equals(radioArtist)){
							goNext(current);
						} else {
							createPlayList(radio, artist);
						}	
					} else if (s.getSimilarRadioType() == Similar.CURRENT_RADIO) {
						goNext(current);
						return;
					} else {
						Log.e(TAG, "internal error. invalid similar radio type:" + s.getSimilarRadioType());
						return;
					}
				} else if (radio.getType() != current) {
					createPlayList(radio, null);
					return;
				} else {
					goNext(current);
					return;
				}
			}

			private void createPlayList(Radio radio, String similar){
				synchronized (RadioListActivity.this){
					mProgressd = new ConnectDialog(RadioListActivity.this, radio);
					mProgressd.setOnDismissListener(new OnDismissListener(){

						@Override
						public void onDismiss(DialogInterface dialog) {
							// TODO Auto-generated method stub
							mProgressd.unsetShowFlag();
						}

					});
					mProgressd.show();
				}

				Intent intent = new Intent(RadioListActivity.this, PlayManager.class);
				intent.setAction(PlayManager.STOP_ACTION);
				RadioListActivity.this.startService(intent);

				intent = new Intent(RadioListActivity.this, PlayManager.class);
				intent.setAction(PlayManager.CREATE_PLAYLIST_ACTION);
				intent.putExtra(Radio.TYPE, radio.getType());
				if (similar!=null)intent.putExtra(Radio.SIMILAR_ARTIST_KEY, similar);
				RadioListActivity.this.startService(intent);

				intent = new Intent(RadioListActivity.this, PlayManager.class);
				intent.setAction(PlayManager.PLAY_ACTION);
				intent.putExtra(Radio.TYPE, radio.getType());
				startService(intent);
			}

			private void goNext(int type){
				Intent intent = new Intent( mContext, PlayingActivity.class );
				intent.putExtra(Radio.TYPE, type);
				startActivity(intent);
			}
		});



		mCallback = new IPlayManagerCallback.Stub() {

			@Override
			public void onFinishedCreatePlayList(int ret, int type, boolean fill) throws RemoteException {
				// TODO Auto-generated method stub
				Log.d(TAG, "onfinishedcreateplaylist");
				if (!fill) {
					if (ret == 0) {
						Intent intent = new Intent( mContext, PlayingActivity.class );
						intent.putExtra(Radio.TYPE, type);
						startActivity(intent);
					}
					if (mProgressd != null) mProgressd.dismiss();					
				}
			}

			@Override
			public void onStarted(Track track) throws RemoteException {
				// TODO Auto-generated method stub
				mHandler.post(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						mAdapter.notifyDataSetChanged();		
					}

				});
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
		mConnection = new PlayManagerServiceConnection(mCallback, this, null);
		mHandler = new Handler();

		setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));

		ImageView logo = (ImageView)this.findViewById(R.id.logo2);
		logo.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String name = Util.getName(RadioListActivity.this);
				String url = "http://m.last.fm/"+(name!=null? "user/"+URLEncoder.encode(name) : "");
				Log.d(TAG, "onartworkclicklistener url:" + url);
				Uri uri = Uri.parse(url);
				Intent i = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(i); 
			}

		});

		mAdapter = new IntentItemArrayAdapter(this,  R.layout.radio_list_raw);
		String appPath = this.getFilesDir().getAbsolutePath();
		mAdapter.add(new Library(Util.getName(this)));
		mAdapter.add(new Mix(Util.getName(this)));
		mAdapter.add(new Recommend(Util.getName(this)));
		mAdapter.add(new Neighbour(Util.getName(this)));
		mAdapter.add(new Friend(Util.getName(this)));
		mAdapter.add(new Similar(Util.getName(this), Similar.CURRENT_TRACK));        
		mAdapter.add(new Similar(Util.getName(this), Similar.CURRENT_RADIO));
		mListView.setAdapter(mAdapter);


		if (!Util.VALID_RADIO) finish();
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

		Intent intent = new Intent(mContext, PlayManager.class);
		bindService(intent, mConnection, BIND_AUTO_CREATE);

		if (Auth.getSessionkey(this.getFilesDir().getAbsolutePath()) == null){
			intent = new Intent(this, HitsujiRadioMobileActivity.class);
			startActivity(intent);
		} 
	}

	@Override
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onresume");
		mHandler.post(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mAdapter.notifyDataSetChanged();		
			}

		});
		synchronized (this){
			if (mProgressd!=null && mProgressd.getShowFlag())mProgressd.dismiss();
		}
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
		Log.d(TAG, "onDestroied");
		super.onDestroy();
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
		menu.add(Menu.NONE, MENU_ID_DONATE, Menu.NONE, "Donate");
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
			intent = new Intent(RadioListActivity.this, com.hitsuji.radio.pref.Settings.class);
			RadioListActivity.this.startActivity(intent);
			break;
		case MENU_ID_DONATE:
			ret = true;
			intent = new Intent(RadioListActivity.this, HitsujiRadioDonateActivity.class);
			RadioListActivity.this.startActivity(intent);
			break;   
		case MENU_ID_EXIT:
			ret = true;
			intent = new Intent(RadioListActivity.this, PlayManager.class);
			intent.setAction(PlayManager.FINISH_ACTION);
			RadioListActivity.this.startService(intent);
			RadioListActivity.this.finish();
			break;            
		}
		return ret;
	}

	private int getCurrentRadio(){
		int current = -1;
		try {
			if (mConnection.getService()!=null) {
				current = mConnection.getService().getCurrentRadio();
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		return current;
	}
	private int getCurrentRadioState(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? 0 : pm.getCurrentRadioState();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}
	private void doToasting(String msg) {
		mHandler.post(new ToastRunner(msg));
	}


	private class ToastRunner implements Runnable{
		private String mMsg;
		private ToastRunner(String msg){
			mMsg = msg;
		}
		@Override
		public void run(){
			Toast.makeText(RadioListActivity.this, mMsg, 1).show();
		}
	}
	public String getCurrentArtist(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? "" : pm.getCurrentArtist();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	public String getSimilarRadioArtist(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? "" : pm.getSimilarRadioArtist();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
}