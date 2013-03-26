package com.hitsuji.radio.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hitsuji.play.Track;
import com.util.Log;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

public class LocalAudioInfo  {
	private static final String TAG = LocalAudioInfo.class.getSimpleName();
	private List<LocalArtist> mArtists = Collections.synchronizedList(new ArrayList<LocalArtist>());
	private List<LocalPlaylist> mPlaylists = Collections.synchronizedList(new ArrayList<LocalPlaylist>());

	public void init(Context c) {
		loadLocalAudio(c, null, null, null);
		loadLocalPlaylist(c, null, null, null);
	}
	public List<LocalArtist> getArtists(){
		return mArtists;
	}
	public List<LocalPlaylist> getPlaylists(){
		return mPlaylists;
	}
	private boolean has(String id) {
		for (LocalArtist a: mArtists){
			if (a.getId()!=null && id!=null && 
					a.getId().equals(id)) 
				return true;
		}
		return false;
	}
	private boolean containInPlaylists(String id) {
		for (LocalPlaylist a: mPlaylists){
			if (a.getId()!=null && id!=null && 
					a.getId().equals(id)) 
				return true;
		}
		return false;
	}
	private LocalArtist findArtist(String id){
		for (LocalArtist a: mArtists){
			if (a.getId()!=null && id!=null && 
					a.getId().equals(id)) 
				return a;
		}
		return null;
	}
	private LocalPlaylist findPlaylist(String id){
		for (LocalPlaylist a: mPlaylists){
			if (a.getId()!=null && id!=null && 
					a.getId().equals(id)) 
				return a;
		}
		return null;
	}	
	private int loadLocalPlaylist(Context c, String playlistId, String albumId, String title){
		mPlaylists.clear();

		List<String> args = new ArrayList<String> ();
		if (playlistId!=null && playlistId.length()>0) args.add(playlistId);

		String[] argsArr = args.size()==0 ? null : (String[])args.toArray(new String[0]);
		ContentResolver resolver = c.getContentResolver();
		
		Cursor listsCursor = resolver.query(
				MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI , 
				new String[]{
						MediaStore.Audio.Playlists._ID,
						MediaStore.Audio.Playlists.NAME,
				},
				(playlistId==null || playlistId.length()==0 ? "" : 
					(MediaStore.Audio.Playlists._ID + " = ?")), 
					argsArr, null);
		
		if (listsCursor == null) {
			Log.e(TAG, "fail to load music info from MediaStore.");
			return -1;
		}
		for (String name : listsCursor.getColumnNames()){
			Log.d(TAG, "list column:"+name);
		}
		while( listsCursor.moveToNext() ){
			long id = listsCursor.getLong(0);
			String name = listsCursor.getString(1);
			args.clear();
			if (albumId!=null && albumId.length()>0) args.add(albumId);
			if (title!=null && title.length()>0) args.add(title);
			argsArr = args.size()==0 ? null : (String[])args.toArray(new String[0]);

			Cursor cursor = resolver.query(
					MediaStore.Audio.Playlists.Members.getContentUri("external", id),
					new String[]{
							MediaStore.Audio.Media.ALBUM ,
							MediaStore.Audio.Media.ALBUM_ID ,
							MediaStore.Audio.Media.ARTIST ,
							MediaStore.Audio.Media.ARTIST_ID ,
							MediaStore.Audio.Media.TITLE,
							MediaStore.Audio.Media.DISPLAY_NAME,
							MediaStore.Audio.Media.TRACK,
							MediaStore.Audio.Media.MIME_TYPE,
							MediaStore.Audio.Media.DURATION,
							MediaStore.Audio.Playlists.Members.DATA,
							MediaStore.Audio.Playlists.Members.PLAY_ORDER,
							MediaStore.Audio.Playlists.Members.PLAYLIST_ID,
					},
					MediaStore.Audio.Media.IS_ALARM + " = 0 AND " + 
							MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " + 
									MediaStore.Audio.Media.IS_NOTIFICATION + " = 0 AND " + 
									MediaStore.Audio.Media.IS_RINGTONE + " = 0 AND " +
									MediaStore.Audio.Media.ALBUM + " IS NOT NULL AND " +
									" LENGTH("+MediaStore.Audio.Media.ALBUM + ") > 0 AND " +
									MediaStore.Audio.Media.ARTIST + " IS NOT NULL AND " +
									" LENGTH("+MediaStore.Audio.Media.ARTIST + ") > 0 AND " +
									MediaStore.Audio.Media.DISPLAY_NAME + " IS NOT NULL AND " +
									" LENGTH("+MediaStore.Audio.Media.DISPLAY_NAME + ") > 0 AND " +
									MediaStore.Audio.Media.DATA + " IS NOT NULL AND " +
									" LENGTH("+MediaStore.Audio.Media.DATA + ") > 0 "   +
									(albumId==null || albumId.length()==0 ? "" : 
											(" AND "+MediaStore.Audio.Media.ALBUM_ID + " = ?")) + 
											(title==null || title.length()==0 ? "" : 
												(" AND "+MediaStore.Audio.Media.TITLE + " = ?")),									
												argsArr,
												MediaStore.Audio.Playlists.Members.PLAY_ORDER
					);

			if (cursor == null) {
				Log.e(TAG, "fail to load music info from MediaStore.");
				listsCursor.close();
				return -1;
			}
			while( cursor.moveToNext() ){
				LocalTrack t = LocalTrack.createPlaylistLocal(cursor, id, name);
				if (t==null)  continue;
				if (!this.containInPlaylists(t.getLPlaylistId())){
					mPlaylists.add(LocalPlaylist.create(t.getPlaylistName(), t.getLPlaylistId()));
				}
				LocalPlaylist p = findPlaylist(t.getLPlaylistId());
				if (p != null){
					if (!p.has(t.getAlbumId())){
						p.add(LocalAlbum.create(
								t.getAlbum(), t.getAlbumId(), t.getArtist(), t.getArtistId(), id));
					}
					LocalAlbum al = p.findAlbum(t.getAlbumId());
					if (al!=null) {
						if (!al.has(t.getTitle()))
							al.add(t);
					}
				}
			}

			cursor.close();
			
		}
		listsCursor.close();

		return 0;
	}
	private int loadLocalAudio(Context c, String artistId, String albumId, String title){
		mArtists.clear();

		List<String> args = new ArrayList<String> ();
		if (artistId!=null && artistId.length()>0) args.add(artistId);
		if (albumId!=null && albumId.length()>0) args.add(albumId);
		if (title!=null && title.length()>0) args.add(title);
		String[] argsArr = args.size()==0 ? null : (String[])args.toArray(new String[0]);
		ContentResolver resolver = c.getContentResolver();
		Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI , 
				new String[]{
						MediaStore.Audio.Media.ALBUM ,
						MediaStore.Audio.Media.ALBUM_ID ,
						MediaStore.Audio.Media.ARTIST ,
						MediaStore.Audio.Media.ARTIST_ID ,
						MediaStore.Audio.Media.TITLE,
						MediaStore.Audio.Media.DISPLAY_NAME,
						MediaStore.Audio.Media.TRACK,
						MediaStore.Audio.Media.DATA,
						MediaStore.Audio.Media.MIME_TYPE,
						MediaStore.Audio.Media.DURATION
				},
				MediaStore.Audio.Media.IS_ALARM + " = 0 AND " + 
						MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " + 
								MediaStore.Audio.Media.IS_NOTIFICATION + " = 0 AND " + 
								MediaStore.Audio.Media.IS_RINGTONE + " = 0 AND " +
								MediaStore.Audio.Media.ALBUM + " IS NOT NULL AND " +
								" LENGTH("+MediaStore.Audio.Media.ALBUM + ") > 0 AND " +
								MediaStore.Audio.Media.ARTIST + " IS NOT NULL AND " +
								" LENGTH("+MediaStore.Audio.Media.ARTIST + ") > 0 AND " +
								MediaStore.Audio.Media.DISPLAY_NAME + " IS NOT NULL AND " +
								" LENGTH("+MediaStore.Audio.Media.DISPLAY_NAME + ") > 0 AND " +
								MediaStore.Audio.Media.DATA + " IS NOT NULL AND " +
								" LENGTH("+MediaStore.Audio.Media.DATA + ") > 0 "   +
								(artistId==null || artistId.length()==0 ? "" : 
									(" AND "+MediaStore.Audio.Media.ARTIST_ID + " = ?")) + 
									(albumId==null || albumId.length()==0 ? "" : 
										(" AND "+MediaStore.Audio.Media.ALBUM_ID + " = ?")) + 
										(title==null || title.length()==0 ? "" : 
											(" AND "+MediaStore.Audio.Media.TITLE + " = ?"))
											,
											argsArr,
											MediaStore.Audio.Media.ARTIST + ", " +
													MediaStore.Audio.Media.ALBUM + ", " + 
													MediaStore.Audio.Media.TRACK 
				);

		if (cursor == null) {
			Log.e(TAG, "fail to load music info from MediaStore.");
			return -1;
		}
		while( cursor.moveToNext() ){
			LocalTrack t = LocalTrack.createLocal(cursor);
			if (t==null)  continue;
			if (!has(t.getArtistId())){
				mArtists.add(LocalArtist.create(t.getArtist(), t.getArtistId()));
			}
			LocalArtist a = findArtist(t.getArtistId());
			if (a != null){
				if (!a.has(t.getAlbumId())){
					a.add(LocalAlbum.create(
							t.getAlbum(), t.getAlbumId(), t.getArtist(), t.getArtistId(), null));
				}
				LocalAlbum al = a.findAlbum(t.getAlbumId());
				if (al!=null) {
					if (!al.has(t.getTitle()))
						al.add(t);
				}
			}
		}

		cursor.close();
		return 0;
	}


	public List<Track> createTrackList(Context c, String artistId, String albumId, String title, String playlistId){
		List<Track> list = new ArrayList<Track>();
		if (playlistId == null) {
			loadLocalAudio(c, artistId, albumId, title);
			for (LocalArtist artist : mArtists){
				for (LocalAlbum album : artist.getAlbums()) {
					for (LocalTrack track : album.getTracks()) {
						list.add(track);
					}
				}
			}
		} else {
			loadLocalPlaylist(c, playlistId, albumId, title);
			for (LocalPlaylist play : mPlaylists){
				for (LocalAlbum album : play.getAlbums()) {
					for (LocalTrack track : album.getTracks()) {
						list.add(track);
					}
				}
			}			
		}
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
		boolean shuffle = sharedPreferences.getBoolean("shuffle", false);
		if (shuffle)Collections.shuffle(list);

		return list; 
	}
}
