package com.zjp.tencentvideo.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.zjp.tencentvideo.R;
import com.zjp.tencentvideo.beautysettings.BeautyDialogFragment;
import com.zjp.tencentvideo.utils.BitmapUtils;
import com.zjp.tencentvideo.utils.TCUtils;

/**
 * Created by zjp on 2018/5/29 14:48.
 * 推流
 */

public class RTMPActivity extends AppCompatActivity implements ITXLivePushListener, BeautyDialogFragment.OnBeautyParamsChangeListener, View.OnClickListener {

    private TXCloudVideoView mTxCloudVideoView;
    private TXLivePushConfig mLivePushConfig;
    private TXLivePusher mLivePusher;
    private Button mBtnFaceBeauty, mBtnMsgInput, mBtnCameraChange, mBtnClose;
    private View mRootView;

    private int mBeautyLevel = 5;
    private int mBeautyStyle = TXLiveConstants.BEAUTY_STYLE_SMOOTH;
    private int mWhiteningLevel = 3;
    private int mRuddyLevel = 2;

    private static final int VIDEO_SRC_CAMERA = 0;
    private static final int VIDEO_SRC_SCREEN = 1;
    private int mVideoSrc = VIDEO_SRC_CAMERA;

    boolean mVideoPublish;

    //美颜管理
    BeautyDialogFragment mBeautyDialogFragment;
    BeautyDialogFragment.BeautyParams mBeautyParams;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtmp);

//        String version = TXLiveBase.getSDKVersionStr();
//        Log.d("zjp", "version=" + version);

        initView();
        initData();
        initListener();
    }

    private void initView() {
        mTxCloudVideoView = findViewById(R.id.txcloutvideo);
        mBtnFaceBeauty = findViewById(R.id.btnFaceBeauty);
        mRootView = findViewById(R.id.rootview);
        mBtnMsgInput = findViewById(R.id.btn_message_input);
        mBtnCameraChange = findViewById(R.id.btnCameraChange);
        mBtnClose = findViewById(R.id.btn_close);

    }

    private void initData() {
        //推流
        mLivePusher = new TXLivePusher(this);
        mLivePushConfig = new TXLivePushConfig();
        mLivePusher.setConfig(mLivePushConfig);

        mVideoPublish = startPusherRtmp();

        mBeautyDialogFragment = new BeautyDialogFragment();
        mBeautyParams = new BeautyDialogFragment.BeautyParams();
        //美颜选择监听
        mBeautyDialogFragment.setBeautyParamsListner(mBeautyParams, this);
    }

    private void initListener() {
        mBtnMsgInput.setOnClickListener(this);
        mBtnFaceBeauty.setOnClickListener(this);
        mBtnCameraChange.setOnClickListener(this);
        mBtnClose.setOnClickListener(this);
    }

    /**
     * 启动推流
     */
    private boolean startPusherRtmp() {

        //开启相机
        if (mVideoSrc != VIDEO_SRC_SCREEN) {
            mTxCloudVideoView.setVisibility(View.VISIBLE);
        }

        Bitmap bitmap = BitmapUtils.decodeResource(getResources(), R.drawable.langman);
        //水印
        mLivePushConfig.setWatermark(bitmap, 0.02f, 0.05f, 0.2f);

        int customModeType = 0;

//        if (isActivityCanRotation()) {
//            onActivityRotation();
//        }
        mLivePushConfig.setCustomModeType(customModeType);
        mLivePusher.setPushListener(this);
        mLivePushConfig.setPauseImg(300, 5);
        mLivePushConfig.setPauseImg(bitmap);
        mLivePushConfig.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
        if (mVideoSrc != VIDEO_SRC_SCREEN) {
            mLivePushConfig.setFrontCamera(true);
            mLivePushConfig.setBeautyFilter(5, 3, 2);
            mLivePusher.setConfig(mLivePushConfig);
            mLivePusher.startCameraPreview(mTxCloudVideoView); // //是将界面元素和 Pusher 对象关联起来，从而能够将手机摄像头采集到的画面渲染到屏幕上。
        } else {
            mLivePusher.setConfig(mLivePushConfig);
            mLivePusher.startScreenCapture();
        }

        String rtmpUrl = "rtmp://24649.livepush.myqcloud.com/live/24649_f6f228af70?bizid=24649&txSecret=6cf88af8599cf6b756261f2ce6ee1d7e&txTime=5B0D78FF";
        mLivePusher.startPusher(rtmpUrl); //告诉 SDK 音视频流要推到哪个推流URL上去
        return true;
    }


    /**
     * 开始直播的回调
     */
    @Override
    public void onPushEvent(int event, Bundle bundle) {
        String msg = bundle.getString(TXLiveConstants.EVT_DESCRIPTION);
        Log.d("zjp", "receive event: " + event + ", " + msg);

        //错误还是要明确的报一下
        if (event < 0) {
            Toast.makeText(getApplicationContext(), bundle.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            if (event == TXLiveConstants.PUSH_ERR_OPEN_CAMERA_FAIL || event == TXLiveConstants.PUSH_ERR_OPEN_MIC_FAIL) {
                stopPublishRtmp();
            }
        }

        if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_WARNING_HW_ACCELERATION_FAIL) {
            Toast.makeText(getApplicationContext(), bundle.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            mLivePushConfig.setHardwareAcceleration(TXLiveConstants.ENCODE_VIDEO_SOFTWARE);
//            mBtnHWEncode.setBackgroundResource(R.drawable.quick2);
            mLivePusher.setConfig(mLivePushConfig);
//            mHWVideoEncode = false;
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_UNSURPORT) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_START_FAILED) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_RESOLUTION) {
            Log.d("zjp", "change resolution to " + bundle.getInt(TXLiveConstants.EVT_PARAM2) + ", bitrate to" + bundle.getInt(TXLiveConstants.EVT_PARAM1));
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_BITRATE) {
            Log.d("zjp", "change bitrate to" + bundle.getInt(TXLiveConstants.EVT_PARAM1));
        } else if (event == TXLiveConstants.PUSH_WARNING_NET_BUSY) {
//            ++mNetBusyCount;
//            Log.d(TAG, "net busy. count=" + mNetBusyCount);
//            showNetBusyTips();
        } else if (event == TXLiveConstants.PUSH_EVT_START_VIDEO_ENCODER) {
            int encType = bundle.getInt(TXLiveConstants.EVT_PARAM1);
//            mHWVideoEncode = (encType == 1);
//            mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
        }
    }

    @Override
    public void onNetStatus(Bundle bundle) {
        String str = getNetStatusString(bundle);
        Log.d("zjp", "Current status, CPU:" + bundle.getString(TXLiveConstants.NET_STATUS_CPU_USAGE) +
                ", RES:" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_WIDTH) + "*" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_HEIGHT) +
                ", SPD:" + bundle.getInt(TXLiveConstants.NET_STATUS_NET_SPEED) + "Kbps" +
                ", FPS:" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_FPS) +
                ", ARA:" + bundle.getInt(TXLiveConstants.NET_STATUS_AUDIO_BITRATE) + "Kbps" +
                ", VRA:" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_BITRATE) + "Kbps");
//        if (mLivePusher != null){
//            mLivePusher.onLogRecord("[net state]:\n"+str+"\n");
//        }
    }

    /**
     * 关闭推流
     */
    private void stopPublishRtmp() {
        mVideoPublish = false;
        mLivePusher.stopBGM();
        mLivePusher.stopCameraPreview(true);
        mLivePusher.stopScreenCapture();
        mLivePusher.setPushListener(null);
        mLivePusher.stopPusher();
        mTxCloudVideoView.setVisibility(View.GONE);

        if (mLivePushConfig != null) {
            mLivePushConfig.setPauseImg(null);
        }
    }

    protected void onActivityRotation() {
        // 自动旋转打开，Activity随手机方向旋转之后，需要改变推流方向
        int mobileRotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
        boolean screenCaptureLandscape = false;
        switch (mobileRotation) {
            case Surface.ROTATION_0:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
                break;
            case Surface.ROTATION_180:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_UP;
                break;
            case Surface.ROTATION_90:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT;
                screenCaptureLandscape = true;
                break;
            case Surface.ROTATION_270:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_LEFT;
                screenCaptureLandscape = true;
                break;
            default:
                break;
        }
        mLivePusher.setRenderRotation(0); //因为activity也旋转了，本地渲染相对正方向的角度为0。
        mLivePushConfig.setHomeOrientation(pushRotation);
        if (mLivePusher.isPushing()) {
            if (VIDEO_SRC_CAMERA == mVideoSrc) {
                mLivePusher.setConfig(mLivePushConfig);
                mLivePusher.stopCameraPreview(true);
                mLivePusher.startCameraPreview(mTxCloudVideoView);
            } else if (VIDEO_SRC_SCREEN == mVideoSrc) {

                mLivePusher.setConfig(mLivePushConfig);
                mLivePusher.stopScreenCapture();
                mLivePusher.startScreenCapture();
            }
        }
    }

    /**
     * 判断Activity是否可旋转。只有在满足以下条件的时候，Activity才是可根据重力感应自动旋转的。
     * 系统“自动旋转”设置项打开；
     *
     * @return false---Activity可根据重力感应自动旋转
     */
    protected boolean isActivityCanRotation() {
        // 判断自动旋转是否打开
        int flag = Settings.System.getInt(this.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        if (flag == 0) {
            return false;
        }
        return true;
    }

    //公用打印辅助函数
    protected String getNetStatusString(Bundle status) {
        String str = String.format("%-14s %-14s %-12s\n%-8s %-8s %-8s %-8s\n%-14s %-14s %-12s\n%-14s %-14s",
                "CPU:" + status.getString(TXLiveConstants.NET_STATUS_CPU_USAGE),
                "RES:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_WIDTH) + "*" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_HEIGHT),
                "SPD:" + status.getInt(TXLiveConstants.NET_STATUS_NET_SPEED) + "Kbps",
                "JIT:" + status.getInt(TXLiveConstants.NET_STATUS_NET_JITTER),
                "FPS:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_FPS),
                "GOP:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_GOP) + "s",
                "ARA:" + status.getInt(TXLiveConstants.NET_STATUS_AUDIO_BITRATE) + "Kbps",
                "QUE:" + status.getInt(TXLiveConstants.NET_STATUS_CODEC_CACHE) + "|" + status.getInt(TXLiveConstants.NET_STATUS_CACHE_SIZE),
                "DRP:" + status.getInt(TXLiveConstants.NET_STATUS_CODEC_DROP_CNT) + "|" + status.getInt(TXLiveConstants.NET_STATUS_DROP_SIZE),
                "VRA:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_BITRATE) + "Kbps",
                "SVR:" + status.getString(TXLiveConstants.NET_STATUS_SERVER_IP),
                "AUDIO:" + status.getString(TXLiveConstants.NET_STATUS_AUDIO_INFO));
        return str;
    }

    /**
     * 美颜回调
     */
    @Override
    public void onBeautyParamsChange(BeautyDialogFragment.BeautyParams params, int key) {
        switch (key) {
            case BeautyDialogFragment.BEAUTYPARAM_BEAUTY:
            case BeautyDialogFragment.BEAUTYPARAM_WHITE:
                if (mLivePusher != null) {
                    mLivePusher.setBeautyFilter(params.mBeautyStyle, params.mBeautyProgress, params.mWhiteProgress, params.mRuddyProgress);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_FACE_LIFT:
                if (mLivePusher != null) {
                    mLivePusher.setFaceSlimLevel(params.mFaceLiftProgress);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_BIG_EYE:
                if (mLivePusher != null) {
                    mLivePusher.setEyeScaleLevel(params.mBigEyeProgress);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_FILTER:
                if (mLivePusher != null) {
                    mLivePusher.setFilter(TCUtils.getFilterBitmap(getResources(), params.mFilterIdx));
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_MOTION_TMPL:
                if (mLivePusher != null) {
                    mLivePusher.setMotionTmpl(params.mMotionTmplPath);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_GREEN:
                if (mLivePusher != null) {
                    mLivePusher.setGreenScreenFile(TCUtils.getGreenFileName(params.mGreenIdx));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_message_input:
                break;

            case R.id.btnFaceBeauty:
                //美颜
                if (mBeautyDialogFragment.isAdded()) {

                    mBeautyDialogFragment.dismiss();
                } else {
                    mBeautyDialogFragment.show(getSupportFragmentManager(), "");
                }
                break;

            case R.id.btnCameraChange:
                //摄像头切换
                mLivePusher.switchCamera();
                break;

            case R.id.btn_close:
                break;
        }
    }
}
