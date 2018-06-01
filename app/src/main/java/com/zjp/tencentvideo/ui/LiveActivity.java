package com.zjp.tencentvideo.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
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
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.zjp.tencentvideo.DanmakuController;
import com.zjp.tencentvideo.R;
import com.zjp.tencentvideo.utils.TCFrequeControl;
import com.zjp.tencentvideo.view.TCInputTextMsgDialog;
import com.zjp.tencentvideo.view.like.TCHeartLayout;

import java.net.MalformedURLException;
import java.net.URL;

import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * Created by zjp on 2018/5/30 13:53.
 */

public class LiveActivity extends AppCompatActivity implements TCInputTextMsgDialog.OnTextSendListener, View.OnClickListener {

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
        //创建 player 对象
        mLivePlayer = new TXLivePlayer(this);
        //关键 player 对象与界面 view
        mLivePlayer.setPlayerView(mTxCloudVideoView);

        mTxCloudVideoView.setVisibility(View.VISIBLE);

        String url = "rtmp://24649.liveplay.myqcloud.com/live/24649_616dc269b2";
        mLivePlayer.startPlay(url, TXLivePlayer.PLAY_TYPE_LIVE_RTMP);
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
}
