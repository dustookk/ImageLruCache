package com.gyh.imagelrucache.utils;

import android.util.Log;

public class LogUtils {
	public final static String TAG = "ImageLruCache.debug";

	public static void d(Object... args) {
		String result = " ";
		for (Object message : args) {
			result += message == null ? "null " : message.toString() + " ";
		}
		Log.d(TAG, result);
	}

	public static void e(Throwable tr) {
		Log.e(TAG, "", tr);
	}

	public static void e(String msg) {
		Log.e(TAG, msg);
	}

}
