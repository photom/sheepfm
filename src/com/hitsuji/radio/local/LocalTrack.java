package com.hitsuji.radio.local;

import java.io.File;

import android.database.Cursor;
import android.provider.MediaStore;
import com.util.Log;
import com.hitsuji.play.Track;

public class LocalTrack extends Track implements TrackInfo {
	private static final String TAG = LocalTrack.class.getSimpleName();
	/**
	 * 
	 */
	private static final long serialVersionUID = -2237619221549040430L;
	private String mMimeType;
	private boolean mFolded = true;
	private boolean mPlaying = false;
	
	public static LocalTrack createLocal(Cursor c) {
		LocalTrack track = new LocalTrack();
		track.mLocation = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));
		File f  = new File(track.mLocation);
		if (!f.exists()){
			Log.e(TAG, "file does not exist:"+track.mLocation);
		}
		
		track.mAlbumId = String.valueOf(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
		track.mAlbum = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
		track.mArtistId = String.valueOf( c.getLong(c.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)) );
		track.mCreator = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
		track.mDuration = c.getInt(c.getColumnIndex(MediaStore.Audio.Media.DURATION));
		track.mTitle = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
		track.mTrackNumber = c.getInt(c.getColumnIndex(MediaStore.Audio.Media.TRACK));
		track.mMimeType = c.getString(c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
		return track;
	}
	
	public static LocalTrack createPlaylistLocal(Cursor c, long id, String name) {
		LocalTrack track = new LocalTrack();
		track.mLocation = c.getString(c.getColumnIndex(MediaStore.Audio.PlaylistsColumns.DATA));
		File f  = new File(track.mLocation);
		if (!f.exists()){
			Log.e(TAG, "file does not exist:"+track.mLocation);
		}
		
		track.mAlbumId = String.valueOf(c.getLong(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
		track.mAlbum = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
		track.mArtistId = String.valueOf( c.getLong(c.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)) );
		track.mCreator = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
		track.mDuration = c.getInt(c.getColumnIndex(MediaStore.Audio.Media.DURATION));
		track.mTitle = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
		track.mTrackNumber = c.getInt(c.getColumnIndex(MediaStore.Audio.Media.TRACK));
		track.mMimeType = c.getString(c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
		track.mPlayOrder = c.getInt(c.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER));				
		track.mPlaylistName = name;
		track.mPlaylistId = String.valueOf(id);

		return track;
	}
	
	
	private LocalTrack(){}
	public LocalTrack(String title, String albumId, String artistId){
		mTitle = title;
		mAlbumId = albumId; 
		mArtistId = artistId;
	}
	
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
		return mTitle;
	}
	@Override
	public String getLArtistId() {
		// TODO Auto-generated method stub
		return mArtistId;
	}
	@Override
	public String getLAlbumId() {
		// TODO Auto-generated method stub
		return mAlbumId;
	}
	@Override
	public String getLPlaylistId() {
		// TODO Auto-generated method stub
		return mPlaylistId;
	}
	public String getPlaylistName() {
		// TODO Auto-generated method stub
		return mPlaylistName;
	}
}
