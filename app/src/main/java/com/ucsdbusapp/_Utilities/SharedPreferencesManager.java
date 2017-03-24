package com.ucsdbusapp._Utilities;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Chris on 9/10/2015.
 */
public class SharedPreferencesManager {

    public static void saveToSharedPreferences(Context context, String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static int getFromSharedPreferences(Context context, String key, int defaultValue) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        return sharedPrefs.getInt(key, defaultValue);
    }

    public static void saveToSharedPreferences(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getFromSharedPreferences(Context context, String key, String defaultValue) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        return sharedPrefs.getString(key, defaultValue);
    }

    public static void removeFromSharedPreferences(Context context, String key)
    {
        SharedPreferences prefs = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.commit();
    }
}