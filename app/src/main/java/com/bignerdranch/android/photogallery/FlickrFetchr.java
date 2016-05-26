package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2/28/16.
 */
public class FlickrFetchr {
    private static final String API_KEY = "c60416eee636c06af44c77305f50e63c";
    private static final String TAG = "FlickrFetchr";
    private static final String URL_S = "url_s";

    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter(Parameter.API_KEY, API_KEY)
            .appendQueryParameter(Parameter.FORMAT, "json")
            .appendQueryParameter(Parameter.NO_JSON_CALLBACK, "1")
            .appendQueryParameter(Parameter.EXTRAS, URL_S)
            .build();



    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0)
                out.write(buffer, 0, bytesRead);

            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(Method.FETCH_RECENT, null);
        return downloadGalleryItems(url);
    }
    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(Method.SEARCH, query);
        return downloadGalleryItems(url);
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public void parseItems(List<GalleryItem> items, JSONObject jsonBody) 
            throws IOException, JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            if (!photoJsonObject.has(URL_S))
                continue;

            GalleryItem item = new GalleryItem();
            item.setCaption(photoJsonObject.getString("title"));
            item.setId(photoJsonObject.getString("id"));
            item.setOwner(photoJsonObject.getString("owner"));
            item.setUrl(photoJsonObject.getString(URL_S));

            items.add(item);
        }
    }



    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();

        try {

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            Log.i(TAG, "JSON parsed");
            this.parseItems(items, jsonBody);

        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON response", je);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        }

        return items;
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uri = ENDPOINT.buildUpon().appendQueryParameter(Parameter.METHOD, method);

        if (method.equals(Method.SEARCH))
            uri.appendQueryParameter(Parameter.TEXT, query);

        return uri.build().toString();
    }


    private class Method {
        private static final String FETCH_RECENT = "flickr.photos.getRecent";
        private static final String SEARCH = "flickr.photos.search";
    }
    private class Parameter {
        private static final String API_KEY = "api_key";
        private static final String EXTRAS = "extras";
        private static final String METHOD = "method";
        private static final String NO_JSON_CALLBACK = "nojsoncallback";
        private static final String TEXT = "text";
        private static final String FORMAT = "format";
    }
}
