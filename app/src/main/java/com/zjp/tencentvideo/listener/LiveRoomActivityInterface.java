package com.zjp.tencentvideo.listener;


/**
 * Created by dennyfeng on 2017/11/22.
 */

public interface LiveRoomActivityInterface {
    LiveRoom getLiveRoom();
    String getSelfUserID();
    String getSelfUserName();
    void     showGlobalLog(boolean enable);
    void     printGlobalLog(String format, Object... args);
    void     setTitle(String s);
}
