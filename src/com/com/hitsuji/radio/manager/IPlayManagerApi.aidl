package com.hitsuji.radio.manager;

import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.play.Track;
import com.hitsuji.radio.table.ImageItem;

interface IPlayManagerApi {
	void register(IPlayManagerCallback listener, String cookie);
	void unregister(IPlayManagerCallback listener);
	int getCurrentRadio();
	int getCurrentRadioState();
	CharSequence getCurrentPosition();
	String getCurrentArtist();
	String getCurrentTitle();
	String loadCurrentContent();
	Bundle getSimilarArtistList();
	String getSimilarRadioArtist();
	ImageItem getImageUrl(int idx);
	Bundle getPlayingTrackInfo();
}
