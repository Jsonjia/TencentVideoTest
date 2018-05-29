package com.zjp.tencentvideo.widget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.liteav.basic.log.TXCLog;
import com.zjp.tencentvideo.R;
import com.zjp.tencentvideo.widget.utils.VideoMaterialDownloadProgress;
import com.zjp.tencentvideo.widget.utils.VideoUtil;

import java.util.ArrayList;
import java.util.List;

public class BeautySettingPannel extends FrameLayout implements SeekBar.OnSeekBarChangeListener {
    private final String TAG = "BeautySettingPannel";
    public static final int ITEM_TYPE_BEAUTY_STYLE = 0;
    public static final int ITEM_TYPE_BEAUTY = 1;
    public static final int ITEM_TYPE_FILTTER = 2;
    public static final int ITEM_TYPE_MOTION = 3;
    public static final int ITEM_TYPE_KOUBEI = 4;
    public static final int ITEM_TYPE_GREEN = 5;

    private int mSencodGradleType = ITEM_TYPE_BEAUTY;
    private ArrayList<String> mFirstGradleArrayString = new ArrayList<String>();
    private ArrayList<String> mSencodeGradleArrayString = new ArrayList<String>();
    private int mThirdGradleIndex = 0;
    private int[][] mSzSeekBarValue = null;
    private int[] mSzSecondGradleIndex = new int[16];

    public static final int BEAUTYPARAM_EXPOSURE = 0;
    public static final int BEAUTYPARAM_BEAUTY = 1;
    public static final int BEAUTYPARAM_WHITE = 2;
    public static final int BEAUTYPARAM_FACE_LIFT = 3;
    public static final int BEAUTYPARAM_BIG_EYE = 4;
    public static final int BEAUTYPARAM_FILTER = 5;
    public static final int BEAUTYPARAM_FILTER_MIX_LEVEL = 6;
    public static final int BEAUTYPARAM_MOTION_TMPL = 7;
    public static final int BEAUTYPARAM_GREEN = 8;
    public static final int BEAUTYPARAM_BEAUTY_STYLE = 9;
    public static final int BEAUTYPARAM_RUDDY = 10;
    public static final int BEAUTYPARAM_NOSESCALE = 11;
    public static final int BEAUTYPARAM_CHINSLIME = 12;
    public static final int BEAUTYPARAM_FACEV = 13;
    public static final int BEAUTYPARAM_FACESHORT = 14;
    public static final int BEAUTYPARAM_SHARPEN = 15;
    public static final int BEAUTYPARAM_CAPTURE_MODE = 16;

    static public class BeautyParams {
        public float mExposure = 0;
        public int mBeautyLevel = 5;
        public int mWhiteLevel = 3;
        public int mRuddyLevel = 2;
        public int mSharpenLevel = 3;
        public int mBeautyStyle = 0;
        public int mFilterMixLevel = 0;
        public int mBigEyeLevel;
        public int mFaceSlimLevel;
        public int mNoseScaleLevel;
        public int mChinSlimLevel;
        public int mFaceVLevel;
        public int mFaceShortLevel;
        public Bitmap mFilterBmp;
        public String mMotionTmplPath;
        public String mGreenFile;
        public int mCaptureMode = 0;
    }

    private String[] mFirstGradleString = {
            "风格",
            "美颜",
            "滤镜",
            "动效",
            "抠背",
            "绿幕",
//            "采集"
    };

    private String[] mBeautyStyleString = {
            "光滑",
            "自然",
            "朦胧"
    };
    private String[] mBeautyString = {
            "美颜",
            "美白",
            "红润",
//            "清晰",
            "曝光",
            "大眼",
            "瘦脸",
            "V脸",
            "下巴",
            "短脸",
            "小鼻",
    };
    private String[] mBeautyFilterTypeString = {
            "无",
            "美白",
            "浪漫",
            "清新",
            "唯美",
            "粉嫩",
            "怀旧",
            "蓝调",
            "清凉",
            "日系",
    };
    private String[] mMotionTypeString = {
            "无动效",
            "Boom",
            "霓虹鼠",
            "星耳",
            "疯狂打call",
            "Q星座",
            "彩色丝带",
            "刘海发带",
            "变脸",
            "紫色小猫",
            "花仙子",
            "小公举",
    };
    private String[] mGreenString = {
            "无",
            "Good Luck"
    };
    private String[] mKoubeiString = {
            "无",
            "AI抠背"
    };

//    private String[] mCaptureModeString = {
//            "低采",
//            "高采"
//    };

    private List<MotionData> motionDataList = new ArrayList<>();
    private List<MotionData> motionDataKoubeiList = new ArrayList<>();
    private MotionData mMotionData;

    private SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(VideoUtil.getContext());


    private void initMotionData() {
        motionDataList.add(new MotionData("none", "无动效", "", ""));        // 0
        motionDataList.add(new MotionData("video_boom", "Boom", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_boomAndroid.zip",
                mPrefs.getString("video_boom", "")));                       // 1
        motionDataList.add(new MotionData("video_nihongshu", "霓虹鼠", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_nihongshuAndroid.zip",
                mPrefs.getString("video_nihongshu", "")));                  // 2
        motionDataList.add(new MotionData("video_starear", "星耳", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_starearAndroid.zip",
                mPrefs.getString("video_starear", "")));  // 3
        motionDataList.add(new MotionData("video_fengkuangdacall", "疯狂打call", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_fengkuangdacallAndroid.zip",
                mPrefs.getString("video_fengkuangdacall", "")));            // 4
        motionDataList.add(new MotionData("video_Qxingzuo", "Q星座", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_QxingzuoAndroid.zip",
                mPrefs.getString("video_Qxingzuo", "")));                   // 5
        motionDataList.add(new MotionData("video_caidai", "彩色丝带", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_caidaiAndroid.zip",
                mPrefs.getString("video_caidai", "")));                     // 6
        motionDataList.add(new MotionData("video_liuhaifadai", "刘海发带", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_liuhaifadaiAndroid.zip",
                mPrefs.getString("video_liuhaifadai", "")));                // 7
        motionDataList.add(new MotionData("video_lianpu", "变脸", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_lianpuAndroid.zip",
                mPrefs.getString("video_lianpu", "")));                    // 8
        motionDataList.add(new MotionData("video_purplecat", "紫色小猫", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_purplecatAndroid.zip",
                mPrefs.getString("video_purplecat", "")));                  // 9
        motionDataList.add(new MotionData("video_huaxianzi", "花仙子", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_huaxianziAndroid.zip",
                mPrefs.getString("video_huaxianzi", "")));                  // 10
        motionDataList.add(new MotionData("video_baby_agetest", "小公举", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_baby_agetestAndroid.zip",
                mPrefs.getString("video_baby_agetest", "")));               // 11
        // 单独把 抠背 的动效拿出来
        motionDataKoubeiList.add(new MotionData("none", "无", "", ""));        // 0
        motionDataKoubeiList.add(new MotionData("video_xiaofu", "校服", "http://dldir1.qq.com/hudongzhibo/AISpecial/Android/170/video_xiaofuAndroid.zip",
                mPrefs.getString("video_xiaofu", "")));
    }

    class MotionData {
        public MotionData(String motionId, String motionName, String motionUrl, String motionPath) {
            this.motionId = motionId;
            this.motionName = motionName;
            this.motionUrl = motionUrl;
            this.motionPath = motionPath;
        }

        public String motionId;
        public String motionName;
        public String motionUrl;
        public String motionPath;
    }

    public interface IOnBeautyParamsChangeListener {
        void onBeautyParamsChange(BeautyParams params, int key);
    }

    // 新界面
    TXHorizontalPickerView mFirstGradlePicker;
    ArrayAdapter<String> mFirstGradleAdapter;
    private final int mFilterBasicLevel = 5;

    private final int mBeautyBasicLevel = 5;
    private final int mWhiteBasicLevel = 3;
    private final int mRuddyBasicLevel = 2;
    private int mExposureLevel = -1;
    private final int mSharpenLevel = 3;

    TXHorizontalPickerView mSecondGradlePicker;
    ArrayAdapter<String> mSecondGradleAdapter;

    LinearLayout mSeekBarLL = null;
    TextView mSeekBarValue = null;
    CustomProgressDialog mCustomProgressDialog;

    private SeekBar mThirdGradleSeekBar;

    private Context mContext;

    private IOnBeautyParamsChangeListener mBeautyChangeListener;

    public BeautySettingPannel(Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.beauty_pannel, this);
        mContext = context;
        initView(view);
    }

    public void setBeautyParamsChangeListener(IOnBeautyParamsChangeListener listener) {
        mBeautyChangeListener = listener;
    }

    public void disableExposure() {
        mBeautyString = new String[]{
                "美颜",
                "美白",
                "红润",
                "大眼",
                "瘦脸",
                "V脸",
                "下巴",
                "短脸",
                "小鼻",
        };
        setFirstPickerType(null);
    }

    private void initView(View view) {
        mThirdGradleSeekBar = (SeekBar) view.findViewById(R.id.ThirdGradle_seekbar);
        mThirdGradleSeekBar.setOnSeekBarChangeListener(this);

        mFirstGradlePicker = (TXHorizontalPickerView) view.findViewById(R.id.FirstGradePicker);
        mSecondGradlePicker = (TXHorizontalPickerView) view.findViewById(R.id.secondGradePicker);

        mSeekBarLL = (LinearLayout) view.findViewById(R.id.layoutSeekBar);

        mSeekBarValue = (TextView) view.findViewById(R.id.TextSeekBarValue);

        setFirstPickerType(view);

        initMotionData();
    }

    private void setFirstPickerType(View view) {
        mFirstGradleArrayString.clear();
        for (int i = 0; i < mFirstGradleString.length; i++) {
            mFirstGradleArrayString.add(mFirstGradleString[i]);
        }
        mFirstGradleAdapter = new ArrayAdapter<String>(mContext, 0, mFirstGradleArrayString) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                String value = getItem(position);
                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                }
                TextView view = (TextView) convertView.findViewById(android.R.id.text1);
                view.setTag(position);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                view.setText(value);
                view.setPadding(15, 5, 30, 5);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int index = (int) view.getTag();
                        ViewGroup group = (ViewGroup) mFirstGradlePicker.getChildAt(0);
                        for (int i = 0; i < mFirstGradleAdapter.getCount(); i++) {
                            View v = group.getChildAt(i);
                            if (v instanceof TextView) {
                                if (i == index) {
                                    ((TextView) v).setTextColor(mContext.getResources().getColor(R.color.colorAccent));
                                } else {
                                    ((TextView) v).setTextColor(Color.WHITE);
                                }
                            }
                        }
                        setSecondPickerType(index);
                    }
                });
                return convertView;

            }
        };
        mFirstGradlePicker.setAdapter(mFirstGradleAdapter);
        mFirstGradlePicker.setClicked(ITEM_TYPE_BEAUTY);
    }

    private void setSecondPickerType(int type) {
        mSencodeGradleArrayString.clear();
        mSencodGradleType = type;

        String[] typeString = null;
        switch (type) {
            case ITEM_TYPE_BEAUTY_STYLE:
                typeString = mBeautyStyleString;
                break;
            case ITEM_TYPE_BEAUTY:
                typeString = mBeautyString;
                break;
            case ITEM_TYPE_FILTTER:
                typeString = mBeautyFilterTypeString;
                break;
            case ITEM_TYPE_MOTION:
                typeString = mMotionTypeString;
                break;
            case ITEM_TYPE_KOUBEI:
                typeString = mKoubeiString;
                break;
            case ITEM_TYPE_GREEN:
                typeString = mGreenString;
                break;
//            case ITEM_TYPE_CAPTURE:
//                typeString = mCaptureModeString;
//                break;
            default:
                break;
        }
        for (int i = 0; i < typeString.length; i++) {
            mSencodeGradleArrayString.add(typeString[i]);
        }
        mSecondGradleAdapter = new ArrayAdapter<String>(mContext, 0, mSencodeGradleArrayString) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                String value = getItem(position);
                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                }
                TextView view = (TextView) convertView.findViewById(android.R.id.text1);
                view.setTag(position);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                view.setText(value);
                view.setPadding(15, 5, 30, 5);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final int index = (int) view.getTag();
                        ViewGroup group = (ViewGroup) mSecondGradlePicker.getChildAt(0);
                        for (int i = 0; i < mSecondGradleAdapter.getCount(); i++) {
                            View v = group.getChildAt(i);
                            if (v instanceof TextView) {
                                if (i == index) {
                                    ((TextView) v).setTextColor(mContext.getResources().getColor(R.color.colorAccent));
                                } else {
                                    ((TextView) v).setTextColor(Color.WHITE);
                                }
                            }
                        }
                        if (mSencodGradleType != ITEM_TYPE_MOTION && mSencodGradleType != ITEM_TYPE_KOUBEI) {
                            setPickerEffect(mSencodGradleType, index);
                        } else {
                            if (mSencodGradleType == ITEM_TYPE_MOTION) {
                                mMotionData = motionDataList.get(position);
                            } else if (mSencodGradleType == ITEM_TYPE_KOUBEI) {
                                mMotionData = motionDataKoubeiList.get(position);
                            }

                            if (mMotionData.motionId.equals("none") || !TextUtils.isEmpty(mMotionData.motionPath)) {
                                setPickerEffect(mSencodGradleType, index);
                            } else if (TextUtils.isEmpty(mMotionData.motionPath)) {
                                VideoMaterialDownloadProgress videoMaterialDownloadProgress = new VideoMaterialDownloadProgress(mMotionTypeString[position], mMotionData.motionUrl);
                                videoMaterialDownloadProgress.start(new VideoMaterialDownloadProgress.Downloadlistener() {
                                    @Override
                                    public void onDownloadFail(final String errorMsg) {
                                        ((Activity) mContext).runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mCustomProgressDialog != null) {
                                                    mCustomProgressDialog.dismiss();
                                                }
                                                Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onDownloadProgress(final int progress) {
                                        ((Activity) mContext).runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                TXCLog.i(TAG, "onDownloadProgress, progress = " + progress);
                                                if (mCustomProgressDialog == null) {
                                                    mCustomProgressDialog = new CustomProgressDialog();
                                                    mCustomProgressDialog.createLoadingDialog(mContext, "");
                                                    mCustomProgressDialog.setCancelable(false); // 设置是否可以通过点击Back键取消
                                                    mCustomProgressDialog.setCanceledOnTouchOutside(false); // 设置在点击Dialog外是否取消Dialog进度条
                                                    mCustomProgressDialog.show();
                                                }
                                                mCustomProgressDialog.setMsg(progress + "%");
                                            }
                                        });
                                    }

                                    @Override
                                    public void onDownloadSuccess(String filePath) {
                                        mMotionData.motionPath = filePath;
                                        mPrefs.edit().putString(mMotionData.motionId, filePath).apply();
                                        ((Activity) mContext).runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mCustomProgressDialog != null) {
                                                    mCustomProgressDialog.dismiss();
                                                    mCustomProgressDialog = null;
                                                }
                                                setPickerEffect(mSencodGradleType, index);
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    }
                });
                return convertView;
            }
        };
        mSecondGradlePicker.setAdapter(mSecondGradleAdapter);
        mSecondGradlePicker.setClicked(mSzSecondGradleIndex[mSencodGradleType]);
    }

    private void setPickerEffect(int type, int index) {
        initSeekBarValue();
        mSzSecondGradleIndex[type] = index;
        mThirdGradleIndex = index;

        switch (type) {
            case ITEM_TYPE_BEAUTY_STYLE:
                mThirdGradleSeekBar.setVisibility(View.GONE);
                mSeekBarValue.setVisibility(View.GONE);
                setBeautyStyle(index);
                break;
            case ITEM_TYPE_BEAUTY:
                mThirdGradleSeekBar.setVisibility(View.VISIBLE);
                mSeekBarValue.setVisibility(View.VISIBLE);
                mThirdGradleSeekBar.setProgress(mSzSeekBarValue[type][index]);
                break;
            case ITEM_TYPE_FILTTER:
                setFilter(index);
                mThirdGradleSeekBar.setVisibility(View.VISIBLE);
                mSeekBarValue.setVisibility(View.VISIBLE);
                mThirdGradleSeekBar.setProgress(mSzSeekBarValue[type][index]);
                break;
            case ITEM_TYPE_MOTION:
                mThirdGradleSeekBar.setVisibility(View.GONE);
                mSeekBarValue.setVisibility(View.GONE);
                setDynamicEffect(type, index);
                break;
            case ITEM_TYPE_KOUBEI:
                mThirdGradleSeekBar.setVisibility(View.GONE);
                mSeekBarValue.setVisibility(View.GONE);
                setDynamicEffect(type, index);
                break;
            case ITEM_TYPE_GREEN:
                mThirdGradleSeekBar.setVisibility(View.GONE);
                mSeekBarValue.setVisibility(View.GONE);
                setGreenScreen(index);
                break;
//            case ITEM_TYPE_CAPTURE:
//                mThirdGradleSeekBar.setVisibility(View.GONE);
//                mSeekBarValue.setVisibility(View.GONE);
//                setCaptureMode(index);
//                break;
            default:
                break;
        }

    }

    public void initProgressValue(int type, int index, int progress) {
        switch (type) {
            case ITEM_TYPE_BEAUTY:
            case ITEM_TYPE_FILTTER:
                mSzSeekBarValue[type][index] = progress;
                setPickerEffect(type, index);
                // 复位
                setPickerEffect(type, 0);
                break;
        }
    }

    private Bitmap decodeResource(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.openRawResource(id, value);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inTargetDensity = value.density;
        return BitmapFactory.decodeResource(resources, id, opts);
    }

    //设置滤镜
    private void setFilter(int index) {
        Bitmap bmp = null;
        switch (index) {
            case 1:
                bmp = decodeResource(getResources(), R.drawable.filter_white);
                break;
            case 2:
                bmp = decodeResource(getResources(), R.drawable.filter_langman);
                break;
            case 3:
                bmp = decodeResource(getResources(), R.drawable.filter_qingxin);
                break;
            case 4:
                bmp = decodeResource(getResources(), R.drawable.filter_weimei);
                break;
            case 5:
                bmp = decodeResource(getResources(), R.drawable.filter_fennen);
                break;
            case 6:
                bmp = decodeResource(getResources(), R.drawable.filter_huaijiu);
                break;
            case 7:
                bmp = decodeResource(getResources(), R.drawable.filter_landiao);
                break;
            case 8:
                bmp = decodeResource(getResources(), R.drawable.filter_qingliang);
                break;
            case 9:
                bmp = decodeResource(getResources(), R.drawable.filter_rixi);
                break;
            default:
                bmp = null;
                break;
        }
        if (mBeautyChangeListener != null) {
            BeautyParams params = new BeautyParams();
            params.mFilterBmp = bmp;
            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_FILTER);
        }
    }

    //切换采集模式
    private void setCaptureMode(int index) {
        if (mBeautyChangeListener != null) {
            BeautyParams params = new BeautyParams();
            params.mCaptureMode = index;
            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_CAPTURE_MODE);
        }
    }

    //设置绿幕
    private void setGreenScreen(int index) {
        String file = "";
        switch (index) {
            case 1:
                file = "green_1.mp4";
                break;
            default:
                break;
        }
        if (mBeautyChangeListener != null) {
            BeautyParams params = new BeautyParams();
            params.mGreenFile = file;
            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_GREEN);
        }
    }

    //设置动效
    private void setDynamicEffect(int type, int index) {
//        String path = "";
        MotionData motionData;
        if (type == ITEM_TYPE_MOTION) {
            motionData = motionDataList.get(index);
        } else {
            motionData = motionDataKoubeiList.get(index);
        }
        String path = motionData.motionPath;
//        switch (index) {
//            case 1:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_3DFace_dogglasses2Android";
//                break;
//            case 2:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_baby_agetest";
//                break;
//            case 3:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_boom";
//                break;
//            case 4:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_caidai";
//                break;
//            case 5:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_fengkuangdacall";
//                break;
//            case 6:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_huaxianzi";
//                break;
//            case 7:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_hudiezhenzhu";
//                break;
//            case 8:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_liuhaifadai";
//                break;
//            case 9:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_multiGrid_zuanshimaotuAndroid";
//                break;
//            case 10:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_nihongshu";
//                break;
//            case 11:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_purplecat";
//                break;
//            case 12:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_Q520ma";
//                break;
//            case 13:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_Qxingzuo";
//                break;
//            case 14:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_rainbow";
//                break;
//            case 15:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_xiaofu";
//                break;
//            case 16:
//                path = "assets://camera/camera_video/CameraVideoAnimal/video_yueliangtoushi";
//                break;
//            default:
//                break;
//        }
        if (mBeautyChangeListener != null) {
            BeautyParams params = new BeautyParams();
            params.mMotionTmplPath = path;
            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_MOTION_TMPL);
        }
    }

    // 设置美颜类型
    private void setBeautyStyle(int index) {
        int style = index;
        if (index >= 3) {
            style = 3;
        }
        if (mBeautyChangeListener != null) {
            BeautyParams params = new BeautyParams();
            params.mBeautyStyle = style;
            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_BEAUTY_STYLE);
        }
    }

    public void setViewVisibility(int id, int visible) {
        LinearLayout contentLayout = (LinearLayout) getChildAt(0);
        int count = contentLayout.getChildCount();
        for (int i = 0; i < count; ++i) {
            View view = contentLayout.getChildAt(i);
            if (view.getId() == id) {
                view.setVisibility(visible);
                return;
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        initSeekBarValue();
        mSzSeekBarValue[mSencodGradleType][mThirdGradleIndex] = progress;   // 记录设置的值
        mSeekBarValue.setText(String.valueOf(progress));

        if (seekBar.getId() == R.id.ThirdGradle_seekbar) {
            if (mSencodGradleType == ITEM_TYPE_BEAUTY) {
                switch (mSencodeGradleArrayString.get(mThirdGradleIndex)) {
                    case "美颜":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mBeautyLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_BEAUTY);
                        }
                        break;
                    case "美白":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mWhiteLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_WHITE);
                        }
                        break;
                    case "红润":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mRuddyLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_RUDDY);
                        }
                        break;
//                    case "清晰":
//                        if (mBeautyChangeListener != null) {
//                            BeautyParams params = new BeautyParams();
//                            params.mSharpenLevel = progress;
//                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_SHARPEN);
//                        }
//                        break;
                    case "曝光":
                        if (mBeautyChangeListener != null && (0 != progress || mExposureLevel > 0)) {
                            mExposureLevel = progress;
                            BeautyParams params = new BeautyParams();
                            params.mExposure = ((float) progress - 10.0f) / 10.0f;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_EXPOSURE);
                        }
                        break;
                    case "大眼":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mBigEyeLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_BIG_EYE);
                        }
                        break;
                    case "瘦脸":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mFaceSlimLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_FACE_LIFT);
                        }
                        break;
                    case "V脸":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mFaceVLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_FACEV);
                        }
                        break;
                    case "下巴":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mChinSlimLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_CHINSLIME);
                        }
                        break;
                    case "短脸":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mFaceShortLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_FACESHORT);
                        }
                        break;
                    case "小鼻":
                        if (mBeautyChangeListener != null) {
                            BeautyParams params = new BeautyParams();
                            params.mNoseScaleLevel = progress;
                            mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_NOSESCALE);
                        }
                        break;
                    default:
                        break;
                }
            } else if (mSencodGradleType == ITEM_TYPE_FILTTER) {
                if (mBeautyChangeListener != null) {
                    BeautyParams params = new BeautyParams();
                    params.mFilterMixLevel = progress;
                    mBeautyChangeListener.onBeautyParamsChange(params, BEAUTYPARAM_FILTER_MIX_LEVEL);
                }
            }
        }

    }

    private void initSeekBarValue() {
        if (null == mSzSeekBarValue) {
            mSzSeekBarValue = new int[16][24];
            for (int i = 1; i < mSzSeekBarValue[ITEM_TYPE_FILTTER].length; i++) {
                mSzSeekBarValue[ITEM_TYPE_FILTTER][i] = mFilterBasicLevel;
            }
            for (int i = 0; i < mSzSeekBarValue[ITEM_TYPE_BEAUTY].length; i++) {
                if (i >= mSencodeGradleArrayString.size()) {
                    break;
                }
                switch (mSencodeGradleArrayString.get(i)) {
                    case "美颜":
                        mSzSeekBarValue[ITEM_TYPE_BEAUTY][i] = mBeautyBasicLevel;
                        break;
                    case "美白":
                        mSzSeekBarValue[ITEM_TYPE_BEAUTY][i] = mWhiteBasicLevel;
                        break;
                    case "红润":
                        mSzSeekBarValue[ITEM_TYPE_BEAUTY][i] = mRuddyBasicLevel;
                        break;
                    case "曝光":
                        mSzSeekBarValue[ITEM_TYPE_BEAUTY][i] = mExposureLevel;
                        break;
//                    case "清晰":
//                        mSzSeekBarValue[ITEM_TYPE_BEAUTY][i] = mSharpenLevel;
//                        break;

                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
