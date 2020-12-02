package com.theappineer.truckdemo.here;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.OnMapRenderListener;
import com.here.android.mpa.mapping.SupportMapFragment;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;
import com.theappineer.truckdemo.AppConstant;
import com.theappineer.truckdemo.R;
import com.theappineer.truckdemo.ui.activity.HereMapActivity;
import com.theappineer.truckdemo.utils.MessageUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MapFragmentView {
    private SupportMapFragment m_mapFragment;
    private HereMapActivity m_activity;
    private Button m_naviControlButton;
    private Button clearButton;
    private Map m_map;
    private NavigationManager m_navigationManager;
    private GeoBoundingBox m_geoBoundingBox;
    private Route m_route;
    private boolean m_foregroundServiceStarted;
    private MapMarker m_positionIndicatorFixed = null;
    private PointF m_mapTransformCenter;
    private boolean m_returningToRoadViewMode = false;
    private ArrayList<GeoCoordinate> m_addedPoints = new ArrayList<>();
    private MapRoute mMapRoute;
    private boolean isNavigating = false;
    private NavigationManager.MapUpdateMode preMapUpdateMode = NavigationManager.MapUpdateMode.ROADVIEW;
    private PointF longPressPoint;

    public MapFragmentView(HereMapActivity activity) {
        m_activity = activity;
        initMapFragment();
        initNaviControlButton();
    }

    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        m_activity.showProgressDialog();
        m_mapFragment = getMapFragment();
        String diskCacheRoot = AppConstant.MAPDATA_PATH;
        String intentName = "";
        try {
            ApplicationInfo ai = m_activity.getPackageManager().getApplicationInfo(m_activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(), "Failed to find intent name, NameNotFound: " + e.getMessage());
        }
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot, intentName);

        if (!success) {
            m_activity.hidProgressDialog();
        } else {
            if (m_mapFragment != null) {
            /* Initialize the SupportMapFragment, results will be given via the called back. */
                m_mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                        m_activity.hidProgressDialog();
                        if (error == Error.NONE) {
                            m_map = m_mapFragment.getMap();
                            m_map.setZoomLevel(17);
                            PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK);

                            MapMarker curMarker = new MapMarker();
                            curMarker.setCoordinate(new GeoCoordinate(m_activity.curLat, m_activity.curLng, 0.0));
                            m_map.addMapObject(curMarker);

                            MapMarker destMarker = new MapMarker();
                            destMarker.setCoordinate(new GeoCoordinate(m_activity.destLat, m_activity.destLng, 0.0));
                            m_map.addMapObject(destMarker);

                            createRoute();
                        } else {
                            Toast.makeText(m_activity,"ERROR: Cannot initialize Map with error " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }

        m_mapFragment.addOnMapRenderListener(new OnMapRenderListener() {
            @Override
            public void onPreDraw() {
                if (m_positionIndicatorFixed != null) {
                    if (NavigationManager.getInstance().getMapUpdateMode().equals(NavigationManager.MapUpdateMode.ROADVIEW)) {
                        if (!m_returningToRoadViewMode && isNavigating) {
                            m_positionIndicatorFixed.setCoordinate(m_map.pixelToGeo(m_mapTransformCenter));
                        }
                    }
                }
            }

            @Override
            public void onPostDraw(boolean var1, long var2) {
                int a=0;
                a++;
                int b=a;
            }

            @Override
            public void onSizeChanged(int var1, int var2) {
                int a=0;
                a++;
                int b=a;
            }

            @Override
            public void onGraphicsDetached() {
                int a=0;
                a++;
                int b=a;
            }

            @Override
            public void onRenderBufferCreated() {
                int a=0;
                a++;
                int b=a;
            }
        });

    }

    private void createRoute() {
        CoreRouter coreRouter = new CoreRouter();

        RoutePlan routePlan = new RoutePlan();

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.TRUCK);
        routeOptions.setHighwaysAllowed(true);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);

        routeOptions
                .setTruckWidth(m_activity.width)
                .setTruckHeight(m_activity.height)
                .setTruckLimitedWeight(m_activity.weight)
//                .setTruckWeightPerAxle(m_activity.axle)
//                .setTruckTrailersCount(1)
                .setTruckLength(m_activity.length);

//        routeOptions.setRouteCount(1);
        routePlan.setRouteOptions(routeOptions);

        RouteWaypoint startPoint = new RouteWaypoint(new GeoCoordinate(m_activity.curLat, m_activity.curLng));
        RouteWaypoint destination = new RouteWaypoint(new GeoCoordinate(m_activity.destLat, m_activity.destLng));

        routePlan.addWaypoint(startPoint);

        for(int i =0; i<m_addedPoints.size(); i++){
            RouteWaypoint midPoint = new RouteWaypoint(m_addedPoints.get(i));
            routePlan.addWaypoint(midPoint);
        }

        routePlan.addWaypoint(destination);

        if(mMapRoute != null){
            m_map.removeMapObject(mMapRoute);
        }
        if(m_positionIndicatorFixed != null){
            m_map.removeMapObject(m_positionIndicatorFixed);
        }

        m_activity.showProgressDialog();

        coreRouter.calculateRoute(routePlan, new Router.Listener<List<RouteResult>, RoutingError>() {
                    @Override
                    public void onProgress(int i) { /* The calculation progress can be retrieved in this callback. */ }
                    @Override
                    public void onCalculateRouteFinished(List<RouteResult> routeResults, RoutingError routingError) {
                        m_activity.hidProgressDialog();
                        /* Calculation is done.Let's handle the result */
                        if (routingError == RoutingError.NONE) {
                            if (routeResults.get(0).getRoute() != null) {
                                m_route = routeResults.get(0).getRoute();
                                mMapRoute = new MapRoute(routeResults.get(0).getRoute());
                                mMapRoute.setManeuverNumberVisible(true);
                                m_map.addMapObject(mMapRoute);

                                m_map.setCenter(routePlan.getWaypoint(0).getNavigablePosition(), Map.Animation.NONE);

                                m_mapTransformCenter = new PointF(m_map.getTransformCenter().x, (m_map.getTransformCenter().y * 85 / 50));
//                                m_map.setTransformCenter(m_mapTransformCenter);

                                Image icon = new Image();
                                m_positionIndicatorFixed = new MapMarker();
                                try {
                                    icon.setImageResource(R.drawable.gps_position);
                                    m_positionIndicatorFixed.setIcon(icon);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                m_positionIndicatorFixed.setVisible(true);
                                m_positionIndicatorFixed.setCoordinate(routePlan.getWaypoint(0).getNavigablePosition());
                                m_map.addMapObject(m_positionIndicatorFixed);

                                m_mapFragment.getPositionIndicator().setVisible(false);

                                m_navigationManager = NavigationManager.getInstance();
                                m_navigationManager.setMap(m_map);
                                m_navigationManager.getRoadView().addListener(new WeakReference<NavigationManager.RoadView.Listener>(roadViewListener));
                                m_navigationManager.addNavigationManagerEventListener(new WeakReference<NavigationManager.NavigationManagerEventListener>(m_navigationManagerEventListener));
                                PositioningManager.getInstance().addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(mapPositionHandler));
                                m_mapFragment.getMapGesture().addOnGestureListener(gestureListener, 100, true);

                                m_geoBoundingBox = routeResults.get(0).getRoute().getBoundingBox();
                                m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION);
                            } else {
                                MessageUtil.showError(m_activity, "RouteResults's route is never.");
                            }
                        } else {
                            MessageUtil.showError(m_activity, routingError.toString());
//                            showAlertNotGetRoute();
                        }
                    }
                });
    }

    private void showAlertNotGetRoute(){
        MessageUtil.showError(m_activity, "No routes found.");
    }

    private void initNaviControlButton() {
        m_naviControlButton = (Button) m_activity.findViewById(R.id.naviCtrlButton);
        m_naviControlButton.setText(R.string.start_navi);
        m_naviControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_route == null) {
                    return;
                } else {
                    if(m_naviControlButton.getText().toString().equals(m_activity.getString(R.string.start_navi))) {
                        startNavigation();
                    } else {
                        stopForegroundService();
                    }
                }
            }
        });

        clearButton = (Button) m_activity.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isNavigating) {
                    m_addedPoints.clear();
                    createRoute();
                }
            }
        });
    }

    private void startForegroundService() {
        if (!m_foregroundServiceStarted) {
            m_foregroundServiceStarted = true;
            Intent startIntent = new Intent(m_activity, ForegroundService.class);
            startIntent.setAction(ForegroundService.START_ACTION);
            m_activity.getApplicationContext().startService(startIntent);
        }
    }

    private void stopForegroundService() {
        isNavigating = false;
        m_positionIndicatorFixed.setVisible(true);
        m_navigationManager.stop();
        m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE, 0f);
        m_naviControlButton.setText(R.string.start_navi);
        m_navigationManager.setMapUpdateMode(preMapUpdateMode);
        if (m_foregroundServiceStarted) {
            m_foregroundServiceStarted = false;
            Intent stopIntent = new Intent(m_activity, ForegroundService.class);
            stopIntent.setAction(ForegroundService.STOP_ACTION);
            m_activity.getApplicationContext().startService(stopIntent);
        }
    }

    private void startNavigation() {
        m_naviControlButton.setText(R.string.stop_navi);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(m_activity);
        alertDialogBuilder.setTitle("Navigation");
        alertDialogBuilder.setMessage("Choose Mode");
        alertDialogBuilder.setNegativeButton("Navigation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                isNavigating = true;
                m_positionIndicatorFixed.setVisible(true);
                preMapUpdateMode = m_navigationManager.getMapUpdateMode();
                m_navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);
                m_map.setTilt(60);
                m_navigationManager.startNavigation(m_route);
                startForegroundService();
            };
        });
        alertDialogBuilder.setPositiveButton("Simulation",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                isNavigating = true;
                m_positionIndicatorFixed.setVisible(true);
                preMapUpdateMode = m_navigationManager.getMapUpdateMode();
                m_navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);
                m_map.setTilt(60);
                m_navigationManager.simulate(m_route,35);//Simualtion speed is set to 15 m/s
                startForegroundService();
            };
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    final private NavigationManager.RoadView.Listener roadViewListener = new NavigationManager.RoadView.Listener() {
        @Override
        public void onPositionChanged(GeoCoordinate geoCoordinate) {
            // an active RoadView provides coordinates that is the map transform center of it's
            // movements.
            m_mapTransformCenter = m_map.projectToPixel(geoCoordinate).getResult();
        }
    };
    private PositioningManager.OnPositionChangedListener mapPositionHandler = new PositioningManager.OnPositionChangedListener() {
        @Override
        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position,
                                      boolean isMapMatched) {
            NavigationManager.MapUpdateMode mapUpdateMode = NavigationManager.getInstance().getMapUpdateMode();
            if (NavigationManager.getInstance().getMapUpdateMode().equals(NavigationManager.MapUpdateMode.ROADVIEW) && !m_returningToRoadViewMode)
                // use this updated position when map is not updated by RoadView.
                m_positionIndicatorFixed.setCoordinate(position.getCoordinate());
        }

        @Override
        public void onPositionFixChanged(PositioningManager.LocationMethod method, PositioningManager.LocationStatus status) {

        }
    };
    private MapGesture.OnGestureListener gestureListener = new MapGesture.OnGestureListener() {
        @Override
        public void onPanStart() { }
        @Override
        public void onPanEnd() { }
        @Override
        public void onMultiFingerManipulationStart() { }
        @Override
        public void onMultiFingerManipulationEnd() { }
        @Override
        public boolean onMapObjectsSelected(List<ViewObject> objects) {
            return false;
        }
        @Override
        public boolean onTapEvent(PointF p) {
            return false;
        }
        @Override
        public boolean onDoubleTapEvent(PointF p) { return false; }
        @Override
        public void onPinchLocked() { }
        @Override
        public boolean onPinchZoomEvent(float scaleFactor, PointF p) { return false; }
        @Override
        public void onRotateLocked() { }
        @Override
        public boolean onRotateEvent(float rotateAngle) {
            return false;
        }
        @Override
        public boolean onTiltEvent(float angle) { return false; }
        @Override
        public boolean onLongPressEvent(PointF p) {
            longPressPoint = p;
            if(!isNavigating && longPressPoint != null) {
                GeoCoordinate touchLocation = m_map.pixelToGeo(longPressPoint);
                double lat = touchLocation.getLatitude();
                double lon = touchLocation.getLongitude();
                String StrGeo = String.format("%.6f, %.6f", lat, lon);

                m_addedPoints.add(touchLocation);

                m_activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createRoute();
                    }
                });
            }
            return false;
        }
        @Override
        public void onLongPressRelease() {
        }
        @Override
        public boolean onTwoFingerTapEvent(PointF p) {
            return false;
        }
    };
    private NavigationManager.NavigationManagerEventListener m_navigationManagerEventListener = new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onRunningStateChanged() {
//            Toast.makeText(m_activity, "Running state changed", Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onNavigationModeChanged() {
//            Toast.makeText(m_activity, "Navigation mode changed", Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onEnded(NavigationManager.NavigationMode navigationMode) {
            Toast.makeText(m_activity, "Navigation was ended", Toast.LENGTH_SHORT).show();
            stopForegroundService();
        }
        @Override
        public void onMapUpdateModeChanged(NavigationManager.MapUpdateMode mapUpdateMode) {
//            Toast.makeText(m_activity, "Map update mode is changed to " + mapUpdateMode, Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onRouteUpdated(Route route) {
//            Toast.makeText(m_activity, "Route updated", Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onCountryInfo(String s, String s1) {
//            Toast.makeText(m_activity, "Country info updated from " + s + " to " + s1, Toast.LENGTH_SHORT).show();
        }
    };

    public void onDestroy() {
        m_map.removeMapObject(m_positionIndicatorFixed);
        if (m_navigationManager != null) {
            stopForegroundService();
            m_navigationManager.stop();
        }
        PositioningManager.getInstance().stop();
    }
}
