package com.hitsuji.radio.imp;

import com.hitsuji.radio.imp.Radio;

public class Library extends Radio {
	
	public Library(String user){
		super(user);
		mName = "Library Radio";
		mLocalFile = "library";
		mKind = Radio.KIND.LIBRARY;
		mStation = "lastfm://user/%s/library";
	}
}
