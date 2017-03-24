package com.ucsdbusapp._Utilities;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


/**
 * Created by Chris on 6/23/2015.
 */
public class GPS {

    private static Context context;
    private static GoogleApiClient mGoogleApiClient;

    private static boolean isGettingUsersLocation = false;

    private static Handler handler = new Handler();

    private static boolean debug = true;
    private static String TAG = "testing";


    private static OnLocationReceivedListener onLocationReceivedListener;
    public interface OnLocationReceivedListener {
        void onLocationReceived(Location location, boolean success);
    }


    public static void initializeGPS(final Context context) {
        GPS.context = context;

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {

                @Override
                public void onConnected(Bundle bundle) {
                    Log.d(TAG, "FusedLocation Connected");

                    if (isGettingUsersLocation)
                        getUsersLocation(onLocationReceivedListener);
                    else
                        isGettingUsersLocation = false;
                }

                @Override
                public void onConnectionSuspended(int i) {
                    Log.d(TAG, "FusedLocation Suspended: " + i);
                    onLocationReceivedListener.onLocationReceived(null, false);
                    isGettingUsersLocation = false;
                }

            }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {

                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    Log.d(TAG, "FusedLocation Failed: " + connectionResult.getErrorCode());
                    onLocationReceivedListener.onLocationReceived(null, false);
                    isGettingUsersLocation = false;
                }

            }).build();

            mGoogleApiClient.connect();

        } else if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.reconnect();
        }
    }

    public static void getUsersLocation(final OnLocationReceivedListener listener) {
        isGettingUsersLocation = true;
        onLocationReceivedListener = listener;

        if(mGoogleApiClient == null || (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()))
        {
            initializeGPS(context);
        }
        else if (!mGoogleApiClient.isConnected() && mGoogleApiClient.isConnecting())
        {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("testing", "isConnecting handler");
                    getUsersLocation(listener);
                }
            }, 3000);
        }
        else if(mGoogleApiClient.isConnected())
        {
            LocationRequest mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {

                    isGettingUsersLocation = false;
                    onLocationReceivedListener.onLocationReceived(location, true);
                    mGoogleApiClient.disconnect();
                    Log.d("testing", "GOT LOCATION");
                }
            });
        }
    }

    public static boolean isIsGettingUsersLocation()
    {
        return isGettingUsersLocation;
    }

    private static void debugOutput(String output)
    {
        if(debug)
        {
            Log.d("testing", output);
        }
    }
}
