package com.hitsuji.radio.manager;

import com.hitsuji.radio.manager.IPlayManagerCallback;
import com.hitsuji.play.Track;

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
	String[] getImageUrl(int idx);
	Bundle getPlayingTrackInfo();
}
