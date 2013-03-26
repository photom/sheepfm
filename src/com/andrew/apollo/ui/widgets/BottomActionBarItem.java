/**
 * 
 */

package com.andrew.apollo.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

/**
 * @author Andrew Neal
 */
public class BottomActionBarItem extends ImageButton implements OnLongClickListener,
        OnClickListener, OnMenuItemClickListener {

    private final Context mContext;

    private static final int EFFECTS_PANEL = 0;

    public BottomActionBarItem(Context context) {
        super(context);
        mContext = context;
    }

    public BottomActionBarItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnLongClickListener(this);
        setOnClickListener(this);
        mContext = context;
    }

    public BottomActionBarItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    public boolean onLongClick(View v) {
        //Toast.makeText(getContext(), v.getContentDescription(), Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            default:
                break;
        }
    }

    /**
     * @param v
     */
    private void showPopup(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.setOnMenuItemClickListener(this);
        //popup.inflate(R.menu.overflow_library);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            default:
                break;
        }
        return false;
    }

}
