package com.example.salestrendztask.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.salestrendztask.R;
import com.example.salestrendztask.SalesTrendz;
import com.example.salestrendztask.ui.base.BaseActivity;
import com.example.salestrendztask.utils.Constant;
import com.firebase.jobdispatcher.Job;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import static com.example.salestrendztask.utils.Constant.MY_PERMISSIONS_REQUEST_LOCATION;

public class MainActivity extends BaseActivity implements OnMapReadyCallback, BaseActivity.LocationListener, View.OnClickListener {

    //dispatcher object init
    public static final String JOB_STATE_CHANGED = "jobStateChanged";
    public static final String LOCATION_ACQUIRED = "locAcquired";
    private static MoveThread moveThread;
    private static Handler handler;
    private double srcLatitude, srcLongitude;
    private double dstLatitude, dstLongitude;
    private GoogleMap map;
    private float zoomCamera = 15.5f;
    private ArrayList<LatLng> locationList;
    private TextView tv_loc;
    private Marker myCurMarker;
    private BroadcastReceiver jobStateChanged = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(LOCATION_ACQUIRED)) {
                if (intent.getExtras() != null) {
                    Bundle b = intent.getExtras();
                    Location location = b.getParcelable("location");
                    assert location != null;
                    tv_loc.setText("" + location.getLatitude() + " - " + location.getLongitude());
                    dstLatitude = location.getLatitude();
                    dstLongitude = location.getLongitude();
                    LatLng latLng = new LatLng(dstLatitude, dstLongitude);
                    locationList.add(latLng);
                    updateDstPosition(latLng);
                } else {
                    Log.d("intent", "null");
                }
            }
        }
    };

    public static boolean checkLocationPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constant.MY_PERMISSIONS_REQUEST_LOCATION);

            return false;
        } else {
            return true;
        }
    }

    static void animateMarkerToICS(Marker marker, LatLng finalPosition) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(3000);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                handler.post(moveThread);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }

    public static LatLng interpolate(float fraction, LatLng a, LatLng b) {
        // function to calculate the in between values of old latlng and new latlng.
        // To get more accurate tracking(Car will always be in the road even when the latlng falls away from road), use roads api from Google apis.
        // As it has quota limits I didn't have used that method.

        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lngDelta = b.longitude - a.longitude;

        // Take the shortest path across the 180th meridian.
        if (Math.abs(lngDelta) > 180) {
            lngDelta -= Math.signum(lngDelta) * 360;
        }
        double lng = lngDelta * fraction + a.longitude;
        return new LatLng(lat, lng);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUp();
    }

    @Override
    protected void setUp() {
        setLocationListener(this);
        handler = new Handler();
        locationList = new ArrayList<>();

        tv_loc = findViewById(R.id.tv_loc);
        SupportMapFragment mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mMapFragment == null) {
            mMapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mMapFragment).commit();
        }
        mMapFragment.getMapAsync(this);

        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setScrollGesturesEnabled(true);

        if (checkLocationPermission(this)) {
            if (srcLatitude == 0.0 || SalesTrendz.getLATITUDE() == 0.0) {
                updateLocation(this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // location-related task you need to do.
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    updateLocation(this);
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (srcLatitude == 0.0) {
            srcLatitude = location.getLatitude();
            srcLongitude = location.getLongitude();
            locationList.add(new LatLng(srcLatitude, srcLongitude));
            updateSrcPosition();
        }
    }

    private void updateDstPosition(LatLng latLng) {
        if (myCurMarker == null) {
            //src marker
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Destination Location");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            myCurMarker = map.addMarker(markerOptions);

            moveThread = new MoveThread();
            moveThread.setNewPoint(latLng, zoomCamera);
            handler.post(moveThread);
        } else {
            moveThread.setNewPoint(latLng, map.getCameraPosition().zoom); // set the map zoom to current map's zoom level as user may zoom the map while tracking.
            if (myCurMarker != null)
                animateMarkerToICS(myCurMarker, latLng); // animate the marker smoothly
        }

        PolylineOptions lineOptions = new PolylineOptions();
        lineOptions.addAll(locationList);
        lineOptions.width(12);
        lineOptions.color(Color.BLUE);
        lineOptions.geodesic(true);
        map.addPolyline(lineOptions);
    }

    private void updateSrcPosition() {
        LatLng latLng = new LatLng(srcLatitude, srcLongitude);

        //src marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Source Location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        map.addMarker(markerOptions);
        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(srcLatitude, srcLongitude));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoomCamera);
        map.moveCamera(center);
        map.animateCamera(zoom);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_start) {
            Toast.makeText(this, "Tracking start", Toast.LENGTH_SHORT).show();
            startBackgroundService();
        } else if (view.getId() == R.id.btn_stop) {
            Toast.makeText(this, "Tracking end", Toast.LENGTH_SHORT).show();
            stopBackgroundService();
        }
    }

    private void startBackgroundService() {
        IntentFilter i = new IntentFilter(JOB_STATE_CHANGED);
        i.addAction(LOCATION_ACQUIRED);
        LocalBroadcastManager.getInstance(this).registerReceiver(jobStateChanged, i);

        //creating new job and adding it with dispatcher
        Job job = LocationJobService.createJob(SalesTrendz.getInstance().getJobDispatcher(), "LAT_LNG_SERVICE");
        SalesTrendz.getInstance().getJobDispatcher().mustSchedule(job);
    }

    private void stopBackgroundService() {
        if (SalesTrendz.getInstance().getJobDispatcher() != null)
            SalesTrendz.getInstance().getJobDispatcher().cancel("LAT_LNG_SERVICE");
    }

    private class MoveThread implements Runnable {
        LatLng newPoint;
        float zoom = zoomCamera;

        void setNewPoint(LatLng latLng, float zoom) {
            this.newPoint = latLng;
            this.zoom = zoom;
        }

        @Override
        public void run() {
            final CameraUpdate point = CameraUpdateFactory.newLatLngZoom(newPoint, zoom);
            runOnUiThread(() -> map.animateCamera(point));
        }
    }
}
