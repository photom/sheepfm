package com.hitsuji.radio;

import java.io.IOException;

import com.hitsuji.radio.R;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class HitsujiRadioActivity extends Activity {
	private Auth mAuth;
	private Button mToken;
	private Button mReqauth;
	private Button mSessionKey;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mAuth = new Auth(this.getFilesDir().getAbsolutePath());
        
        mToken = (Button)findViewById(R.id.token);
        mToken.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				new Thread(){
					public void run(){
						try {
							mAuth.fetchToken();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
					}
				}.start();
			}
        	
        });
        
        mReqauth = (Button)findViewById(R.id.reqauth);
        mReqauth.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String url = mAuth.getAuthUrl();
				showPopUp(url);
			}
        	
        });
        mSessionKey = (Button)findViewById(R.id.sk);
        mSessionKey.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				new Thread(){
					public void run(){
						try {
							mAuth.fetchSessionkey();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}.start();

			}
        	
        });
        setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));
    }
    
	
	public void showPopUp(String url){
		try{

			Dialog dialog = new Dialog(this);
			LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
			View vi = inflater.inflate(R.layout.popup, null);
			dialog.setContentView(vi);
			dialog.setTitle("Auth");
			dialog.setCancelable(true);
			WebView wb = (WebView) vi.findViewById(R.id.Login);
			wb.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.55 Safari/533.4");
			wb.loadUrl(url);
			wb.setWebViewClient(new MyWebViewClient());
			dialog.show();

		}catch(Exception e){
			System.out.println("Exception while showing PopUp : " + e.getMessage());
		}
	}

	private class MyWebViewClient extends WebViewClient {
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}
}