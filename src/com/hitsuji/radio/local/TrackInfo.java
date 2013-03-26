package com.hitsuji.radio.local;

public interface TrackInfo {
	public boolean isFolded ();
	public void fold();
	public void unfold();
	public boolean isPlaying();
	public void setPlaying(boolean s);
	public String getLTitle();
	public String getLArtistId();
	public String getLAlbumId();
	public String getLPlaylistId();
}
