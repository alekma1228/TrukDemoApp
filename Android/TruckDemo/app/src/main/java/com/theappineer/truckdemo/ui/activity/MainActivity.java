package com.theappineer.truckdemo.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.theappineer.truckdemo.AppConstant;
import com.theappineer.truckdemo.ui.MyProgressDialog;
import com.theappineer.truckdemo.ui.adapter.PlaceArrayAdapter;
import com.theappineer.truckdemo.R;
import com.theappineer.truckdemo.utils.BaseTask;
import com.theappineer.truckdemo.utils.MessageUtil;
import com.theappineer.truckdemo.utils.TextChangeListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    public static MainActivity instance;

    public MyProgressDialog dlg_progress;

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    @BindView(R.id.txt_cur)
    AutoCompleteTextView txt_cur;

    @BindView(R.id.txt_dest)
    AutoCompleteTextView txt_dest;

    @BindView(R.id.txt_length)
    EditText txt_length;

    @BindView(R.id.txt_width)
    EditText txt_width;

    @BindView(R.id.txt_height)
    EditText txt_height;

    @BindView(R.id.txt_weight)
    EditText txt_weight;

    @BindView(R.id.txt_axle)
    EditText txt_axle;

    private static final int GOOGLE_API_CLIENT_ID = 0;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mCurPlaceArrayAdapter;
    private boolean isSelectedCurPlace = false;
    private boolean isSelectedDestPlace = false;
    private PlaceArrayAdapter mDestPlaceArrayAdapter;
    private LatLng mCurLatLng = new LatLng(37.398160, -122.180831);
    private LatLng mDestLatLng = new LatLng(37.398160, -122.180831);
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));
    private AdapterView.OnItemClickListener mCurAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mCurPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mCurUpdatePlaceDetailsCallback);
        }
    };
    private ResultCallback<PlaceBuffer> mCurUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
            mCurLatLng = place.getLatLng();
            isSelectedCurPlace = true;
            CharSequence attributions = places.getAttributions();
        }
    };
    private AdapterView.OnItemClickListener mDestAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mDestPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mDestUpdatePlaceDetailsCallback);
        }
    };
    private ResultCallback<PlaceBuffer> mDestUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
            mDestLatLng = place.getLatLng();
            isSelectedDestPlace = true;
            CharSequence attributions = places.getAttributions();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        instance = this;

        dlg_progress = new MyProgressDialog(this);

        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            init();
        } else {
            ActivityCompat.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }

        txt_cur.addTextChangedListener(new TextChangeListener() {
            @Override
            public void onTextChange(CharSequence s) {
                isSelectedCurPlace = false;
            }
        });
        txt_dest.addTextChangedListener(new TextChangeListener() {
            @Override
            public void onTextChange(CharSequence s) {
                isSelectedDestPlace = false;
            }
        });
//        txt_cur.setText("3148 Davie blvd fort lauderdale fl");
//        txt_dest.setText("1314 sw 13 street boca raton fl");
//        txt_length.setText("20");
//        txt_width.setText("3");
//        txt_height.setText("3");
//        txt_weight.setText("6");
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

                init();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void init(){
        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();

        txt_cur.setThreshold(3);
        txt_cur.setOnItemClickListener(mCurAutocompleteClickListener);
        mCurPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, BOUNDS_MOUNTAIN_VIEW, null);
        txt_cur.setAdapter(mCurPlaceArrayAdapter);

        txt_dest.setThreshold(3);
        txt_dest.setOnItemClickListener(mDestAutocompleteClickListener);
        mDestPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, BOUNDS_MOUNTAIN_VIEW, null);
        txt_dest.setAdapter(mDestPlaceArrayAdapter);
    }
    @Override
    public void onResume(){
        super.onResume();

    }
    @OnClick(R.id.btn_view_map)
    public void onViewMap(){
        if(isValid()){
            Intent intent = new Intent(this, MapsActivity.class);

            intent.putExtra("CurLat", mCurLatLng.latitude);
            intent.putExtra("CurLng", mCurLatLng.longitude);
            intent.putExtra("DestLat", mDestLatLng.latitude);
            intent.putExtra("DestLng", mDestLatLng.longitude);
            float length = Float.parseFloat(txt_length.getText().toString().trim());
            intent.putExtra("Length", length);
            float width = Float.parseFloat(txt_width.getText().toString().trim());
            intent.putExtra("Width", width);
            float height = Float.parseFloat(txt_height.getText().toString().trim());
            intent.putExtra("Height", height);
            float weight = Float.parseFloat(txt_weight.getText().toString().trim());
            intent.putExtra("Weight", weight);
            int axle = 4;//Float.parseInt(txt_axle.getText().toString().trim());
            intent.putExtra("Axle", axle);

            startActivity(intent);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mCurPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        mDestPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
    }
    @Override
    public void onConnectionSuspended(int i) {
        mCurPlaceArrayAdapter.setGoogleApiClient(null);
        mDestPlaceArrayAdapter.setGoogleApiClient(null);
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }

    private boolean isValid(){
        if(TextUtils.isEmpty(txt_cur.getText().toString().trim())){
            MessageUtil.showError(this, "Please enter current location.");
            return false;
        }
        if (!isSelectedCurPlace){
            MessageUtil.showError(this, "Please enter correct current location. Please select address dropdown list.");
            return false;
        }
        if(TextUtils.isEmpty(txt_dest.getText().toString().trim())){
            MessageUtil.showError(this, "Please enter destination.");
            return false;
        }
        if (!isSelectedDestPlace){
            MessageUtil.showError(this, "Please enter correct destination. Please select address dropdown list.");
            return false;
        }
        if(TextUtils.isEmpty(txt_length.getText().toString().trim())){
            MessageUtil.showError(this, "Please enter length.");
            return false;
        }
        if(TextUtils.isEmpty(txt_width.getText().toString().trim())){
            MessageUtil.showError(this, "Please enter width.");
            return false;
        }
        if(TextUtils.isEmpty(txt_height.getText().toString().trim())){
            MessageUtil.showError(this, "Please enter height.");
            return false;
        }
        if(TextUtils.isEmpty(txt_weight.getText().toString().trim())){
            MessageUtil.showError(this, "Please enter weight.");
            return false;
        }
//        if(TextUtils.isEmpty(txt_axle.getText().toString().trim())){
//            MessageUtil.showError(this, "Please enter axle load.");
//            return false;
//        }

        try{
            float length = Float.parseFloat(txt_length.getText().toString().trim());
        } catch (Exception e){
            MessageUtil.showError(this, "Please enter correct length.");
            return false;
        }
        try{
            float width = Float.parseFloat(txt_width.getText().toString().trim());
        } catch (Exception e){
            MessageUtil.showError(this, "Please enter correct width.");
            return false;
        }
        try{
            float height = Float.parseFloat(txt_height.getText().toString().trim());
        } catch (Exception e){
            MessageUtil.showError(this, "Please enter correct height.");
            return false;
        }
        try{
            float weight = Float.parseFloat(txt_weight.getText().toString().trim());
        } catch (Exception e){
            MessageUtil.showError(this, "Please enter correct weight.");
            return false;
        }
//        try{
//            float axle = Float.parseFloat(txt_axle.getText().toString().trim());
//        } catch (Exception e){
//            MessageUtil.showError(this, "Please enter correct axle.");
//            return false;
//        }

        return true;
    }
}
