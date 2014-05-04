package com.hitsuji.radio;

public class Jni {
	static {
		System.loadLibrary("jni");	
	}
	//sync
	public static native void a();
	//get api secret
	public static native String b();
	//get secret key for encrypt session key
	public static native String c();
}
