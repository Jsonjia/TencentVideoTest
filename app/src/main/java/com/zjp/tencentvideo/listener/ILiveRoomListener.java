package com.zjp.tencentvideo.listener;

import java.util.List;

public interface ILiveRoomListener {

    /**
     * 获取房间成员通知
     * @param pusherList    房间成员列表
     */
    void onGetPusherList(List<PusherInfo> pusherList);

    /**
     * 新成员加入房间通知
     * @param pusherInfo    成员信息
     */

    void onPusherJoin(PusherInfo pusherInfo);
    /**
     * 成员离开房间通知
     * @param pusherInfo    成员信息
     */

    void onPusherQuit(PusherInfo pusherInfo);

    /**
     * 大主播收到连麦请求
     * @param userID	    连麦请求者ID
     * @param userName      连麦请求者昵称
     * @param userAvatar    连麦请求者头像地址
     */
    void onRecvJoinPusherRequest(String userID, String userName, String userAvatar);

    /**
     * 小主播收到被大主播踢开消息
     */
    void onKickOut();

    /**
     * 主播收到PK请求
     * @param userID	    PK请求者ID
     * @param userName      PK请求者昵称
     * @param userAvatar    PK请求者头像地址
     * @param streamUrl     PK请求者流地址
     */
    void onRecvPKRequest(String userID, String userName, String userAvatar, String streamUrl);

    /**
     * 主播收到结束PK的请求
     * @param userID	    PK请求者ID
     */
    void onRecvPKFinishRequest(String userID);

    /**
     * 收到房间文本消息
     * @param roomID        房间ID
     * @param userID        发送者ID
     * @param userName      发送者昵称
     * @param userAvatar    发送者头像
     * @param message       文本消息
     */
    void onRecvRoomTextMsg(String roomID, String userID, String userName, String userAvatar, String message);

    /**
     * 收到房间自定义消息
     * @param roomID        房间ID
     * @param userID        发送者ID
     * @param userName      发送者昵称
     * @param userAvatar    发送者头像
     * @param cmd           自定义cmd
     * @param message       自定义消息内容
     */
    void onRecvRoomCustomMsg(String roomID, String userID, String userName, String userAvatar, String cmd, String message);

    /**
     * 收到房间解散通知
     * @param roomID        房间ID
     */
    void onRoomClosed(String roomID);

    /**
     * 日志回调
     * @param log           日志内容
     */
    void onDebugLog(String log);

    /**
     * 错误回调
     * @param errorCode     错误码
     * @param errorMessage  错误描述
     */
    void onError(int errorCode, String errorMessage);
}
