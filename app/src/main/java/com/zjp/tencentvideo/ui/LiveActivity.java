package com.zjp.tencentvideo.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.opensource.svgaplayer.SVGADrawable;
import com.opensource.svgaplayer.SVGAImageView;
import com.opensource.svgaplayer.SVGAParser;
import com.opensource.svgaplayer.SVGAVideoEntity;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.ugc.TXRecordCommon;
import com.zjp.tencentvideo.DanmakuController;
import com.zjp.tencentvideo.R;
import com.zjp.tencentvideo.utils.TCFrequeControl;
import com.zjp.tencentvideo.view.TCInputTextMsgDialog;
import com.zjp.tencentvideo.view.like.TCHeartLayout;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * Created by zjp on 2018/5/30 13:53.
 */

public class LiveActivity extends AppCompatActivity implements TCInputTextMsgDialog.OnTextSendListener, View.OnClickListener, ITXLivePlayListener {

    private TXCloudVideoView mTxCloudVideoView;
    private DanmakuView danmakuView;
    private SwitchCompat mSwitchBt;
    private RecyclerView mRv_user_avatar;
    private ImageView mBtn_message_input, mbtn_record, mbtn_like, mclose_record, mrecord, mretry_record, mPlayBtn, mbtn_linkmic, mBtnRenderRotation;
    private Button mbtn_back;
    private ListView mim_msg_listview;
    private ProgressBar mrecord_progress;
    private TCHeartLayout mheart_layout;
    private DanmakuView mDanmakuView;
    private TCHeartLayout mHeartLayout;
    private TextView mtv_animation;
    private SVGAImageView mimageView;

    private TCFrequeControl mLikeFrequeControl;

    private int mCurrentRenderMode;
    private int mCurrentRenderRotation;

    private boolean flag;
    private boolean isStopAnimation;

    private TXLivePlayConfig mPlayConfig;
    private long mStartPlayTS = 0;

    /**
     * inputDialog
     */
    TCInputTextMsgDialog mInputTextMsgDialog;

    /**
     * 弹幕
     */
    DanmakuController mDanmakuController;

    private TXLivePlayer mLivePlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        initView();
        initData();
        initListener();
    }

    private void initView() {

        mCurrentRenderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
        mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;

        mTxCloudVideoView = findViewById(R.id.video_view);
        danmakuView = findViewById(R.id.danmaku);
        mSwitchBt = findViewById(R.id.switch_bt);
        mRv_user_avatar = findViewById(R.id.rv_user_avatar);
        mBtn_message_input = findViewById(R.id.btn_message_input);
        mtv_animation = findViewById(R.id.tv_animation);
        mbtn_record = findViewById(R.id.btn_record);
        mbtn_like = findViewById(R.id.btn_like);
        mbtn_back = findViewById(R.id.btn_back);
        mim_msg_listview = findViewById(R.id.im_msg_listview);
        mrecord_progress = findViewById(R.id.record_progress);
        mclose_record = findViewById(R.id.close_record);
        mrecord = findViewById(R.id.record);
        mretry_record = findViewById(R.id.retry_record);
        mheart_layout = findViewById(R.id.heart_layout);
        mDanmakuView = findViewById(R.id.danmakuView);
        mPlayBtn = findViewById(R.id.play_btn);
        mbtn_linkmic = findViewById(R.id.btn_linkmic);
        mHeartLayout = findViewById(R.id.heart_layout);
        mBtnRenderRotation = findViewById(R.id.btnOrientation);
        mimageView = findViewById(R.id.imageView);
    }

    private void initData() {

        mbtn_record.setVisibility(View.GONE);


        mInputTextMsgDialog = new TCInputTextMsgDialog(this, R.style.InputDialog);
        mInputTextMsgDialog.setmOnTextSendListener(this);
        initRtmpPlayer();
        initDanmaKu();
    }

    private void initListener() {
        mBtn_message_input.setOnClickListener(this);
        mbtn_linkmic.setOnClickListener(this);
        mbtn_like.setOnClickListener(this);
        mBtnRenderRotation.setOnClickListener(this);
        mtv_animation.setOnClickListener(this);
    }

    /**
     * 初始化播放
     */
    private void initRtmpPlayer() {

        mTxCloudVideoView.setLogMargin(12, 12, 110, 60);
        mTxCloudVideoView.showLog(false);
        //创建 player 对象
        mLivePlayer = new TXLivePlayer(this);
        //关键 player 对象与界面 view
        mLivePlayer.setPlayerView(mTxCloudVideoView);


        mPlayConfig = new TXLivePlayConfig();
        //自动模式
        mPlayConfig.setAutoAdjustCacheTime(true);
        mPlayConfig.setCacheTime(5.0f);
        mPlayConfig.setMaxAutoAdjustCacheTime(5.0f);
        mPlayConfig.setMinAutoAdjustCacheTime(1.0f);

        // 设置播放器缓存时间.
        mPlayConfig.setMaxCacheItems(2);
        //设置播放器重连次数.
        mPlayConfig.setConnectRetryCount(5);
        //设置播放器重连间隔.
        mPlayConfig.setConnectRetryInterval(3);
        //开启就近选路 非腾讯云不用
        mPlayConfig.setEnableNearestIP(false);
        //设置自动调整时播放器最大缓存时间.
        mPlayConfig.setMaxAutoAdjustCacheTime(5.0f);

        //设置RTMP传输通道的类型
//        mPlayConfig.setRtmpChannelType(TXLivePlayer.PLAY_TYPE_LIVE_RTMP);
        mLivePlayer.setConfig(mPlayConfig);
        mLivePlayer.setPlayListener(this);
        //将图像等比例铺满整个屏幕，多余部分裁剪掉，此模式下画面不会留黑边，但可能因为部分区域被裁剪而显示不全。
        mLivePlayer.setRenderMode(TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN);
        //正常播放
        mLivePlayer.setRenderRotation(TXLiveConstants.RENDER_ROTATION_PORTRAIT);
        //硬件加速
        mLivePlayer.enableHardwareDecode(true);

        mTxCloudVideoView.setVisibility(View.VISIBLE);

        String url = "rtmp://24649.liveplay.myqcloud.com/live/24649_743e7a0df4";
        mLivePlayer.startPlay(url, TXLivePlayer.PLAY_TYPE_LIVE_RTMP);

        mStartPlayTS = System.currentTimeMillis();
    }

    /**
     * 初始化弹幕
     */
    private void initDanmaKu() {
        mDanmakuController = new DanmakuController(mDanmakuView, this);
        mDanmakuController.initDanmaKu();
        //默认不显示
        mDanmakuView.hide();
        //总开关
        mSwitchBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mDanmakuView.show();
                } else {
                    mDanmakuView.hide();
                }
            }
        });
    }

    /**
     * 发送弹幕后的回调
     */
    @Override
    public void onTextSend(String msg, boolean tanmuOpen) {
        if (msg.length() == 0) {

            return;
        }
        //关联弹幕总开关
        if (tanmuOpen) {
            mDanmakuView.show();
            mSwitchBt.setChecked(true);
        }
        //消息显示
        mDanmakuController.addDanmaku(tanmuOpen, msg);
    }

    /**
     * 发送消息弹出框
     */
    private void showInputMsgDialog() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp = mInputTextMsgDialog.getWindow().getAttributes();
        lp.width = (display.getWidth());
        mInputTextMsgDialog.getWindow().setAttributes(lp);
        mInputTextMsgDialog.setCancelable(true);
        mInputTextMsgDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mInputTextMsgDialog.show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mDanmakuView.getConfig().setDanmakuMargin(20);
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mDanmakuView.getConfig().setDanmakuMargin(40);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_message_input:
                showInputMsgDialog();
                break;
            case R.id.btn_linkmic:
                flag = !flag;
                if (flag) {
                    stopPlay();
                } else {
                    startPlay();
                }
                break;

            case R.id.btn_like:
                /**点赞*/
                if (mHeartLayout != null) {
                    mHeartLayout.setVisibility(View.VISIBLE);
                    mHeartLayout.addFavor();
                }
                //点赞发送请求限制
                if (mLikeFrequeControl == null) {
                    mLikeFrequeControl = new TCFrequeControl();
                    mLikeFrequeControl.init(2, 1);
                }
                break;

            case R.id.btnOrientation:
                if (mLivePlayer == null) {
                    return;
                }

                if (mCurrentRenderRotation == TXLiveConstants.RENDER_ROTATION_PORTRAIT) {
                    mBtnRenderRotation.setBackgroundResource(R.drawable.portrait);
                    mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_LANDSCAPE;
                } else if (mCurrentRenderRotation == TXLiveConstants.RENDER_ROTATION_LANDSCAPE) {
                    mBtnRenderRotation.setBackgroundResource(R.drawable.landscape);
                    mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;
                }

                mLivePlayer.setRenderRotation(mCurrentRenderRotation);

                break;

            case R.id.tv_animation:
                isStopAnimation = !isStopAnimation;
                if (isStopAnimation) {
                    mimageView.setVisibility(View.VISIBLE);
                    SVGAParser parser = new SVGAParser(this);
                    try {
                        parser.parse(new URL("https://github.com/yyued/SVGA-Samples/blob/master/kingset.svga?raw=true"), new SVGAParser.ParseCompletion() {
                            @Override
                            public void onComplete(SVGAVideoEntity mSVGAVideoEntity) {
                                SVGADrawable drawable = new SVGADrawable(mSVGAVideoEntity);
                                mimageView.setImageDrawable(drawable);
                                mimageView.startAnimation();
                            }

                            @Override
                            public void onError() {
                                Toast.makeText(LiveActivity.this, "parse error!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        System.out.print(true);
                    }
                } else {
                    mimageView.stopAnimation();
                    mimageView.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void stopPlay() {
        if (mLivePlayer != null) {
            mLivePlayer.pause(); // 暂停
        }
        mbtn_linkmic.setBackgroundResource(R.drawable.play_start);
    }

    private void startPlay() {
        if (mLivePlayer != null) {
            mLivePlayer.resume(); // 继续
        }
        mbtn_linkmic.setBackgroundResource(R.drawable.play_pause);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
        if (mTxCloudVideoView != null) {
            mLivePlayer.stopPlay(true); // true 代表清除最后一帧画面
            mTxCloudVideoView.onDestroy();

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
    }

    @Override
    public void onPlayEvent(int event, Bundle bundle) {
        String playEventLog = "receive event: " + event + ", " + bundle.getString(TXLiveConstants.EVT_DESCRIPTION);
        Log.d("zjp", "playEventLog=" + playEventLog);

        if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
            Log.d("zjp", "PlayFirstRender,cost=" + (System.currentTimeMillis() - mStartPlayTS));
        } else if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT || event == TXLiveConstants.PLAY_EVT_PLAY_END) {
            stopPlay();
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_LOADING) {
//            startLoadingAnimation();
        } else if (event == TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME) {
//            stopLoadingAnimation();
        } else if (event == TXLiveConstants.PLAY_EVT_CHANGE_RESOLUTION) {
//            streamRecord(false);
        } else if (event == TXLiveConstants.PLAY_EVT_CHANGE_ROTATION) {
            return;
        }

        if (event < 0) {
            Toast.makeText(getApplicationContext(), bundle.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNetStatus(Bundle bundle) {
        String str = getNetStatusString(bundle);
        Log.d("zjp", "str=" + str);
        Log.d("zjp", "Current status, CPU:" + bundle.getString(TXLiveConstants.NET_STATUS_CPU_USAGE) +
                ", RES:" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_WIDTH) + "*" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_HEIGHT) +
                ", SPD:" + bundle.getInt(TXLiveConstants.NET_STATUS_NET_SPEED) + "Kbps" +
                ", FPS:" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_FPS) +
                ", ARA:" + bundle.getInt(TXLiveConstants.NET_STATUS_AUDIO_BITRATE) + "Kbps" +
                ", VRA:" + bundle.getInt(TXLiveConstants.NET_STATUS_VIDEO_BITRATE) + "Kbps");
    }


    //公用打印辅助函数
    protected String getNetStatusString(Bundle status) {
        String str = String.format("%-14s %-14s %-12s\n%-8s %-8s %-8s %-8s\n%-14s %-14s\n%-14s %-14s",
                "CPU:" + status.getString(TXLiveConstants.NET_STATUS_CPU_USAGE),
                "RES:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_WIDTH) + "*" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_HEIGHT),
                "SPD:" + status.getInt(TXLiveConstants.NET_STATUS_NET_SPEED) + "Kbps",
                "JIT:" + status.getInt(TXLiveConstants.NET_STATUS_NET_JITTER),
                "FPS:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_FPS),
                "GOP:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_GOP) + "s",
                "ARA:" + status.getInt(TXLiveConstants.NET_STATUS_AUDIO_BITRATE) + "Kbps",
                "QUE:" + status.getInt(TXLiveConstants.NET_STATUS_CODEC_CACHE)
                        + "|" + status.getInt(TXLiveConstants.NET_STATUS_CACHE_SIZE)
                        + "," + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_CACHE_SIZE)
                        + "," + status.getInt(TXLiveConstants.NET_STATUS_V_DEC_CACHE_SIZE)
                        + "|" + status.getInt(TXLiveConstants.NET_STATUS_AV_RECV_INTERVAL)
                        + "," + status.getInt(TXLiveConstants.NET_STATUS_AV_PLAY_INTERVAL)
                        + "," + String.format("%.1f", status.getFloat(TXLiveConstants.NET_STATUS_AUDIO_PLAY_SPEED)).toString(),
                "VRA:" + status.getInt(TXLiveConstants.NET_STATUS_VIDEO_BITRATE) + "Kbps",
                "SVR:" + status.getString(TXLiveConstants.NET_STATUS_SERVER_IP),
                "AUDIO:" + status.getString(TXLiveConstants.NET_STATUS_AUDIO_INFO));
        return str;
    }
}
