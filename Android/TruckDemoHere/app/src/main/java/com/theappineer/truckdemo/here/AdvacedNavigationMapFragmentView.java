package com.theappineer.truckdemo.here;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

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
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.mapping.OnMapRenderListener;
import com.here.android.mpa.mapping.SupportMapFragment;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.theappineer.truckdemo.AppConstant;
import com.theappineer.truckdemo.R;
import com.theappineer.truckdemo.ui.activity.HereMapActivity;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class AdvacedNavigationMapFragmentView {
    private SupportMapFragment m_mapFragment;
    private Map m_map;

    private MapMarker m_positionIndicatorFixed = null;
    private PointF m_mapTransformCenter;
    private boolean m_returningToRoadViewMode = false;
    private HereMapActivity m_activity;

    public AdvacedNavigationMapFragmentView(HereMapActivity activity) {
        m_activity = activity;
        initMapFragment();
    }

    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
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

        } else {
            if (m_mapFragment != null) {
                m_mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                        if (error == OnEngineInitListener.Error.NONE) {
//                            m_mapFragment.getMapGesture().addOnGestureListener(gestureListener, 100, true);
                            m_map = m_mapFragment.getMap();
                            m_map.setZoomLevel(15);
//                            m_map.addTransformListener(onTransformListener);

                            PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK);
                            final RoutePlan routePlan = new RoutePlan();

                            // these two waypoints cover suburban roads
                            routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(m_activity.curLat, m_activity.curLng)));
                            routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(m_activity.destLat, m_activity.destLng)));

                            try {
                                // calculate a route for navigation
                                CoreRouter coreRouter = new CoreRouter();
                                coreRouter.calculateRoute(routePlan, new CoreRouter.Listener() {
                                    @Override
                                    public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
                                        if (routingError == RoutingError.NONE) {
                                            Route route = list.get(0).getRoute();

                                            MapRoute mapRoute = new MapRoute(list.get(0).getRoute());
                                            mapRoute.setManeuverNumberVisible(true);
                                            m_map.addMapObject(mapRoute);

                                            m_map.setCenter(routePlan.getWaypoint(0).getNavigablePosition(), Map.Animation.NONE);

                                            NavigationManager.getInstance().setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);
                                            m_map.setTilt(60);

                                            m_mapTransformCenter = new PointF(m_map.getTransformCenter().x, (m_map.getTransformCenter().y * 85 / 50));
                                            m_map.setTransformCenter(m_mapTransformCenter);

                                            // create a map marker to show current position
                                            Image icon = new Image();
                                            m_positionIndicatorFixed = new MapMarker();
                                            try {
                                                icon.setImageResource(R.drawable.gps_position);
                                                m_positionIndicatorFixed.setIcon(icon);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            m_positionIndicatorFixed.setVisible(true);
                                            m_positionIndicatorFixed.setCoordinate(m_map.getCenter());
                                            m_map.addMapObject(m_positionIndicatorFixed);

                                            m_mapFragment.getPositionIndicator().setVisible(false);

                                            NavigationManager.getInstance().setMap(m_map);

                                            PositioningManager.getInstance().addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(mapPositionHandler));

                                            NavigationManager.getInstance().getRoadView().addListener(new WeakReference<NavigationManager.RoadView.Listener>(roadViewListener));

                                            NavigationManager.getInstance().simulate(route, 13);
                                        } else {
                                            Toast.makeText(m_activity, "Error:route calculation returned error code: " + routingError, Toast.LENGTH_LONG).show();
                                        }
                                    }

                                    @Override
                                    public void onProgress(int i) {

                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(m_activity,"ERROR: Cannot initialize Map with error " + error,Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }

        m_mapFragment.addOnMapRenderListener(new OnMapRenderListener() {
            @Override
            public void onPreDraw() {
                if (m_positionIndicatorFixed != null) {
                    if (NavigationManager.getInstance()
                            .getMapUpdateMode().equals(NavigationManager.MapUpdateMode.ROADVIEW)) {
                        if (!m_returningToRoadViewMode) {
                            // when road view is active, we set the position indicator to align
                            // with the current map transform center to synchronize map and map
                            // marker movements.
                            m_positionIndicatorFixed.setCoordinate(m_map.pixelToGeo(m_mapTransformCenter));
                        }
                    }
                }
            }

            @Override
            public void onPostDraw(boolean var1, long var2) {
            }

            @Override
            public void onSizeChanged(int var1, int var2) {
            }

            @Override
            public void onGraphicsDetached() {
            }

            @Override
            public void onRenderBufferCreated() {
            }
        });
    }

    // listen for positioning events
    private PositioningManager.OnPositionChangedListener mapPositionHandler = new PositioningManager.OnPositionChangedListener() {
        @Override
        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position,
                                      boolean isMapMatched) {
            if (NavigationManager.getInstance().getMapUpdateMode().equals(NavigationManager
                    .MapUpdateMode.NONE) && !m_returningToRoadViewMode)
                // use this updated position when map is not updated by RoadView.
                m_positionIndicatorFixed.setCoordinate(position.getCoordinate());
        }

        @Override
        public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                         PositioningManager.LocationStatus status) {

        }
    };

    final private NavigationManager.RoadView.Listener roadViewListener = new NavigationManager.RoadView.Listener() {
        @Override
        public void onPositionChanged(GeoCoordinate geoCoordinate) {
            // an active RoadView provides coordinates that is the map transform center of it's
            // movements.
            m_mapTransformCenter = m_map.projectToPixel
                    (geoCoordinate).getResult();
        }
    };

    public void onDestroy() {
        m_map.removeMapObject(m_positionIndicatorFixed);
        NavigationManager.getInstance().stop();
        PositioningManager.getInstance().stop();
    }

}
