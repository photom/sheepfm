package com.hitsuji.radio.shout;

public abstract class ShoutItem {
	protected String mName;
	protected int mPos = -1;
	protected ShoutItem(String name){
		mName = name;
	}
	
	public String getName(){
		return mName;
	}
	
	public int getPos() {
		return mPos;
	}
	public void setPos(int pos) {
		mPos = pos;
	}
}
