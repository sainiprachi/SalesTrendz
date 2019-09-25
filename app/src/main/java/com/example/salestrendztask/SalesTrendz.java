package com.example.salestrendztask;

import android.app.Application;

import com.example.salestrendztask.roomdb.AppDataManager;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;


public class SalesTrendz extends Application {

    private static double LATITUDE;
    private static double LONGITUDE;
    private static AppDataManager appInstance;
    private static SalesTrendz instance;

    private FirebaseJobDispatcher dispatcher;

    public static synchronized SalesTrendz getInstance() {
        if (instance != null) {
            return instance;
        }
        return new SalesTrendz();
    }

    public static AppDataManager getDataManager() {
        return appInstance;
    }

    public static double getLATITUDE() {
        return LATITUDE;
    }

    public static void setLATITUDE(double LATITUDE) {
        SalesTrendz.LATITUDE = LATITUDE;
    }

    public static double getLONGITUDE() {
        return LONGITUDE;
    }

    public static void setLONGITUDE(double LONGITUDE) {
        SalesTrendz.LONGITUDE = LONGITUDE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appInstance = AppDataManager.getInstance(this);
    }

    public FirebaseJobDispatcher getJobDispatcher() {
        //creating new firebase job dispatcher
        if (dispatcher == null)
            dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getApplicationContext()));

        return dispatcher;
    }

}
