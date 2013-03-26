package com.hitsuji.radio.pref;

import com.hitsuji.radio.Auth;
import com.hitsuji.radio.R;
import com.util.Util;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity {
	private static final String TAG = Settings.class.getSimpleName();
	
	private AccountDialogPreference mAccountPref;
	private Dialog mDialog;
	private Handler mHandler;
	@Override  
	public void onCreate(Bundle savedInstanceState){  
		super.onCreate(savedInstanceState);  
		addPreferencesFromResource(R.xml.pref);  
		//mDialog = (AccountDialogPreference)this.findViewById(R.id.AccountDialogPreference);
		PreferenceScreen ps = this.getPreferenceScreen();
		mAccountPref = (AccountDialogPreference)ps.findPreference("accountclear");
		mAccountPref.setParent(this);
		mHandler = new Handler();
		setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));
	}

	@Override
	public void onResume(){
		super.onResume();
		
		PreferenceScreen ps = this.getPreferenceScreen();
		PreferenceScreen login = (PreferenceScreen)ps.findPreference("lastfmaccount");
		Preference scrobble = ps.findPreference("scrobble");
		if (Auth.getSessionkey(this.getFilesDir().getAbsolutePath()) == null){
			login.setEnabled(true);
			login.setTitle("Login");
			login.setSummary("");
			scrobble.setEnabled(false);
		} else {
			login.setEnabled(false);
			scrobble.setEnabled(true);
			login.setTitle("Account");
			String name = Util.getName(this);
			if (name!=null) {
				login.setSummary(name);
			}
		}
	}
	
	public void callFinish() {
		// TODO Auto-generated method stub
		mHandler.post(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Settings.this.finish();
			}
			
		});
	}
}
