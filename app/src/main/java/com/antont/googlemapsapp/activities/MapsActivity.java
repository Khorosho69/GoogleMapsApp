package com.antont.googlemapsapp.activities;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import architecture_components.ActivityViewModel;

import static android.graphics.Paint.*;
import static android.support.v4.app.ActivityCompat.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnRequestPermissionsResultCallback {

    private static final int PERMISSION_REQUEST_CODE = 13;
    private static final int mMarkersCount = 10;
    // Radius of the Earth's sphere, required to convert meters to longitude and latitude
    private static final double EARTH_RADIUS = 6378137.0;

    private ActivityViewModel mActivityViewModel;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;

    private List<MarkerOptions> mMarkerList;

    private EditText mRadiusEditText;
    private Button mShowMarkersButton;

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                LatLng pos = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                mLocationManager.removeUpdates(mLocationListener);
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
        mShowMarkersButton = findViewById(R.id.create_markers_button);

        mShowMarkersButton.setOnClickListener((View view) -> createMarkers());

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mActivityViewModel = ViewModelProviders.of(this).get(ActivityViewModel.class);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        enableMyLocation();

        if (mActivityViewModel.getCameraPosition() != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mActivityViewModel.getCameraPosition()));
            mMarkerList = mActivityViewModel.getMarkers();
            if (mMarkerList != null) {
                for (int i = 0; i < mMarkerList.size(); i++) {
                    mMap.addMarker(mMarkerList.get(i));
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

        if (!(isGPSEnabled || isNetworkEnabled)) {
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
        mMarkerList = new ArrayList<>();

        List<String> items = getUniqueLettersList(mMarkersCount);
        for (int i = 0; i < mMarkersCount; i++) {
            MarkerOptions marker = new MarkerOptions()
                    .position(getRandomPosition(mCurrentLocation, markerRadius))
                    .icon(BitmapDescriptorFactory.fromBitmap(getMarkerIconWithText(items.get(i))));

            mMarkerList.add(marker);
        }
        showMarkers();
    }

    private void showMarkers() {
        mMap.clear();
        for (MarkerOptions markers: mMarkerList) {
            mMap.addMarker(markers);
        }
        zoomCameraToShowMarkers();
    }

    // Animated zoom camera to show all the markers on the screen
    private void zoomCameraToShowMarkers() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (MarkerOptions marker : mMarkerList) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

    private LatLng getRandomPosition(Location currentLocation, double radius) {
        // Getting the offset in both directions around the user's position from radian offset
        double latOffset = metersToLat(radius / 2.0) * 180 / Math.PI;
        double lonOffset = metersToLon(radius / 2.0, currentLocation.getLongitude()) * 180 / Math.PI;

        // Create a random position marker
        LatLng markerLocation = new LatLng(genRandomDoubleBetween(currentLocation.getLatitude() - latOffset, currentLocation.getLatitude() + latOffset),
                genRandomDoubleBetween(currentLocation.getLongitude() - lonOffset, currentLocation.getLongitude() + lonOffset));

        // If the marker goes beyond the user position, create a new marker
        while (!isPointAroundUser(currentLocation, markerLocation, radius)) {
            markerLocation = new LatLng(genRandomDoubleBetween(currentLocation.getLatitude() - latOffset, currentLocation.getLatitude() + latOffset),
                    genRandomDoubleBetween(currentLocation.getLongitude() - lonOffset, currentLocation.getLongitude() + lonOffset));
        }
        return markerLocation;
    }

    // Calculate latitude offset in radians
    private double metersToLat(double meters) {
        return meters / EARTH_RADIUS;
    }

    // Calculate longitude offset in radians
    private double metersToLon(double meters, double lon) {
        return meters / (EARTH_RADIUS * Math.cos(Math.PI * lon / 180));
    }

    public double genRandomDoubleBetween(double min, double max) {
        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }

    // Make sure the point is inside the user area using the circle formula
    private Boolean isPointAroundUser(Location pos, LatLng newPos, double radius) {
        return (newPos.latitude - pos.getLatitude()) * (newPos.latitude - pos.getLatitude())
                + (newPos.longitude - pos.getLongitude()) * (newPos.longitude - pos.getLongitude()) <= radius * radius;
    }

    // Create a list filled in with unique letters
    private List<String> getUniqueLettersList(int count) {
        List<String> items = new ArrayList<>();
        while (items.size() < count) {
            char item = getRandomLetter();
            if (!items.contains(String.valueOf(item))) {
                items.add(String.valueOf(item));
            }
        }
        return items;
    }

    private static char getRandomLetter() {
        int rnd = (int) (Math.random() * 52);
        char base = (rnd < 26) ? 'A' : 'a';
        return (char) (base + rnd % 26);

    }

    private Bitmap getMarkerIconWithText(String text) {
        int textSize = 40;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.marker_icon).copy(Bitmap.Config.ARGB_4444, true);
        Bitmap resized = Bitmap.createScaledBitmap(bm, 50, 80, true);
        Paint paint = getPaint(textSize);

        float offset = (resized.getWidth() - paint.measureText(text)) / 2;
        Canvas canvas = new Canvas(resized);
        canvas.drawText(text, offset, resized.getHeight() / 2, paint);

        return resized;
    }

    @NonNull
    private Paint getPaint(int textSize) {
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(textSize);
        return paint;
    }

    private boolean checkMapPermission() {
        return !(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    private void showSnackBar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied_message, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok, view -> {
                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (showRationale) {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
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
        super.onDestroy();

        mActivityViewModel.setMarkers(mMarkerList);
        mActivityViewModel.setCameraPosition(mMap.getCameraPosition());
    }
}
