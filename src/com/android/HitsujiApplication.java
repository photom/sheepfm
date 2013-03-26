package com.android;

import java.io.File;

import com.hitsuji.play.Track;
import com.util.Util;

import de.umass.lastfm.Caller;
import de.umass.lastfm.cache.FileSystemCache;

import android.app.Application;
import android.content.Context;

public class HitsujiApplication extends Application {
	
	private static Context context; 
	
	public static Context ctx(){
		return context;
	}
	
    @Override
    public void onCreate() {
        super.onCreate(); 
        context = (Context) this.getApplicationContext();
        
        Util.initUserAgent(this.getApplicationContext());
        Caller.getInstance().setUserAgent(Util.getUserAgent());
        File dir = new File(this.getApplicationContext().getFilesDir()+File.separator+ "cache");
        if (!dir.exists())dir.mkdirs();
        Caller.getInstance().setCache(new  FileSystemCache(dir));
        
        Track.removeOldArtistDir(this.getApplicationContext().getFilesDir());
        //Caller.getInstance().setDebugMode(true);
    }

    @Override
    public void onTerminate() {
    	super.onTerminate();
    }


}
