package com.bignerdranch.android.photogallery;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MULTIPLIER_BYTES = 1024;
    private static final String TAG = "ThumbnailDownloader";

    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ThumbnailCache mThumbnailCache;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;



    public ThumbnailDownloader(Handler responseHandler, Context context) {
        super(TAG);

        mResponseHandler = responseHandler;

        // Init cache with size
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int maxMem = am.getMemoryClass() * MULTIPLIER_BYTES;
        mThumbnailCache = new ThumbnailCache(maxMem / 8);
    }


    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got URL: " + url);

        if (url == null)
            mRequestMap.remove(target, url);
        else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }



    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);

            if (url == null)
                return;

            Bitmap cachedBitmap = mThumbnailCache.get(url);
            final Bitmap bitmap;
            if (cachedBitmap == null) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mThumbnailCache.put(url, bitmap);
                Log.i(TAG, "Bitmap created and cached");
            } else {
                bitmap = cachedBitmap;
                Log.i(TAG, "Bitmap loaded from cache");
            }

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url)
                        return;

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }



    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    private class ThumbnailCache extends LruCache<String, Bitmap> {

        public ThumbnailCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / MULTIPLIER_BYTES;
        }
    }
}
