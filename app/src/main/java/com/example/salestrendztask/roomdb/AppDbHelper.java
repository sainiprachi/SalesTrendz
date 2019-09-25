package com.example.salestrendztask.roomdb;

import android.content.Context;


import java.util.List;


public final class AppDbHelper implements DbHelper {

    private static AppDbHelper instance;

    private final Appdatabase mAppDatabase;

    private AppDbHelper(Context context) {
        this.mAppDatabase = Appdatabase.getDatabaseInstance(context);
    }

    public synchronized static DbHelper getDbInstance(Context context) {
        if (instance == null) {
            instance = new AppDbHelper(context);
        }
        return instance;
    }




}



