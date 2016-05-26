package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.List;

public class PollService extends IntentService {

//    private static final long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    private static final int POLL_INTERVAL = 60 * 1000;

    private static final String TAG = "PollService";



    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";

    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String PERMISION_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";



    public static boolean isSerbiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);

        return pi != null;
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        if (isOn) {
            am.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    POLL_INTERVAL,
                    pi
            );
        } else {
            am.cancel(pi);
            pi.cancel();
        }

        QueryPreferences.setAlarmOn(context, isOn);
    }



    public PollService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isNetworkAvailableAndConnected())
            return;

        String lastResultId = QueryPreferences.getLastResultId(this);
        String query = QueryPreferences.getStoredQuery(this);

        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if (items.size() == 0)
            return;

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);

            showBackgrounNotification(0, this.createNotification());

        }

        QueryPreferences.setLastResultId(this, resultId);

    }



    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    private Notification createNotification() {
        Resources resources = getResources();
        Intent i = PhotoGalleryActivity.newIntent(this);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        return new NotificationCompat.Builder(this)
                .setTicker(resources.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

//        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
//        nm.notify(0, notification);
//        sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERMISION_PRIVATE);


    }

    private void showBackgrounNotification(int requestCode, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);

        sendOrderedBroadcast(i, PERMISION_PRIVATE, null, null, Activity.RESULT_OK, null, null);
    }
}
