package com.handysparksoft.flyingbirds;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by davasens on 10/19/2015.
 */
public class AppPreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String LOG_TAG = AppPreferenceChangeListener.class.getSimpleName();
    private Context context;

    public AppPreferenceChangeListener(Context context) {
        this.context = context;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("nickname")) {
            Log.d(LOG_TAG, "Nickname changed to: " + sharedPreferences.getString(key, ""));
        }

    }
}
