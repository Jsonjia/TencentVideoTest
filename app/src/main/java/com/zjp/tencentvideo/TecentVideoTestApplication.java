package com.zjp.tencentvideo;

import android.support.multidex.MultiDexApplication;

/**
 * Created by zjp on 2018/5/29 17:20.
 */

public class TecentVideoTestApplication extends MultiDexApplication {

    private static TecentVideoTestApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static TecentVideoTestApplication getApplication() {
        return instance;
    }
}