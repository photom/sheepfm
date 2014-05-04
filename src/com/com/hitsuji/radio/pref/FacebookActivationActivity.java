package com.hitsuji.radio.pref;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.hitsuji.radio.Auth;
import com.hitsuji.radio.R;
import com.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class FacebookActivationActivity extends Activity {
	private static final String TAG =  FacebookActivationActivity.class.getSimpleName();
    private Facebook facebook = new Facebook(Auth.FACEBOOK_API_KEY);
    private SharedPreferences mPrefs;
    private DisplayHandler mDHandler;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDHandler = new DisplayHandler();
        setContentView(R.layout.facebook);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }
        Log.d(TAG, "token:"+access_token + " expires:"+expires);
        /*
         * Only call authorize if the access_token has expired.
         */
        if(!facebook.isSessionValid()) {
	        facebook.authorize(this,new String[] { "publish_actions" },
	        		new DialogListener() {
	            @Override
	            public void onComplete(Bundle values) {
	                SharedPreferences.Editor editor = mPrefs.edit();
	                editor.putString("access_token", facebook.getAccessToken());
	                editor.putLong("access_expires", facebook.getAccessExpires());
	                editor.commit();
	            }
	
	            @Override
	            public void onFacebookError(FacebookError error) {
	            	mDHandler.sendMessage(mDHandler.obtainMessage(FERROR, error));
	            	
	            }
	
	            @Override
	            public void onError(DialogError e) {
	            	mDHandler.sendMessage(mDHandler.obtainMessage(DERROR, e));
	            }
	
	            @Override
	            public void onCancel() {}
	        });
        } else {
        	finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
        finish();
    }
    private static int FERROR = 0;
    private static int DERROR = 1;
    private class DisplayHandler extends Handler {
    	
    	@Override
    	public void handleMessage(Message msg){
    		if (msg.what == FERROR) {
    			FacebookError e = (FacebookError)msg.obj;
    			Toast.makeText(FacebookActivationActivity.this, e.getMessage(), 2);
    		} else if (msg.what == DERROR) {
    			DialogError e = (DialogError)msg.obj;
    			Toast.makeText(FacebookActivationActivity.this, e.getMessage(), 2);
    		}
    	}
    }
}
