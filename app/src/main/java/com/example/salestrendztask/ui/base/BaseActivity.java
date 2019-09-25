package com.example.salestrendztask.ui.base;

import android.Manifest;
import android.app.Activity;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.salestrendztask.SalesTrendz;
import com.example.salestrendztask.roomdb.AppDataManager;
import com.example.salestrendztask.utils.Constant;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    protected FusedLocationProviderClient mFusedLocationClient;
    protected LocationRequest locationRequest;
    private LocationListener locationListener;
    @NonNull
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                for (Location location : locationList) {
                    if (locationListener != null) locationListener.onLocationChanged(location);
                    setLatLng(location);
                }
                // Location location = locationList.get(locationList.size() - 1);
            }
        }
    };
    private Activity activity = this;


    protected Activity getActivity() {
        return activity;
    }

    protected AppDataManager getDataManager() {
        return SalesTrendz.getDataManager();
    }

    public void setLocationListener(LocationListener locationListener) {
        this.locationListener = locationListener;
    }

    /**
     * location getting task start here
     * when location not available this method on gps when user click ok
     */
    protected void onGpsAutomatic() {
        int permissionLocation = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionLocation == PackageManager.PERMISSION_GRANTED) {

            locationRequest = new LocationRequest();
            locationRequest.setInterval(3000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);
            builder.setNeedBle(true);

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());

            Task<LocationSettingsResponse> task =
                    LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

            task.addOnCompleteListener(task1 -> {
                try {
                    //getting target response use below code
                    LocationSettingsResponse response = task1.getResult(ApiException.class);

                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    int permissionLocation1 = ContextCompat
                            .checkSelfPermission(activity,
                                    Manifest.permission.ACCESS_FINE_LOCATION);
                    if (permissionLocation1 == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(activity, location -> {
                                    // Got last known location. In some rare situations this can be null.
                                    if (location != null) {
                                        if (locationListener != null)
                                            locationListener.onLocationChanged(location);
                                        // Logic to handle location object
                                        setLatLng(location);
                                    } else {
                                        //Location not available
                                        Log.e("Test", "Location not available");
                                    }
                                });
                    }
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        activity,
                                        Constant.REQUEST_CHECK_SETTINGS_GPS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            });
        }
    }

    /**
     * this method get location when available and store in static variable
     */
    public void updateLocation(LocationListener locationListener) {
        this.locationListener = locationListener;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(activity, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        if (locationListener != null) locationListener.onLocationChanged(location);
                        // Logic to handle location object
                        setLatLng(location);
                    } else {
                        //Location not available
                        onGpsAutomatic();
                    }
                });
    }

    protected void setLatLng(@NonNull Location location) {
        SalesTrendz.setLATITUDE(location.getLatitude());
        SalesTrendz.setLONGITUDE(location.getLongitude());
        Log.e("Location", String.valueOf(SalesTrendz.getLATITUDE()));

        /*if (address.isEmpty()) {
            address = getAddressFromLatLng(Agrinvest.LATITUDE, Agrinvest.LONGITUDE);
            AppLogger.e("Location ", address);
        }*/
    }

    protected String getAddressFromLatLng(Double latitude, Double longitude) {
        String result;
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(activity, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);

            // String city = addresses.get(0).getLocality();
            //  String addressLine = addresses.get(0).getAddressLine(1);
            // String state = addresses.get(0).getAdminArea();
            // String country = addresses.get(0).getCountryName();

            result = addresses.get(0).getAddressLine(0);// Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (Exception e) {
            result = "";
        }
        return result;
    }
    /* location getting task end here */

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mFusedLocationClient == null)
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    /* Remove the location listener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        if (mFusedLocationClient == null)
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    protected abstract void setUp();

    public interface LocationListener {
        void onLocationChanged(Location location);
    }
}
