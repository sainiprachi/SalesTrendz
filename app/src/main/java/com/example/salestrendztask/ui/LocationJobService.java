package com.example.salestrendztask.ui;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.salestrendztask.R;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;


public class LocationJobService extends JobService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String ACTION_STOP_JOB = "actionStopJob";
    public static final String JOB_STATE_CHANGED = "jobStateChanged";
    public static final String LOCATION_ACQUIRED = "locAcquired";
    public static boolean isJobRunning = false;
    Handler handler;
    FusedLocationProviderClient mFusedLocationProviderClient;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    JobParameters jobParameters;
    GoogleApiClient mGoogleApiClient;
    ArrayList<Location> updatesList = new ArrayList<>();
    private BroadcastReceiver stopJobReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(ACTION_STOP_JOB)) {
                Log.d("unregister", " job stop receiver");
                onJobFinished();
            }
        }
    };

    public static Job createJob(@NonNull FirebaseJobDispatcher dispatcher, String TAG) {
        return dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                //call this service when the criteria are met.
                .setService(LocationJobService.class)
                //unique id of the task
                .setTag(TAG)
                //don't overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // one-off job
                .setRecurring(false)
                // Run between 30 - 60 seconds from now.
                .setTrigger(Trigger.executionWindow(0, 10))
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                //.setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                //Run this job only when the network is available.
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
    }

    public static Job updateJob(FirebaseJobDispatcher dispatcher, String TAG) {
        return dispatcher.newJobBuilder()
                //update if any task with the given tag exists.
                .setReplaceCurrent(true)
                //Integrate the job you want to start.
                .setService(LocationJobService.class)
                .setTag(TAG)
                // Run between 30 - 60 seconds from now.
                .setTrigger(Trigger.executionWindow(0, 10))
                .build();
    }

    private void onJobFinished() {
        Log.d("job finish", " called");
        isJobRunning = false;
        stopLocationUpdates();
        jobFinished(jobParameters, false);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        handler = new Handler();
        this.jobParameters = jobParameters;
        /*th = new LocationThread();
        handler.post(th);*/
        Log.e("job start", "onStartJob");
        buildGoogleApiClient();
        config();
        isJobRunning = true;
        return true;
    }

    private void config() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setSmallestDisplacement(10);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        startLocationUpdates();
        LocalBroadcastManager.getInstance(LocationJobService.this).registerReceiver(stopJobReceiver, new IntentFilter(ACTION_STOP_JOB));
    }

    private void startLocationUpdates() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    Intent i = new Intent(LOCATION_ACQUIRED);
                    i.putExtra("location", location);

                    LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);

                    updatesList.add(location); // add location points to the list
                    /*if (NetworkUtils.isNetworkConnected(getApplicationContext())) { // check whether internet is available or not
                        updatesList.add(location); //if available add latest location point and send list to server
                        *//*Intent i1 = new Intent(LocationJobService.this, UploadLocationService.class);
                        i1.putParcelableArrayListExtra("points", updatesList);
                        startService(i1); *//*//i have disabled the call as the server URL in intent service is dummy URL. Change the URL to your server URL and call this intent service
                        for (Location latLng : updatesList) {
                            doUpdateLatLngForTracking(latLng.getLatitude(), latLng.getLongitude());
                        }
                        updatesList.clear();

                        jobFinished(jobParameters, false);
                    } else { // if there is no internet connection
                        updatesList.add(location); // add location points to the list
                    }*/
                }
            }

        };
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(LocationJobService.this);
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
        Intent jobStartedMessage = new Intent(JOB_STATE_CHANGED);
        jobStartedMessage.putExtra("isStarted", true);
        Log.d("send broadcast", " as job started");
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(jobStartedMessage);
        createNotification();
        //Toast.makeText(getApplicationContext(),"Location job service started",Toast.LENGTH_SHORT).show();
    }

    private void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();

        } else {
            Log.e("api client", "not null");
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("job", "stopped");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        isJobRunning = false;
        stopLocationUpdates();
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("track.JobService", "google API client connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("track.JobService", "google API client suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("track.JobService", "google API client failed");
    }

    private void createNotification() {
        Notification.Builder mBuilder = new Notification.Builder(getBaseContext());
        Notification notification;

        String CHANNEL_ID = getString(R.string.default_notification_channel_id);// The id of the channel.
        CharSequence name = "Abc";// The user-visible name of the channel.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = mBuilder.setSmallIcon(R.mipmap.ic_launcher).setTicker("Tracking").setWhen(0)
                    .setAutoCancel(false)
                    .setContentTitle("Tracking")
                    .setContentText("Track in progress")
                    .setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary))
                    .setChannelId(CHANNEL_ID)
                    .setShowWhen(true)
                    .setOngoing(true)
                    .build();
        } else {
            notification = mBuilder.setSmallIcon(R.mipmap.ic_launcher).setTicker("Tracking").setWhen(0)
                    .setAutoCancel(false)
                    .setContentTitle("Tracking")
                    .setContentText("Track in progress")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setShowWhen(true)
                    .setOngoing(true)
                    .build();
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(mChannel);
        }
        /*assert notificationManager != null;
        notificationManager.notify(0, notification);*/
        startForeground(1, notification); //for foreground service, don't use 0 as id. it will not work.
    }

    private void removeNotification() {
        /*NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(0);*/ //use this for normal service
        stopForeground(true); // use this for foreground service
    }

    private void stopLocationUpdates() {

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        Log.d("stop location ", " updates called");
        if (mLocationCallback != null && mFusedLocationProviderClient != null) {
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
            // Toast.makeText(getApplicationContext(), "Location job service stopped.", Toast.LENGTH_SHORT).show();
        }
        getSharedPreferences("track", MODE_PRIVATE).edit().putBoolean("isServiceStarted", false).apply();
        Intent jobStoppedMessage = new Intent(JOB_STATE_CHANGED);
        jobStoppedMessage.putExtra("isStarted", false);
        Log.d("broadcasted", "job state change");
        removeNotification();
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(jobStoppedMessage);
    }
}
