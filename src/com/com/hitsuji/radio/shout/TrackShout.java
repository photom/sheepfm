package com.hitsuji.radio.shout;

import de.umass.lastfm.Shout;

public class TrackShout extends ShoutItem {
	private Shout mShout;
	public TrackShout(String name, Shout shout) {
		super(name);
		// TODO Auto-generated constructor stub
		mShout = shout;
	}
	public String getAuthor(){
		return mShout.getAuthor();
	}
	public String getBody(){
		return mShout.getBody();
	}
}
