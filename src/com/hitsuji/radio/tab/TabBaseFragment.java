package com.hitsuji.radio.tab;

import java.util.ArrayList;
import java.util.List;

import com.android.HitsujiApplication;
import com.hitsuji.radio.R;
import com.hitsuji.radio.RadioListActivity;
import com.hitsuji.radio.imp.Radio;
import com.hitsuji.radio.local.LocalAudioListActivity;
import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.radio.manager.PlayManager;
import com.hitsuji.radio.manager.PlayManagerServiceConnection;
import com.util.Log;
import com.hitsuji.radio.PlayingActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


public class TabBaseFragment extends Fragment {
	private static final String TAG = TabBaseFragment.class.getSimpleName();

	private static List<Fragment> FragmentList = new ArrayList<Fragment>();
	
	protected PlayingActivity mParent;
	protected RelativeLayout mBody;
	protected LinearLayout mEmpty;
	
	
	public static void clearFragments(FragmentManager fm){
		FragmentTransaction ft = fm.beginTransaction();
		for (Fragment f: FragmentList){
			Log.d(TAG, "remove:"+f.getClass().getSimpleName());
			ft.remove(f);
		}
		ft.commit();
		FragmentList.clear();
	}
	
	public TabBaseFragment(){
		super();
		Log.d(this.getClass().getSimpleName(), "instantiate :"+this.hashCode());
		FragmentList.add(this);
	}
	
	@Override
	public void onAttach(Activity a) {
		super.onAttach(a);
		Log.d(this.getClass().getSimpleName(), "onattach");
		if(a instanceof PlayingActivity) {
			mParent = (PlayingActivity)a;
		} else {
			Log.d(TAG, "PlayingActivity is null");
		}
	}
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.d(this.getClass().getSimpleName(), "oncreate :"+this.hashCode());
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle){
		return super.onCreateView(inflater, container, icicle);
	}
	
	@Override
	public void onStart(){
		super.onStart();
		Log.d(this.getClass().getSimpleName(), "onStart :"+this.hashCode());

	}
	@Override
	public void onResume(){
		super.onResume();
		Log.d(this.getClass().getSimpleName(), "onResume :"+this.hashCode());    	
	}
	@Override
	public void onPause(){
		Log.d(this.getClass().getSimpleName(), "onPause :"+this.hashCode());		
		super.onPause();
	}
	@Override
	public void onStop(){
		Log.d(this.getClass().getSimpleName(), "onStop :"+this.hashCode());		
		super.onStop();
	}

	@Override
	public void onDestroy(){
		Log.d(this.getClass().getSimpleName(), "onDestroy :"+this.hashCode());		
		super.onDestroy();
	}	
	@Override
	public void onDetach(){
		Log.d(this.getClass().getSimpleName(), "onDetach :"+this.hashCode());		
		super.onDetach();
	}
   @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setUserVisibleHint(true);
    }
	public void setParent(PlayingActivity a){
		mParent = a;
	}
	
	public RelativeLayout getBody(){
		return mBody;
	}
}
