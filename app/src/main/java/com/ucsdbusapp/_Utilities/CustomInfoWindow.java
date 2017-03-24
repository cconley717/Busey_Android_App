package com.ucsdbusapp._Utilities;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;


/**
 * Created by Chris on 3/26/2015.
 */
public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

    private Activity activity;

    public CustomInfoWindow(Activity activity) {
        this.activity = activity;
    }

    @Override
    public View getInfoContents(Marker marker) {

        LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        title.setText(marker.getTitle());

        TextView snippet = new TextView(activity);
        snippet.setTextColor(Color.GRAY);
        snippet.setText(marker.getSnippet());

        info.addView(title);
        info.addView(snippet);

        return info;
    }

    @Override
    public View getInfoWindow(final Marker marker) {

        return null;
    }
}