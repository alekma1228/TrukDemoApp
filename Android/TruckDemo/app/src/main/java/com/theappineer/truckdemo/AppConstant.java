package com.theappineer.truckdemo;

import android.os.Environment;

import java.io.File;

public class AppConstant {
    public static final String mapbox_base_url = "https://api.mapbox.com/valhalla/v1/";
    public static final String routing_sub_url = "route";

    public static final String bingmap_base_url = "http://dev.virtualearth.net/REST/v1/Routes/";
    public static final String bingmap_sub_url = "Truck";
    public static final String bingmap_sub_url_async = "TruckAsync";
    public static final String bingmap_sub_url_async_callback = "TruckAsyncCallback";

    public static final String bingmap_key = "AhBLkkEDLT07WtYDRFp56rCM4JcBvsZRB1BODWcPlksvvltnOGe5ErhZAshzpK0q";

    public static String MAPDATA_BASE_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator;
    public static String MAPDATA_DIRECTORY = "isolatedheremaps";
    public static String MAPDATA_PATH = AppConstant.MAPDATA_BASE_PATH + MAPDATA_DIRECTORY;
}
