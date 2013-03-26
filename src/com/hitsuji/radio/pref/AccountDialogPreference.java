package com.hitsuji.radio.pref;

import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.manager.PlayManager;
import com.util.Log;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class AccountDialogPreference extends DialogPreference {
	private static final String TAG = AccountDialogPreference.class.getSimpleName();

	private Context mContext;
	private Settings mSettings;

	public AccountDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mContext = context;


	}

	public void setParent(Settings s){
		mSettings = s;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		Log.d(TAG, "ondialogclosed:"+positiveResult);
		if(positiveResult){

			Intent intent = new Intent(mContext, PlayManager.class);
			intent.setAction(PlayManager.CLEAR_AND_FINISH_ACTION);
			mContext.startService(intent);
			Radio.setExit();
			mSettings.callFinish();
		}

		super.onDialogClosed(positiveResult);
	}


}
