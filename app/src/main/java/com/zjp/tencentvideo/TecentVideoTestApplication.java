package com.zjp.tencentvideo;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

/**
 * Created by zjp on 2018/5/28 14:26.
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
