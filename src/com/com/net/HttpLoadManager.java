package com.net;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class HttpLoadManager {
	private CommunicationClient mComm;
	private static final int MAX_LENGTH = 2048;
	private static AtomicBoolean lastfmLock = new AtomicBoolean(false);
	private static AtomicBoolean echonestLock = new AtomicBoolean(false);
	
	public HttpLoadManager(){
		mComm = new CommunicationClient();
	}
	public String execLastfm (String url) 
			throws IOException{
		return execLastfm(url, null);
	}
	public String execLastfm (String url, String post) 
			throws IOException{
		return blockingLoad(url, post, lastfmLock);
	}
	public String execEchonest (String url) 
			throws IOException{
		return execEchonest(url, null);
	}
	public String execEchonest (String url, String post) 
			throws IOException{
		return blockingLoad(url, post, echonestLock);
	}
	private void lock(AtomicBoolean lock){
		synchronized(lock) {
			if (lock.get()){
				try {
					lock.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
			}
			lock.set(true);
		}
	}
	private void unlock(AtomicBoolean lock){
		synchronized (lock) {
			lock.set(false);
			lock.notify();
		}
	}
	
	private String blockingLoad(String url, String post, AtomicBoolean lock) 
			throws IOException{
		lock(lock);
		try {
			byte[] buff = new byte[MAX_LENGTH];
			int ret = mComm.connect(url, post);
			if (ret < 0) return null;
	
			StringBuffer sb = new StringBuffer();
			
			while((ret = mComm.read(buff, MAX_LENGTH)) > 0) {
				sb.append(new String(buff, 0, ret));
			}

			if (ret == -2)return null;
			else return sb.toString();
		} finally {
			mComm.consume();
			unlock(lock);
		}
	}

	
}
