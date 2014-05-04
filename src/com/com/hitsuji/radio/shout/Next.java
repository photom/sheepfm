package com.hitsuji.radio.shout;

public class Next extends ShoutItem {
	public static final int ARTIST = 0;
	public static final int TRACK = 1;
	
	private int mNextPage;
	private int mType;
	
	public Next(int type, int next) {
		super("Get More");
		// TODO Auto-generated constructor stub
		mType = type;
		mNextPage = next;
	}
	public int getType(){
		return mType;
	}
	public int getNextPage(){
		return mNextPage;
	}
	public void setNextPage(int p){
		mNextPage = p;
	}

}
