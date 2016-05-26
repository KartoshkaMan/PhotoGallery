package com.bignerdranch.android.photogallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Created by user on 3/12/16.
 */
public class PhotoPageFragment extends VisibleFragment {

    private static final String ARG_URI = "photo_page_uri";

    private ProgressBar mProgressBar;
    private Uri mUri;
    private WebView mWebView;

    public static PhotoPageFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);

        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public boolean canGoBack() {
        return mWebView.canGoBack();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_page, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.fragment_photo_page_progress_bar);
        mProgressBar.setMax(100);

        mWebView = (WebView) view.findViewById(R.id.fragment_photo_page_web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setTitle(title);
            }
        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
                    return false;
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(uri);
                    startActivity(i);
                    return true;
                }
            }


        });
        mWebView.loadUrl(mUri.toString());

        return view;
    }


    public void goBack() {
        mWebView.goBack();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUri = getArguments().getParcelable(ARG_URI);
    }
}
