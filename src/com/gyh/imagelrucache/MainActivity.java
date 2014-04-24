package com.gyh.imagelrucache;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.gyh.imagelrucache.utils.ImageLruCache;
import com.gyh.imagelrucache.utils.LogUtils;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// put a Bitmap to cache
		ImageLruCache.putImage("SOME_KEY", new BitmapDrawable());
		
		// get a Bitmap from cache
		BitmapDrawable result = ImageLruCache.getImage("SOME_KEY");
		
		LogUtils.d(result);
		
	}
}
