package com.zjp.tencentvideo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.tencent.rtmp.TXLiveBase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String versionStr = TXLiveBase.getSDKVersionStr();
        Log.d("zjp", "versinoStr=" + versionStr);
    }
}
