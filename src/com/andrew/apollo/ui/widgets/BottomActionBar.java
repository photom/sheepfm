/**
 * 
 */

package com.andrew.apollo.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author Andrew Neal
 */
public class BottomActionBar extends LinearLayout implements OnClickListener, OnLongClickListener {

	public BottomActionBar(Context context) {
		super(context);
	}

	public BottomActionBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnClickListener(this);
		setOnLongClickListener(this);
	}

	public BottomActionBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Updates the bottom ActionBar's info
	 * 
	 * @param activity
	 * @throws RemoteException
	 */
	public void updateBottomActionBar(Activity activity) {
	}

	@Override
	public void onClick(View v) {


	}

	@Override
	public boolean onLongClick(View v) {
		return true;
	}
}
