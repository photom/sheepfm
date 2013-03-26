package com.hitsuji.radio.local;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.hitsuji.play.Track;

public class LocalAlbum implements TrackInfo {

	private String mId;
	private String mName;
	private boolean mPlaying = false;	
	private String mArtist;
	private String mArtistId;
	private String mPlaylistId;
	
	
	private List<LocalTrack> mTracks = Collections.synchronizedList(new ArrayList<LocalTrack>());
	
	public static LocalAlbum create(String album, String albumId, String artist, String artistId, Long playlistId) {
		// TODO Auto-generated method stub
		LocalAlbum a = new LocalAlbum();
		a.mId = albumId;
		a.mName = album;
		a.mArtist = artist;
		a.mArtistId = artistId;
		if(playlistId!=null)
			a.mPlaylistId = String.valueOf(playlistId); 
		return a;
	}
	private LocalAlbum(){};
	public LocalAlbum(String albumId, String artistId) {
		mId = albumId;
		mArtistId = artistId;
	}
	
	public void add(LocalTrack t) {
		mTracks.add(t);
	}
	public String getId(){
		return mId;
	}
	public String getName(){
		return mName;
	}
	public String getArtist(){
		return mArtist;
	}
	public String getArtistId(){
		return mArtistId;
	}
	public List<LocalTrack> getTracks(){
		return mTracks;
	}
	
	public String getCoverImageName(){
		return getArtistId()+"_"+getId()+".png";
	}
	public boolean has(String title) {
		// TODO Auto-generated method stub
		for (LocalTrack t : mTracks){
			if (t.getTitle() != null && t.getTitle().length()>0 &&
					title!=null && title.length()>0 &&
					t.getTitle().equals(title)){
				return true;
			}
		}
		return false;
	}
	
	private boolean mFolded = true;
	@Override
	public boolean isFolded() {
		// TODO Auto-generated method stub
		return mFolded;
	}

	@Override
	public void fold() {
		// TODO Auto-generated method stub
		mFolded = true;
	}

	@Override
	public void unfold() {
		// TODO Auto-generated method stub
		mFolded = false;
	}
	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return mPlaying;
	}
	@Override
	public void setPlaying(boolean s) {
		// TODO Auto-generated method stub
		mPlaying = s;
	}
	@Override
	public String getLTitle() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getLArtistId() {
		// TODO Auto-generated method stub
		return mArtistId;
	}
	@Override
	public String getLAlbumId() {
		// TODO Auto-generated method stub
		return mId;
	}
	@Override
	public String getLPlaylistId() {
		// TODO Auto-generated method stub
		return mPlaylistId;
	}
}
