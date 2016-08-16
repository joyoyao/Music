package com.way.longplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Hotseat extends LinearLayout {
    public static final String TAG = LongPlayView.TAG + ".Hotseat";
    private static final int VIEW_IDS[][] = {
            {R.id.bwc_btn_app1, R.id.bwc_text_app1, R.id.bwc_layout_app1},
            {R.id.bwc_btn_app2, R.id.bwc_text_app2, R.id.bwc_layout_app2},
            {R.id.bwc_btn_app3, R.id.bwc_text_app3, R.id.bwc_layout_app3},
            {R.id.bwc_btn_app4, R.id.bwc_text_app4, R.id.bwc_layout_app4},
            {R.id.bwc_btn_app5, R.id.bwc_text_app5, R.id.bwc_layout_app5},
            {R.id.bwc_btn_app6, R.id.bwc_text_app6, R.id.bwc_layout_app6},
    };
    private static final String APP_CLASS_NAME[][] = {
            {"com.xiami.walkman", "com.xiami.walkman.activities.MainActivity", "fm.xiami.yunos", "fm.xiami.bmamba.activity.StartActivity"},
            {"com.aliyun.video", "com.aliyun.video.VideoCenterActivity"},
            {"com.aliyun.soundrecorder", "com.aliyun.soundrecorder.AliSoundRecorder"},
            {"com.caf.fmradio", "com.caf.fmradio.FMRadio"},
            {"", ""},
            {"", ""},
    };
    private Context mContext;

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
        setVisibility(View.INVISIBLE);
    }

    public void init() {
        View view = null;
        for (int i = 0; i < VIEW_IDS.length; i++) {
            view = findViewById(VIEW_IDS[i][0]);
            if (view != null) {
                Intent intent = getValidIntent(i);
                view.setClickable(true);
                view.setTag(intent);
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent1 = (Intent) view.getTag();
                        try {
                            if (intent1 != null) {
                                mContext.startActivity(intent1);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Can't start activity:" + intent1);
                        }
                    }
                });
                if (intent == null) {
                    View view1 = findViewById(VIEW_IDS[i][2]);
                    if (view1 != null) {
                        view1.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private Intent getValidIntent(int pos) {
        if (pos < APP_CLASS_NAME.length) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                for (int i = 0; i < APP_CLASS_NAME[pos].length && i + 1 < APP_CLASS_NAME[pos].length; i += 2) {
                    ComponentName componentName = new ComponentName(APP_CLASS_NAME[pos][i], APP_CLASS_NAME[pos][i + 1]);
                    intent.setComponent(componentName);
                    if (mContext.getPackageManager().resolveActivity(intent, 0) == null) {
                        Log.e(TAG, "getValidIntent curr i=" + i + "; APP_CLASS_NAME[+" + pos + "]:" + APP_CLASS_NAME[pos]);
                        continue;
                    } else {
                        return intent;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "getValidIntent", e);
            }
        }
        return null;
    }

    public ComponentName getHotseat(int pos) {
        ComponentName componentName = null;
        Intent intent = getValidIntent(pos);
        if (intent != null) {
            componentName = intent.getComponent();
        }
        Log.d(TAG, "getHotseat pos=" + pos + "; return : " + componentName);
        return componentName;
    }

    public void setHotseat(int pos, Drawable drawable, CharSequence appName) {
        Log.d(TAG, "setHotseat pos=" + pos + ";drawable=" + drawable);
        if (drawable != null) {
            ImageView bwcBtn = null;
            TextView bwcText = null;
            if (pos < VIEW_IDS.length) {
                bwcBtn = (ImageView) findViewById(VIEW_IDS[pos][0]);
                bwcText = (TextView) findViewById(VIEW_IDS[pos][1]);
                if (bwcBtn != null) {
                    bwcBtn.setImageDrawable(drawable);
                }
                if (bwcText != null) {
                    bwcText.setText(appName);
                }
            } else {
                Log.e(TAG, "setHotseat VIEW_IDS.length=" + VIEW_IDS.length + ";VIEW_IDS=" + VIEW_IDS);
            }
        }
    }

}
