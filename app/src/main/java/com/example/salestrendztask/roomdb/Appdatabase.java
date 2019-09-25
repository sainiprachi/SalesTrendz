package com.example.salestrendztask.roomdb;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;




@Database(entities = {LocationModel.class},
        version = 1, exportSchema = false)
abstract  class Appdatabase extends RoomDatabase {
    private static Appdatabase mAppDatabase;

    synchronized static Appdatabase getDatabaseInstance(Context context) {
        if (mAppDatabase == null) {
            mAppDatabase = Room.databaseBuilder(context, Appdatabase.class, "getContact")
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return mAppDatabase;
    }

    abstract LocationDao getContactDao();








}
