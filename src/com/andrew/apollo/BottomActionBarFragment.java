/**
 * 
 */

package com.andrew.apollo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

import com.andrew.apollo.ui.widgets.BottomActionBar;
import com.andrew.apollo.ui.widgets.BottomActionBarItem;
import com.android.HitsujiApplication;
import com.hitsuji.radio.Auth;
import com.hitsuji.radio.R;
import com.hitsuji.radio.manager.PlayManager;
import com.util.Log;

/**
 * @author Andrew Neal
 */
public class BottomActionBarFragment extends Fragment {
	private static final String TAG = BottomActionBarFragment.class.getSimpleName();
	private BottomActionBar mBottomActionBar;
	private Context mContext;
	
	public BottomActionBarFragment(Context c) {
		super();
		mContext = c;
	}
	public BottomActionBarFragment() {
		super();
		mContext = HitsujiApplication.ctx();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		Log.d(TAG, "oncreateview");
		View root = inflater.inflate(R.layout.bottom_action_bar, container, false);
		mBottomActionBar = new BottomActionBar(getActivity());
		final BottomActionBarItem love = (BottomActionBarItem)root.findViewById(R.id.heart_img);
		love.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
					love.setImageResource(R.drawable.heart_pushed);
				} else if (arg1.getAction()==MotionEvent.ACTION_UP ||
						arg1.getAction()==MotionEvent.ACTION_CANCEL) {
					love.setImageResource(R.drawable.heart);
				}
				return false;
			}
		});
		love.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(mContext, PlayManager.class);
				intent.setAction(PlayManager.LIKE_ACTION);
				mContext.startService(intent);
			}
		});
		final BottomActionBarItem dislike = (BottomActionBarItem)root.findViewById(R.id.cross_img);
		dislike.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
					dislike.setImageResource(R.drawable.cross_pushed);
				} else if (arg1.getAction()==MotionEvent.ACTION_UP ||
						arg1.getAction()==MotionEvent.ACTION_CANCEL) {
					dislike.setImageResource(R.drawable.cross);
				}
				return false;
			}
		});
		dislike.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(mContext, PlayManager.class);
				intent.setAction(PlayManager.DISLIKE_ACTION);
				mContext.startService(intent);
			}
		});
		
		if (Auth.getSessionkey(mContext.getFilesDir().getAbsolutePath()) == null){
			love.setVisibility(View.GONE);
			dislike.setVisibility(View.GONE);
		} else {
			love.setVisibility(View.VISIBLE);
			dislike.setVisibility(View.VISIBLE);
		}
			
		
		final BottomActionBarItem stop = (BottomActionBarItem)root.findViewById(R.id.stop_img);
		stop.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
					stop.setImageResource(R.drawable.stop_pushed);
				} else if (arg1.getAction()==MotionEvent.ACTION_UP ||
						arg1.getAction()==MotionEvent.ACTION_CANCEL) {
					stop.setImageResource(R.drawable.stop);
				}
				return false;
			}
		});
		stop.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(mContext, PlayManager.class);
				intent.setAction(PlayManager.STOP_ACTION);
				mContext.startService(intent);
			}
		});
		final BottomActionBarItem next = (BottomActionBarItem)root.findViewById(R.id.next_img);
		next.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
					next.setImageResource(R.drawable.next_pushed);
				} else if (arg1.getAction()==MotionEvent.ACTION_UP ||
						arg1.getAction()==MotionEvent.ACTION_CANCEL) {
					next.setImageResource(R.drawable.next);
				}
				return false;
			}
		});
		next.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(mContext, PlayManager.class);
				intent.setAction(PlayManager.NEXT_ACTION);
				mContext.startService(intent);
			}
		});
		return root;
	}

	/**
	 * Update the list as needed
	 */
	private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (mBottomActionBar != null) {
				mBottomActionBar.updateBottomActionBar(getActivity());
			}
		}
	};

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}
}
