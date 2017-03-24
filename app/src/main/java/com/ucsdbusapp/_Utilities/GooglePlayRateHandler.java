package com.ucsdbusapp._Utilities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Chris on 6/24/2016.
 */
public class GooglePlayRateHandler {

    public static void openAppStore(Activity activity) {
        Uri uri = Uri.parse("market://details?id=" + activity.getPackageName());

        Log.d("testing", "package name: " + activity.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            activity.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "Couldn't open the app store for some reason..", Toast.LENGTH_LONG).show();
        }
    }
}
