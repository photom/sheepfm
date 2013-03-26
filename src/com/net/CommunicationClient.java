package com.net;


import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpRequest;

import com.util.Util;

public class CommunicationClient {
	private HttpResponse mResponse;
	private BasicHttpRequest mRequest;
	private URL mUrl;
	private BufferedInputStream mBis = null;
	private HttpURLConnection mConn;
	private String mPost;
	
	static {
		CookieManager manager = new CookieManager();
		manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(manager);
	}
	
	public CommunicationClient(){
	}
	public CommunicationClient(String post){
		mPost = post;
		
	}
	public int connect(String url) throws IOException{
		return connect(url, null);
	}
	public int connect(String url, String post) throws IOException {
		OutputStream os = null;
		PrintStream ps = null;
		mConn = null;

		try {
			mUrl = new URL(url);
			URLConnection c = mUrl.openConnection();
			mConn = (HttpURLConnection)c;
			setParam();
			if (post!=null) {
				//mConn.setDoInput(false);
				mConn.setDoOutput(true);
				os = c.getOutputStream();
				ps = new PrintStream(os);
				ps.print(post);
				ps.flush();
				ps.close();
			} else {
				mConn.setRequestMethod("GET");
				//mConn.setDoOutput(false);
				mConn.setDoInput(true);
				mConn.connect();
			}
			
			if (mBis!=null){
				mBis.close();
				mBis = null;
			}
			try {
				mBis = new BufferedInputStream( mConn.getInputStream() );
			} catch(FileNotFoundException e) {
				IOException ie = new IOException("confirm network environment");
				
				throw ie;
			}

			return 0;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (os != null) os.close();
			if (ps != null) ps.close();
			if (mConn != null) mConn.disconnect();

			throw e;
		} 
		return -1;
	}
	
	private void setParam() {
		// TODO Auto-generated method stub
		if (mConn!=null) {
			mConn.setRequestProperty("User-Agent", Util.getUserAgent());
		}
	}
	public int read(byte[] buffer, int len) throws IOException{
		if (mBis == null)return -2;

		int ret = mBis.read(buffer, 0, len);
		return ret;

	}
	
	
	public void consume (){
		if (mBis!=null)
			try {
				mBis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		if (mConn!=null)mConn.disconnect();
	}
}
