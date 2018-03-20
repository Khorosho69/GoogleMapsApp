package com.antont.googlemapsapp.activities;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.antont.googlemapsapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import architecture_components.ActivityViewModel;
import utils.Utils;

import static android.support.v4.app.ActivityCompat.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnRequestPermissionsResultCallback {

    private static final int PERMISSION_REQUEST_CODE = 13;
    private static final int mMarkersCount = 10;

    private ActivityViewModel mActivityViewModel;

    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;
    private Utils mUtils;
    private EditText mRadiusEditText;

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                mLocationManager.removeUpdates(this);
                mCurrentLocation = location;
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_view);
        mapFragment.getMapAsync(this);

        mRadiusEditText = findViewById(R.id.radius_edit_text);
        Button showMarkersButton = findViewById(R.id.create_markers_button);
        showMarkersButton.setOnClickListener((View view) -> createMarkers());

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mActivityViewModel = ViewModelProviders.of(this).get(ActivityViewModel.class);

        mUtils = new Utils(getResources());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        enableMyLocation();

        if (mActivityViewModel.getCameraPosition() != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mActivityViewModel.getCameraPosition()));
            if (mActivityViewModel.getMarkers() != null) {
                for (MarkerOptions marker : mActivityViewModel.getMarkers()) {
                    mMap.addMarker(marker);
                }
            }
        }
    }

    private void enableMyLocation() {
        if (checkMapPermission()) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            String[] permissions = new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
        boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            return;
        }

        if (!checkMapPermission()) return;
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, mLocationListener);
        mCurrentLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    public void createMarkers() {
        if (mCurrentLocation == null) {
            return;
        }
        double markerRadius;
        try {
            markerRadius = Double.parseDouble(mRadiusEditText.getText().toString());
        } catch (Exception e) {
            Toast.makeText(this, R.string.wrong_input_message, Toast.LENGTH_SHORT).show();
            return;
        }
        mActivityViewModel.clearList();

        List<String> items = mUtils.getUniqueLettersList(mMarkersCount);
        for (String markerText : items) {
            MarkerOptions marker = new MarkerOptions()
                    .position(mUtils.getRandomPosition(mCurrentLocation, markerRadius))
                    .icon(BitmapDescriptorFactory.fromBitmap(mUtils.getMarkerIconWithText(markerText)));

            mActivityViewModel.addMarker(marker);
        }
        showMarkers();
    }

    private void showMarkers() {
        mMap.clear();
        if (mActivityViewModel.getMarkers() == null) return;
        for (MarkerOptions markers : mActivityViewModel.getMarkers()) {
            mMap.addMarker(markers);
        }
        zoomCameraToShowMarkers();
    }

    // Animated zoom camera to show all the markers on the screen
    private void zoomCameraToShowMarkers() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        if (mActivityViewModel.getMarkers() == null) return;
        for (MarkerOptions marker : mActivityViewModel.getMarkers()) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }


    private boolean checkMapPermission() {
        boolean fineLocationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean accessCoarseLocationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fineLocationGranted || accessCoarseLocationGranted;
    }

    private void showSnackBar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied_message, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok, view -> {
                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
                    if (showRationale) {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                    } else {
                        openAppPermissionsPage();
                    }
                }).show();
    }

    private void openAppPermissionsPage() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST_CODE || grantResults.length == 0) {
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!checkMapPermission()) return;
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            showSnackBar();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMap != null) {
            mActivityViewModel.setCameraPosition(mMap.getCameraPosition());
        }
        super.onDestroy();
    }
}
