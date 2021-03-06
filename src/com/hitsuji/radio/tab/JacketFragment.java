package com.hitsuji.radio.tab;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.HitsujiApplication;
import com.devsmart.android.ui.HorizontalListView;
import com.hitsuji.radio.PlayingActivity;
import com.hitsuji.radio.R;
import com.hitsuji.radio.R.layout;
import com.hitsuji.radio.manager.IPlayManagerApi;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.play.Track;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.util.Log;
import com.util.Util;
import com.view.GifView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.Layout.Alignment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class JacketFragment extends TabBaseFragment {
	private static final String TAG = JacketFragment.class.getSimpleName();
	private static final int ARTIST_ID = 1;
	private static final int TITLE_ID = 2;
	private static final int TIME_ID = 3;
	private static final int ART_ID = 4;
	private static final int EMPTY_ID = 5;
	private static final int INIT_IMG_VIEW = 0;
	private static final int UPDATE_IMG_VIEW = 1;
	private static final int CLEAR_IMG_VIEW = 2;


	private ImageView mAlbumArt;
	private TextView mArtist;
	//private TextView mAlbum;
	private TextView mTitle;
	private TextView mTime;
	private ListView mImageViews;

	private String mTitleStr;
	private String mArtistStr;
	private String mTimeStr;

	private String mThumbnailUrl;
	private String mSourceUrl;

	private Handler mUpdateTextHandler;
	private UpdateImageHandler mUpdateImageHandler;
	private HandlerThread mWorkerThread;
	private Handler mWorkerHandler;

	private ViewThread mLooper;

	private int mWidth;
	private int mHeight;
	private int mCounter;
	private List<Integer> mCachedImageIndexArr;
	private ImageViewAdapter mAdapter;

	public JacketFragment() {
		super();
	}

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mCounter = 0;

		mWorkerThread = new HandlerThread(TAG);
		mWorkerThread.start();
		mWorkerHandler = new Handler(mWorkerThread.getLooper());

		mUpdateTextHandler = new Handler();
		mUpdateImageHandler = new UpdateImageHandler();
		mCachedImageIndexArr = new ArrayList<Integer>();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {

		mBody = new RelativeLayout(mParent);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		//container.addView(mBody, params);

		mAdapter = new ImageViewAdapter(mParent, R.layout.jacket_image_list_raw, new ArrayList<String[]>());
		mImageViews = new ListView(mParent);
		mImageViews.setAdapter(mAdapter);
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mBody.addView(mImageViews, 0);  

		mEmpty = (LinearLayout)inflater.inflate(R.layout.empty_view, null);
		mEmpty.setId(EMPTY_ID);		
		params = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 
				mParent.getResources().getDimensionPixelSize(R.dimen.bottom_action_bar_height));
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);		
		mBody.addView(mEmpty, params);


		mTime = new TextView(mParent);
		mTime.setId(TIME_ID);
		mTime.setGravity(Gravity.RIGHT);
		//mTime.setGravity(Gravity.CENTER);
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ABOVE, mEmpty.getId());
		mBody.addView(mTime, params);

		mTitle = new TextView(mParent);
		mTitle.setId(TITLE_ID);
		mTitle.setGravity(Gravity.RIGHT);
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ABOVE, mTime.getId());
		mBody.addView(mTitle, params);

		mArtist = new TextView(mParent);
		mArtist.setId(ARTIST_ID);
		mArtist.setGravity(Gravity.RIGHT);
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ABOVE, mTitle.getId());
		mBody.addView(mArtist, params);

		return mBody;
	}

	public void updateInfo(String artist, String title, String[] urls){
		synchronized (this) {
			mArtistStr = artist;
			mTitleStr = title;
		}

		if (mUpdateImageHandler!=null) {
			Message msg = mUpdateImageHandler.obtainMessage(INIT_IMG_VIEW, urls);
			mUpdateImageHandler.sendMessage(msg);
		} else {
			Log.e(TAG, "updateinfo: update image handler is null" + " self:"+this.hashCode());
		}

	}

	@Override
	public void onStart(){
		super.onStart();
		
		if (mParent==null){
			Log.e(TAG, "parent is null");
			return;
		}
		int state = mParent.getCurrentRadioState();
		String artist = mParent.getCurrentArtist();
		String title = mParent.getCurrentTitle();
		String[] url = mParent.getImageUrl(0);
		Log.d(TAG, "onstart state:"+state + " artist:"+artist + " title:"+title + " url:"+url);
		if (state == PlayManager.STARTED &&
			artist !=null && title!=null) {
			if (url!=null && url.length==2)
				updateInfo(artist, title, url);
			else if(Util.isImageCached(mParent) && Util.isPoorNetwork(mParent))
				updateInfo(artist, title, new String[2]);
			
		}
	}
	
	@Override
	public void onResume(){
		super.onResume();
		mLooper = new ViewThread();
		mLooper.start();
	}

	@Override
	public void onPause(){
		mLooper.unsetLoop();
		super.onPause();
	}

	@Override
	public void onDestroy(){
		mWorkerThread.quit();
		super.onDestroy();
	}

	@Override
	public void onDetach(){
		super.onDetach();
	}

	class ViewThread extends Thread {
		boolean mLoop;
		Bitmap mBitmap;

		public ViewThread(){
			super();
			mLoop = true;    		
		}

		public void unsetLoop(){
			synchronized(JacketFragment.this) {
				mLoop = false;
			}
		}
		public boolean loop(){
			synchronized(JacketFragment.this) {
				return mLoop;
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

			while(loop()) {
				mUpdateTextHandler.post(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						synchronized(JacketFragment.this) {
							mTitle.setText(mParent.getCurrentTitle());
							mArtist.setText(mParent.getCurrentArtist());
							if (mParent!=null)
								mTime.setText(mParent.getCurrentPosition());
						}
					}

				});
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				mCounter = (mCounter + 1) % 60;
			}
		}

	}

	public class UpdateImageHandler extends Handler{

		public UpdateImageHandler(){
			super();
		}
		public UpdateImageHandler(Looper l){
			super(l);
		}
		
		@Override
		public void handleMessage(Message msg){
			if (msg.what == JacketFragment.INIT_IMG_VIEW) {
				Log.d(TAG, "initialize jacket view");
				initCachedImageIdx();
				
				RelativeLayout parent = mBody;
				Log.d(TAG, "clear images w:"+ mImageViews.getWidth() + " h:"+mImageViews.getHeight());
				int width = parent.getWidth();
				int height = parent.getHeight()-3 - mArtist.getHeight() - mTitle.getHeight() - mTime.getHeight() -mEmpty.getHeight();
				if (width > 0 && height > 0) {
					mImageViews.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
				}
				mAdapter.clear();
				
				if (msg.obj != null && msg.obj instanceof String[]){
					String[] urls = (String[])msg.obj;
					mAdapter.add(urls);
					mImageViews.invalidate();
				} else if( Util.isImageCached(mParent) && Util.isPoorNetwork(mParent)){
					mAdapter.add(new String[2]);
					mImageViews.invalidate();
				} else
					Log.d(TAG, "url is null");
			} else if (msg.what == JacketFragment.UPDATE_IMG_VIEW) {

			} else if (msg.what == JacketFragment.CLEAR_IMG_VIEW) {
				mAdapter.clear();
			}
		}

	}


	private void initCachedImageIdx(){
		mCachedImageIndexArr.clear();
		if (mArtistStr==null)
			return;
		File dir = mParent.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		File artistDir = Track.getArtistImageDir(dir, mArtistStr);
		if (artistDir == null || !artistDir.exists() || !artistDir.isDirectory())
			return;
		
		for(int i=0; i<artistDir.list().length; i++)
			mCachedImageIndexArr.add(i);

		Collections.shuffle(mCachedImageIndexArr);
		Log.d(TAG, "shuffle idx");
		
		if (!(Util.isImageCached(mParent) && Util.isPoorNetwork(mParent))) {
			for (File f : artistDir.listFiles()) {
				f.delete();
			}
		}
				
	}
	
	private File getTargetImage(int idx){
		Integer pos = null;
		Log.d(TAG, "gettargetimage idx:"+idx + "idxarrsize:"+mCachedImageIndexArr.size());
		if (Util.isImageCached(mParent) && Util.isPoorNetwork(mParent))
			pos = idx<mCachedImageIndexArr.size() ? mCachedImageIndexArr.get(idx) : null;
		else
			pos = idx;
		
		Log.d(TAG, "gettargetimage pos:"+pos + " artiststr:"+mArtistStr);
		if (pos == null || mArtistStr==null || mArtistStr.length()==0)
			return null;
		
		File dir = Track.getArtistImageDir(mParent.getExternalFilesDir(Environment.DIRECTORY_PICTURES), mArtistStr);
		Log.d(TAG, "gettargetimage dir:"+dir.toString());
		if (dir==null || !dir.exists())
			return null;

		File f = new File(dir, String.valueOf(pos));
		Log.d(TAG, "loaded artist image:"+f.getAbsolutePath());
		if (f.exists())
			return f;
		else
			return null;
	}
	private int getTargetImageNum(){
		File dir = Track.getArtistImageDir(mParent.getExternalFilesDir(Environment.DIRECTORY_PICTURES), mArtistStr);
		Log.d(TAG, "gettargetimage dir:"+dir.toString());
		if (dir==null || !dir.exists())
			return 0;
		else
			return dir.listFiles().length;
	}
	

	public class ImageViewAdapter extends ArrayAdapter<String[]>{
		private List<String[]> items;
		private LayoutInflater inflater;

		public ImageViewAdapter(Context context, int resourceId, List<String[]> list) {
			super(context, resourceId, list);
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			items = list;
		}


		@Override
		public View getView(int pos, View convertView, ViewGroup parent){
			final String[] urls = (String[])items.get(pos);
			Log.d(TAG, "getView pos:"+pos + " url:"+((urls!=null&&urls.length>2)?urls[0]:null) + " memptyh:"+mEmpty.getHeight());
			View v = convertView;
			if(v == null){
				v = inflater.inflate(R.layout.jacket_image_list_raw, null);
			}  
			//ImageView image = (ImageView)v.findViewById(R.id.jacket_image);
			LinearLayout layout = (LinearLayout)v.findViewById(R.id.jacket_layout);

			File cfile = getTargetImage(pos);
			if (cfile!=null && cfile.exists()) {
				ImageView image = new ImageView (mParent);
				Bitmap cbitmap = BitmapFactory.decodeFile(cfile.getAbsolutePath());
				image.setImageBitmap(cbitmap);
				
				if (urls[0]!=null) {
					new LoadBitmap(pos, urls[0]).loadNext();
					image.setOnClickListener(new OnArtworkClickListener(urls[1]));
				}
				else if ((Util.isImageCached(mParent) && Util.isPoorNetwork(mParent))
						&& pos<getTargetImageNum() && pos==mAdapter.getCount()-1)
					mAdapter.add(new String[2]);

				layout.removeAllViews();
				layout.addView(image);

			} else if (!(Util.isImageCached(mParent) && Util.isPoorNetwork(mParent))){
				mWorkerHandler.post( new LoadBitmap(pos, urls[0]) );

				RelativeLayout parentLayout = mBody;
				int width = parentLayout.getWidth()+3;
				int height = parentLayout.getHeight();
				if (width <=0 || height <=0 ) {
					Log.e(TAG, "invalid disp param width:"+width + " height:"+height);
					return null;
				}
				GifView image = new GifView(mParent, null) ;
				image.setResouceId(R.drawable.loading);
				height -= (mArtist.getHeight() + mTitle.getHeight() + mTime.getHeight() + mEmpty.getHeight());
				//image.setImageBitmap(Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565));
				image.setLayoutParams(new LinearLayout.LayoutParams(width, height));
				layout.removeAllViews();
				layout.addView(image);
				layout.setGravity(Gravity.CENTER_HORIZONTAL);
			}

			return v;  
		}


	}

	public class OnArtworkClickListener implements OnClickListener{
		private String mUri;
		private OnArtworkClickListener(String uri) {
			mUri = uri;
		}
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onartworkclicklistener url:"+mUri);
			Uri uri = Uri.parse(mUri);
			Intent i = new Intent(Intent.ACTION_VIEW,uri);
			startActivity(i); 
		}

	}

	private class LoadBitmap extends Thread {
		private int mIdx;
		private File mCurrent;
		private String mUrl;

		private LoadBitmap(int idx, String url) {
			mIdx = idx;
			mUrl = url;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, "load bitmap idx:"+mIdx + " url:"+mUrl);
			mCurrent = getJacketBitmap(mIdx, mUrl);

			mUpdateImageHandler.post(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					if (mCurrent!=null)mAdapter.notifyDataSetChanged();
					loadNext();
				}

			});
		}

		private File getJacketBitmap(int pos, String url) {
			RelativeLayout layout = mBody;
			int width = layout.getWidth();
			int height = layout.getHeight();
			if (width <=0 || height <=0 ) {
				Log.e(TAG, "invalid disp param width:"+width + " height:"+height);
				return null;
			}

			File artistDir = Track.getArtistImageDir(mParent.getExternalFilesDir(Environment.DIRECTORY_PICTURES), mArtistStr);
			if (artistDir==null)
				return null;
			
			height -= (mArtist.getHeight() + mTitle.getHeight() + mTime.getHeight() + mEmpty.getHeight());
			Track.loadImage(artistDir.getAbsolutePath(), pos, width, height, url);

			File file = new File(artistDir, String.valueOf(pos));
			if (file.exists())
				return file;
			else 
				return null;
		}


		private void loadNext(){
			if (mParent.isLowMemory()) return;
			if (mIdx == mAdapter.items.size()-1) {
				String[] urls = mParent.getImageUrl(mIdx+1);
				if (urls!=null) mAdapter.add(urls);
			}
		}

	}
	
	public void clearArtwork() {
		// TODO Auto-generated method stub
		if (mUpdateImageHandler!=null)
			mUpdateImageHandler.sendMessage(mUpdateImageHandler.obtainMessage(CLEAR_IMG_VIEW));
		else 
			Log.e(TAG, "clearartwork: imagehandler is null" + " self:"+this.hashCode());
	}
}
