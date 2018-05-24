package com.yaerin.sqlite;

import android.app.Application;

import com.yaerin.sqlite.util.Crashlytics;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Crashlytics(this));
    }
}
