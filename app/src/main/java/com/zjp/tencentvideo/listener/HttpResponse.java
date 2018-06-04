package com.zjp.tencentvideo.listener;


import java.util.List;

/**
 * Created by jac on 2017/10/30.
 */

public class HttpResponse {
    public int code;

    public String message;

    public transient static int CODE_OK = 0;

    public static class LoginResponse extends HttpResponse {
        public String userID;
        public String token;
    }

    public static class RoomList extends HttpResponse {
        public List<RoomInfo> rooms;
    }

    public static class PusherList extends HttpResponse {
        public String roomID;
        public String roomInfo;
        public String roomCreator;
        public String mixedPlayURL;
        public List<PusherInfo> pushers;
    }

    public static class AudienceList extends HttpResponse {
        public List<RoomInfo.Audience> audiences; //观众列表
    }

    public static class CreateRoom extends HttpResponse {
        public String roomID;
    }

    public static class PushUrl extends HttpResponse {
        public String pushURL;
    }

    public static class MergeStream extends HttpResponse {
        public static class Result {
            int    code;
            String message;
            long   timestamp;
        }
        public Result result;
    }
}
