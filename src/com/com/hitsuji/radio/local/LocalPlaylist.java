package com.hitsuji.radio.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalPlaylist implements TrackInfo{
	private String mId;
	private String mName;
	private boolean mPlaying;
	private List<LocalAlbum> mAlbums = Collections.synchronizedList(new ArrayList<LocalAlbum>());

	public static LocalPlaylist create(String name, String id) {
		// TODO Auto-generated method stub
		LocalPlaylist a = new LocalPlaylist();
		a.mId = id;
		a.mName = name;
		return a;
	}
	private LocalPlaylist(){}
	public LocalPlaylist(String id){
		mId = id;
	}
	public String getId(){
		return mId;
	}	
	public String getName(){
		return mName;
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
		return null;
	}

	@Override
	public String getLAlbumId() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getLPlaylistId(){
		return mId;
	}
	
	public boolean has(String albumId) {
		// TODO Auto-generated method stub
		for (LocalAlbum a : mAlbums){
			if (a.getId() != null && a.getId().length()>0 &&
					albumId!=null && albumId.length()>0 &&
					a.getId().equals(albumId)){
				return true;
				
			}
		}
		return false;
	}
	public void add(LocalAlbum a) {
		mAlbums.add(a);
	}
	public LocalAlbum findAlbum(String id) {
		// TODO Auto-generated method stub
		for (LocalAlbum a: mAlbums){
			if (a.getId()!=null && id!=null && 
					a.getId().equals(id)) 
				return a;
		}
		return null;
	}	
	public List<LocalAlbum> getAlbums(){
		return mAlbums;
	}
	public String getCoverImageName(){
		return "pl_"+getId()+".png";
	}
	
}
