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

public class DownloadManager {
	private CommunicationClient mComm;
	private static final int MAX_LENGTH = 2048;
	private static AtomicBoolean lock = new AtomicBoolean(false);
	
	public DownloadManager(){
		mComm = new CommunicationClient();
	}
	public int download (String url, String path) 
			throws IOException{
		return download(url, null, path);
	}
	public int download (String url, String post, String path) 
			throws IOException{
		return blockingDownload(url, post, path);
	}
	private void lock(){
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
	private void unlock(){
		synchronized (lock) {
			lock.set(false);
			lock.notify();
		}
	}
	
	private int blockingDownload(String url, String post, String path) 
			throws IOException{
		lock();
		try {
			byte[] buff = new byte[MAX_LENGTH];
			int ret = mComm.connect(url, post);
			if (ret < 0) return -1;
	
			BufferedOutputStream bufOutStream = null; 
			try {
				File out = new File(path);
				if (!out.exists() && !out.createNewFile()) return -1; 
				bufOutStream = new BufferedOutputStream(new FileOutputStream(path));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			} 
			
			while((ret = mComm.read(buff, MAX_LENGTH)) > 0) {
				try {
					bufOutStream.write(buff, 0, ret);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					ret = -2;
					break;
				}
			}
	
			try {
				bufOutStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mComm.consume();
			if (ret == -2)return -1;
			else return 0;
		} finally {
			unlock();
		}
	}

	
}
