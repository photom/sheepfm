package com.hitsuji.radio.imp;

public class Friend extends Radio {

	public Friend(String user) {
		super(user);
		// TODO Auto-generated constructor stub
		mName = "Friends Radio";
		mLocalFile = "friends";
		mKind = Radio.KIND.FRIEND;
		mStation = "lastfm://user/%s/friends";
	}

}
