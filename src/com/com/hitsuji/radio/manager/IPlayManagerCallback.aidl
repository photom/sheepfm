package com.hitsuji.radio.manager;
import com.hitsuji.play.Track;

interface IPlayManagerCallback {
	void onFinishedCreatePlayList(int ret, int type, boolean fill);
	void onStarted(in Track track);
	void onStarting();
	void onLoadedTrackInfo(in Track track);
	void onUnbind();
	void toast(String msg);
}
