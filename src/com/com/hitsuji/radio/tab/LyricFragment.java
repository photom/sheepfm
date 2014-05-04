package com.hitsuji.radio.tab;

import java.net.URLEncoder;

import com.hitsuji.radio.PlayingActivity;
import com.hitsuji.radio.R;
import com.hitsuji.radio.R.layout;
import com.hitsuji.radio.RadioListActivity;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.local.LocalAudioListActivity;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.play.Track;
import com.util.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class LyricFragment extends TabBaseFragment {

	private static final String TAG = LyricFragment.class.getSimpleName();
	
	private static final String UA = "Mozilla/5.0 (Linux; U; Android 1.1; en-gb; dream) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2 – G1 Phone";
	private WebView mWebView;
	private Handler mHandler;

	private String mArtist;
	private String mTitle;
	private String mAlbum;
	private Boolean mNewPage = false;
	private ProgressBar mProgressBar;
	
	public LyricFragment() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.d(TAG, "oncreate");
		mHandler = new Handler();		
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
		super.onCreateView(inflater, container, icicle);
		Log.d(TAG, "oncreateview");
		mBody = (RelativeLayout) inflater.inflate(R.layout.lyric_fragment, null);
		mBody.setBackgroundColor(Color.BLACK);
		mEmpty = (LinearLayout)mBody.findViewById(R.id.empty_view);
		mWebView = (WebView)mBody.findViewById(R.id.webview);
		mWebView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String urlStr) {
				return false;
			}
			public void onPageFinished(WebView view , String url){
				super.onPageFinished(view, url);
				if (isNewPage()){
					mWebView.clearHistory();
				}
				setNewPage(false);
			}
		});
		mProgressBar = new ProgressBar(mParent, null, 
				android.R.attr.progressBarStyleHorizontal);
		mProgressBar.setProgressDrawable(mParent.getResources().getDrawable(R.drawable.progressbar_color));
		mWebView.setWebChromeClient(new LyricWebChromeClient());
		mWebView.setWebViewClient(new LyricWebViewClient());
		return mBody;
	}

	private class LyricWebChromeClient extends WebChromeClient{
		public void onProgressChanged(WebView view, int progress) {
			Log.d(TAG, "ongprogresschanged:"+progress);
			if (mProgressBar.getVisibility() == ProgressBar.INVISIBLE){
				mProgressBar.setVisibility(ProgressBar.VISIBLE);
			}
			if (progress == 100) {
				mProgressBar.setVisibility(View.GONE);
				mBody.removeView(mProgressBar);		
			} else {
				mProgressBar.setProgress(progress);
			}
		}	
	}
	private class LyricWebViewClient extends WebViewClient{
		public void onPageFinished(WebView view , String url){
			Log.d(TAG, "onpagefinished:"+url);
			mProgressBar.setVisibility(View.GONE);
			mBody.removeView(mProgressBar);
			super.onPageFinished(view, url);
		}
	  @Override
	  public void onPageStarted(WebView view, String url, Bitmap favicon) {
			mHandler.post(new Runnable(){
				public void run(){
					mProgressBar.setVisibility(View.VISIBLE);
					mBody.removeView(mProgressBar);
					RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, 5);
					rl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					mBody.addView(mProgressBar, rl);
				}
			});
	    
	    }
	}
	private boolean isNewPage(){
		synchronized (mNewPage) {
			return mNewPage;
		}
	}
	private void setNewPage(boolean b){
		synchronized (mNewPage) {
			mNewPage = b;
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onresume");
		RelativeLayout layout = mBody;
		layout.removeView(mWebView);
		RelativeLayout.LayoutParams params = 
				new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.height = layout.getHeight() - mEmpty.getHeight();
		params.width = layout.getWidth();
		layout.addView(mWebView, params);

	}

	public void search(String title, String artist, String album) {
		mTitle = title;
		mArtist = artist;
		mAlbum = album;

		mHandler.post(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				RelativeLayout layout = mBody;
				layout.removeView(mWebView);
				RelativeLayout.LayoutParams params = 
						new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				//params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				params.height = layout.getHeight() - mEmpty.getHeight();
				params.width = layout.getWidth();
				layout.addView(mWebView, params);
				
				mWebView.clearHistory();
				mWebView.clearCache(true);
				mWebView.getSettings().setJavaScriptEnabled(true);
				Log.d(TAG, "ua:"+mWebView.getSettings().getUserAgentString());
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mParent);
				boolean title = sharedPreferences.getBoolean("title", true);
				boolean artist = sharedPreferences.getBoolean("artist", true);
				String word = sharedPreferences.getString("keyword", "Lyrics");

				mWebView.loadUrl("http://www.google.com/search?q="+word+" "+
						(title ? "'" + URLEncoder.encode(mTitle) + "' " : "") + 
						(artist ? "'" + URLEncoder.encode(mArtist) + "' " : "") );
				setNewPage(true);    	
				mWebView.requestFocus(View.FOCUS_DOWN);

			}

		});


	}

	public void hideSoftKeyboard() {
		// TODO Auto-generated method stub
		mHandler.post(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				InputMethodManager imm = (InputMethodManager)mParent.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mWebView.getWindowToken(), 0);
			}

		});
	}

	public boolean goBack() {
		// TODO Auto-generated method stub
		if (mWebView!=null && mWebView.canGoBack()) {
			Log.d(TAG, "goback");
			mWebView.goBack();
			return true;
		} else
			return false;
	}

}
