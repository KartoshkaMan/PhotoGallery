package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by user on 3/4/16.
 */
public class QueryPreferences {

    private static final String PREF_IS_ALARM_ON = "isAlarmOn";
    private static final String PREF_LAST_RESULT_ID = "lastResultId";
    private static final String PREF_SEARCH_QUERY = "searchQuery";



    public static boolean isAlarmOn(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_ALARM_ON, false);
    }
    public static String getLastResultId(Context context) {
        return get(context, PREF_LAST_RESULT_ID);
    }
    public static String getStoredQuery(Context context) {
        return get(context, PREF_SEARCH_QUERY);
    }

    public static void setAlarmOn(Context context, boolean isOn) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON, isOn)
                .apply();
    }
    public static void setLastResultId(Context context, String lastResultId) {
        set(context, PREF_LAST_RESULT_ID, lastResultId);
    }
    public static void setStoredQuery(Context context, String query) {
        set(context, PREF_SEARCH_QUERY, query);
    }



    private static String get(Context context, String prefName) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(prefName, null);
    }

    private static void set(Context context, String prefName, String pref) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(prefName, pref)
                .apply();
    }
}
