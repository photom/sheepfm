
package com.andrew.apollo.adapters;

import com.hitsuji.radio.R;
import com.util.Log;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class ScrollingTabsAdapter implements TabAdapter {
	public static final String TAG = ScrollingTabsAdapter.class.getSimpleName();
	
	public static final int JACKET = 0;
	public static final int DESC = 1;
	public static final int LYRIC = 2;
	public static final int SHOUTS = 3;
	public static final int SIMILAR = 4;

	private final FragmentActivity activity;

	public ScrollingTabsAdapter(FragmentActivity act) {
		activity = act;
	}

	@Override
	public View getView(final int position) {
		LayoutInflater inflater = activity.getLayoutInflater();
		final ImageButton tab = (ImageButton)inflater.inflate(R.layout.tabs, null);
		tab.setPadding(activity.getResources().getDimensionPixelSize(R.dimen.tab_padding_left_right), 0, 
						activity.getResources().getDimensionPixelSize(R.dimen.tab_padding_left_right), 0);
		tab.setLayoutParams(new LayoutParams(
				activity.getResources().getDimensionPixelSize(R.dimen.tab_item_width),
				activity.getResources().getDimensionPixelSize(R.dimen.bottom_action_bar_height)
				));
		
		if (position == JACKET)
			tab.setImageResource(R.drawable.jacket2);
		else if(position == DESC)
			tab.setImageResource(R.drawable.desc2);
		else if(position == LYRIC)
			tab.setImageResource(R.drawable.globe2);
		else if(position == SHOUTS)
			tab.setImageResource(R.drawable.balloon2);
		else if(position == SIMILAR)
			tab.setImageResource(R.drawable.artist2);
		//if (position < mTitles.length)
		//    tab.setText(mTitles[position]);
		Log.d(TAG, "getview w:"+tab.getWidth() + " h:"+tab.getHeight());

		return tab;
	}
}
