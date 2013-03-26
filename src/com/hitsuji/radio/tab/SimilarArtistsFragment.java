package com.hitsuji.radio.tab;

import java.io.File;
import java.util.List;

import com.hitsuji.radio.R;
import com.hitsuji.radio.R.layout;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.play.Track;
import com.hitsuji.radio.Auth;
import com.hitsuji.radio.PlayingActivity;
import com.hitsuji.radio.RadioListActivity;
import com.hitsuji.radio.imp.Library;
import com.hitsuji.radio.imp.Mix;
import com.hitsuji.radio.imp.Neighbour;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.imp.Recommend;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.util.Log;
import com.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SimilarArtistsFragment extends TabBaseFragment {

	private static final String TAG = SimilarArtistsFragment.class.getSimpleName();

	public static int LIST_SETTING = 0;
	public static int SIMILAR_RADIO_SETTING = 1;
	public static final int EMPTY_ID = 101;
	
	private ConnectDialog mProgressd; 
	private Exec mHandler;
	private ListView mListView;
	private ArrayAdapter<Bundle> mAdapter;
	
	public SimilarArtistsFragment() {
		super();
	}
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.d(TAG, "oncreate");
		mHandler = new Exec();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
		super.onCreateView(inflater, container, icicle);
		Log.d(TAG, "oncreateview");
		mBody = (RelativeLayout) new RelativeLayout(mParent);
		
		mEmpty = (LinearLayout) new LinearLayout(mParent);
		mEmpty.setId(EMPTY_ID);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT,
				mParent.getResources().getDimensionPixelSize(R.dimen.bottom_action_bar_height));
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mBody.addView(mEmpty, params);
		
		mListView = new ListView(mParent);
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.ABOVE, EMPTY_ID);
		mListView = new ListView(mParent);
		mBody.addView(mListView, params);
		
		mAdapter = new IntentItemArrayAdapter(mParent,  R.layout.similar_artists_list_raw);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListView listView = (ListView) parent;
				Bundle bundle = (Bundle) listView.getItemAtPosition(position);
				String similar = bundle.getString(Track.SIMILAR_NAME_KEY);
				boolean b = true;
				if (!Util.VALID_RADIO)return;

				synchronized (SimilarArtistsFragment.this) {
					mProgressd = new ConnectDialog(mParent, similar);
					mProgressd.show();
				}

				Intent intent = new Intent(mParent, PlayManager.class);
				intent.setAction(PlayManager.STOP_ACTION);
				mParent.startService(intent);

				intent = new Intent(mParent, PlayManager.class);
				intent.setAction(PlayManager.CREATE_PLAYLIST_ACTION);
				intent.putExtra(Radio.TYPE, Radio.KIND.SIMILAR.Type);
				intent.putExtra(Radio.SIMILAR_ARTIST_KEY, similar);
				mParent.startService(intent);

				intent = new Intent(mParent, PlayManager.class);
				intent.setAction(PlayManager.PLAY_ACTION);
				intent.putExtra(Radio.TYPE, Radio.KIND.SIMILAR.Type);
				intent.putExtra(Radio.SIMILAR_ARTIST_KEY, similar);
				mParent.startService(intent);           

			}
		});
		return mBody;
	}

	@Override
	public void onResume(){
		super.onResume();
		synchronized (this){
			if (mProgressd!=null && mProgressd.getShowFlag())mProgressd.dismiss();
		}
	}


	public void setList(Bundle list){
		Message msg = mHandler.obtainMessage();
		msg.what = LIST_SETTING;
		msg.obj = list;
		mHandler.sendMessage(msg);
	}

	class Exec extends Handler{


		@Override
		public void handleMessage(Message msg){
			if (msg.what == LIST_SETTING) {
				if (msg.obj instanceof Bundle) {
					Bundle list = (Bundle)msg.obj;
					setList(list);
				}
			} else if (msg.what == SIMILAR_RADIO_SETTING) {
				setSimilarRadio(msg.arg1, msg.arg2);
			}
		}

		private void setSimilarRadio (int ret, int type){
			if (ret == 0) {
				if (mParent != null) {
					mParent.setCurrentTab(0);
				}

			}
			if (mProgressd!=null) {
				mProgressd.dismiss();
			}
		}

		private void setList(Bundle list){
	        List<String> names = list.getStringArrayList(Track.SIMILAR_NAME_KEY);
	        List<Integer> matches = list.getIntegerArrayList(Track.SIMILAR_MATCH_KEY);
	        mAdapter.clear();
	        for (int i=0; i<names.size(); i++) {
	        	Bundle item = new Bundle();
	        	item.putString(Track.SIMILAR_NAME_KEY, names.get(i));
	        	item.putInt(Track.SIMILAR_MATCH_KEY, matches.get(i));
	        	item.putInt(Track.SIMILAR_INDEX_KEY, i);
	        	mAdapter.add(item);
	        }
	        mListView.setAdapter(mAdapter);
	        mAdapter.notifyDataSetChanged();
		}

	}

	public void onFinishedCreatePlayList(int ret, int type, boolean fill) {
		if (!fill) {
			Message msg = mHandler.obtainMessage();
			msg.what = SIMILAR_RADIO_SETTING;
			msg.arg1 = ret;
			msg.arg2 = type;
			mHandler.sendMessage(msg);
		}
	}
	private class ConnectDialog extends ProgressDialog {
		private String mName;
		private boolean mShowed = true;
		private ConnectDialog(Context context, String name) {
			super(context);
			mName = name;
			// TODO Auto-generated constructor stub
			setProgressStyle(ProgressDialog.STYLE_SPINNER);
			setMessage("fetching list...");
			setTitle(name);
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
	

	private class IntentItemArrayAdapter extends ArrayAdapter<Bundle> {
		private int resourceId;

		public IntentItemArrayAdapter(Context context, int resourceId) {
			super(context, resourceId);
			this.resourceId = resourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Bundle bundle = (Bundle) getItem(position);
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(resourceId, null);
			}
			TextView name = (TextView) convertView.findViewById(R.id.similar_artist_name);
			name.setText(bundle.getString(Track.SIMILAR_NAME_KEY));

			TextView match = (TextView) convertView.findViewById(R.id.similar_artist_match);
			match.setText(String.format("match:%d", bundle.getInt(Track.SIMILAR_MATCH_KEY)));

			int idx = bundle.getInt(Track.SIMILAR_INDEX_KEY);
			ImageView image = (ImageView) convertView.findViewById(R.id.similar_artist_img);
			Bitmap bitmap = BitmapFactory.decodeFile(
					mParent.getFilesDir().getAbsolutePath() + File.separator +
					Track.SIMILAR_IMG_DIR + File.separator + idx);
			if (bitmap!=null){
				image.setImageBitmap(bitmap);

				if (bitmap.getHeight() < 35){
					match.setHeight(bitmap.getHeight()/2);
					name.setHeight(bitmap.getHeight()/2);
				}
			}

			return convertView;
		}
	}
}
