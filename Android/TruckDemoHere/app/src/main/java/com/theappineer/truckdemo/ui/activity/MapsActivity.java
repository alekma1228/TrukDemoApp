package com.theappineer.truckdemo.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.here.android.mpa.guidance.NavigationManager;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.light.Position;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.theappineer.truckdemo.AppConstant;
import com.theappineer.truckdemo.R;
import com.theappineer.truckdemo.network.CCService;
import com.theappineer.truckdemo.ui.MyProgressDialog;
import com.theappineer.truckdemo.utils.HttpRequestTask;
import com.theappineer.truckdemo.utils.MessageUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.mapbox.api.directions.v5.DirectionsCriteria.OVERVIEW_FULL;
import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {
    private MapView mapView;
    private MapboxMap mMapboxMap;
    public MyProgressDialog dlg_progress;

    private double curLat;
    private double curLng;
    private double destLat;
    private double destLng;
    private float length;
    private float width;
    private float height;
    private float weight;
    private int axle;


    private Retrofit mRetrofit;
    private CCService mCCService;

    private PermissionsManager permissionsManager;
    private Location originLocation;
    private ArrayList<Point> waypoints = new ArrayList<>();
    private ArrayList<Point> orgWaypoints = new ArrayList<>();
    private ArrayList<LatLng> addedWaypoints = new ArrayList<>();
    private ArrayList<ArrayList<Point>> waypointSegmentArray = new ArrayList<>();
    private List<DirectionsRoute> routes = new ArrayList<>();
    private PolylineOptions preDrawPoints;
    private static final int COUNT_PER_SEGMENT = 98;
    private int segmentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_maps);

        ButterKnife.bind(this);

        dlg_progress = new MyProgressDialog(this);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        curLat = getIntent().getDoubleExtra("CurLat", 0);
        curLng = getIntent().getDoubleExtra("CurLng", 0);
        destLat = getIntent().getDoubleExtra("DestLat", 0);
        destLng = getIntent().getDoubleExtra("DestLng", 0);
        length = getIntent().getFloatExtra("Length", 0);
        width = getIntent().getFloatExtra("Width", 0);
        height = getIntent().getFloatExtra("Height", 0);
        weight = getIntent().getFloatExtra("Weight", 0);
        axle = getIntent().getIntExtra("Axle", 4);
        int a=axle;
    }
    @OnClick(R.id.btn_navigation)
    public void onNavigation(){
        if (routes.isEmpty())
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Navigation");
        alertDialogBuilder.setMessage("Choose Mode");
        alertDialogBuilder.setNegativeButton("Navigation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                Intent intent = new Intent(MapsActivity.this, MapBoxNavigationActivity.class);
                intent.putExtra("simulate", false);
                MapBoxNavigationActivity.routes = routes;
                startActivity(intent);
            };
        });
        alertDialogBuilder.setPositiveButton("Simulation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                Intent intent = new Intent(MapsActivity.this, MapBoxNavigationActivity.class);
                intent.putExtra("simulate", true);
                MapBoxNavigationActivity.routes = routes;
                startActivity(intent);
            };
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

//        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
//                .directionsRoute(routes.get(0))
//                .shouldSimulateRoute(true)
//                .build();
//        NavigationLauncher.startNavigation(MapsActivity.this, options);
    }
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
//            LocationComponent locationComponent = mMapboxMap.getLocationComponent();
//            locationComponent.activateLocationComponent(this);
//            locationComponent.setLocationComponentEnabled(true);
//            locationComponent.setCameraMode(CameraMode.TRACKING);
//            originLocation = locationComponent.getLastKnownLocation();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "user location permission explanation", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent();
        } else {
            Toast.makeText(this, "location permission not granted", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        boolean param = hasCapture;
        param = !param;
    }
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        mMapboxMap = mapboxMap;
        enableLocationComponent();
        mMapboxMap.addMarker(new MarkerOptions().position(new LatLng(curLat, curLng)));
        mMapboxMap.addMarker(new MarkerOptions().position(new LatLng(destLat, destLng)));
        mMapboxMap.setOnMapClickListener(this);

        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(curLat, curLng))
                .zoom(14)
                .build();
        mMapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);

        getTruckRouteFromBingMap();
    }
    @Override
    public void onMapClick(@NonNull LatLng point) {
        addedWaypoints.add(point);
        getTruckRouteFromBingMap();
    }

    private void getTruckRouteFromBingMap(){
        dlg_progress.show();

//        curLat = 25.7616798;
//        curLng = -80.1917902;
//        destLat = 40.7127753;
//        destLng = -74.0059728;
//        height = 3;
//        width = 2.5F;
//        length = 8;
//        weight = 10;
//        axle = 4;

        String strStart = String.valueOf(curLat) + "," + String.valueOf(curLng);
        String strDest = String.valueOf(destLat) + "," + String.valueOf(destLng);
        String strHeight = String.valueOf(height);
        String strWidth = String.valueOf(width);
        String strLength = String.valueOf(length);
        String strWeight = String.valueOf(weight * 1000);
        String strAxle = String.valueOf(axle);

        String url = "http://dev.virtualearth.net/REST/v1/Routes/TruckAsync?";
        url= url + "waypoint.1=" + strStart;
        for(int i=0; i<addedWaypoints.size(); i++){
            String strWayPoint = String.valueOf(addedWaypoints.get(i).getLatitude()) + "," + String.valueOf(addedWaypoints.get(i).getLongitude());
            url= url + "&waypoint." + String.valueOf(i+2) + "=" + strWayPoint;
        }
        url= url + "&waypoint." + String.valueOf(addedWaypoints.size()+2) + "=" + strDest;
        url = url + "&optimize=time";  // timeWithTraffic, time
        url = url + "&routeAttributes=routePath";
        url = url + "&weightUnit=kg";
        url = url + "&dimensionUnit=m";
        url = url + "&vehicleHeight=" + strHeight;
        url = url + "&vehicleWidth=" +  strWidth;
        url = url + "&vehicleLength=" + strLength;
        url = url + "&vehicleWeight=" + strWeight;
        url = url + "&vehicleAxles=" + strAxle;
        url = url + "&key=" +  AppConstant.bingmap_key;

        new HttpRequestTask.HttpAsyncTask(dlg_progress, mBingMapResponseListener).execute(url, "");
    }
    private void getStatusRequestToBingMap(String requestId){
        mRetrofit = new Retrofit.Builder()
                .baseUrl(AppConstant.bingmap_base_url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mCCService = mRetrofit.create(CCService.class);

        Call<ResponseBody> callToServer = mCCService.getTruckRoutesStatusRequest(requestId, AppConstant.bingmap_key);

        callToServer.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.body() == null) {
                    showAlertNotGetRoute();
                    dlg_progress.dismiss();
                    return;
                }

                try {
                    String ret = response.body().string();
                    JSONObject responseObject = new JSONObject(ret);
                    JSONArray resourceSets = responseObject.getJSONArray("resourceSets");
                    JSONObject item1 = resourceSets.getJSONObject(0);
                    JSONArray resources = item1.getJSONArray("resources");
                    JSONObject item2 = resources.getJSONObject(0);
                    String requestId = item2.getString("requestId");
                    String resultUrl = item2.getString("resultUrl");
                    downloadResultFromBingMap(resultUrl);
                } catch (IOException e) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getStatusRequestToBingMap(requestId);
                        }
                    }, 1000);
                } catch (JSONException e) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getStatusRequestToBingMap(requestId);
                        }
                    }, 1000);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                MessageUtil.showError(MapsActivity.this, t.getMessage());
                dlg_progress.dismiss();
            }
        });
    }
    private void downloadResultFromBingMap(String resultUrl){
        new HttpRequestTask.HttpAsyncTask(dlg_progress, mDownloadResponseListener).execute(resultUrl, "");
    }

    private HttpRequestTask.HttpResponseListener mBingMapResponseListener = new HttpRequestTask.HttpResponseListener() {
        @Override
        public void onResponse(String ret) {
            try {
                if (!ret.isEmpty()) {
                    JSONObject responseObject = new JSONObject(ret);
                    JSONArray resourceSets = responseObject.getJSONArray("resourceSets");
                    JSONObject item1 = resourceSets.getJSONObject(0);
                    JSONArray resources = item1.getJSONArray("resources");
                    JSONObject item2 = resources.getJSONObject(0);
                    String requestId = item2.getString("requestId");

                    getStatusRequestToBingMap(requestId);
                } else {
                    showAlertNotGetRoute();
                    dlg_progress.dismiss();
                }
            } catch (Exception ex) {
                showAlertNotGetRoute();
                dlg_progress.dismiss();
            }
        }
    };
    private HttpRequestTask.HttpResponseListener mDownloadResponseListener = new HttpRequestTask.HttpResponseListener() {
        @Override
        public void onResponse(String response) {
            try {
                if (!response.isEmpty()) {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray resourceSets = responseObject.getJSONArray("resourceSets");
                    JSONObject item1 = resourceSets.getJSONObject(0);
                    JSONArray resources = item1.getJSONArray("resources");
                    JSONObject item2 = resources.getJSONObject(0);
                    JSONObject routePath = item2.getJSONObject("routePath");
                    JSONObject line = routePath.getJSONObject("line");
                    JSONArray coordinates = line.getJSONArray("coordinates");

                    waypoints.clear();
                    for (int i=0; i<coordinates.length(); i++){
                        JSONArray coordinate = coordinates.getJSONArray(i);
                        double lat = coordinate.getDouble(0);
                        double lng = coordinate.getDouble(1);
                        Point point = Point.fromLngLat(lng, lat);
                        waypoints.add(point);
                    }

                    orgWaypoints.clear();
                    orgWaypoints.addAll(waypoints);
                    getRouteWithMapboxDirections();
                }
            } catch (Exception ex) {
                showAlertNotGetRoute();
                dlg_progress.dismiss();
            }
        }
    };

    private void getRouteWithMapboxDirections(){
        ArrayList<Point> temp = new ArrayList<>();
        ArrayList<LatLng> points = new ArrayList<>();
        for(int i=0; i< waypoints.size(); i++){
            temp.add(waypoints.get(i));
            points.add(new LatLng(waypoints.get(i).latitude(), waypoints.get(i).longitude()));
        }
        List<Point> after = PolylineUtils.simplify(temp, 0.001);

        waypointSegmentArray.clear();
        for(int i=0; i<after.size(); i++){
            if (i != after.size()-1) {
                if (waypointSegmentArray.size() - 1 < i / COUNT_PER_SEGMENT) {
                    waypointSegmentArray.add(new ArrayList<>());
                }
                waypointSegmentArray.get(i / COUNT_PER_SEGMENT).add(after.get(i));
            } else {
                waypointSegmentArray.get(waypointSegmentArray.size()-1).add(after.get(i));
            }
        }

        if (preDrawPoints != null){
            mMapboxMap.removePolyline(preDrawPoints.getPolyline());
        }
        PolylineOptions polylineOptions = new PolylineOptions().addAll(points).color(Color.BLUE).width(8);
        mMapboxMap.addPolyline(polylineOptions);
        preDrawPoints = polylineOptions;

        segmentCount = 0;
        routes.clear();

        for(int i=0; i<waypointSegmentArray.size(); i++) {
//            NavigationRoute.Builder builder = NavigationRoute.builder(this)
//                    .accessToken(Mapbox.getAccessToken())
//                    .origin(waypointSegmentArray.get(i).get(0))
//                    .destination(waypointSegmentArray.get(i).get(waypointSegmentArray.get(i).size() - 1))
//                    .profile(DirectionsCriteria.PROFILE_WALKING);;
//            if (waypointSegmentArray.get(i).size() > 2) {
//                for (int k = 1; k < waypointSegmentArray.get(i).size() - 1; k++) {
//                    builder.addWaypoint(waypointSegmentArray.get(i).get(k));
//                }
//            }
//            builder.build()
//                    .getRoute(new Callback<DirectionsResponse>() {
//                        @Override
//                        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//                            segmentCount++;
//                            if (response.body() == null) {
//                                showAlertNotGetRoute();
//                                dlg_progress.dismiss();
//                                return;
//                            } else if (response.body().routes().size() < 1) {
//                                showAlertNotGetRoute();
//                                dlg_progress.dismiss();
//                                return;
//                            }
//
//                            if (response.isSuccessful()) {
//                                DirectionsRoute route = response.body().routes().get(0);
//                                routes.add(route);
//                            }
//
//                            if (segmentCount >= waypointSegmentArray.size()){
//                                dlg_progress.dismiss();
//                                sortRouteSegmentsArray();
//                            }
//
//                        }
//                        @Override
//                        public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
//                            segmentCount++;
//                            MessageUtil.showError(MapsActivity.this, throwable.getMessage());
//                            if (segmentCount >= waypointSegmentArray.size()){
//                                dlg_progress.dismiss();
//                                sortRouteSegmentsArray();
//                            }
//                        }
//                    });

            MapboxMapMatching.builder()
                    .accessToken(Mapbox.getAccessToken())
                    .coordinates(waypointSegmentArray.get(i))
                    .steps(true)
                    .voiceInstructions(true)
                    .bannerInstructions(true)
                    .build()
                    .enqueueCall(new Callback<MapMatchingResponse>() {
                        @Override
                        public void onResponse(Call<MapMatchingResponse> call, Response<MapMatchingResponse> response) {
                            segmentCount++;
                            if (response.isSuccessful()) {
                                DirectionsRoute route = response.body().matchings().get(0).toDirectionRoute();
                                routes.add(route);
                            }

                            if (segmentCount >= waypointSegmentArray.size()){
                                dlg_progress.dismiss();
                                sortRouteSegmentsArray();
                            }
                        }

                        @Override
                        public void onFailure(Call<MapMatchingResponse> call, Throwable throwable) {
                            segmentCount++;
                            MessageUtil.showError(MapsActivity.this, throwable.getMessage());
                            if (segmentCount >= waypointSegmentArray.size()){
                                dlg_progress.dismiss();
                                sortRouteSegmentsArray();
                            }
                        }
                    });
        }
    }
    private void sortRouteSegmentsArray(){
        Collections.sort(routes, new ComparatorByDistance());
    }

    private void showAlertNotGetRoute(){
        MessageUtil.showError(this, "No routes found.");
    }

    public class ComparatorByDistance implements Comparator<DirectionsRoute> {
        @Override
        public int compare(DirectionsRoute route1, DirectionsRoute route2) {
            Point startPoint = waypoints.get(0);

            LineString lineString1 = LineString.fromPolyline(route1.geometry(), PRECISION_6);
            List<Point> coordinates1 = lineString1.coordinates();
            Point point1 = coordinates1.get(0);
            LineString lineString2 = LineString.fromPolyline(route2.geometry(), PRECISION_6);
            List<Point> coordinates2 = lineString2.coordinates();
            Point point2 = coordinates2.get(0);

            Location locationA1 = new Location("point A1");
            locationA1.setLatitude(startPoint.latitude());
            locationA1.setLongitude(startPoint.longitude());
            Location locationB1 = new Location("point B1");
            locationB1.setLatitude(point1.latitude());
            locationB1.setLongitude(point1.longitude());
            Float d1 = locationA1.distanceTo(locationB1);

            Location locationA2 = new Location("point A2");
            locationA2.setLatitude(startPoint.latitude());
            locationA2.setLongitude(startPoint.longitude());
            Location locationB2 = new Location("point B2");
            locationB2.setLatitude(point2.latitude());
            locationB2.setLongitude(point2.longitude());
            Float d2 = locationA2.distanceTo(locationB2);

            return d1.compareTo(d2);
        }
    }
}
