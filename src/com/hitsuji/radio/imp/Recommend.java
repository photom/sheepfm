package com.hitsuji.radio.imp;

import com.hitsuji.radio.imp.Radio;

public class Recommend extends Radio {
	public Recommend(String user){
		super(user);
		mName = "Recommendation Radio";
		mLocalFile = "recommend";
		mKind = Radio.KIND.RECOMMEND;
		mStation = "lastfm://user/%s/recommended";
	}
}
