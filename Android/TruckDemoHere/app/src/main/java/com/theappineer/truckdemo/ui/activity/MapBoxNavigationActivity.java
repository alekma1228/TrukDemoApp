package com.theappineer.truckdemo.ui.activity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.theappineer.truckdemo.R;
import com.theappineer.truckdemo.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MapBoxNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, OnMapReadyCallback {
    private NavigationView navigationView;
    public static List<DirectionsRoute> routes = new ArrayList<>();
    private int currentNavigationIndex = 0;
    private MapboxMap mMapboxMap;
    private FeatureCollection dashedLineDirectionsFeatureCollection;
    private boolean showArrivedAlert = false;
    private boolean isSimulate = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_box_navigation);
        navigationView = findViewById(R.id.navigationView);
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this);

        isSimulate = getIntent().getBooleanExtra("simulate", false);
    }
    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
// If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        currentNavigationIndex = 0;
        startNavigationWithNewRoute(routes.get(currentNavigationIndex));
    }

    private void startNavigationWithNewRoute(DirectionsRoute route){
        MapboxNavigationOptions navigationOptions = MapboxNavigationOptions.builder()
                .build();
        NavigationViewOptions viewOptions = NavigationViewOptions.builder()
                .directionsRoute(route)
                .shouldSimulateRoute(isSimulate)
                .navigationOptions(navigationOptions)
                .navigationListener(new NavigationListener() {
                    @Override
                    public void onCancelNavigation() { onBackPressed(); }
                    @Override
                    public void onNavigationFinished() { }
                    @Override
                    public void onNavigationRunning() { }
                })
                .progressChangeListener(new ProgressChangeListener() {
                    @Override
                    public void onProgressChange(Location location, RouteProgress routeProgress) {
                        Point point = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                        int remainCount = routeProgress.remainingWaypoints();
                        double remainDuration = routeProgress.durationRemaining();
                        if(remainDuration < 5){
                            currentNavigationIndex++;
                            if(currentNavigationIndex >= routes.size()){
                                if(!showArrivedAlert) {
                                    showArrivedAlert = true;
                                    MessageUtil.showAlertDialog(MapBoxNavigationActivity.this, MessageUtil.TYPE_SUCCESS, "You have arrived destination.", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            onBackPressed();
                                        }
                                    });
                                }
                            } else {
                                navigationView.stopNavigation();
                                startNavigationWithNewRoute(routes.get(currentNavigationIndex));
                            }
                        }
                    }
                })
                .routeListener(new RouteListener() {
                    @Override
                    public boolean allowRerouteFrom(Point offRoutePoint) {
                        return true;
                    }
                    @Override
                    public void onOffRoute(Point offRoutePoint) {

                    }
                    @Override
                    public void onRerouteAlong(DirectionsRoute directionsRoute) {

                    }
                    @Override
                    public void onFailedReroute(String errorMessage) {

                    }
                    @Override
                    public void onArrival() {

                    }
                })
                .build();
        navigationView.startNavigation(viewOptions);
    }

    private void initDottedLineSourceAndLayer() {
        dashedLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {});
        GeoJsonSource geoJsonSource = new GeoJsonSource("SOURCE_ID", dashedLineDirectionsFeatureCollection);
        mMapboxMap.addSource(geoJsonSource);
        LineLayer dashedDirectionsRouteLayer = new LineLayer("DIRECTIONS_LAYER_ID", "SOURCE_ID");
        dashedDirectionsRouteLayer.withProperties(lineWidth(4.5f), lineColor(Color.BLUE));
        mMapboxMap.addLayerBelow(dashedDirectionsRouteLayer, "road-label-small");
    }
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        mMapboxMap = mapboxMap;
        initDottedLineSourceAndLayer();
        drawNavigationPolylineRoute();
    }
    private void drawNavigationPolylineRoute() {
        ArrayList<Point> coordinates =  new ArrayList<>();
        for(int i=0; i< routes.size(); i++){
            LineString lineString = LineString.fromPolyline(routes.get(i).geometry(), PRECISION_6);
            List<Point> points = lineString.coordinates();
            coordinates.addAll(points);
        }
        ArrayList<LatLng> points = new ArrayList<>();
        for(int i=0; i< coordinates.size(); i++){
            points.add(new LatLng(coordinates.get(i).latitude(), coordinates.get(i).longitude()));
        }
        Polyline mapMatchedRoute = mMapboxMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(8));
    }
}
