/**
 * 
 */

package com.hitsuji.radio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Toast;

import com.andrew.apollo.BottomActionBarFragment;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.adapters.ScrollingTabsAdapter;
import com.andrew.apollo.ui.widgets.ScrollableTabView;
import com.andrew.apollo.ui.widgets.OnSelectCb;
import com.hitsuji.play.Track;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.local.LocalAudioListActivity;
import com.hitsuji.radio.manager.ConnectionListener;
import com.hitsuji.radio.manager.IPlayManagerApi;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.hitsuji.radio.tab.ArtistDescFragment;
import com.hitsuji.radio.tab.JacketFragment;
import com.hitsuji.radio.tab.LyricFragment;
import com.hitsuji.radio.tab.ShoutFragment;
import com.hitsuji.radio.tab.SimilarArtistsFragment;
import com.hitsuji.radio.tab.TabBaseFragment;
import com.util.Log;
import com.util.Util;

/**
 * @author Andrew Neal
 * @Note This is the "holder" for all of the tabs
 */
public class PlayingActivity extends FragmentActivity {
	private static final String TAG = "PlayingActivity";
	private static final int TOAST = 0;
	private static final int TITLE = 1;

	private Context mContext;
	private IPlayManagerCallback mCallback;
	private PlayManagerServiceConnection mConnection;

	
	private boolean mArtistTab;
	private boolean mSimilarArtistsTab;
	private boolean mLyricTab;
	private boolean mJacketTab;
	private boolean mShoutTab;
	private boolean mLowMemory;
	private DisplayHandler mHandler;
	private ViewPager mViewPager;

	private boolean mReinstantiate;
	
	public PlayingActivity(){
		super();
		Log.d(TAG, "initialize :"+this.hashCode());
	}
	
	@Override
	protected void onCreate(Bundle icicle) {
		Log.d(TAG, "oncreate icicle:"+ (icicle==null ? "null" : icicle.getBoolean("reinstantiate", false)));
		// Landscape mode on phone isn't ready
		if (!Util.isTablet(this))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);		
		setProgressBarIndeterminateVisibility(true);
		setProgressBarVisibility(true);
		
		mContext = this;
		mArtistTab = false;
		mJacketTab = false;
		mLyricTab = false;
		mSimilarArtistsTab = false;
		mReinstantiate = icicle==null ? false : icicle.getBoolean("reinstantiate", false);
		
		// Scan for music
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Control Media volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		super.onCreate(icicle);
		
		// Layout
		setContentView(R.layout.library_browser);

		// Important!
		initPager();
		// Update the BottomActionBar
		initBottomActionBar();

		mCallback = new IPlayManagerCallback.Stub() {

			@Override
			public void onFinishedCreatePlayList(int ret, int type, boolean fill) throws RemoteException {
				// TODO Auto-generated method stub
				Fragment f = ((PagerAdapter) mViewPager.getAdapter()).getItem(ScrollingTabsAdapter.SIMILAR);
				if (f != null && f instanceof SimilarArtistsFragment) {
					SimilarArtistsFragment saa = (SimilarArtistsFragment) f;
					saa.onFinishedCreatePlayList(ret, type, fill);
				}
			}

			@Override
			public void onStarted(Track track) throws RemoteException {
				// TODO Auto-generated method stub
				Log.d(TAG, "onStarted:"+(track==null ? "null" : " title:"+track.getTitle() + " artist:"+track.getArtist()));

				setJacketFlag(false);
				setLyricFlag(false);
				setSimilarFlag(false);
				setShoutFlag(false);
				
				if (track == null) return;
				if (getCurrentRadioState() != PlayManager.PREPARED &&
						getCurrentRadioState() != PlayManager.STARTED ) 
					return;
				
				int pos = mViewPager.getCurrentItem();
				Fragment f = ((PagerAdapter) mViewPager.getAdapter()).getItem(pos);

				if (f != null && f instanceof LyricFragment) {
					LyricFragment la = (LyricFragment) ((PagerAdapter) mViewPager.getAdapter()).getItem(ScrollingTabsAdapter.LYRIC);
					if (getLyricFlag()) {
						la.setParent(PlayingActivity.this);
						String ti = getCurrentTitle();
						String ar = getCurrentArtist();
						la.search(ti, ar, "");
						setLyricFlag(true);
					}
				}

				if (f != null && f instanceof ShoutFragment) {
					ShoutFragment sa = (ShoutFragment)  ((PagerAdapter) mViewPager.getAdapter()).getItem(ScrollingTabsAdapter.SHOUTS);
					sa.setParent(PlayingActivity.this);
					if (!getShoutFlag()) {
						sa.initShoutLists(track.getTitle(), track.getArtist());
						setShoutFlag(true);
					}
				}

				Radio.KIND kind = getCurrentRadio();
				if (kind != null && !kind.equals(Radio.KIND.SIMILAR)){
					PlayingActivity.this.updateTitle(kind.Name + " Radio");
				} else if (kind !=null){
					PlayingActivity.this.updateTitle(getSimilarRadioArtist() +" "+kind.Name + " Radio");
				}

				Log.d(TAG, "end onStarted:"+(track==null ? "null" : " title:"+track.getTitle() + " artist:"+track.getArtist()));
			}

			@Override
			public void toast(String msg) throws RemoteException {
				// TODO Auto-generated method stub
				doToasting(msg);
			}

			@Override
			public void onStarting() throws RemoteException {
				// TODO Auto-generated method stub

				Fragment f = ((PagerAdapter) mViewPager.getAdapter()).getItem(ScrollingTabsAdapter.JACKET);
				if (f != null && f instanceof JacketFragment) {
					JacketFragment jtab = (JacketFragment) f;
					//jtab.updateAlbumArt();
					jtab.clearArtwork();

				}
			}

			@Override
			public void onUnbind() throws RemoteException {
				// TODO Auto-generated method stub
				mConnection.unsetBind();
			}

			@Override
			public void onLoadedTrackInfo(Track track) throws RemoteException {
				// TODO Auto-generated method stub
				if (track == null) return;
				if (getCurrentRadioState() != PlayManager.PREPARED &&
						getCurrentRadioState() != PlayManager.STARTED ) 
					return;

				int pos = mViewPager.getCurrentItem();
				Log.d(TAG, "onloadedtrackinfo activity:"+PlayingActivity.this.hashCode() + " pos:"+pos);
				Fragment f = ((PagerAdapter) mViewPager.getAdapter()).getItem(pos);
				if (f != null && f instanceof JacketFragment) {
					JacketFragment jtab = (JacketFragment) f;
					//jtab.updateAlbumArt();
					if (!getJacketFlag()) {
						jtab.setParent(PlayingActivity.this);
						jtab.updateInfo(track.getArtist(), track.getTitle(), getImageUrl(0));
						setJacketFlag(true);
					}
				}

				if (f != null && f instanceof ArtistDescFragment) {
					ArtistDescFragment ada = (ArtistDescFragment) f;
					ada.setParent(PlayingActivity.this);
					ada.setDesc();
				} 

				if (f != null && f instanceof LyricFragment) {
					LyricFragment la = (LyricFragment) f;
					la.setParent(PlayingActivity.this);
					String ti = getCurrentTitle();
					String ar = getCurrentArtist();
					la.search(ti, ar, "");
					setLyricFlag(true);
				}

				if (f != null && f instanceof SimilarArtistsFragment) {
					SimilarArtistsFragment saa = (SimilarArtistsFragment) f;
					saa.setParent(PlayingActivity.this);
					if (!getSimilarFlag()) {
						IPlayManagerApi pm = mConnection.getService();
						try {
							if (pm!=null)
								saa.setList(pm.getSimilarArtistList());
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						setSimilarFlag(true);
					}

				}

				if (f != null && f instanceof ShoutFragment) {
					ShoutFragment sa = (ShoutFragment) f;
					sa.setParent(PlayingActivity.this);
					if (!getShoutFlag()) {
						sa.initShoutLists(track.getTitle(), track.getArtist());
						setShoutFlag(true);
					}
				}
			}

		};
		mConnection = new PlayManagerServiceConnection(mCallback, this, new PlayManagerConnectionListener());

		mHandler = new DisplayHandler();
		Intent intent = new Intent(mContext, PlayManager.class);
		bindService(intent, mConnection, BIND_AUTO_CREATE);
	}


	@Override
	protected void onStart() {
		super.onStart();
		
		mLowMemory = false;
		Log.d(TAG, "viewpager:"+mViewPager);
		Log.d(TAG, "viewpager num:"+mViewPager.getChildCount());

	}
	@Override
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onResume pagerh:"+findViewById(R.id.scrollingTabs).getHeight() + " pagerh:"+findViewById(R.id.viewPager).getHeight());    	    	
	}
	
	@Override
	protected void onStop() {
		// Unbind
		Log.d(TAG, "onStop");
		try {
			if (mConnection.getService()!=null)
				mConnection.getService().unregister(mCallback);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.onStop();
	}
	
	@Override
	public void onDestroy(){
		unbindService(mConnection);
		Log.d(TAG, "ondestroied");
		((PagerAdapter)mViewPager.getAdapter()).clear();
		super.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState){
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onsaveinstancestate");
		outState.putBoolean("reinstantiate", true);
	}
	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(TAG, "onrestoreinstancestate state:"+savedInstanceState.getBoolean("reinstantiate", false));
		if (savedInstanceState.getBoolean("reinstantiate")) 
			this.mReinstantiate = true;
	}
	
	/**
	 * Initiate ViewPager and PagerAdapter
	 */
	public void initPager() {
		if (this.mReinstantiate) {
			Log.d(TAG, "clear fragments");
			TabBaseFragment.clearFragments(getSupportFragmentManager());
		}
		
		// Initiate PagerAdapter		
		final PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());
		
		// Recently added tracks
		Fragment f0 = new JacketFragment();
		Log.d(TAG, "initpager jacketf:"+f0.hashCode());
		pagerAdapter.addFragment(f0);
		// Artists
		pagerAdapter.addFragment(new ArtistDescFragment());
		// Albums
		pagerAdapter.addFragment(new LyricFragment());
		// // Tracks
		pagerAdapter.addFragment(new ShoutFragment());
		// // Playlists
		pagerAdapter.addFragment(new SimilarArtistsFragment());

		// Initiate ViewPager
		mViewPager = (ViewPager)findViewById(R.id.viewPager);
		mViewPager.setPageMargin(getResources().getInteger(R.integer.viewpager_margin_width));
		mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
		mViewPager.setOffscreenPageLimit(pagerAdapter.getCount());
		mViewPager.setAdapter(pagerAdapter);
		mViewPager.setCurrentItem(0);
		Log.d(TAG, "initpager pagerAdapter num:"+pagerAdapter.getCount());
		Fragment f = ((PagerAdapter) mViewPager.getAdapter()).getItem(0);
		Log.d(TAG, "initpager f:"+f.getClass().getSimpleName() + " fid:"+f.hashCode());
		
		// Tabs
		initScrollableTabs(mViewPager);

	}

	/**
	 * Initiate the tabs
	 */
	public void initScrollableTabs(ViewPager viewPager) {
		ScrollableTabView scrollingTabs = (ScrollableTabView)findViewById(R.id.scrollingTabs);
		ScrollingTabsAdapter scrollingTabsAdapter = new ScrollingTabsAdapter(this);
		scrollingTabs.setAdapter(scrollingTabsAdapter);
		scrollingTabs.setViewPager(viewPager);
		scrollingTabs.setOnSelectedCallback(new OnSelectCb(){

			@Override
			public void onSelected(int pos) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onpageselected state:"+getCurrentRadioState() + " pos:"+pos + " artist:"+getCurrentArtist() + " titele:"+getCurrentTitle());
				if (getCurrentRadioState() != PlayManager.PREPARED &&
						getCurrentRadioState() != PlayManager.STARTED ) 
					return;

				PagerAdapter pa = (PagerAdapter) mViewPager.getAdapter();
				Fragment f = pa.getItem(pos);


				if (f != null && f instanceof JacketFragment) {
					JacketFragment jtab = (JacketFragment) f;
					if (!getJacketFlag() ){
						jtab.updateInfo(getCurrentArtist(), getCurrentTitle(), getImageUrl(0));
						setJacketFlag(true);
					}

				} else if (f != null && f instanceof ArtistDescFragment) {
					ArtistDescFragment ada = (ArtistDescFragment) f;
					ada.setParent(PlayingActivity.this);
					ada.setDesc();
				} else if (f != null && f instanceof LyricFragment) {
					LyricFragment la = (LyricFragment) f;
					if (!getLyricFlag()) {
						la.setParent(PlayingActivity.this);
						String ti = getCurrentTitle();
						String ar = getCurrentArtist();
						la.search(ti, ar, "");
						setLyricFlag(true);
					}

				} else if (f != null && f instanceof SimilarArtistsFragment) {
					SimilarArtistsFragment ada = (SimilarArtistsFragment) f;
					ada.setParent(PlayingActivity.this);

					if (!getSimilarFlag()) {
						IPlayManagerApi pm = mConnection.getService();
						try {
							if (pm!=null)
								ada.setList(pm.getSimilarArtistList());
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						setSimilarFlag(true);
					}
				} else if (f != null && f instanceof ShoutFragment) {
					ShoutFragment sa = (ShoutFragment) f;
					if (!getShoutFlag()) {
						sa.setParent(PlayingActivity.this);
						sa.initShoutLists(getCurrentTitle(), getCurrentArtist());
						setShoutFlag(true);
					}
				}

				Fragment la = pa.getItem(2);
				if (la != null && la instanceof LyricFragment) {
					((LyricFragment )la).hideSoftKeyboard();
				}
			}
			
		});
	}

	/**
	 * Initiate the BottomActionBar
	 */
	private void initBottomActionBar() {
		PagerAdapter pagerAdatper = new PagerAdapter(getSupportFragmentManager());
		pagerAdatper.addFragment(new BottomActionBarFragment(this));
		ViewPager viewPager = (ViewPager)findViewById(R.id.bottomActionBarPager);
		viewPager.setAdapter(pagerAdatper);
	}


	public String loadCurrentContent(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? "" : pm.loadCurrentContent();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	public CharSequence getCurrentPosition(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? "" : pm.getCurrentPosition();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
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
	public String getCurrentTitle(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? "" : pm.getCurrentTitle();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	public String[] getImageUrl(int idx){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? null : pm.getImageUrl(idx);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	public Radio.KIND getCurrentRadio(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? null : Radio.getKind( pm.getCurrentRadio() );
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	public int getCurrentRadioState(){
		IPlayManagerApi pm = mConnection.getService();
		try {
			return pm==null ? 0 : pm.getCurrentRadioState();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
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
	public void setCurrentTab(int t) {
		mViewPager.setCurrentItem(t);
	}

	@Override
	public void onLowMemory(){
		this.mLowMemory = true;
	}
	public boolean isLowMemory(){
		return mLowMemory;
	}
	private void doToasting(String msg) {
		mHandler.sendMessage(mHandler.obtainMessage(TOAST, msg));
	}

	private void updateTitle(String msg) {
		mHandler.sendMessage(mHandler.obtainMessage(TITLE, msg));
	}
	private synchronized boolean getJacketFlag(){
		return mJacketTab;
	}
	private synchronized void setJacketFlag(boolean flag){
		mJacketTab = flag;
	}
	private synchronized boolean getLyricFlag(){
		return mLyricTab;
	}
	private synchronized void setLyricFlag(boolean flag){
		mLyricTab = flag;
	}
	private synchronized boolean getSimilarFlag(){
		return mSimilarArtistsTab;
	}
	private synchronized void setSimilarFlag(boolean flag){
		this.mSimilarArtistsTab = flag;
	}
	private synchronized boolean getShoutFlag(){
		return mShoutTab;
	}
	private synchronized void setShoutFlag(boolean flag){
		this.mShoutTab = flag;
	}

	public void goBack(){
		Log.d(TAG, "do goback");
		Intent intent = null;

		Radio.KIND kind = getCurrentRadio();
		intent = new Intent(this, LocalAudioListActivity.class);
		
		if (kind !=null && kind.equals(Radio.KIND.LOCAL)){
			intent = new Intent(this, LocalAudioListActivity.class);
		} else if (kind != null){
			intent = new Intent(this, RadioListActivity.class);
		}
		if (intent != null) {
			startActivity(intent);
			this.finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(this.getClass().getName(), "keydown code:"+keyCode);
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Log.d(this.getClass().getName(), "back button pressed");
			PagerAdapter pa = (PagerAdapter) mViewPager.getAdapter();
			if (pa.getItem(mViewPager.getCurrentItem()) instanceof LyricFragment){
				Log.d(this.getClass().getName(), "goback lyric fragment");
				LyricFragment lf = (LyricFragment)pa.getItem(mViewPager.getCurrentItem());
				if (lf.goBack()) {
					return true;
				} else {
					Log.d(this.getClass().getName(), "fail to goback webview");
					goBack();
				}		
			} else {
				goBack();				
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private class DisplayHandler extends Handler{

		@Override
		public void handleMessage(Message msg){
			if (msg.what == TOAST && msg.obj != null && msg.obj instanceof String){
				Toast.makeText(PlayingActivity.this, (String)msg.obj, 1).show();
			} else if (msg.what == TITLE && msg.obj != null && msg.obj instanceof String){
				PlayingActivity.this.setTitle((String)msg.obj);
			}
		}
	}

	private class PlayManagerConnectionListener implements ConnectionListener{

		@Override
		public void onConnected() {
			// TODO Auto-generated method stub
			PagerAdapter pa = (PagerAdapter) mViewPager.getAdapter();
			Fragment f = pa.getItem(0);
			if (f != null && f instanceof JacketFragment) {
				JacketFragment jf = (JacketFragment) f;
				int state = getCurrentRadioState();
				if (state == PlayManager.STARTED) {
					//jf.updateInfo(getCurrentArtist(), getCurrentTitle(), getImageUrl(0));
				}
			}
			Radio.KIND kind = getCurrentRadio();
			if (kind !=null && kind.equals(Radio.KIND.SIMILAR)){
				PlayingActivity.this.updateTitle( getSimilarRadioArtist()+" "+kind.Name + " Radio");
			} else if (kind != null){
				PlayingActivity.this.updateTitle(kind.Name + " Radio");
			}
		}

		@Override
		public void onDisconnected() {
			// TODO Auto-generated method stub

		}

	}
	

	private static final int MENU_ID_BACK = (Menu.FIRST + 1);

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ID_BACK, Menu.NONE, "Back");
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
		case MENU_ID_BACK:
			ret = true;
			intent = new Intent(this, LocalAudioListActivity.class);
			//intent = new Intent(this, RadioListActivity.class);
			startActivity(intent);
			break;
		}
		return ret;
	}

}
