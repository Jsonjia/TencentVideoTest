package com.zjp.tencentvideo.listener;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.tencent.imsdk.TIMCallBack;
import com.tencent.imsdk.TIMConnListener;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMCustomElem;
import com.tencent.imsdk.TIMElem;
import com.tencent.imsdk.TIMGroupManager;
import com.tencent.imsdk.TIMGroupSystemElem;
import com.tencent.imsdk.TIMGroupSystemElemType;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMMessageListener;
import com.tencent.imsdk.TIMSdkConfig;
import com.tencent.imsdk.TIMTextElem;
import com.tencent.imsdk.TIMUserConfig;
import com.tencent.imsdk.TIMValueCallBack;

import java.util.FormatFlagsConversionMismatchException;
import java.util.List;


/**
 * Created by jac on 2017/11/4.
 * Copyright © 2013-2017 Tencent Cloud. All Rights Reserved.
 */

public class IMMessageMgr implements TIMMessageListener {
    private static final String TAG = IMMessageMgr.class.getSimpleName();

    private Context mContext;
    private Handler mHandler;

    private static boolean                  mConnectSuccess = false;
    private boolean                         mLoginSuccess = false;

    private String mSelfUserID;
    private String mSelfUserSig;
    private String mGroupID;

    private TIMSdkConfig                    mTIMSdkConfig;
    private IMMessageConnCallback           mIMConnListener;
    private IMMessageLoginCallback          mIMLoginListener;
    private IMMessageCallback               mMessageListener;

    /**
     * 函数级公共Callback定义
     */
    public interface Callback{
        void onError(int code, String errInfo);
        void onSuccess(Object... args);
    }

    /**
     * 模块回调Listener定义
     */
    public interface IMMessageListener {
        /**
         * IM连接成功
         */
        void onConnected();

        /**
         * IM断开连接
         */
        void onDisconnected();

        /**
         * IM群组里推流者成员变化通知
         */
        void onPusherChanged();

        /**
         * 收到群文本消息
         */
        void onGroupTextMessage(String groupID, String senderID, String userName, String headPic, String message);

        /**
         * 收到自定义的群消息
         */
        void onGroupCustomMessage(String groupID, String senderID, String message);

        /**
         * 收到自定义的C2C消息
         */
        void onC2CCustomMessage(String sendID, String cmd, String message);

        /**
         * IM群组销毁回调
         */
        void onGroupDestroyed(final String groupID);

        /**
         * 日志回调
         */
        void onDebugLog(String log);
    }

    public IMMessageMgr(final Context context) {
        this.mContext = context.getApplicationContext();
        this.mHandler = new Handler(this.mContext.getMainLooper());
        this.mMessageListener = new IMMessageCallback(null);
    }

    /**
     * 设置回调
     * @param listener
     */
    public void setIMMessageListener(IMMessageListener listener){
        this.mMessageListener.setListener(listener);
    }

    /**
     * 初始化
     * @param userID    用户ID
     * @param userSig   签名
     * @param appID     appID
     * @param callback
     */
    public void initialize(final String userID, final String userSig, final int appID, final Callback callback){
        if (userID == null || userSig == null) {
            mMessageListener.onDebugLog("参数错误，请检查 UserID， userSig 是否为空！");
            callback.onError(-1, "参数错误");
            return;
        }

        this.mSelfUserID  = userID;
        this.mSelfUserSig = userSig;

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                long initializeStartTS = System.currentTimeMillis();
                mIMConnListener = new IMMessageConnCallback(initializeStartTS, callback);

                mTIMSdkConfig = new TIMSdkConfig(appID);
                mTIMSdkConfig.setServiceEnabled(false);
                mTIMSdkConfig.setConnectionListener(mIMConnListener);

                TIMManager.getInstance().addMessageListener(IMMessageMgr.this);
                if( TIMManager.getInstance().init(mContext, mTIMSdkConfig) ){
                    login(new Callback() {
                        @Override
                        public void onError(int code, String errInfo) {
                            printDebugLog("login failed: %s(%d)", errInfo, code);
                            mLoginSuccess = false;
                            callback.onError(code, "IM登录失败");
                        }

                        @Override
                        public void onSuccess(Object... args) {
                            printDebugLog("login success");
                            mLoginSuccess = true;
                            mConnectSuccess = true;
                            callback.onSuccess();
                        }
                    });
                }
                else {
                    printDebugLog("init failed");
                    callback.onError(-1, "IM初始化失败");
                }
            }
        });
    }

    /**
     * 反初始化
     */
    public void unInitialize(){
        mContext = null;
        mHandler = null;

        if (mTIMSdkConfig != null) {
            mTIMSdkConfig.setConnectionListener(null);
            mTIMSdkConfig = null;
        }
        if (mIMConnListener != null) {
            mIMConnListener.clean();
            mIMConnListener  = null;
        }
        if (mIMLoginListener != null) {
            mIMLoginListener.clean();
            mIMLoginListener = null;
        }
        if (mMessageListener != null) {
            mMessageListener.setListener(null);
            mMessageListener = null;
        }

        TIMManager.getInstance().removeMessageListener(IMMessageMgr.this);
        logout(null);
    }

    /**
     * 创建IM群组
     * @param groupId   群ID
     * @param groupName 群名称
     * @param callback
     */
    public void createGroup(final String groupId, final String groupName, final Callback callback){
        if (!mConnectSuccess || !mLoginSuccess){
            mMessageListener.onDebugLog("[createGroup] IM 没有初始化");
            callback.onError(-1, "IM 没有初始化");
            return;
        }

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                TIMGroupManager.CreateGroupParam param = new TIMGroupManager.CreateGroupParam("AVChatRoom", groupName);
                param.setGroupId(groupId);

                TIMGroupManager.getInstance().createGroup(param, new TIMValueCallBack<String>() {
                    @Override
                    public void onError(int i, String s) {
                        printDebugLog("创建群 \"%s(%s)\" 失败: %s(%d)", groupName, groupId, s, i);
                        callback.onError(i, s);
                    }

                    @Override
                    public void onSuccess(String s) {
                        printDebugLog("创建群 \"%s(%s)\" 成功", groupName, groupId);
                        mGroupID = groupId;
                        callback.onSuccess();
                    }
                });
            }
        });
    }

    /**
     * 加入IM群组
     * @param groupId  群ID
     * @param callback
     */
    public void jionGroup(final String groupId, final Callback callback){
        if (!mConnectSuccess || !mLoginSuccess){
            mMessageListener.onDebugLog("[jionGroup] IM 没有初始化");
            callback.onError(-1, "IM 没有初始化");
            return;
        }

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                TIMGroupManager.getInstance().applyJoinGroup(groupId, "who care?", new TIMCallBack() {
                    @Override
                    public void onError(int i, String s) {
                        printDebugLog("加入群 {%s} 失败:%s(%d)", groupId, s, i);
                        if (i == 10010) {
                            s = "房间已解散";
                        }
                        callback.onError(i, s);
                    }

                    @Override
                    public void onSuccess() {
                        printDebugLog("加入群 {%s} 成功", groupId);
                        mGroupID = groupId;
                        callback.onSuccess();
                    }
                });
            }
        });
    }

    /**
     * 退出IM群组
     * @param groupId  群ID
     * @param callback
     */
    public void quitGroup(final String groupId, final Callback callback){
        if (!mConnectSuccess || !mLoginSuccess){
            mMessageListener.onDebugLog("[quitGroup] IM 没有初始化");
            callback.onError(-1, "IM 没有初始化");
            return;
        }

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                TIMGroupManager.getInstance().quitGroup(groupId, new TIMCallBack() {
                    @Override
                    public void onError(int i, String s) {
                        if (i == 10010) {
                            printDebugLog("群 {%s} 已经解散了", groupId);
                            onSuccess();
                        }
                        else{
                            printDebugLog("退出群 {%s} 失败： %s(%d)", groupId, s, i);
                            callback.onError(i, s);
                        }
                    }

                    @Override
                    public void onSuccess() {
                        printDebugLog("退出群 {%s} 成功", groupId);
                        mGroupID = groupId;
                        callback.onSuccess();
                    }
                });
            }
        });
    }

    /**
     * 销毁IM群组
     * @param groupId  群ID
     * @param callback
     */
    public void destroyGroup(final String groupId, final Callback callback){
        if (!mConnectSuccess || !mLoginSuccess){
            mMessageListener.onDebugLog("IM 没有初始化");
            callback.onError(-1, "IM 没有初始化");
            return;
        }

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                TIMGroupManager.getInstance().deleteGroup(groupId, new TIMCallBack() {
                    @Override
                    public void onError(int i, String s) {
                        printDebugLog("解散群 {%s} 失败：%s(%d)", groupId, s, i);
                        callback.onError(i, s);
                    }

                    @Override
                    public void onSuccess() {
                        printDebugLog("解散群 {%s} 成功", groupId);
                        mGroupID = groupId;
                        callback.onSuccess();
                    }
                });
            }
        });
    }

    /**
     * 发送IM群文本消息
     * @param userName  发送者用户名
     * @param headPic   发送者头像
     * @param text      文本内容
     * @param callback
     */
    public void sendGroupTextMessage(final @NonNull String userName, final @NonNull String headPic, final @NonNull String text, final Callback callback){
        if (!mConnectSuccess || !mLoginSuccess){
            mMessageListener.onDebugLog("[sendGroupTextMessage] IM 没有初始化");
            if (callback != null)
                callback.onError(-1, "IM 没有初始化");
            return;
        }

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                TIMMessage message = new TIMMessage();
                try {
                    CommonJson<UserInfo> txtHeadMsg = new CommonJson<UserInfo>();
                    txtHeadMsg.cmd = "CustomTextMsg";
                    txtHeadMsg.data = new UserInfo();
                    txtHeadMsg.data.nickName = userName;
                    txtHeadMsg.data.headPic = headPic;
                    String strCmdMsg = new Gson().toJson(txtHeadMsg, new TypeToken<CommonJson<UserInfo>>(){}.getType());

                    TIMCustomElem customElem = new TIMCustomElem();
                    customElem.setData(strCmdMsg.getBytes("UTF-8"));

                    TIMTextElem textElem = new TIMTextElem();
                    textElem.setText(text);

                    message.addElement(customElem);
                    message.addElement(textElem);
                }
                catch (Exception e) {
                    printDebugLog("[sendGroupTextMessage] 发送群{%s}消息失败，组包异常", mGroupID);
                    if (callback != null) {
                        callback.onError(-1, "发送群消息失败");
                    }
                    return;
                }

                TIMConversation conversation = TIMManager.getInstance().getConversation(TIMConversationType.Group, mGroupID);
                conversation.sendMessage(message, new TIMValueCallBack<TIMMessage>() {
                    @Override
                    public void onError(int i, String s) {
                        printDebugLog("[sendGroupTextMessage] 发送群{%s}消息失败: %s(%d)", mGroupID, s, i);

                        if (callback != null)
                            callback.onError(i, s);
                    }

                    @Override
                    public void onSuccess(TIMMessage timMessage) {
                        printDebugLog("[sendGroupTextMessage] 发送群消息成功");

                        if (callback != null)
                            callback.onSuccess();
                    }
                });
            }
        });
    }

    /**
     * 发送自定义群消息
     * @param content   自定义消息的内容
     * @param callback
     */
    public void sendGroupCustomMessage(final @NonNull String content, final Callback callback) {
        if (!mConnectSuccess || !mLoginSuccess){
            mMessageListener.onDebugLog("[sendGroupCustomMessage] IM 没有初始化");
            if (callback != null)
                callback.onError(-1, "IM 没有初始化");
            return;
        }

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                TIMMessage message = new TIMMessage();
                try {
                    TIMCustomElem customElem = new TIMCustomElem();
                    customElem.setData(content.getBytes("UTF-8"));
                    message.addElement(customElem);
                }
                catch (Exception e) {
                    printDebugLog("[sendGroupCustomMessage] 发送自定义群{%s}消息失败，组包异常", mGroupID);
                    if (callback != null) {
                        callback.onError(-1, "发送CC消息失败");
                    }
                    return;
                }

                TIMConversation conversation = TIMManager.getInstance().getConversation(TIMConversationType.Group, mGroupID);
                conversation.sendMessage(message, new TIMValueCallBack<TIMMessage>() {
                    @Override
                    public void onError(int i, String s) {
                        printDebugLog("[sendGroupCustomMessage] 发送自定义群{%s}消息失败: %s(%d)", mGroupID, s, i);

                        if (callback != null)
                            callback.onError(i, s);
                    }

                    @Override
                    public void onSuccess(TIMMessage timMessage) {
                        printDebugLog("[sendGroupCustomMessage] 发送自定义群消息成功");

                        if (callback != null)
                            callback.onSuccess();
                    }
                });
            }
        });
    }

    /**
     * 发送CC（端到端）自定义消息
     * @param toUserID  接收者userID
     * @param content   自定义消息的内容
     * @param callback
     */
    public void sendC2CCustomMessage(final @NonNull String toUserID, final @NonNull String content, final Callback callback) {
        if (!mConnectSuccess || !mLoginSuccess){
            mMessageListener.onDebugLog("[sendCustomMessage] IM 没有初始化");
            if (callback != null)
                callback.onError(-1, "IM 没有初始化");
            return;
        }

        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                TIMMessage message = new TIMMessage();
                try {
                    TIMCustomElem customElem = new TIMCustomElem();
                    customElem.setData(content.getBytes("UTF-8"));
                    message.addElement(customElem);
                }
                catch (Exception e) {
                    printDebugLog("[sendCustomMessage] 发送CC{%s}消息失败，组包异常", toUserID);
                    if (callback != null) {
                        callback.onError(-1, "发送CC消息失败");
                    }
                    return;
                }

                TIMConversation conversation = TIMManager.getInstance().getConversation(TIMConversationType.C2C, toUserID);
                conversation.sendMessage(message, new TIMValueCallBack<TIMMessage>() {
                    @Override
                    public void onError(int i, String s) {
                        printDebugLog("[sendCustomMessage] 发送CC{%s}消息失败: %s(%d)", toUserID, s, i);

                        if (callback != null)
                            callback.onError(i, s);
                    }

                    @Override
                    public void onSuccess(TIMMessage timMessage) {
                        printDebugLog("[sendCustomMessage] 发送CC消息成功");

                        if (callback != null)
                            callback.onSuccess();
                    }
                });
            }
        });
    }

    @Override
    public boolean onNewMessages(List<TIMMessage> list) {
        for (TIMMessage message : list) {

            for (int i = 0; i < message.getElementCount(); i++) {
                TIMElem element = message.getElement(i);

                printDebugLog("onNewMessage type = %s", element.getType());

                switch (element.getType()){

                    case GroupSystem:{
                        TIMGroupSystemElemType systemElemType = ((TIMGroupSystemElem) element).getSubtype();

                        switch (systemElemType){

                            case TIM_GROUP_SYSTEM_DELETE_GROUP_TYPE:{
                                printDebugLog("onNewMessage subType = %s", systemElemType);
                                if (mMessageListener != null)
                                    mMessageListener.onGroupDestroyed(((TIMGroupSystemElem) element).getGroupId());
                                return true;
                            }

                            case TIM_GROUP_SYSTEM_CUSTOM_INFO:{

                                byte[] userData = ((TIMGroupSystemElem) element).getUserData();
                                if (userData == null || userData.length == 0){
                                    printDebugLog("userData == null");
                                    return true;
                                }

                                String data = new String(userData);
                                printDebugLog("onNewMessage subType = %s content = %s", systemElemType, data);
                                try {
                                    CommonJson<Object> commonJson = new Gson().fromJson(data, new TypeToken<CommonJson<Object>>(){}.getType());
                                    if (commonJson.cmd.equals("notifyPusherChange")) {
                                        mMessageListener.onPusherChanged();
                                    }
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                return true;
                            }
                        }

                        break;
                    }//case GroupSystem

                    case Custom: {
                        byte[] userData = ((TIMCustomElem) element).getData();
                        if (userData == null || userData.length == 0){
                            printDebugLog("userData == null");
                            return true;
                        }

                        String data = new String(userData);
                        printDebugLog("onNewMessage subType = Custom content = %s", data);
                        try {
                            CommonJson<Object> commonJson =  new Gson().fromJson(data, new TypeToken<CommonJson<Object>>(){}.getType());
                            if (commonJson.cmd != null) {
                                if (commonJson.cmd.equalsIgnoreCase("CustomTextMsg")) {
                                    ++i;
                                    UserInfo userInfo = new Gson().fromJson(new Gson().toJson(commonJson.data), UserInfo.class);
                                    if (userInfo != null && i < message.getElementCount()) {
                                        TIMElem nextElement = message.getElement(i);
                                        TIMTextElem textElem = (TIMTextElem) nextElement;
                                        String text = textElem.getText();
                                        if (text != null){
                                            mMessageListener.onGroupTextMessage(mGroupID, message.getSender(), userInfo.nickName, userInfo.headPic, text);
                                        }
                                    }
                                }
                                else if (commonJson.cmd.equalsIgnoreCase("linkmic") || commonJson.cmd.equalsIgnoreCase("pk")) {
                                    mMessageListener.onC2CCustomMessage(message.getSender(), commonJson.cmd, new Gson().toJson(commonJson.data));
                                }
                                else if (commonJson.cmd.equalsIgnoreCase("CustomCmdMsg")) {
                                    mMessageListener.onGroupCustomMessage(mGroupID, message.getSender(),  new Gson().toJson(commonJson.data));
                                }
                            }
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void login(final Callback cb){
        if (mSelfUserID == null || mSelfUserSig == null ){
            cb.onError(-1, "没有 UserId");
            return;
        }

        Log.i(TAG, "start login: userId = " + this.mSelfUserID);
        
        final long loginStartTS = System.currentTimeMillis();

        mIMLoginListener = new IMMessageLoginCallback(loginStartTS, cb);

        TIMUserConfig timUserConfig = new TIMUserConfig();
        TIMManager.getInstance().login(this.mSelfUserID, this.mSelfUserSig, timUserConfig, mIMLoginListener);
    }

    private void logout(final Callback callback){
        if (!mConnectSuccess || !mLoginSuccess){
            return;
        }

        TIMManager.getInstance().logout(null);
    }

    private void printDebugLog(String format, Object...args){
        String log;
        try {
            log = String.format(format, args);
            Log.e(TAG, log);
            if (mMessageListener != null) {
                mMessageListener.onDebugLog(log);
            }
        } catch (FormatFlagsConversionMismatchException e) {
            e.printStackTrace();
        }
    }

    /**
     * 辅助类 IM Connect Listener
     */
    private class IMMessageConnCallback implements TIMConnListener {
        private long            initializeStartTS = 0;
        private Callback        callback;
        
        public IMMessageConnCallback(long ts, Callback cb) {
            initializeStartTS = ts;
            callback = cb;
        }

        public void clean() {
            initializeStartTS = 0;
            callback = null;
        }
        
        @Override
        public void onConnected() {
            printDebugLog("connect success，initialize() time cost %.2f secs", (System.currentTimeMillis() - initializeStartTS) / 1000.0);
            mMessageListener.onConnected();
            mConnectSuccess = true;
        }

        @Override
        public void onDisconnected(int i, String s) {
            printDebugLog("disconnect: %s(%d)", s, i);
            if (mLoginSuccess) {
                mMessageListener.onDisconnected();
            }
            else {
                callback.onError(i, s);
            }
            mConnectSuccess = false;
        }

        @Override
        public void onWifiNeedAuth(String s) {
            printDebugLog("onWifiNeedAuth(): %s", s);
            if (mLoginSuccess){
                mMessageListener.onDisconnected();
            }
            else {
                callback.onError(-1, s);
            }
            mConnectSuccess = false;
        }
    }

    /**
     * 辅助类 IM Login Listener
     */
    private class IMMessageLoginCallback implements TIMCallBack {
        private long      loginStartTS ;
        private Callback  callback;
        
        public IMMessageLoginCallback(long ts, Callback cb) {
            loginStartTS = ts;
            callback = cb;
        }

        public void clean() {
            loginStartTS = 0;
            callback = null;
        }

        @Override
        public void onError(int i, String s) {
            if (callback != null) {
                callback.onError(i, s);
            }
        }

        @Override
        public void onSuccess() {
            printDebugLog("login success, time cost %.2f secs", (System.currentTimeMillis()- loginStartTS) / 1000.0);
            if (callback != null) {
                callback.onSuccess();
            }
        }
    };
    
    /**
     * 辅助类 IM Message Listener
     */
    private class IMMessageCallback implements IMMessageListener {
        private IMMessageListener listener;

        public IMMessageCallback(IMMessageListener listener) {
            this.listener = listener;
        }

        public void setListener(IMMessageListener listener) {
            this.listener = listener;
        }

        @Override
        public void onConnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onConnected();
                }
            });
        }

        @Override
        public void onDisconnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onDisconnected();
                }
            });
        }
        @Override
        public void onPusherChanged() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onPusherChanged();
                }
            });
        }

        @Override
        public void onGroupDestroyed(final String groupID) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onGroupDestroyed(groupID);
                }
            });
        }

        @Override
        public void onDebugLog(final String line) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onDebugLog("[IM] "+line);
                }
            });
        }

        @Override
        public void onGroupTextMessage(final String roomID, final String senderID, final String userName, final String headPic, final String message) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onGroupTextMessage(roomID, senderID, userName, headPic, message);
                }
            });
        }

        @Override
        public void onGroupCustomMessage(final String groupID, final String senderID, final String message) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onGroupCustomMessage(groupID, senderID, message);
                }
            });
        }

        @Override
        public void onC2CCustomMessage(final String senderID, final String cmd, final String message) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onC2CCustomMessage(senderID, cmd, message);
                }
            });
        }
    }

    private static class CommonJson<T> {
        String cmd;
        T      data;
    }

    private static final class UserInfo {
        String nickName;
        String headPic;
    }
}
