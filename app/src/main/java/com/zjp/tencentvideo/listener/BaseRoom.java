package com.zjp.tencentvideo.listener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dennyfeng on 2017/12/8.
 */

public abstract class BaseRoom implements IMMessageMgr.IMMessageListener {

    public static String ROOM_SERVICE_DOMAIN = "https://room.qcloud.com/weapp/";;

    protected Context mContext;
    protected Handler mHandler;

    protected long                          mAppID;
    protected SelfAccountInfo               mSelfAccountInfo;
    protected String mSelfPushUrl;
    protected String mCurrRoomID;

    protected TXLivePusher mTXLivePusher;
    protected TXLivePushListenerImpl        mTXLivePushListener;

    protected IMMessageMgr                  mIMMessageMgr;          //IM SDK相关
    protected HttpRequests                  mHttpRequest;           //HTTP CGI请求相关
    protected HeartBeatThread               mHeartBeatThread;       //心跳

    protected HashMap<String, PlayerItem> mPlayers                = new LinkedHashMap<>();
    protected HashMap<String, PusherInfo> mPushers                = new LinkedHashMap<>();
    protected ArrayList<RoomInfo> mRoomList               = new ArrayList<>();


    public BaseRoom(Context context) {
        mContext = context.getApplicationContext();
        mHandler = new Handler(mContext.getMainLooper());

        mIMMessageMgr = new IMMessageMgr(context);
        mIMMessageMgr.setIMMessageListener(this);

        mHeartBeatThread = new HeartBeatThread();
    }

    public void login(String serverDomain, final LoginInfo loginInfo, final IMMessageMgr.Callback callback) {
        if (mHttpRequest != null) {
            mHttpRequest.cancelAllRequests();
            mHttpRequest = null;
        }
        mHttpRequest = new HttpRequests(serverDomain);

        mHttpRequest.login(loginInfo.sdkAppID, loginInfo.accType, loginInfo.userID, loginInfo.userSig, new HttpRequests.OnResponseCallback<HttpResponse.LoginResponse>() {
            @Override
            public void onResponse(int retcode, @Nullable String retmsg, @Nullable HttpResponse.LoginResponse data) {
                if (retcode == 0 && data != null) {
                    mHttpRequest.setUserID(data.userID);
                    mHttpRequest.setToken(data.token);

                    mAppID = loginInfo.sdkAppID;
                    mSelfAccountInfo = new SelfAccountInfo(data.userID, loginInfo.userName, loginInfo.userAvatar, loginInfo.userSig, loginInfo.accType, loginInfo.sdkAppID);

                    // 初始化IM SDK，内部完成login
                    if (mIMMessageMgr != null) {
                        mIMMessageMgr.initialize(mSelfAccountInfo.userID, mSelfAccountInfo.userSig, (int)mSelfAccountInfo.sdkAppID, callback);
                    }
                }
                else {
                    callback.onError(-1, "RoomServer登录失败");
                }
            }
        });


    }

    public void logout() {
        mContext = null;
        mHandler = null;

        if (mHttpRequest != null) {
            mHttpRequest.logout(new HttpRequests.OnResponseCallback<HttpResponse>() {
                @Override
                public void onResponse(int retcode, @Nullable String retmsg, @Nullable HttpResponse data) {
                    mHttpRequest.cancelAllRequests();
                    mHttpRequest = null;
                }
            });
        }

        if (mIMMessageMgr != null) {
            mIMMessageMgr.setIMMessageListener(null);
            mIMMessageMgr.unInitialize();
            mIMMessageMgr = null;
        }

        mHeartBeatThread.stopHeartbeat();
        mHeartBeatThread.quit();
    }

    public void sendRoomTextMsg(@NonNull String message, final IMMessageMgr.Callback callback){
        mIMMessageMgr.sendGroupTextMessage(mSelfAccountInfo.userName, mSelfAccountInfo.userAvatar, message, callback);
    }

    public void sendRoomCustomMsg(@NonNull String cmd, @NonNull String message, final IMMessageMgr.Callback callback) {
        CommonJson<CustomMessage> customMessage = new CommonJson<>();
        customMessage.cmd = "CustomCmdMsg";
        customMessage.data = new CustomMessage();
        customMessage.data.userName = mSelfAccountInfo.userName;
        customMessage.data.userAvatar = mSelfAccountInfo.userAvatar;
        customMessage.data.cmd = cmd;
        customMessage.data.msg = message ;
        String content = new Gson().toJson(customMessage, new TypeToken<CommonJson<CustomMessage>>(){}.getType());
        mIMMessageMgr.sendGroupCustomMessage(content, callback);
    }

    public synchronized void startLocalPreview(final @NonNull TXCloudVideoView videoView) {
        invokeDebugLog("[BaseRoom] startLocalPreview");
        initLivePusher();
        if (mTXLivePusher != null) {
            if (videoView != null) {
                videoView.setVisibility(View.VISIBLE);
            }
            mTXLivePusher.startCameraPreview(videoView);
        }
    }

    public synchronized void stopLocalPreview() {
        if (mTXLivePusher != null) {
            mSelfPushUrl = "";
            mTXLivePusher.setPushListener(null);
            mTXLivePusher.stopCameraPreview(true);
            mTXLivePusher.stopPusher();
            mTXLivePusher = null;
        }

        unInitLivePusher();
    }

    protected interface PlayCallback {
        void onPlayBegin();
        void onPlayError();
        void onPlayEvent(int event);
    }

    public void addRemoteView(final @NonNull TXCloudVideoView videoView, final @NonNull PusherInfo pusherInfo, final PlayCallback callback) {
        invokeDebugLog("[BaseRoom] 开始播放 UserID{"+pusherInfo.userID + "}, URL{" + pusherInfo.accelerateURL + "}");

        synchronized (this) {
            if (mPlayers.containsKey(pusherInfo.userID)) {
                PlayerItem pusherPlayer = mPlayers.get(pusherInfo.userID);
                if (pusherPlayer.player.isPlaying()) {
                    return;
                }
                else {
                    pusherPlayer = mPlayers.remove(pusherInfo.userID);
                    pusherPlayer.destroy();
                }
            }

            final TXLivePlayer player = new TXLivePlayer(mContext);

            videoView.setVisibility(View.VISIBLE);
            player.setPlayerView(videoView);
            player.enableHardwareDecode(true);
            player.setRenderMode(TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN);

            PlayerItem pusherPlayer = new PlayerItem(videoView, pusherInfo, player);
            mPlayers.put(pusherInfo.userID, pusherPlayer);

            player.setPlayListener(new ITXLivePlayListener() {
                @Override
                public void onPlayEvent(final int event, Bundle param) {
                    if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onPlayBegin();
                                }
                            }
                        });
                    }
                    else if (event == TXLiveConstants.PLAY_EVT_PLAY_END || event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onPlayError();
                                }
                            }
                        });

                        //结束播放
                        if (mPlayers.containsKey(pusherInfo.userID)) {
                            PlayerItem item = mPlayers.remove(pusherInfo.userID);
                            if (item != null) {
                                item.destroy();
                            }
                        }

                        // 刷新下pushers
                        // onPusherChanged();
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onPlayEvent(event);
                                }
                            }
                        });
                    }
                }

                @Override
                public void onNetStatus(Bundle status) {

                }
            });

            int result = player.startPlay(pusherInfo.accelerateURL, TXLivePlayer.PLAY_TYPE_LIVE_RTMP_ACC);
            if (result != 0){
                invokeDebugLog(String.format("[BaseRoom] 播放成员 {%s} 地址 {%s} 失败", pusherInfo.userID, pusherInfo.accelerateURL));
            }
        }
    }

    public void deleteRemoteView(final @NonNull PusherInfo pusherInfo) {
        invokeDebugLog(String.format("[BaseRoom] 停止播放 UserID{%s}, URL{%s}", pusherInfo.userID, pusherInfo.accelerateURL));

        synchronized (this){
            if (mPlayers.containsKey(pusherInfo.userID)){
                PlayerItem pusherPlayer = mPlayers.remove(pusherInfo.userID);
                pusherPlayer.destroy();
            }

            if (mPushers.containsKey(pusherInfo.userID)) {
                mPushers.remove(pusherInfo.userID);
            }
        }
    }

    public void switchToBackground(){
        invokeDebugLog("[BaseRoom] onPause");

        if (mTXLivePusher != null && mTXLivePusher.isPushing()) {
            mTXLivePusher.pausePusher();
        }

        synchronized (this) {
            for (Map.Entry<String, PlayerItem> entry : mPlayers.entrySet()) {
                entry.getValue().pause();
            }
        }
    }

    public void switchToForeground(){
        invokeDebugLog("[BaseRoom] onResume");

        if (mTXLivePusher != null && mTXLivePusher.isPushing()) {
            mTXLivePusher.resumePusher();
        }

        synchronized (this) {
            for (Map.Entry<String, PlayerItem> entry : mPlayers.entrySet()) {
                entry.getValue().resume();
            }
        }
    }

    public void switchCamera() {
        if (mTXLivePusher != null) {
            mTXLivePusher.switchCamera();
        }
    }

    public boolean turnOnFlashLight(boolean enable) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.turnOnFlashLight(enable);
        }
        return false;
    }

    public void setMute(boolean isMute) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setMute(isMute);
        }
    }

    public boolean setBeautyFilter(int style, int beautyLevel, int whiteningLevel, int ruddyLevel) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.setBeautyFilter(style, beautyLevel, whiteningLevel, ruddyLevel);
        }
        return false;
    }

    public boolean setZoom(int value) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.setZoom(value);
        }
        return false;
    }

    public boolean setMirror(boolean enable) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.setMirror(enable);
        }
        return false;
    }

    public void setExposureCompensation(float value) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setExposureCompensation(value);
        }
    }

    public boolean setMicVolume(float x) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.setMicVolume(x);
        }
        return false;
    }

    public boolean setBGMVolume(float x) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.setBGMVolume(x);
        }
        return false;
    }

    public void setBGMNofify(TXLivePusher.OnBGMNotify notify){
        if (mTXLivePusher != null) {
            mTXLivePusher.setBGMNofify(notify);
        }
    }

    public int getMusicDuration(String path) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.getMusicDuration(path);
        }
        return 0;
    }

    public boolean playBGM(String path) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.playBGM(path);
        }
        return false;
    }

    public boolean stopBGM() {
        if (mTXLivePusher != null) {
            return mTXLivePusher.stopBGM();
        }
        return false;
    }

    public boolean pauseBGM() {
        if (mTXLivePusher != null) {
            return mTXLivePusher.pauseBGM();
        }
        return false;
    }

    public boolean resumeBGM() {
        if (mTXLivePusher != null) {
            return mTXLivePusher.resumeBGM();
        }
        return false;
    }

    public void setFilter(Bitmap bmp) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setFilter(bmp);
        }
    }

    public void setMotionTmpl(String specialValue) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setMotionTmpl(specialValue);
        }
    }

    public boolean setGreenScreenFile(String file) {
        if (mTXLivePusher != null) {
            return mTXLivePusher.setGreenScreenFile(file);
        }
        return false;
    }

    public void setEyeScaleLevel(int level) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setEyeScaleLevel(level);
        }
    }

    public void setFaceSlimLevel(int level) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setFaceSlimLevel(level);
        }
    }

    public void setFaceVLevel(int level) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setFaceVLevel(level);
        }
    }

    public void setSpecialRatio(float ratio) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setSpecialRatio(ratio);
        }
    }

    public void setFaceShortLevel(int level) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setFaceShortLevel(level);
        }
    }

    public void setChinLevel(int scale) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setChinLevel(scale);
        }
    }

    public void setNoseSlimLevel(int scale) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setNoseSlimLevel(scale);
        }
    }

    public void setReverb(int reverbType) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setReverb(reverbType);
        }
    }
    
    public void setVoiceChangerType(int voiceChangerType) {
        if (mTXLivePusher != null) {
            mTXLivePusher.setVoiceChangerType(voiceChangerType);
        }
    }

    public void setPauseImage(final @Nullable Bitmap bitmap) {
        if (mTXLivePusher != null) {
            TXLivePushConfig config = mTXLivePusher.getConfig();
            config.setPauseImg(bitmap);
            config.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
            mTXLivePusher.setConfig(config);
        }
    }

    public void setPauseImage(final @IdRes int id){
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), id);
        if (mTXLivePusher != null) {
            TXLivePushConfig config = mTXLivePusher.getConfig();
            config.setPauseImg(bitmap);
            config.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
            mTXLivePusher.setConfig(config);
        }
    }

    public void updateSelfUserInfo(String userName, String userAvatar) {
        if (mSelfAccountInfo != null) {
            mSelfAccountInfo.userName = userName;
            mSelfAccountInfo.userAvatar = userAvatar;
        }
    }

    protected void initLivePusher() {
        if (mTXLivePusher == null) {
            TXLivePushConfig config = new TXLivePushConfig();
            config.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);
            mTXLivePusher = new TXLivePusher(this.mContext);
            mTXLivePusher.setConfig(config);
            mTXLivePusher.setBeautyFilter(TXLiveConstants.BEAUTY_STYLE_SMOOTH, 5, 3, 2);

            mTXLivePushListener = new TXLivePushListenerImpl();
            mTXLivePusher.setPushListener(mTXLivePushListener);
        }
    }

    protected void unInitLivePusher() {
        if (mTXLivePusher != null) {
            mSelfPushUrl = "";
            mTXLivePushListener = null;
            mTXLivePusher.setPushListener(null);
            mTXLivePusher.stopCameraPreview(true);
            mTXLivePusher.stopPusher();
            mTXLivePusher = null;
        }
    }

    protected interface PusherStreamCallback {
        void onError(int errCode, String errInfo);
        void onSuccess();
    }

    protected void startPushStream(final String url, final int videoQuality, final PusherStreamCallback callback){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTXLivePushListener != null) {
                    if (mTXLivePushListener.cameraEnable() == false) {
                        callback.onError(-1, "获取摄像头权限失败");
                        return;
                    }
                    if (mTXLivePushListener.micEnable() == false) {
                        callback.onError(-1, "获取麦克风权限失败");
                        return;
                    }
                }
                if (mTXLivePusher != null) {
                    invokeDebugLog("[BaseRoom] 开始推流 PushUrl = " + url);
                    mSelfPushUrl = url;
                    mTXLivePushListener.setCallback(callback);
                    mTXLivePusher.setVideoQuality(videoQuality, false, false);
                    mTXLivePusher.startPusher(url);
                }
            }
        });
    }

    protected void startPushStream(final String url, final PusherStreamCallback callback){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTXLivePushListener != null) {
                    if (mTXLivePushListener.cameraEnable() == false) {
                        callback.onError(-1, "获取摄像头权限失败");
                        return;
                    }
                    if (mTXLivePushListener.micEnable() == false) {
                        callback.onError(-1, "获取麦克风权限失败");
                        return;
                    }
                }
                if (mTXLivePusher != null) {
                    invokeDebugLog("[BaseRoom] 开始推流 PushUrl = " + url);
                    mSelfPushUrl = url;
                    mTXLivePushListener.setCallback(callback);
                    mTXLivePusher.startPusher(url);
                }
            }
        });
    }

    protected interface CreateRoomCallback {
        void onError(int errCode, String errInfo);
        void onSuccess(String roomID);
    }

    protected void doCreateRoom(final String roomID, String roomInfo, final CreateRoomCallback callback){
        mHttpRequest.createRoom(roomID, mSelfAccountInfo.userID, roomInfo,
                new HttpRequests.OnResponseCallback<HttpResponse.CreateRoom>() {
                    @Override
                    public void onResponse(int retcode, @Nullable String retmsg, @Nullable HttpResponse.CreateRoom data) {
                        if (retcode != HttpResponse.CODE_OK || data == null || data.roomID == null) {
                            invokeDebugLog("[BaseRoom] 创建直播间错误： " + retmsg);
                            callback.onError(retcode, retmsg);
                        } else {
                            invokeDebugLog("[BaseRoom] 创建直播间 ID{" + data.roomID + "} 成功 ");
                            callback.onSuccess(data.roomID);
                        }
                    }//onResponse
                });
    }

    protected interface JoinGroupCallback {
        void onError(int errCode, String errInfo);
        void onSuccess();
    }

    protected void jionGroup(final String roomID, final JoinGroupCallback callback){
        mIMMessageMgr.jionGroup(roomID, new IMMessageMgr.Callback() {
            @Override
            public void onError(int code, String errInfo) {
                callback.onError(code, errInfo);
            }

            @Override
            public void onSuccess(Object... args) {
                callback.onSuccess();
            }
        });
    }

    protected interface AddPusherCallback {
        void onError(int errCode, String errInfo);
        void onSuccess();
    }

    protected void addPusher(final String roomID, final String pushURL, final AddPusherCallback callback) {
        mHttpRequest.addPusher(roomID,
                mSelfAccountInfo.userID,
                mSelfAccountInfo.userName,
                mSelfAccountInfo.userAvatar,
                pushURL, new HttpRequests.OnResponseCallback<HttpResponse>() {
                    @Override
                    public void onResponse(int retcode, @Nullable String retmsg, @Nullable HttpResponse data) {
                        if (retcode == HttpResponse.CODE_OK) {
                            invokeDebugLog("[BaseRoom] Enter Room 成功");
                            callback.onSuccess();
                        } else {
                            invokeDebugLog("[BaseRoom] Enter Room 失败");
                            callback.onError(retcode, retmsg);
                        }
                    }
                });
    }

    protected interface UpdatePushersCallback {
        void onUpdatePushersComplete(int errcode, List<PusherInfo> newPushers, List<PusherInfo> delPushers, HashMap<String, PusherInfo> mergedPushers);
    }

    protected void updatePushers(final boolean excludeRoomCreator, final UpdatePushersCallback callback){
        mHttpRequest.getPushers(mCurrRoomID, new HttpRequests.OnResponseCallback<HttpResponse.PusherList>() {
            @Override
            public void onResponse(final int retcode, @Nullable String retmsg, @Nullable final HttpResponse.PusherList data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (retcode == HttpResponse.CODE_OK) {
                            if (data != null && data.pushers != null && data.pushers.size() > 0) {
                                List<PusherInfo> pusherList = data.pushers;
                                if (excludeRoomCreator) {
                                    if (pusherList != null && pusherList.size() > 0) {
                                        Iterator<PusherInfo> it = pusherList.iterator();
                                        while (it.hasNext()) {
                                            PusherInfo pusher = it.next();
                                            // 从房间成员列表里过滤过自己和大主播（房间创建者）
                                            if (pusher.userID != null) {
                                                if (pusher.userID.equalsIgnoreCase(getRoomCreator(mCurrRoomID))) {
                                                    it.remove();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }

                                List<PusherInfo> newPushers = new ArrayList<>();
                                List<PusherInfo> delPushers = new ArrayList<>();
                                HashMap<String, PusherInfo> mergedPushers = new HashMap<String, PusherInfo>();
                                mergerPushers(pusherList, newPushers, delPushers, mergedPushers);

                                if (callback != null) {
                                    callback.onUpdatePushersComplete(retcode, newPushers, delPushers, mergedPushers);
                                }
                            }
                            else {
                                invokeDebugLog("[BaseRoom] updatePushers 返回空数据");
                            }
                        }
                        else {
                            if (callback != null) {
                                callback.onUpdatePushersComplete(retcode, null, null, null);
                            }
                        }
                    }
                });
            }
        });
    }

    protected synchronized void mergerPushers(List<PusherInfo> pushers, List<PusherInfo> newPushers, List<PusherInfo> delPushers, HashMap<String, PusherInfo> mergedPushers){
        if (pushers == null) {
            if (delPushers != null) {
                delPushers.clear();
                for (Map.Entry<String, PusherInfo> entry : mPushers.entrySet()) {
                    delPushers.add(entry.getValue());
                }
            }
            mPushers.clear();
            return;
        }

        for (PusherInfo member : pushers) {
            if (member.userID != null && (!member.userID.equals(mSelfAccountInfo.userID))){
                if (!mPushers.containsKey(member.userID)) {
                    if (newPushers != null) {
                        newPushers.add(member);
                    }
                }
                mergedPushers.put(member.userID, member);
            }
        }

        if (delPushers != null) {
            for (Map.Entry<String, PusherInfo> entry : mPushers.entrySet()) {
                if (!mergedPushers.containsKey(entry.getKey())) {
                    delPushers.add(entry.getValue());
                }
            }
        }
    }

    protected void cleanPlayers() {
        synchronized (this) {
            for (Map.Entry<String, PlayerItem> entry : mPlayers.entrySet()) {
                entry.getValue().destroy();
            }
            mPlayers.clear();
        }
    }

    protected void runOnUiThread(final Runnable runnable){
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            });
        }
    }

    protected void runOnUiThreadDelay(final Runnable runnable, long delayMills){
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }, delayMills);
        }
    }

    protected String getRoomCreator(String roomID) {
        for (RoomInfo item: mRoomList) {
            if (roomID.equalsIgnoreCase(item.roomID)) {
                return item.roomCreator;
            }
        }
        return null;
    }

    protected String getMixedPlayUrlByRoomID(String roomID) {
        for (RoomInfo item : mRoomList) {
            if (item.roomID != null && item.roomID.equalsIgnoreCase(roomID)) {
                return item.mixedPlayURL;
            }
        }
        return null;
    }

    protected String getAcceleratePlayUrlByRoomID(String roomID) {
        for (RoomInfo item : mRoomList) {
            if (item.roomID != null && item.roomID.equalsIgnoreCase(roomID)) {
                for (PusherInfo info: item.pushers) {
                    if (info.userID != null && info.userID.equalsIgnoreCase(item.roomCreator)) {
                        return info.accelerateURL;
                    }
                }
                return null;
            }
        }
        return null;
    }

    protected String getSelfAcceleratePlayUrl() {
        String accelerateUrl = "";

        //推流地址域名里的livepush替换为liveplay即为低时延加速拉流地址
        if (mSelfPushUrl != null && mSelfPushUrl.length() > 0) {
            String strTemp = mSelfPushUrl.substring(mSelfPushUrl.indexOf("://") + 3);
            String pushDomain = strTemp.substring(0, strTemp.indexOf("/"));
            String playDomain = pushDomain.replace("livepush", "liveplay");

            int index = mSelfPushUrl.indexOf(pushDomain);
            accelerateUrl = mSelfPushUrl.substring(0, index) + playDomain + mSelfPushUrl.substring(index + pushDomain.length());
        }

        return accelerateUrl;
    }

    protected String getRoomInfoByRoomID(String roomID) {
        for (RoomInfo item: mRoomList) {
            if (roomID.equalsIgnoreCase(item.roomID)) {
                return item.roomInfo;
            }
        }
        return null;
    }

    protected int getPlayType(String playUrl) {
        int playType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
        if (playUrl.startsWith("rtmp://")) {
            playType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
        } else if ((playUrl.startsWith("http://") || playUrl.startsWith("https://")) && playUrl.contains(".flv")) {
            playType = TXLivePlayer.PLAY_TYPE_LIVE_FLV;
        }
        return playType;
    }

    protected class SelfAccountInfo {
        public String userID;
        public String userName;
        public String userAvatar;
        public String userSig;
        public String accType;
        public long     sdkAppID;

        public SelfAccountInfo(String userID, String userName, String headPicUrl, String userSig, String accType, long sdkAppID) {
            this.userID = userID;
            this.userName = userName;
            this.userAvatar = headPicUrl;
            this.userSig = userSig;
            this.accType = accType;
            this.sdkAppID = sdkAppID;
        }
    }

    private  class PlayerItem {
        public TXCloudVideoView view;
        public PusherInfo       pusher;
        public TXLivePlayer player;

        public PlayerItem(TXCloudVideoView view, PusherInfo pusher, TXLivePlayer player) {
            this.view = view;
            this.pusher = pusher;
            this.player = player;
        }

        public void resume(){
            this.player.resume();
        }

        public void pause(){
            this.player.pause();
        }

        public void destroy(){
            this.player.stopPlay(true);
            this.view.onDestroy();
        }
    }

    protected class HeartBeatThread extends HandlerThread {
        private Handler handler;
        private boolean running = false;

        public HeartBeatThread() {
            super("HeartBeatThread");
            this.start();
            handler = new Handler(this.getLooper());
        }

        private Runnable heartBeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (mSelfAccountInfo != null && mSelfAccountInfo.userID != null && mSelfAccountInfo.userID.length() > 0 && mCurrRoomID != null && mCurrRoomID.length() > 0) {
                    boolean success = mHttpRequest.heartBeat(mSelfAccountInfo.userID, mCurrRoomID);
                    if (success) {
                        handler.postDelayed(heartBeatRunnable, 5000);
                    }
                }
            }
        };

        public boolean running() {
            return running;
        }

        public void startHeartbeat(){
            running = true;
            handler.postDelayed(heartBeatRunnable, 1000);
        }

        public void stopHeartbeat(){
            running = false;
            handler.removeCallbacks(heartBeatRunnable);
        }
    }

    private class TXLivePushListenerImpl implements ITXLivePushListener {
        private boolean mCameraEnable = true;
        private boolean mMicEnable = true;
        private PusherStreamCallback mCallback = null;

        public void setCallback(PusherStreamCallback callback) {
            mCallback = callback;
        }

        public boolean cameraEnable() {
            return mCameraEnable;
        }

        public boolean micEnable() {
            return mMicEnable;
        }

        @Override
        public void onPushEvent(int event, Bundle param) {
            if (event == TXLiveConstants.PUSH_EVT_PUSH_BEGIN) {
                invokeDebugLog("[BaseRoom] 推流成功");
                if (mCallback != null) {
                    mCallback.onSuccess();
                }
            } else if (event == TXLiveConstants.PUSH_ERR_OPEN_CAMERA_FAIL) {
                mCameraEnable = false;
                invokeDebugLog("[BaseRoom] 推流失败：打开摄像头失败");
                if (mCallback != null) {
                    mCallback.onError(-1, "获取摄像头权限失败");
                }
                else {
                    invokeError(-1, "获取摄像头权限失败");
                }
            } else if (event == TXLiveConstants.PUSH_ERR_OPEN_MIC_FAIL) {
                mMicEnable = false;
                invokeDebugLog("[BaseRoom] 推流失败：打开麦克风失败");
                if (mCallback != null) {
                    mCallback.onError(-1, "获取麦克风权限失败");
                }
                else {
                    invokeError(-1, "获取麦克风权限失败");
                }
            } else if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {
                invokeDebugLog("[BaseRoom] 推流失败：网络断开");
                invokeError(-1, "网络断开，推流失败");
            }
        }

        @Override
        public void onNetStatus(Bundle status) {

        }
    }

    protected class MainCallback<C, T> {

        private C callback;

        public MainCallback(C callback) {
            this.callback = callback;
        }

        public void onError(final int errCode, final String errInfo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Method onError = callback.getClass().getMethod("onError", int.class, String.class);
                        onError.invoke(callback, errCode, errInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        public void onSuccess(final T obj) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Method onSuccess = callback.getClass().getMethod("onSuccess", obj.getClass());
                        onSuccess.invoke(callback, obj);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        public void onSuccess() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Method onSuccess = callback.getClass().getMethod("onSuccess");
                        onSuccess.invoke(callback);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected class CustomMessage{
        public String userName;
        public String userAvatar;
        public String cmd;
        public String msg;
    }

    protected class CommonJson<T> {
        public String cmd;
        public T      data;
        public CommonJson() {
        }
    }

    protected abstract void invokeDebugLog(String log);

    protected abstract void invokeError(int errorCode, String errorMessage);
}
