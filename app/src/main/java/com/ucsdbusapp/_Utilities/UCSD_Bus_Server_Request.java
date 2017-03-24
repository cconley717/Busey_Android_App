package com.ucsdbusapp._Utilities;

import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by Chris on 10/5/2015.
 */
public class UCSD_Bus_Server_Request {

    private OkHttpClient client = new OkHttpClient();

    private OnServerRespondListener onServerRespondListener;

    public interface OnServerRespondListener {
        public void onServerRespond(boolean success, String jsonString);
    }

    public void getBusRoutes(OnServerRespondListener listener) {
        onServerRespondListener = listener;
        sendRequest("http://ucsdbus.com/Region/0/Routes");
    }

    public void getBusesWithRoute(int routeNumber, OnServerRespondListener listener) {
        onServerRespondListener = listener;
        sendRequest("http://ucsdbus.com/Route/" + routeNumber + "/vehicles");
    }

    public void getRoutePath(int routeNumber, OnServerRespondListener listener) {
        onServerRespondListener = listener;
        sendRequest("http://ucsdbus.com/Route/" + routeNumber + "/waypoints");
    }
    public void getRouteStops(int routeNumber, OnServerRespondListener listener) {
        onServerRespondListener = listener;
        sendRequest("http://ucsdbus.com/Route/" + routeNumber + "/direction/0/stops");
    }

    public void getRoutesForStop(int stopNumber, OnServerRespondListener listener) {
        onServerRespondListener = listener;
        sendRequest("http://ucsdbus.com/Stop/" + stopNumber + "/routes");
    }

    public void getArrivalTimes(int routeID, int stopID, OnServerRespondListener listener) {
        onServerRespondListener = listener;
        sendRequest("http://www.ucsdbus.com/Route/" + routeID + "/Stop/" + stopID + "/Arrivals");
    }

    private void sendRequest(final String url) {

        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(Void... params) {

                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try {
                    Response response = client.newCall(request).execute();

                    if (response.code() == 200) {
                        String body = response.body().string();
                        response.body().close();

                        return body;
                    } else {
                        response.body().close();
                        return null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null)
                    onServerRespondListener.onServerRespond(true, result);
                else
                    onServerRespondListener.onServerRespond(false, null);
            }
        }.execute(null, null, null);
    }
}