package com.example.hanming;

import android.app.Application;

import com.example.hanming.utils.MyExceptionHandler;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MyExceptionHandler handler = new MyExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }
}
