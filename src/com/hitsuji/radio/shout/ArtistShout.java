package com.hitsuji.radio.shout;

import de.umass.lastfm.Shout;

public class ArtistShout extends ShoutItem {
	private Shout mShout;
	
	public ArtistShout(String name, Shout s) {
		super(name);
		// TODO Auto-generated constructor stub
		mShout = s;
	}
	public String getAuthor(){
		return mShout.getAuthor();
	}
	public String getBody(){
		return mShout.getBody();
	}
}
