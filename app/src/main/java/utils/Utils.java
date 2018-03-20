package utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.support.annotation.NonNull;

import com.antont.googlemapsapp.R;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utils {

    private Resources mResources;

    // Radius of the Earth's sphere, required to convert meters to longitude and latitude
    private static final double EARTH_RADIUS = 6378137.0;

    public Utils(Resources resources) {
        mResources = resources;
    }

    public LatLng getRandomPosition(Location currentLocation, double radius) {
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

    private double genRandomDoubleBetween(double min, double max) {
        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }

    // Make sure the point is inside the user area using the circle formula
    private Boolean isPointAroundUser(Location pos, LatLng newPos, double radius) {
        return (newPos.latitude - pos.getLatitude()) * (newPos.latitude - pos.getLatitude())
                + (newPos.longitude - pos.getLongitude()) * (newPos.longitude - pos.getLongitude()) <= radius * radius;
    }

    public Bitmap getMarkerIconWithText(String text) {
        int textSize = 40;
        Bitmap bm = BitmapFactory.decodeResource(mResources, R.drawable.marker_icon).copy(Bitmap.Config.ARGB_4444, true);
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
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(textSize);
        return paint;
    }

    // Create a list filled in with unique letters
    public List<String> getUniqueLettersList(int count) {
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
}
