package com.zjp.tencentvideo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.tencent.rtmp.TXLiveBase;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

public class MainActivity extends AppCompatActivity {


    private TXCloudVideoView mTxCloudVideoView;
    private TXLivePushConfig mLivePushConfig;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTxCloudVideoView = findViewById(R.id.txcloutvideo);

       /* String versionStr = TXLiveBase.getSDKVersionStr();
        Log.d("zjp", "versinoStr=" + versionStr);*/


       //推流
        TXLivePusher mLivePusher = new TXLivePusher(this);
        mLivePushConfig = new TXLivePushConfig();
        mLivePusher.setConfig(mLivePushConfig);

        String rtmpUrl = "rtmp://24649.liveplay.myqcloud.com/live/24649_ef53d4eab4";
        mLivePusher.startPusher(rtmpUrl);

        mLivePusher.startCameraPreview(mTxCloudVideoView);
    }
}
