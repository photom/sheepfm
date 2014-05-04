package com.hitsuji.radio.imp;

public class Neighbour extends Radio {
	
	public Neighbour(String user){
		super(user);
		mName = "Neighbours Radio";
		mLocalFile = "neighbours";
		mKind = Radio.KIND.NEIGHTBOUR;
		mStation = "lastfm://user/%s/neighbours";
	}
}
