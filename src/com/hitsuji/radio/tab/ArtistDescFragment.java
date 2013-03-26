package com.hitsuji.radio.tab;

import java.net.URLEncoder;
import java.util.Locale;

import com.hitsuji.radio.Auth;
import com.hitsuji.radio.PlayingActivity;
import com.hitsuji.radio.R;
import com.hitsuji.radio.R.layout;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.play.Track;
import com.util.Log;
import com.util.Util;

import de.umass.lastfm.Artist;
import de.umass.lastfm.User;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ArtistDescFragment extends TabBaseFragment {

	private static final String TAG = ArtistDescFragment.class.getSimpleName();

	private WebView mWebView;
	private Handler mHandler;

	public ArtistDescFragment() {
		super();
	}
	@Override
	public void onCreate(Bundle b){
		super.onCreate(b);
		Log.d(TAG, "oncreate");
		mHandler = new Handler();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
		super.onCreateView(inflater, container, icicle);
		Log.d(TAG, "oncreateview");
		mBody = (RelativeLayout) inflater.inflate(R.layout.artistdesc_fragment, null).findViewById(R.id.tab_body);
		mWebView = (WebView)mBody.findViewById(R.id.webview_artistdesc);
		mEmpty = (LinearLayout)mBody.findViewById(R.id.empty_view_artistdesc);
		
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mWebView.getLayoutParams();
		params.height = mBody.getHeight() - mEmpty.getHeight();
		mWebView.setLayoutParams(params);
		mWebView.setWebViewClient(new WebViewClient(){
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.d(TAG, "onpagestarted");
				super.onPageStarted(view, url, favicon);
			}
			@Override
			public void onPageFinished(WebView view, String url) {
				Log.d(TAG, "onpagefinished webviewh:"+view.getHeight());
				super.onPageFinished(view, url);
				mHandler.post(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.d(TAG, "pagefinished webview w:"+mWebView.getWidth()+ " h:"+mWebView.getHeight() + " basew:"+mBody.getWidth() + " baseh:"+mBody.getHeight() + " emph:"+mEmpty.getHeight());					
					}

				});
			}
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
		        Intent intent = new Intent(Intent.ACTION_VIEW);
		        intent.setData(Uri.parse(url));
		        mParent.startActivity(intent);
		        return true;
		    }
		});
		return mBody;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, "onactivitycreated webview w:"+mWebView.getWidth()+ " h:"+mWebView.getHeight() + " basew:"+mBody.getWidth() + " baseh:"+mBody.getHeight() + " emph:"+mEmpty.getHeight() );
	}
	@Override
	public void onResume(){
		super.onResume();
		setDesc();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	public void setDesc(){
		Log.d(TAG, "setDesc");

		ViewSetting vs = new ViewSetting();
		//vs.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		vs.execute();
	}

	class ViewSetting extends AsyncTask<Object, Integer, String>{
		String mContent;

		ViewSetting(){
		}

		@Override
		protected String doInBackground(Object... arg0) {
			// TODO Auto-generated method stub
			if (mParent==null) {
				Log.e(TAG, "fail to set parent activity");
				return null;
			}
			mContent = mParent.loadCurrentContent();
			if (mContent!=null) 
				mContent = mContent.replaceAll("(\r\n|\n)", "<br />");

			return mContent;

		}

		@Override
		protected void onPostExecute(String result) {
			if (result == null) return ;

			mHandler.post(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					final String fm = "<html><head><LINK href=\"shoutbody.css\" type=\"text/css\" rel=\"stylesheet\"/></head><body>%s</body></html>";

					RelativeLayout layout = mBody;
					layout.removeView(mWebView);
					
					RelativeLayout.LayoutParams params = 
							new RelativeLayout.LayoutParams(
									mBody.getWidth(), 
									mBody.getHeight() - mParent.findViewById(R.id.bottomActionBarPager).getHeight());
									//mBody.getHeight()-mEmpty.getHeight());
					//params.addRule(RelativeLayout.ABOVE, R.id.empty_view_artistdesc);
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					layout.addView(mWebView, params);
				
					mWebView.setBackgroundColor(Color.BLACK);
					mWebView.loadDataWithBaseURL (
							"file:///android_asset/", 
							String.format(fm, mContent), 
							"text/html", 
							"utf-8",
							"about:blank");
					Log.d(TAG, "load webview w:"+mWebView.getWidth()+ " h:"+mWebView.getHeight() + " basew:"+mBody.getWidth() + " baseh:"+mBody.getHeight() + " emph:"+mEmpty.getHeight());
					Log.d(TAG, "i pagerh:"+mParent.findViewById(R.id.scrollingTabs).getHeight() + " pagerh:"+mParent.findViewById(R.id.viewPager).getHeight() + " bottomh:"+mParent.findViewById(R.id.bottomActionBarPager).getHeight());
				}

			});

		}


	}
}
