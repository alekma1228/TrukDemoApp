package com.theappineer.truckdemo.network;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.theappineer.truckdemo.AppConstant;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface CCService {

    @GET(AppConstant.routing_sub_url)
    Call<ResponseBody> getRoutes(@Query("json") String json, @Query("access_token") String access_token);

    @GET(AppConstant.bingmap_sub_url_async)
    Call<ResponseBody> getTruckRoutes(
            @Query("wayPoint.1") String wayPoint1,
            @Query("wayPoint.2") String wayPoint2,
            @Query("avoid") String avoid, // avoid=highways
            @Query("routeAttributes") String routeAttributes, //routeAttributes=routePath
            @Query("dimensionUnit") String dimensionUnit, // dimensionUnit=m
            @Query("vehicleHeight") String vehicleHeight,
            @Query("vehicleWidth") String vehicleWidth,
            @Query("vehicleLength") String vehicleLength,
            @Query("vehicleWeight") String vehicleWeight,
            @Query("vehicleAxles") String vehicleAxles,
            @Query("key") String key
    );
    @GET(AppConstant.bingmap_sub_url_async_callback)
    Call<ResponseBody> getTruckRoutesStatusRequest(
            @Query("requestId") String requestId,
            @Query("key") String key
    );

    // wayPoint.1={wayPpoint1}
    // &viaWaypoint.2={viaWaypoint2}
    // &waypoint.3={waypoint3}
    // &wayPoint.n={waypointN}
    // &heading={heading}
    // &optimize={optimize}
    // &avoid={avoid}
    // &distanceBeforeFirstTurn={distanceBeforeFirstTurn}
    // &routeAttributes={routeAttributes}
    // &dateTime={dateTime}
    // &tolerances={tolerances}
    // &distanceUnit={distanceUnit}
    // &vehicleHeight={vehicleHeight}
    // &vehicleWidth={vehicleWidth}
    // &vehicleLength={vehicleLength}
    // &vehicleWeight={vehicleWeight}
    // &vehicleAxles={vehicleAxles}
    // &vehicleTrailers={vehicleTrailers}
    // &vehicleSemi={vehicleSemi}
    // &vehicleMaxGradient={vehicleMaxGradient}
    // &vehicleMinTurnRadius={vehicleMinTurnRadius}
    // &vehicleAvoidCrossWind={vehicleAvoidCrossWind}
    // &vehicleAvoidGroundingRisk={vehicleAvoidGroundingRisk}
    // &vehicleHazardousMaterials={vehicleHazardousMaterials}
    // &vehicleHazardousPermits={vehicleHazardousPermits}
    // &key={BingMapsKey}
}
