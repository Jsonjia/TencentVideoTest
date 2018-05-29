package com.zjp.tencentvideo.ui;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.zjp.tencentvideo.R;
import com.zjp.tencentvideo.widget.BeautySettingPannel;

public class MainActivity extends AppCompatActivity implements BeautySettingPannel.IOnBeautyParamsChangeListener {

    private int mVideoQuality = TXLiveConstants.VIDEO_QUALITY_HIGH_DEFINITION;
    private boolean mAutoBitrate = false;
    private boolean mAutoResolution = false;

    private TXCloudVideoView mTxCloudVideoView;
    private TXLivePushConfig mLivePushConfig;

    private Button mBtnFaceBeauty;
    private BeautySettingPannel mBeautyPannelView;
    private TXLivePusher mLivePusher;
    private View mRootView;
    private int mBeautyLevel = 5;
    private int mBeautyStyle = TXLiveConstants.BEAUTY_STYLE_SMOOTH;
    private int mWhiteningLevel = 3;
    private int mRuddyLevel = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTxCloudVideoView = findViewById(R.id.txcloutvideo);
        mBtnFaceBeauty = findViewById(R.id.btnFaceBeauty);
        mBeautyPannelView = findViewById(R.id.layoutFaceBeauty);
        mRootView = findViewById(R.id.rootview);
        mBeautyPannelView.setBeautyParamsChangeListener(this);

        mBtnFaceBeauty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeautyPannelView.setVisibility(mBeautyPannelView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });


        //推流
        mLivePusher = new TXLivePusher(this);
        mLivePushConfig = new TXLivePushConfig();
        mLivePusher.setConfig(mLivePushConfig);

        String rtmpUrl = "rtmp://24649.liveplay.myqcloud.com/live/24649_ef53d4eab4";
        mLivePusher.startPusher(rtmpUrl); //告诉 SDK 音视频流要推到哪个推流URL上去

        mLivePusher.startCameraPreview(mTxCloudVideoView); //是将界面元素和 Pusher 对象关联起来，从而能够将手机摄像头采集到的画面渲染到屏幕上。


        mLivePusher.setVideoQuality(mVideoQuality, mAutoBitrate, mAutoResolution);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mBeautyPannelView.setVisibility(View.GONE);
                break;
        }
        return false;
    }

    @Override
    public void onBeautyParamsChange(BeautySettingPannel.BeautyParams params, int key) {

        switch (key) {
            case BeautySettingPannel.BEAUTYPARAM_EXPOSURE:
                if (mLivePusher != null) {
                    mLivePusher.setExposureCompensation(params.mExposure);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_BEAUTY:
                mBeautyLevel = params.mBeautyLevel;
                if (mLivePusher != null) {
                    mLivePusher.setBeautyFilter(mBeautyStyle, mBeautyLevel, mWhiteningLevel, mRuddyLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_WHITE:
                mWhiteningLevel = params.mWhiteLevel;
                if (mLivePusher != null) {
                    mLivePusher.setBeautyFilter(mBeautyStyle, mBeautyLevel, mWhiteningLevel, mRuddyLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_BIG_EYE:
                if (mLivePusher != null) {
                    mLivePusher.setEyeScaleLevel(params.mBigEyeLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FACE_LIFT:
                if (mLivePusher != null) {
                    mLivePusher.setFaceSlimLevel(params.mFaceSlimLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FILTER:
                if (mLivePusher != null) {
                    mLivePusher.setFilter(params.mFilterBmp);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_GREEN:
                if (mLivePusher != null) {
                    mLivePusher.setGreenScreenFile(params.mGreenFile);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_MOTION_TMPL:
                if (mLivePusher != null) {
                    mLivePusher.setMotionTmpl(params.mMotionTmplPath);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_RUDDY:
                mRuddyLevel = params.mRuddyLevel;
                if (mLivePusher != null) {
                    mLivePusher.setBeautyFilter(mBeautyStyle, mBeautyLevel, mWhiteningLevel, mRuddyLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_BEAUTY_STYLE:
                mBeautyStyle = params.mBeautyStyle;
                if (mLivePusher != null) {
                    mLivePusher.setBeautyFilter(mBeautyStyle, mBeautyLevel, mWhiteningLevel, mRuddyLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FACEV:
                if (mLivePusher != null) {
                    mLivePusher.setFaceVLevel(params.mFaceVLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FACESHORT:
                if (mLivePusher != null) {
                    mLivePusher.setFaceShortLevel(params.mFaceShortLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_CHINSLIME:
                if (mLivePusher != null) {
                    mLivePusher.setChinLevel(params.mChinSlimLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_NOSESCALE:
                if (mLivePusher != null) {
                    mLivePusher.setNoseSlimLevel(params.mNoseScaleLevel);
                }
                break;
            case BeautySettingPannel.BEAUTYPARAM_FILTER_MIX_LEVEL:
                if (mLivePusher != null) {
                    mLivePusher.setSpecialRatio(params.mFilterMixLevel / 10.f);
                }
                break;
//            case BeautySettingPannel.BEAUTYPARAM_CAPTURE_MODE:
//                if (mLivePusher != null) {
//                    boolean bEnable = ( 0 == params.mCaptureMode ? false : true);
//                    mLivePusher.enableHighResolutionCapture(bEnable);
//                }
//                break;
//            case BeautySettingPannel.BEAUTYPARAM_SHARPEN:
//                if (mLivePusher != null) {
//                    mLivePusher.setSharpenLevel(params.mSharpenLevel);
//                }
//                break;
        }
    }
}
