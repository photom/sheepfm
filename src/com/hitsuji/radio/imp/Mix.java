package com.hitsuji.radio.imp;

public class Mix extends Radio {
	public Mix(String user){
		super(user);
		mName = "Mix Radio";
		mLocalFile = "mix";
		mKind = Radio.KIND.MIX;
		mStation = "lastfm://user/%s/mix";
	}
}
