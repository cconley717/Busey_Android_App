package com.ucsdbusapp._Main;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.betterspinner.BetterSpinner;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.ucsdbusapp.R;
import com.ucsdbusapp._Utilities.CircleCreator;
import com.ucsdbusapp._Utilities.CustomInfoWindow;
import com.ucsdbusapp._Utilities.CustomMapFragment;
import com.ucsdbusapp._Utilities.GPS;
import com.ucsdbusapp._Utilities.GlobalVariables;
import com.ucsdbusapp._Utilities.GooglePlayRateHandler;
import com.ucsdbusapp._Utilities.MapWrapperLayout;
import com.ucsdbusapp._Utilities.UCSD_Bus_Server_Request;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pnikosis.materialishprogress.ProgressWheel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import at.markushi.ui.CircleButton;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class Bus_Map extends AppCompatActivity {

    private Bus_Map instance;
    private Activity activity;

    private GoogleMap googleMap;
    private BetterSpinner routesSpinner;

    // Bus Route IDs
    private final int CITY_SHUTTLE_ROUTE = 2092;
    private final int COASTER_EAST_ROUTE = 312;
    private final int COASTER_EAST_WEST_ROUTE = 314;
    private final int CHANCELLOR_ROUTE = 3168;
    private final int HILLCREST_AM_ROUTE = 1263;
    private final int HILLCREST_PM_ROUTE = 1264;
    private final int CLOCKWISE_CAMPUS_LOOP_ROUTE = 1114;
    private final int EVENING_CLOCKWISE_CAMPUS_LOOP_ROUTE = 3442;
    private final int COUNTER_CLOCKWISE_CAMPUS_LOOP_ROUTE = 1113;
    private final int EVENING_COUNTER_CLOCKWISE_CAMPUS_LOOP_ROUTE = 3440;
    private final int MESA_ROUTE = 3159;
    private final int REGENTS_ROUTE = 1098;
    private final int SIO_LOOP_ROUTE = 2399;
    private final int SANFORD_CONSORTIUM_ROUTE = 1434;
    private final int COASTER_WEST_ROUTE = 313;
    private final int EAST_CAMPUS_CONNECTION = 3849;

    private CircleButton findUserLocation;
    private CircleButton resetMapPosition;
    private CircleButton aboutAuthor;

    private ProgressWheel progressWheel;

    private JSONArray busRoutes;
    private ArrayList<Marker> busMarkers = new ArrayList<>();
    private HashMap<Marker, JSONObject> stopMarkers = new HashMap<>();
    private ArrayList<JSONObject> routeLocationsArray;
    private ArrayAdapter<String> sortAdapter;

    private Timer busRefreshTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus__map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        instance = this;
        activity = this;

        registerResources();
        registerListeners();

        configureGoogleMap();

        initializeRoutesSpinner();
        getBusRoutes();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopBusRefreshTimer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Bus_MapPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void registerResources() {
        routesSpinner = (BetterSpinner) findViewById(R.id.routesSpinner);

        findUserLocation = (CircleButton) findViewById(R.id.findUserLocation);
        resetMapPosition = (CircleButton) findViewById(R.id.resetMapPosition);
        aboutAuthor = (CircleButton) findViewById(R.id.aboutAuthor);

        progressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);
        progressWheel.setBarColor(Color.parseColor("#90caf9"));
        progressWheel.spin();
    }

    // Click Listener
    private void registerListeners() {
        routesSpinner.setOnItemClickedListener(new BetterSpinner.OnItemClicked() {
            @Override
            public void onItemClicked(AdapterView<?> adapterView, View view, int i, long l) {
                GlobalVariables.setLastRouteName(routesSpinner.getText().toString());
                GlobalVariables.setLastRouteIndex(i);
                goToRoute(i);
            }
        });

        // Reset Map Location to UCSD
        resetMapPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleMap.clear();

                stopBusRefreshTimer();

                moveToLocation(14.220199f, 32.8806346048968f, -117.23714388906954f);
            }
        });

        findUserLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bus_MapPermissionsDispatcher.goToUsersLocationWithCheck(instance);
                //goToUsersLocation();
            }
        });

        aboutAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(activity)
                        .setTitle("UCSD Bus App")
                        .setMessage("This app was created by Chris Conley because he remembered back when he was a scrub and how he got lost a lot at UCSD.")
                        .setPositiveButton("Rate", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                GooglePlayRateHandler.openAppStore(activity);
                            }
                        })
                        .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        });
    }

    @NeedsPermission({Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    public void goToUsersLocation()
    {
        GPS.getUsersLocation(new GPS.OnLocationReceivedListener() {
            @Override
            public void onLocationReceived(Location location, boolean success) {
                Log.d("testing", "settinglocation");
                moveToLocation(14.220199f, (float) location.getLatitude(), (float) location.getLongitude());

                /*
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .title("You!")
                        .snippet("You are here."));
                        */
            }
        });
    }

    @OnShowRationale({Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    void showRationaleForGPS(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage("Busey requires access to your device's GPS in order to find your location.\n\nTouch 'ok' to proceed.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        request.proceed();
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    void showDeniedForGPS() {
        //Toast.makeText(this, "unable to continue", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    void showNeverAskForGPS() {
        //Toast.makeText(this, "never ask", Toast.LENGTH_SHORT).show();
    }

    private void goToRoute(int i)
    {
        JSONObject route = routeLocationsArray.get(i);

        try {
            int routeID = route.getInt("routeID");
            float zoomLevel = (float) route.getDouble("zoomLevel");
            float latitude = (float) route.getDouble("latitude");
            float longitude = (float) route.getDouble("longitude");

            moveToLocation(zoomLevel, latitude, longitude);
            showRouteOnMap(routeID);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void stopBusRefreshTimer()
    {
        if(busRefreshTimer != null)
            busRefreshTimer.cancel();
    }

    private void configureGoogleMap() {

        CustomMapFragment customMapFragment = ((CustomMapFragment) getSupportFragmentManager().findFragmentById(findViewById(R.id.map).getId()));
        customMapFragment.setOnDragListener(new MapWrapperLayout.OnDragListener() {
            @Override
            public void onDrag(MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                }
            }
        });

        customMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMapTemp) {
                googleMap = googleMapTemp;

                googleMap.setInfoWindowAdapter(new CustomInfoWindow(activity));

                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {

                    }
                });

                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        Log.d("testing", "zoom: " + googleMap.getCameraPosition().zoom + " Latitude: " + googleMap.getCameraPosition().target.latitude + " Longitude: " + googleMap.getCameraPosition().target.longitude);
                    }
                });

                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(final Marker marker) {

                        marker.showInfoWindow();

                        if (stopMarkers.containsKey(marker)) {
                            JSONObject stopMetaData = stopMarkers.get(marker);
                            showBusStopRoutesAndArrivalTimes(stopMetaData, marker);

                            float latitude = (float) marker.getPosition().latitude;
                            float longitude = (float) marker.getPosition().longitude;
                            moveToLocation(googleMap.getCameraPosition().zoom, latitude, longitude);
                        }

                        return true;
                    }
                });

                googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(Marker marker) {
                        Log.d("testing", "info window touched");
                        marker.hideInfoWindow();

                    }
                });

                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                //googleMap.setMyLocationEnabled(false);

                moveToLocation(14.220199f, 32.8806346048968f, -117.23714388906954f);
            }
        });
    }

    // Changes the user's view
    private void moveToLocation(float zoomLevel, float latitude, float longitude) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoomLevel), 1000, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private void showRouteOnMap(int routeNumber) {
        googleMap.clear();
        progressWheel.setVisibility(View.VISIBLE);

        showPathOnMap(routeNumber);
        showBusesOnMap(routeNumber);
        showBusStopsOnMap(routeNumber);
    }

    private void getBusRoutes() {
        new UCSD_Bus_Server_Request().getBusRoutes(new UCSD_Bus_Server_Request.OnServerRespondListener() {
            @Override
            public void onServerRespond(boolean success, String jsonString) {
                if (success) {
                    Log.d("testing", jsonString);

                    try {
                        busRoutes = new JSONArray(jsonString);

                        configureBusRoutesInteraction();

                        int lastRoute = GlobalVariables.getLastRouteIndex();
                        String lastRouteName = GlobalVariables.getLastRouteName();
                        if(lastRoute > -1 && !lastRouteName.equals("")) {
                            routesSpinner.setHint(lastRouteName);
                            goToRoute(lastRoute);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getBusRoutes();
                        }
                    }, 3000);
                }
            }
        });
    }

    private void showPathOnMap(int routeNumber) {
        new UCSD_Bus_Server_Request().getRoutePath(routeNumber, new UCSD_Bus_Server_Request.OnServerRespondListener() {
            @Override
            public void onServerRespond(boolean success, String jsonString) {
                if (success) {
                    Log.d("testing", jsonString);

                    PolylineOptions options = new PolylineOptions().width(10).color(Color.BLUE).geodesic(true);

                    try {
                        JSONArray mainArray = new JSONArray(jsonString);
                        for (int i = 0; i < mainArray.length(); i++) {
                            JSONArray temp = mainArray.getJSONArray(i);
                            for (int j = 0; j < temp.length(); j++) {
                                JSONObject coordinates = temp.getJSONObject(j);

                                float latitude = (float) coordinates.getDouble("Latitude");
                                float longitude = (float) coordinates.getDouble("Longitude");

                                options.add(new LatLng(latitude, longitude));
                            }
                        }

                        googleMap.addPolyline(options);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showBusesOnMap(final int routeNumber) {

        if(busRefreshTimer == null)
            busRefreshTimer = new Timer();
        else {
            busRefreshTimer.cancel();
            busRefreshTimer = new Timer();
        }

        busRefreshTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        progressWheel.setVisibility(View.VISIBLE);
                    }
                });

                new UCSD_Bus_Server_Request().getBusesWithRoute(routeNumber, new UCSD_Bus_Server_Request.OnServerRespondListener() {
                    @Override
                    public void onServerRespond(boolean success, final String jsonString) {
                        if (success) {
                            Log.d("testing", jsonString);

                            for (int i = 0; i < busMarkers.size(); i++)
                                busMarkers.get(i).remove();

                            try {
                                JSONArray busArray = new JSONArray(jsonString);

                                for (int i = 0; i < busArray.length(); i++) {
                                    String busType = busArray.getJSONObject(i).getString("IconPrefix");
                                    String busName = busArray.getJSONObject(i).getString("Name");
                                    float busLatitude = (float) busArray.getJSONObject(i).getDouble("Latitude");
                                    float busLongitude = (float) busArray.getJSONObject(i).getDouble("Longitude");
                                    int stopped = busArray.getJSONObject(i).getInt("DoorStatus");
                                    String heading = busArray.getJSONObject(i).getString("Heading");

                                    int rotation;
                                    switch (heading) {
                                        case "N":
                                            rotation = -90;
                                            break;
                                        case "NE":
                                            rotation = -45;
                                            break;
                                        case "E":
                                            rotation = 0;
                                            break;
                                        case "SE":
                                            rotation = 45;
                                            break;
                                        case "S":
                                            rotation = 90;
                                            break;
                                        case "SW":
                                            rotation = 135;
                                            break;
                                        case "W":
                                            rotation = 180;
                                            break;
                                        default:
                                            rotation = -135;
                                            break;
                                    }

                                    MarkerOptions markerOptions = new MarkerOptions()
                                            .position(new LatLng(busLatitude, busLongitude))
                                            .title(busName)
                                            .snippet(busType)
                                            .rotation(rotation);

                                    if (stopped == 0)
                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_moving));
                                    else
                                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stopped));


                                    Marker marker = googleMap.addMarker(markerOptions);

                                    busMarkers.add(marker);
                                }
                            } catch (JSONException e) {
                                Log.d("testing", "showBusesOnMap: " + e.getMessage());
                            }
                        } else {
                            Log.d("testing", "bad");
                        }
                        progressWheel.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }, 0, 5000);
    }

    private void showBusStopsOnMap(final int routeID) {
        new UCSD_Bus_Server_Request().getRouteStops(routeID, new UCSD_Bus_Server_Request.OnServerRespondListener() {
            @Override
            public void onServerRespond(boolean success, String jsonString) {
                if (success) {
                    Log.d("testing", jsonString);

                    try {
                        JSONArray stopsArray = new JSONArray(jsonString);
                        for (int i = 0; i < stopsArray.length(); i++) {
                            float stopLatitude = (float) stopsArray.getJSONObject(i).getDouble("Latitude");
                            float stopLongitude = (float) stopsArray.getJSONObject(i).getDouble("Longitude");
                            int stopID = stopsArray.getJSONObject(i).getInt("ID");
                            String stopName = stopsArray.getJSONObject(i).getString("Name");
                            String stopNumber = stopsArray.getJSONObject(i).getString("RtpiNumber");

                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(new LatLng(stopLatitude, stopLongitude))
                                    .title(stopName)
                                    .snippet("Stop #" + stopNumber + "\n")
                                    .icon(BitmapDescriptorFactory.fromBitmap(CircleCreator.createColoredCircle(10)));

                            Marker marker = googleMap.addMarker(markerOptions);

                            JSONObject stopMetaData = new JSONObject();
                            stopMetaData.put("routeID", routeID);
                            stopMetaData.put("stopID", stopID);
                            stopMetaData.put("originalSnippet", marker.getSnippet());

                            stopMarkers.put(marker, stopMetaData);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showBusStopRoutesAndArrivalTimes(JSONObject stopMetaData, final Marker marker) {
        try {
            final int routeID = stopMetaData.getInt("routeID");
            final int stopID = stopMetaData.getInt("stopID");
            String originalSnippet = stopMetaData.getString("originalSnippet");

            marker.setSnippet(originalSnippet);

            new UCSD_Bus_Server_Request().getRoutesForStop(stopID, new UCSD_Bus_Server_Request.OnServerRespondListener() {
                @Override
                public void onServerRespond(boolean success, final String routesJSON) {
                    if (success) {
                        Log.d("testing", routesJSON);

                        new UCSD_Bus_Server_Request().getArrivalTimes(routeID, stopID, new UCSD_Bus_Server_Request.OnServerRespondListener() {
                            @Override
                            public void onServerRespond(boolean success, String arrivalsJSON) {
                                if (success) {
                                    Log.d("testing", arrivalsJSON);

                                    try {
                                        JSONArray routes = new JSONArray(routesJSON);
                                        JSONArray arrivals = new JSONObject(arrivalsJSON).getJSONArray("Predictions");

                                        JSONObject current;

                                        for (int i = 0; i < routes.length(); i++) {
                                            current = routes.getJSONObject(i);
                                            marker.setSnippet(marker.getSnippet() + "\n" + current.getString("Name"));
                                        }


                                        if(arrivals.length() > 0) {
                                            marker.setSnippet(marker.getSnippet() + "\n");

                                            for (int i = 0; i < arrivals.length(); i++) {
                                                current = arrivals.getJSONObject(i);
                                                marker.setSnippet(marker.getSnippet() + "\n" + "Bus #" + current.getString("BusName") + " arriving in " + current.getInt("Minutes") + " minutes");
                                            }
                                        }

                                        marker.showInfoWindow();

                                    } catch (JSONException e) {
                                        Log.d("testing", e.getMessage());
                                    }

                                }
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeRoutesSpinner() {
        routesSpinner.setHint("Loading bus routes...");
        routesSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_color, new LinkedList<String>()));
    }

    private void configureBusRoutesInteraction() {
        try {
            List<String> routeNames = new LinkedList<>();
            routeLocationsArray = new ArrayList<>();

            JSONObject route;
            JSONObject routeLocationInfo;
            int routeID;
            for (int i = 0; i < busRoutes.length(); i++) {
                route = busRoutes.getJSONObject(i);

                routeNames.add(route.getString("Name")); //DisplayName

                routeID = route.getInt("ID");

                routeLocationInfo = getRouteLocationInfo(routeID);
                routeLocationsArray.add(routeLocationInfo);
            }

            sortAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_color, routeNames);
            routesSpinner.setAdapter(sortAdapter);

            routesSpinner.setHint("Select a Bus Route");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
    The locations of bus routes are not provided by UCSDBus.com.
    They must be hard coded.
     */
    private JSONObject getRouteLocationInfo(int routeID) {
        JSONObject routeLocation = new JSONObject();

        try {
            routeLocation.put("routeID", routeID);

            switch (routeID) {
                case CITY_SHUTTLE_ROUTE:
                    routeLocation.put("zoomLevel", 14.181455f);
                    routeLocation.put("latitude", 32.86911611644047f);
                    routeLocation.put("longitude", -117.22821582108736f);
                    break;
                case CLOCKWISE_CAMPUS_LOOP_ROUTE:
                    routeLocation.put("zoomLevel", 14.408011f);
                    routeLocation.put("latitude", 32.88058110733446f);
                    routeLocation.put("longitude", -117.23654072731733f);
                    break;
                case EVENING_CLOCKWISE_CAMPUS_LOOP_ROUTE:
                    routeLocation.put("zoomLevel", 14.408011f);
                    routeLocation.put("latitude", 32.88058110733446f);
                    routeLocation.put("longitude", -117.23654072731733f);
                    break;
                case COUNTER_CLOCKWISE_CAMPUS_LOOP_ROUTE:
                    routeLocation.put("zoomLevel", 14.408011f);
                    routeLocation.put("latitude", 32.88058110733446f);
                    routeLocation.put("longitude", -117.23654072731733f);
                    break;
                case EVENING_COUNTER_CLOCKWISE_CAMPUS_LOOP_ROUTE:
                    routeLocation.put("zoomLevel", 14.408011f);
                    routeLocation.put("latitude", 32.88058110733446f);
                    routeLocation.put("longitude", -117.23654072731733f);
                    break;
                case COASTER_EAST_ROUTE:
                    routeLocation.put("zoomLevel", 13.630302f);
                    routeLocation.put("latitude", 32.887070970675424f);
                    routeLocation.put("longitude", -117.22676508128642f);
                    break;
                case COASTER_WEST_ROUTE:
                    routeLocation.put("zoomLevel", 13.60606f);
                    routeLocation.put("latitude", 32.885407583754755f);
                    routeLocation.put("longitude", -117.2330991178751f);
                    break;
                case COASTER_EAST_WEST_ROUTE:
                    routeLocation.put("zoomLevel", 13.6584215f);
                    routeLocation.put("latitude", 32.88784634382045f);
                    routeLocation.put("longitude", -117.22984191030262f);
                    break;
                case HILLCREST_AM_ROUTE:
                    routeLocation.put("zoomLevel", 11.544835f);
                    routeLocation.put("latitude", 32.81261737770714f);
                    routeLocation.put("longitude", -117.19200767576694f);
                    break;
                case HILLCREST_PM_ROUTE:
                    routeLocation.put("zoomLevel", 11.544835f);
                    routeLocation.put("latitude", 32.81261737770714f);
                    routeLocation.put("longitude", -117.19200767576694f);
                    break;
                case MESA_ROUTE:
                    routeLocation.put("zoomLevel", 13.84421f);
                    routeLocation.put("latitude", 32.872835164233265f);
                    routeLocation.put("longitude", -117.2305067628622f);
                    break;
                case REGENTS_ROUTE:
                    routeLocation.put("zoomLevel", 14.426807f);
                    routeLocation.put("latitude", 32.881491124629385f);
                    routeLocation.put("longitude", -117.22721099853517f);
                    break;
                case CHANCELLOR_ROUTE:
                    routeLocation.put("zoomLevel", 14.736957f);
                    routeLocation.put("latitude", 32.87715606987217f);
                    routeLocation.put("longitude", -117.21317902207375f);
                    break;
                case SIO_LOOP_ROUTE:
                    routeLocation.put("zoomLevel", 14.627642f);
                    routeLocation.put("latitude", 32.87027969082793f);
                    routeLocation.put("longitude", -117.24596466869117f);
                    break;
                case SANFORD_CONSORTIUM_ROUTE:
                    routeLocation.put("zoomLevel", 14.01568f);
                    routeLocation.put("latitude", 32.88050142406329f);
                    routeLocation.put("longitude", -117.23274070769548f);
                    break;
                case EAST_CAMPUS_CONNECTION:
                    routeLocation.put("zoomLevel", 14.181455f);
                    routeLocation.put("latitude", 32.876358076827714f);
                    routeLocation.put("longitude", -117.2180824354291f);
                    break;
                default:
                    routeLocation = null;
                    break;
            }
        } catch (JSONException e) {
            routeLocation = null;
            e.printStackTrace();
        }

        return routeLocation;
    }
}