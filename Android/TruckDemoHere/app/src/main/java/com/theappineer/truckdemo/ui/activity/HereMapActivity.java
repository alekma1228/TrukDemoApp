package com.theappineer.truckdemo.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.theappineer.truckdemo.R;
import com.theappineer.truckdemo.here.AdvacedNavigationMapFragmentView;
import com.theappineer.truckdemo.here.MapFragmentView;
import com.theappineer.truckdemo.here.ThreeDModelMapFragmentView;
import com.theappineer.truckdemo.ui.MyProgressDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HereMapActivity extends AppCompatActivity {
    public MyProgressDialog dlg_progress;

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    private MapFragmentView m_mapFragmentView;
//    private AdvacedNavigationMapFragmentView m_mapFragmentView;
//    private ThreeDModelMapFragmentView m_mapFragmentView;

    public double curLat;
    public double curLng;
    public double destLat;
    public double destLng;
    public float length;
    public float width;
    public float height;
    public float weight;
    public float axle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_here_map);

        dlg_progress = new MyProgressDialog(this);

        curLat = getIntent().getDoubleExtra("CurLat", 0);
        curLng = getIntent().getDoubleExtra("CurLng", 0);
        destLat = getIntent().getDoubleExtra("DestLat", 0);
        destLng = getIntent().getDoubleExtra("DestLng", 0);
        length = getIntent().getFloatExtra("Length", 0);
        width = getIntent().getFloatExtra("Width", 0);
        height = getIntent().getFloatExtra("Height", 0);
        weight = getIntent().getFloatExtra("Weight", 0);
        axle = getIntent().getFloatExtra("Axle", 0);

//        curLat = 25.7616798;
//        curLng = -80.1917902;
//        destLat = 40.7127753;
//        destLng = -74.0059728;

        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            setupMapFragmentView();
        } else {
            ActivityCompat.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }
    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat
                                .shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                            + " not granted. "
                                            + "Please go to settings and turn on for sample app",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                setupMapFragmentView();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setupMapFragmentView() {
        m_mapFragmentView = new MapFragmentView(this);
//        m_mapFragmentView = new AdvacedNavigationMapFragmentView(this);
//        m_mapFragmentView = new ThreeDModelMapFragmentView(this);
    }

    @Override
    public void onDestroy() {
//        m_mapFragmentView.onDestroy();
        super.onDestroy();

        if (dlg_progress != null)
            dlg_progress.dismiss();
    }
    @Override
    public void onRestart(){
        super.onRestart();

        if (dlg_progress == null){
            dlg_progress = new MyProgressDialog(this);
        }
    }
    public void showProgressDialog(){
        dlg_progress.show();
    }
    public void hidProgressDialog(){
        dlg_progress.dismiss();
    }
}
