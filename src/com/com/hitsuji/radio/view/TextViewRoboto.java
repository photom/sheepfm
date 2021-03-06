package com.hitsuji.radio.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextViewRoboto extends TextView {
	
	public TextViewRoboto(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createFont();
	}

	public TextViewRoboto(Context context, AttributeSet attrs) {
		super(context, attrs);
		createFont();
	}

	public TextViewRoboto(Context context) {
		super(context);
		createFont();
	}

	public void createFont() {
		Typeface font = Typeface.createFromAsset(getContext().getAssets(), "font/Roboto-Light.ttf");
		setTypeface(font);
	}
}
