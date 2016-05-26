package com.bignerdranch.android.photogallery;

import android.net.Uri;

/**
 * Created by user on 2/29/16.
 */
public class GalleryItem {

    private String mCaption;
    private String mId;
    private String mOwner;
    private String mUrl;


    public String getCaption() {
        return mCaption;
    }
    public String getId() {
        return mId;
    }
    public String getOwner() {
        return mOwner;
    }
    public String getUrl() {
        return mUrl;
    }
    @Override
    public String toString() {
        return mCaption;
    }

    public Uri getPhotoPageUri() {
        return Uri.parse("http://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }



    public void setCaption(String caption) {
        mCaption = caption;
    }
    public void setId(String id) {
        mId = id;
    }
    public void setOwner(String owner) {
        mOwner = owner;
    }
    public void setUrl(String url) {
        mUrl = url;
    }
}
