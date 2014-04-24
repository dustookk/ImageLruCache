package com.gyh.imagelrucache.utils;

import java.io.FileDescriptor;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;

import com.gyh.imagelrucache.utils.LogUtils;
import com.gyh.imagelrucache.utils.LruCache;
import com.gyh.imagelrucache.utils.ReflectUtils;
import com.gyh.imagelrucache.utils.VersionUtils;

public class ImageLruCache {
	// ==========================================================================
	// Constants
	// ==========================================================================
	// Use 1/8th of the available memory for this memory cache.
	public static final int CACHE_SIZE = (int) (Runtime.getRuntime()
			.maxMemory() / 1024) / 8;
	// ==========================================================================
	// Fields
	// ==========================================================================
	private static Set<SoftReference<Bitmap>> mReusableBitmaps = Collections
			.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
	private static LruCache<Object, BitmapDrawable> mMemoryCache = new LruCache<Object, BitmapDrawable>(
			CACHE_SIZE) {
		/**
		 * Called for entries that have been evicted or removed.
		 */
		@Override
		protected void entryRemoved(boolean evicted, Object key,
				BitmapDrawable oldValue, BitmapDrawable newValue) {
			if (oldValue == null || oldValue.getBitmap() == null) {
				return;
			}

			if (oldValue == newValue) {
				return; // the new value is the same as the old one.
			}

			// add to the soft map,save for later.
			if (VersionUtils.hasHoneycomb()) {
				mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue
						.getBitmap()));
			} else {
				oldValue.getBitmap().recycle();
			}

		}

		@Override
		protected int sizeOf(Object key, BitmapDrawable value) {
			final int bitmapSize = getBitmapSize(value) / 1024;
			return bitmapSize == 0 ? 1 : bitmapSize;
		}
	};

	// ==========================================================================
	// Constructors
	// ==========================================================================
	private ImageLruCache() {
	}

	// ==========================================================================
	// Getters
	// ==========================================================================
	// ==========================================================================
	// Setters
	// ==========================================================================
	// ==========================================================================
	// Methods
	// ==========================================================================
	
	/**
	 * measure the size of the bitmap
	 */
	public static int getBitmapSize(BitmapDrawable value) {
		Bitmap bitmap = value.getBitmap();

		if (VersionUtils.hasKitKat()) {

			// bitmap.getAllocationByteCount();
			return ReflectUtils.invoke(int.class, Bitmap.class,
					"getAllocationByteCount", null, bitmap, null, true);
		}
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	/**
	 * save to memory cache
	 */
	public static void putImage(Object key, BitmapDrawable value) {
		if (key == null || value == null) {
			return;
		}
		// Add to memory cache
		mMemoryCache.put(key, value);
		LogUtils.d("put to cache key:", key, "value:", value, "size:",
				getBitmapSize(value) / 1024, " mem total:", mMemoryCache.size());
		// LogUtils.g("put",value,"into cache","to replce",put,"key is",key);
	}

	/**
	 * get bitmap from memory cache
	 */
	public static BitmapDrawable getImage(Object key) {
		BitmapDrawable bitmapDrawable = mMemoryCache.get(key);
		return bitmapDrawable;
	}

	/**
	 * clear the cache
	 */
	public static void clear() {
		mMemoryCache.evictAll();
	}

	public static void remove(Object key) {
		mMemoryCache.remove(key);
	}

	/**
	 * This method iterates through the reusable bitmaps, looking for one to use for inBitmap:  
	 * 
	 * @param options
	 *            - BitmapFactory.Options with out* options populated
	 * @return Bitmap that case be used for inBitmap
	 */
	protected static Bitmap getBitmapFromReusableSet(
			BitmapFactory.Options options) {
		Bitmap bitmap = null;

		if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
			synchronized (mReusableBitmaps) {
				final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps
						.iterator();
				Bitmap item;

				while (iterator.hasNext()) {
					item = iterator.next().get();

					if (null != item && item.isMutable()) {
						if (canUseForInBitmap(item, options)) {
							bitmap = item;
							LogUtils.d("found a Bitmap can be reused ", bitmap);
							iterator.remove();
							break;
						}
					} else {
						// Remove from the set if the reference has been
						// cleared.
						iterator.remove();
					}
				}
			}
		}

		return bitmap;
	}

	/**
	 * Check to see it the item can be used for inBitmap.  
	 * @param targetOptions
	 *            - Options that have the out* value populated
	 * @return true if <code>candidate</code> can be used for inBitmap re-use
	 *         with <code>targetOptions</code>
	 */
	private static boolean canUseForInBitmap(Bitmap candidate,
			BitmapFactory.Options targetOptions) {
		if (!VersionUtils.hasKitKat()) {
			// On earlier versions, the dimensions must match exactly and the
			// inSampleSize must be 1
			return candidate.getWidth() == targetOptions.outWidth
					&& candidate.getHeight() == targetOptions.outHeight
					&& targetOptions.inSampleSize == 1;
		}

		// Android 4.4 (KitKat)
		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;
		int byteCount = width * height
				* getBytesPerPixel(candidate.getConfig());

		// candidate.getAllocationByteCount()
		int allocationByteCount = ReflectUtils.invoke(int.class, Bitmap.class,
				"getAllocationByteCount", null, candidate, null, true);

		return byteCount <= allocationByteCount;
	}

	private static int getBytesPerPixel(Config config) {
		if (config == Config.ARGB_8888) {
			return 4;
		} else if (config == Config.RGB_565) {
			return 2;
		} else if (config == Config.ARGB_4444) {
			return 2;
		} else if (config == Config.ALPHA_8) {
			return 1;
		}
		return 1;
	}

	/**
	 * Get bitmap from a image file.
	 * @param fileDescriptor
	 * @return
	 */
	public static Bitmap decodeBitmapFromDescriptor(
			FileDescriptor fileDescriptor) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

		// Calculate inSampleSize
		options.inSampleSize = 1;

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;

		// If we're running on Honeycomb or newer, try to use inBitmap
		if (VersionUtils.hasHoneycomb()) {
			addInBitmapOptions(options);
		}

		return BitmapFactory
				.decodeFileDescriptor(fileDescriptor, null, options);
	}

	
	/**
	 * try to ass inBitmap option to Bitmap.
	 * @param options
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static void addInBitmapOptions(BitmapFactory.Options options) {
		options.inMutable = true;
		Bitmap inBitmap = getBitmapFromReusableSet(options);

		if (inBitmap != null) {
			options.inBitmap = inBitmap;
		}
	}

	// ==========================================================================
	// Inner/Nested Classes
	// ==========================================================================
}
