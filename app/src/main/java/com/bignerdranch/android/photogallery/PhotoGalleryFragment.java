package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2/28/16.
 */
public class PhotoGalleryFragment extends VisibleFragment {

    private static final int minGridWidth = 348;
    private static final String TAG = "PhotoGalleryFragment";


    private List<GalleryItem> mItems = new ArrayList<>();
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;



    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getContext(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                Activity activity = getActivity();
                boolean shouldStartAlarm = !PollService.isSerbiceAlarmOn(activity);
                PollService.setServiceAlarm(activity, shouldStartAlarm);
                activity.invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        initProgressBar(view);
        initRecyclerView(view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.initThumbnailThread();
        this.setHasOptionsMenu(true);
        this.setRetainInstance(true);
        this.updateItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getContext(), query);

                View view = getView();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                updateItems();
                return true;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getContext());
                searchView.setQuery(query, false);
            }
        });

        MenuItem togleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isSerbiceAlarmOn(getActivity())) {
            togleItem.setTitle(R.string.stop_polling);
        } else {
            togleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background Thread destryed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void initPollService() {
//        Intent i = PollService.newIntent(getActivity());
//        getActivity().startService(i);

        PollService.setServiceAlarm(getActivity(), true);
    }
    private void initProgressBar(View view) {
        mProgressBar = (ProgressBar) view.findViewById(R.id.photo_gallery_progress_bar);
    }
    private void initRecyclerView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.photo_gallery_recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mRecyclerView
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = mRecyclerView.getWidth();
                        int count = width / minGridWidth;

                        mRecyclerView.setLayoutManager( new GridLayoutManager(
                                getActivity(),
                                width / minGridWidth
                        ));
                        mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        Log.i(TAG, "onGlobalLayout: " + count);
                    }
                });
        setupAdapter();
    }
    private void initThumbnailThread() {
        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler, getActivity());
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        Log.i(TAG, "Background Thread started");
    }
    private void setupAdapter() {
        if (isAdded())
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
    }
    private void setupProgressBar(boolean isVisible) {
        if (isAdded()){
            if (isVisible) {
                mProgressBar.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                mProgressBar.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getContext());
        new FetchItemsTask(query).execute();
    }


    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        private String mQuery;



        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();
            setupProgressBar(false);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setupProgressBar(true);
        }
    }
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        List<GalleryItem> mGalleryItems;



        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }


        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_photo, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem item = mGalleryItems.get(position);
            Drawable drawable = getResources().getDrawable(R.drawable.bill_up_close);

            holder.bindDrawable(drawable);
            holder.bindGalleryItem(item);

            mThumbnailDownloader.queueThumbnail(holder, item.getUrl());
        }
    }
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private GalleryItem mGalleryItem;
        private ImageView mImageView;



        public PhotoHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }
}