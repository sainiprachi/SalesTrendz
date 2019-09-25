package com.example.salestrendztask.roomdb;

import android.content.Context;


import java.util.List;


public class AppDataManager implements DataManager {
    private static AppDataManager instance;
    private final DbHelper mDbHelper;

    private AppDataManager(Context context) {
        mDbHelper = AppDbHelper.getDbInstance(context);
        //Gson mGson = new GsonBuilder().create();
    }

    public synchronized static AppDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppDataManager(context);
        }
        return instance;
    }




}
