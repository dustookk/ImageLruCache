ImageLruCache
=============

This is a simple implementation of LruCache for Android Development.

Just call ImageLruCache.putImage() to save a Bitmap to memory cache,and call ImageLruCache.getImage(key) to get a Bitmap from memory cache.

And to decode a Bitmap from a image file , please use ImageLruCache.decodeBitmapFromDescriptor(), it helps you to reuse the memory that has been allocated.

For more detail, check this out: <https://developer.android.com/training/displaying-bitmaps/manage-memory.html>
