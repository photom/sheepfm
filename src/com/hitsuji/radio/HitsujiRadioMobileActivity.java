package com.hitsuji.radio;

import java.io.IOException;

import com.hitsuji.radio.R;
import com.hitsuji.radio.tab.LyricFragment;
import com.util.Log;
import com.util.Result;
import com.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HitsujiRadioMobileActivity extends Activity {
	private static final String TAG = HitsujiRadioMobileActivity.class.getSimpleName();
	
	private Auth mAuth;
	private EditText mUser;
	private EditText mPass;
	private Button mSend;
	private ImageView mLogo;
	
	private Handler mHandler;
	private ProgressDialog mProgressd;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_mobile);
        mAuth = new Auth(this.getFilesDir().getAbsolutePath());
        
        mUser = (EditText)findViewById(R.id.user);
        mPass = (EditText)findViewById(R.id.pass);
        mSend = (Button)findViewById(R.id.send);
        mLogo = (ImageView) findViewById(R.id.logo);
        
        mSend.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String user = mUser.getText().toString();
				String pass = mPass.getText().toString();
				if (user == null || user.length() == 0 ||
					pass == null || pass.length() == 0){
					mHandler.post(new Runnable(){

						@Override
						public void run() {
							// TODO Auto-generated method stub
							Toast.makeText(HitsujiRadioMobileActivity.this, "Input User Name and Password", 1).show();
						}
						
					});
					
				} else {
					new LoginTask().execute(user, pass);
				}
				
			}
        	
        });
        
        mHandler = new Handler();
        
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();
        int width = disp.getWidth();
        int height = disp.getHeight();
        
        mSend.setWidth(width/2);
        
		BitmapFactory.Options option = new BitmapFactory.Options();
		option.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(getResources(), R.drawable.lastfm_white, option);

        int space = (width/2 - option.outWidth)/2;
        
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.RIGHT_OF, mSend.getId());
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        if (space>0)lp.setMargins(space, 0, space, 0);
        else lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mLogo.setLayoutParams(lp);
        mLogo.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String url = "http://m.last.fm";
				Log.d(TAG, "onartworkclicklistener url:" + url);
				Uri uri = Uri.parse(url);
				Intent i = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(i); 
			}
        	
        });
        setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));
        
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	//pass.setLayoutParams(new LinearLayout.LayoutParams(id.getWidth(), pass.getHeight()));

        
    }
    @Override
    public void onDestroy(){
    	super.onDestroy();
    }
    
    private int login(String user, String pass){
    	Result ret;
		try {
			ret = Auth.loadMobileSession(this.getFilesDir().getAbsolutePath(), user, pass);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			new ToastThread(e1.toString()).start();
			return -1;
		}
    	if (ret == null) {
    		new ToastThread("Fail to login.").start();
    		return -1;
    	}
    	else if (ret.ret != 0) {
    		new ToastThread(ret.msg).start();
    		return -1;
    	} else {
    		
    		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    		Editor e = sp.edit();
    		e.putString("name", ret.retStr2);
    		if (!e.commit()) {
    			new ToastThread("Fail to login.").start();
    			return -1;
    		}
    		if (Auth.storeSk(this.getFilesDir().getAbsolutePath(), ret.retStr1) != 0) {
    			new ToastThread("Fail to login.").start();
    			return -1;
    		}
    		new ToastThread("Logined").start();
    		return 0;
    	}
    }
    
    private class ToastThread extends Thread{
    	private String mMsg;
    	private ToastThread(String msg){
    		mMsg = msg;
    	}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			mHandler.post(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Toast.makeText(HitsujiRadioMobileActivity.this, mMsg, 1).show();
				}
				
			});
		}
    	
    }
    
    private class LoginDialog extends ProgressDialog {
    	
		public LoginDialog(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
            setProgressStyle(ProgressDialog.STYLE_SPINNER);
            setMessage("conecting Last.fm...");
            setTitle("Login");
            setCancelable(false);
		}
    }
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            // When the user center presses, let them pick a contact.
        	this.moveTaskToBack(true);
        
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private class LoginTask extends AsyncTask<String, Integer, Integer> {
    	
    	protected void onPreExecute(){
			mProgressd = new LoginDialog(HitsujiRadioMobileActivity.this);
			mProgressd.show();
    	}
		@Override
		protected Integer doInBackground(String... params) {
			// TODO Auto-generated method stub
			return login(params[0], params[1]);
		}
    	protected void onPostExecute(Integer ret){
			mProgressd.dismiss();
			if (ret == 0) HitsujiRadioMobileActivity.this.finish();
    	}
    }
}
