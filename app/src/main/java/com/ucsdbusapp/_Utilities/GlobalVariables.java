package com.ucsdbusapp._Utilities;

import android.app.Application;
import android.content.Context;

/**
 * Created by Chris on 8/6/2016.
 */
public class GlobalVariables extends Application {
    private static Context context;

    @Override
    public void onCreate()
    {
        super.onCreate();
        context = getApplicationContext();

        GPS.initializeGPS(context);
    }

    public static int getLastRouteIndex()
    {
        return SharedPreferencesManager.getFromSharedPreferences(context, "lastRouteIndex", -1);
    }

    public static void setLastRouteIndex(int index)
    {
        SharedPreferencesManager.saveToSharedPreferences(context, "lastRouteIndex", index);
    }

    public static String getLastRouteName()
    {
        return SharedPreferencesManager.getFromSharedPreferences(context, "lastRouteName", "");
    }

    public static void setLastRouteName(String routeName)
    {
        SharedPreferencesManager.saveToSharedPreferences(context, "lastRouteName", routeName);
    }
}
