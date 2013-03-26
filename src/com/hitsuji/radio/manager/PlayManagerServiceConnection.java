package com.hitsuji.radio.manager;


import com.hitsuji.radio.manager.IPlayManagerApi;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.util.Log;

import android.app.Activity;
import android.app.Service;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class PlayManagerServiceConnection implements ServiceConnection{
	public static final String TAG = PlayManagerServiceConnection.class.getSimpleName();
	private IPlayManagerCallback mCallback;
	private IPlayManagerApi mService;
	private Boolean mBind = false;
	private String mName;
	private Activity mActivity;
	private ConnectionListener mListener;
	
	public PlayManagerServiceConnection(IPlayManagerCallback call, Activity activity, ConnectionListener l) {
		mCallback = call;
		mBind = false;
		mName = activity.getLocalClassName() + l.hashCode();
		mActivity = activity;
		mListener = l;
	}
	
	public IPlayManagerApi getService() {
		return  mService;
	}
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// TODO Auto-generated method stub
		mService = IPlayManagerApi.Stub.asInterface(service);
		try{
			mService.register(mCallback, mName);
		}catch(RemoteException e){
			e.printStackTrace();
		}
		synchronized (mBind) {
			mBind = true;
		}
		if (mListener!=null)mListener.onConnected(); 
		Log.d(TAG, "bind start name:"+name.toString() + " class:"+mName);
		
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub
		Log.d(TAG, "unbind service:"+name);
		mService = null;
		mBind = false;
		if (mListener!=null)mListener.onDisconnected();
	}
	
	public boolean isBind(){
		synchronized(mBind) {
			Log.d(TAG, "isbind:"+mBind);
			return mBind;
		}
	}
	public void unsetBind(){
		synchronized (mBind) {
			Log.d(TAG, "unsetbind:");
			mBind = false;
		}
	}
}
