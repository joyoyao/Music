
package com.way.longplay;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

import com.way.lrc.LRC;
import com.way.lrc.LRCParser;
import com.way.lrc.LyricConstants;
import com.way.lrc.LyricListView;
import com.way.lrc.StringConstant;
import com.way.music.MusicCallback;
import com.way.music.MusicControl;
import com.way.music.SysMusicControl;

import java.io.File;

//import com.aliyunos.homeshell.widgetpage.IAliWidgetPage;

public class LongPlayViewEx extends FrameLayout implements LRC.PositionProvider/*, IAliWidgetPage*/ {
    public static final String TAG = "LongPlayViewEx";

    public static final int PLATFORM_SYSTEM = 1;
    public static final int PLATFORM_ALIYUN = 20;
    public static int PLATFORM_ID = /*PLATFORM_SYSTEM;//*/PLATFORM_ALIYUN;

    public static final int MUSIC_SYSTEM_EXISTS = 100;
    public static final int MUSIC_XIAMI_EXISTS = 110;
    public static int MUSIC_USES = MUSIC_XIAMI_EXISTS;
    private static final float STYLUS_MIN_ANGLE = 12.5f;
    private static final float STYLUS_MAX_ANGLE = 18.0f;
    private static final float GESTURE_DETECT_SHRESHOLD = 40;
    private static final String XIAMI_MUSIC_PKG = "fm.xiami.yunos";
    private static final String XIAMI_MUSIC_CLASS = "fm.xiami.bmamba.PlayService";
    private static final String SYSTEM_MUSIC_PKG = "com.andrew.apollo";
    private static final String SYSTEM_MUSIC_CLASS = "com.andrew.apollo.ui.activities.HomeActivity";
    private FrameLayout mTurntableLayout;
    private Context mContext;
    private int mPlayerState;
    private long mSongId;
    private Handler mHandler;
    private ValueAnimator mTurntableAnim;
    private int mTurntableAnimCount = 0;
    private float mTurntableRotationStart;
    private View mAlbumAnimView;
    private ImageView mAlbumView;
    private ImageView mAlbumShadeView;
    private View mAlbumFadeinView;
    private FrameLayout mStylusView;
    private float mStylusPivotX;
    private float mStylusPivotY;
    private ImageView mStylusLight;
    private Animator mStylusAnimator;
    private TextView mSongNameView;
    private TextView mSongArtistView;
    private LyricListView mLyricListView;
    private String mLrcPath;
    private int mAlbumHashcode;
    private MusicCallback mMusicCallback = new MusicCallback() {
        @Override
        public void onTitleChanged(String title) {
            Log.d(TAG, "onTitleChanged title=" + title);
            mSongNameView.setText(title);
        }

        @Override
        public void onArtistChanged(String artist) {
            Log.d(TAG, "onTrackChanged artist=" + artist);
            mSongArtistView.setText(artist);
            updateLyrics(getLyricPath());
        }

        @Override
        public void onStateChanged(int state) {
            Log.d(TAG, "onStateChanged state=" + state);
            doStateChanged(state);
            mPlayerState = state;
        }

        @Override
        public void onLyricChanged(String lrcPath, String staticTxt) {
            Log.d(TAG, "onLyricChanged lrcPath=" + lrcPath + ";staticTxt=" + staticTxt);
            updateLyrics(lrcPath);
        }

        @Override
        public void onAlbumCoverChanged(Bitmap albumCover) {
            if (albumCover == null) {
                mAlbumView.setImageResource(R.drawable.default_album);
                mAlbumHashcode = 0;
            } else if (mAlbumView != null && albumCover.hashCode() != mAlbumHashcode) {
                Bitmap scaled = scaleAlbum(albumCover);
                if (scaled != null) {
                    mAlbumView.setImageBitmap(scaled);
                } else {
                    mAlbumView.setImageResource(R.drawable.default_album);
                }
                mAlbumHashcode = albumCover.hashCode();
            }
        }
    };
    private boolean mWidgetPagePaused;
    private boolean bClickAndBind = false;
    private MusicControl mMusicControl;
    private AnimRunnable mAnimRunnable;
    private AnimRunnable mStylusViewAnimRunnable;
    private AnimRunnable mAlbumFadeinRunnable;
    private Rect mAlbumViewRect;
    private OnTouchListener mTurntableLayoutOnTouchListener = new OnTouchListener() {
        private float mLastX;
        private float mLastY;
        private float centerY = 0.0f;
        private boolean dragFlag = false;
        private boolean clickFlag = false;
        private long longPressTimeout = 0;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            //Log.d(TAG, "mTurntableLayoutOnTouchListener event="+event);
            float x = event.getRawX();
            float y = event.getRawY();
            float alpha = 1.0f;
            if (mAnimRunnable.isRunning()) {
                return true;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = x;
                    mLastY = y;
                    dragFlag = false;
                    clickFlag = false;
                    if (centerY == 0.0f) {
                        centerY = mContext.getResources().getDimension(R.dimen.turntable_height_anim) / 2;
                    }
                    if (longPressTimeout == 0) {
                        longPressTimeout = 500 + ViewConfiguration.getLongPressTimeout();
                    }
                    if (mAlbumViewRect == null) {
                        mAlbumViewRect = new Rect(mAlbumView.getLeft(), mAlbumView.getTop(), mAlbumView.getRight(), mAlbumView.getBottom());
                    }
                    mTurntableLayout.clearAnimation();
                    if (mLastY > centerY * 2 + 50) {
                        return false;
                    }
                    if (!mStylusView.isClickable()) {
                        Log.d(TAG, "mTurntableLayoutOnTouchListener ACTION_DOWN! mStylusView.isClickable()=" + false);
                        return false;
                    }
                    Log.d(TAG, "mAlbumViewRect=" + mAlbumViewRect + ";longPressTimeout=" + longPressTimeout);
                    if (mAlbumViewRect != null) {
                        int xx = (int) event.getX();
                        int yy = (int) event.getX();
                        if (mAlbumViewRect.contains(xx, yy)) {
                            Log.d(TAG, "mAlbumViewRect contains (" + xx + "," + yy + ");");
                            clickFlag = true;
                            mAlbumShadeView.setVisibility(View.VISIBLE);
                            postDelayed(onLongClick, longPressTimeout);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!dragFlag && event.getEventTime() - event.getDownTime() <= 300) {
                        if (Math.abs(mLastY - y) > 5) {
                            dragFlag = true;
                            clickFlag = false;
                            mAlbumShadeView.setVisibility(View.GONE);
                            removeCallbacks(onLongClick);
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    float translationY = 0;
                    if (mLastY != y) {
                        clickFlag = false;
                        mAlbumShadeView.setVisibility(View.GONE);
                        removeCallbacks(onLongClick);
                    }
                    if (mLastY > y) {//up
                        translationY = centerY - mLastY + y;
                        if (translationY < 0) {
                            break;
                        }
                        if (mLastY - y < 0) {
                            alpha = 1.0f;
                        } else {
                            alpha = (mLastY - y) / centerY;
                        }
                        mAlbumAnimView.setTranslationY(0);
                        mAlbumAnimView.setAlpha(1.0f);
                        mAlbumFadeinView.setTranslationY(translationY);
                        mAlbumFadeinView.setVisibility(View.VISIBLE);
                        mAlbumFadeinView.setAlpha(alpha);
                        break;
                    } else { //down
                        translationY = y - mLastY;
                        if (mLastY - y > centerY) {
                            alpha = 0;
                        } else {
                            alpha = ((centerY - Math.abs(mLastY - y)) / centerY);
                        }
                        mAlbumFadeinView.setVisibility(View.GONE);
                        mAlbumAnimView.setTranslationY(translationY);
                        mAlbumAnimView.setAlpha(alpha);
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mAlbumShadeView.setVisibility(View.GONE);
                    removeCallbacks(onLongClick);
                    if (dragFlag) {
                        dragFlag = false;
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else if (clickFlag) {
                        clickFlag = false;
                        if (event.getEventTime() - event.getDownTime() <= 500) {
                            boolean isClickable = mStylusView.isClickable();
                            Log.d(TAG, "mTurntableLayoutOnTouchListener onClick! mStylusView.isClickable()=" + isClickable);
                            if (isClickable) {
                                int state = mMusicControl.getState();
                                startStylusAnim(state == MusicControl.STATE_PLAYING ? MusicControl.STATE_PAUSED : MusicControl.STATE_PLAYING);
                            }
                            //mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                            return true;
                        }
                    }
                    clickFlag = false;
                    final float deltaY = y - mLastY;
                    startCdDragAnim(Math.abs(deltaY) < centerY / 2, deltaY > 0);
                    break;
            }
            return true;
        }

        private Runnable onLongClick = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "mTurntableLayoutOnTouchListener onLongClick run!");
                startMusicActivity();
            }
        };

    };
    private float mCenterY = 0.0f;
    private ObjectAnimator turnAnim = null;
    private float mStylusViewRawX, mStylusViewRawY;
    private OnTouchListener mStylusViewOnTouchListener = new OnTouchListener() {
        private float mLastX
                ,
                mDownX;
        private float mLastY
                ,
                mDownY;
        private float c2;
        private float c;
        private float cX
                ,
                cY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mStylusView.clearAnimation();
                    if (mStylusViewRawX == 0) {
                        int[] location = new int[2];
                        view.getLocationOnScreen(location);
                        mStylusViewRawX = mStylusPivotX + location[0];
                        mStylusViewRawY = mStylusPivotY + location[1];
                        cX = mStylusViewRawX;
                        cY = mStylusViewRawY + 200;
                        c2 = 200 * 200;
                        c = 200;
                    }
                    mLastX = mDownX = event.getRawX();
                    mLastY = mDownY = event.getRawY();
                    //c2 = (mDownX - mStylusViewRawX) * (mDownX - mStylusViewRawX) + (mDownY - mStylusViewRawY) * (mDownY - mStylusViewRawY);
                    //c = (float) Math.sqrt(c2);
                    Log.d(TAG, "mStylusViewOnTouchListener mDownX=" + mDownX + ";mDownY=" + mDownY + ";mStylusViewRawX=" + mStylusViewRawX + ";mStylusViewRawY=" + mStylusViewRawY);
                    Log.d(TAG, "mStylusViewOnTouchListener c2=" + c2 + ";c=" + c);
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (true) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        float x = event.getRawX();
                        float y = event.getRawY();
                        if (x > mStylusViewRawX) {
                            return false;
                        }
                        if (Math.abs(x - mLastX) <= 2) {
                            return false;
                        }
                        mLastX = x;
                        mLastY = y;
                        Log.d(TAG, "mStylusViewOnTouchListener x=" + x + ";y=" + y);
                        //float a2 = (x - mDownX) * (x - mDownX) + (y - mDownY) * (y - mDownY);
                        float a2 = (x - cX) * (x - cX) + (y - cY) * (y - cY);
                        float b2 = (x - mStylusViewRawX) * (x - mStylusViewRawX) + (y - mStylusViewRawY) * (y - mStylusViewRawY);
                        float b = (float) Math.sqrt(b2);
                        float alpha = (float) Math.acos((b2 + c2 - a2) / (2 * b * c));
                        float rotate = (float) Math.toDegrees(alpha);
                        Log.d(TAG, "mStylusViewOnTouchListener alpha=" + alpha + ";rotate=" + rotate);
                        if (rotate > STYLUS_MAX_ANGLE) {
                            rotate = STYLUS_MAX_ANGLE;
                        } else if (rotate < 0) {
                            rotate = 0;
                        }
                        view.setRotation(rotate);
                    }
                    return false;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (event.getEventTime() - event.getDownTime() <= 500) {
                        Log.d(TAG, "mStylusViewOnTouchListener ACTION_UP touch time < 500ms");
                        float x = event.getRawX();
                        float y = event.getRawY();
                        float pos2 = (x - mDownX) * (x - mDownX) + (y - mDownY) * (y - mDownY);
                        Log.d(TAG, "ACTION_UP pos2=" + pos2);
                        if (pos2 < 25) {
                            return false;
                        }
                    }
                    float rotate = view.getRotation();
                    int state = mMusicControl.getState();
                    if (rotate >= STYLUS_MAX_ANGLE / 2) {
                        if (state != MusicControl.STATE_PLAYING) {
                            startStylusAnim(MusicControl.STATE_PLAYING);
                            //mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
                            //startTurntableAnim(true);
                        } else {
                            view.setRotation(STYLUS_MAX_ANGLE);
                        }
                    } else {
                        if (state == MusicControl.STATE_PLAYING) {
                            startStylusAnim(MusicControl.STATE_PAUSED);
                            //mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
                            //startTurntableAnim(false);
                        } else {
                            view.setRotation(0);
                        }
                    }
                    return true;
            }
            return false;
        }
    };
    /*private Animation createTurntableAnim() {
        Animation rotateAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(30000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        mTurntableLayout.setAnimation(rotateAnimation);


    	return rotateAnimation;
    }*/
    private ObjectAnimator mAnimWheelPlayBack = null;
    private long currentWheelPlayTime = 0L;

    public LongPlayViewEx(Context context) {
        this(context, null, 0);
    }

    public LongPlayViewEx(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LongPlayViewEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.d(TAG, TAG + " start!");
        mContext = context;

        mAnimRunnable = new AnimRunnable();

        mStylusViewAnimRunnable = new AnimRunnable();

        mAlbumFadeinRunnable = new AnimRunnable();

        checkMusicUses(context);
        MUSIC_USES = MUSIC_SYSTEM_EXISTS;

        mMusicControl = new SysMusicControl(context, mMusicCallback);
        mMusicControl.open();
        initInflateView(context);
        Log.d(TAG, TAG + " end!");
    }

    public static String getFileName(String trackName, String artistName) {
        return trackName + StringConstant.LINK_FACTOR + artistName
                + LyricConstants.EXTENSION;
    }

    private static float getStylusAngle(long curTime, long duration) {
        float targetAngle = STYLUS_MAX_ANGLE;
        if (duration != 0) {
            targetAngle = STYLUS_MAX_ANGLE - ((float) curTime / (float) duration)
                    * (STYLUS_MAX_ANGLE - STYLUS_MIN_ANGLE);
        }
        if (targetAngle > STYLUS_MAX_ANGLE) {
            targetAngle = STYLUS_MAX_ANGLE;
        }
        if (targetAngle < STYLUS_MIN_ANGLE) {
            targetAngle = STYLUS_MIN_ANGLE;
        }
        return targetAngle;

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, " onFinishInflate !");
        //init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, " onAttachedToWindow !");
        mMusicControl.register();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, " onDetachedFromWindow !");
        mMusicControl.unregister();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        Log.d(TAG, " onWindowFocusChanged ! hasWindowFocus=" + hasWindowFocus);
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
                                  Rect previouslyFocusedRect) {
        Log.d(TAG, " onFocusChanged ! gainFocus=" + gainFocus);
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public long getPosition() {
        return mMusicControl.getCurrTime();
    }

    @Override
    public long getDuration() {
        return mMusicControl.getDuration();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
        }
    }

    private void startCdDragAnim(final boolean isInvalid, final boolean isAnimCd) {
        Log.d(TAG, "startCdDragAnim(" + isInvalid + "," + isAnimCd + ");");
        View view = isAnimCd ? mAlbumAnimView : mAlbumFadeinView;
        float fy = view.getTranslationY();
        float fromY, toY;
        float fromA, toA;
        if (mCenterY == 0) {
            mCenterY = mContext.getResources().getDimension(R.dimen.turntable_height_anim) / 2;
        }
        if (isAnimCd) {
            if (!isInvalid) {
                fromY = view.getTranslationY();
                toY = mCenterY;
                fromA = view.getAlpha();
                toA = 0.0f;
            } else {
                fromY = view.getTranslationY();
                toY = 0;
                fromA = view.getAlpha();
                toA = 1.0f;
            }
        } else {
            if (isInvalid) {
                fromY = view.getTranslationY();
                toY = mCenterY;
                fromA = view.getAlpha();
                toA = 0.0f;
            } else {
                fromY = view.getTranslationY();
                toY = 0;
                fromA = view.getAlpha();
                toA = 1.0f;
            }
        }
        PropertyValuesHolder localPropertyValuesHolder1;
        PropertyValuesHolder localPropertyValuesHolder2;
        localPropertyValuesHolder1 = PropertyValuesHolder.ofFloat("translationY", new float[]{fromY, toY});
        localPropertyValuesHolder2 = PropertyValuesHolder.ofFloat("alpha", new float[]{fromA, toA});
        turnAnim = ObjectAnimator.ofPropertyValuesHolder(view, new PropertyValuesHolder[]{localPropertyValuesHolder1, localPropertyValuesHolder2});
        turnAnim.setDuration(400L);
        turnAnim.setInterpolator(new AccelerateInterpolator());
        turnAnim.setRepeatCount(0);
        turnAnim.start();
        turnAnim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isInvalid) {
                    mAlbumView.setImageResource(R.drawable.default_album);
                    mAlbumAnimView.setRotation(0);
                    mAlbumAnimView.setAlpha(1.0f);
                    mAlbumAnimView.setTranslationY(0);
                    if (mTurntableAnim != null) {
                        mTurntableAnim.cancel();
                        mTurntableAnimCount = 0;
                    }
                    stopTapeAnimation(true);
                    if (isAnimCd) {
                        mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
                    } else {
                        mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                    }
                    updateLyrics(getLyricPath());
                }
                mAlbumFadeinView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        });
    }

    private String getLyricPath() {
        String fileName = getFileName(mMusicControl.getTrackName(),
                mMusicControl.getArtistName());
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.andrew.apollo/cache/lyric/";
        File file = new File(path, fileName);
        Log.i("liweiping",
                "file = " + file.getAbsolutePath() + ",  file.exists() = " + file.exists());
        if (file.exists()) {
            return file.getAbsolutePath();
        } else {
            return null;
        }
    }

    private void initInflateView(Context context) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(context.getResources().getLayout(R.layout.music_play_ex), this, true);
        initView();
    }

    private void initView() {
        Resources resources = getResources();

        mTurntableLayout = (FrameLayout) findViewById(R.id.turntable);
        mAlbumAnimView = findViewById(R.id.album_anim);
        mAlbumView = (ImageView) findViewById(R.id.album);
        mAlbumShadeView = (ImageView) findViewById(R.id.album_shade);
        mAlbumFadeinView = findViewById(R.id.album_fadein);
        mStylusView = (FrameLayout) findViewById(R.id.stylus);
        mStylusLight = (ImageView) findViewById(R.id.styluslight);
        mSongNameView = (TextView) findViewById(R.id.songname);
        mSongArtistView = (TextView) findViewById(R.id.artist);
        mLyricListView = (LyricListView) findViewById(R.id.lrc_view);

        mLyricListView.setPositionProvider(this);

        mStylusPivotX = resources.getDimension(R.dimen.stylus_pivot_x);
        mStylusPivotY = resources.getDimension(R.dimen.stylus_pivot_y);
        mStylusView.setPivotX(mStylusPivotX);
        mStylusView.setPivotY(mStylusPivotY);

        mStylusView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "mStylusView onClick");
                int state = mMusicControl.getState();
                startStylusAnim(state == MusicControl.STATE_PLAYING ? MusicControl.STATE_PAUSED : MusicControl.STATE_PLAYING);
                //mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        });

        View view = findViewById(R.id.turntable_layout);
        //mTurntableLayout
        view
                .setOnTouchListener(mTurntableLayoutOnTouchListener);
        mStylusView.setOnTouchListener(mStylusViewOnTouchListener);

        initTapeAnimations();
    }

    private void startStylusAnim(final int state) {
        Log.d(TAG, "startStylusAnim state=" + state/*, new Throwable()*/);
        float curAngle = mStylusView.getRotation();
        final float targetAngle;
        if (state == MusicControl.STATE_COMPLETED) {
            targetAngle = STYLUS_MAX_ANGLE;
        } else if (state == MusicControl.STATE_PLAYING) {
            targetAngle = STYLUS_MAX_ANGLE;
            /*long curTime = mMusicControl.getCurrTime();
            long duration = mMusicControl.getDuration();
            if (curTime <= 0 || duration <= 0) {
                targetAngle = STYLUS_MAX_ANGLE;
            } else {
                targetAngle = getStylusAngle(curTime, duration);
            }*/
        } else {
            targetAngle = 0;
        }
        if (curAngle == targetAngle) {
            Log.d(TAG, "startStylusAnim curAngle == targetAngle, donot return");
            //return;
        }
        if (true) {
            mStylusViewAnimRunnable.abortRunning(false);
            mStylusViewAnimRunnable.init((int) (100 * curAngle), (int) (100 * targetAngle), 300, new DecelerateInterpolator(), new AnimRunnableCallback() {

                @Override
                public void onAnimStart() {
                }

                @Override
                public void onAnimRun(int from, int to, int currValue) {
                    mStylusView.setRotation(((float) currValue) / 100);
                }

                @Override
                public void onAnimEnd() {
                    Log.d(TAG, "mStylusViewAnimRunnable onAnimEnd");
                    mStylusView.setClickable(true);
                    mStylusView.clearAnimation();
                    mStylusView.setRotation(targetAngle);
                    if (mStylusView.getRotation() != 0) {
                    } else {
                        mStylusLight.setVisibility(View.GONE);
                    }
                    if (state == MusicControl.STATE_PLAYING) {
                        mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
                    } else {
                        mMusicControl.onKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
                    }
                    updateLyrics(getLyricPath());
                }
            });
            return;
        }
        if (true)
            if (PLATFORM_ID == PLATFORM_ALIYUN) {
                if (mStylusView.getAnimation() != null) {
                    Log.d(TAG, "startStylusAnim mStylusView.getAnimation()=" + mStylusView.getAnimation());
                    return;
                }
                final float fCurAngle = curAngle;
                final float fTargetAngle = targetAngle;
                curAngle -= mStylusView.getRotation();
                //targetAngle -= mStylusView.getRotation();
                Log.d(TAG, "mStylusView curAngle=" + curAngle + ";targetAngle=" + targetAngle);
                Animation anim = new RotateAnimation(curAngle, targetAngle, mStylusPivotX, mStylusPivotY);
                anim.setDuration(300);
                anim.setFillEnabled(true);
                anim.setFillAfter(true);
                anim.setFillBefore(true);
                anim.setInterpolator(new DecelerateInterpolator());
                mStylusView.setClickable(false);
                anim.setAnimationListener(new AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                        Log.d(TAG, "mStylusView onAnimationStart PLATFORM_ALIYUN");
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Log.d(TAG, "mStylusView onAnimationEnd PLATFORM_ALIYUN");
                        mStylusView.setClickable(true);
                        mStylusView.clearAnimation();
                        mStylusView.setRotation(fTargetAngle);
                        if (mStylusView.getRotation() != 0) {
                        } else {
                            mStylusLight.setVisibility(View.GONE);
                        }
                    }
                });
                mStylusView.setAnimation(anim);
                anim.start();
                return;
            }
        ObjectAnimator anim = null;
        anim = ObjectAnimator.ofFloat(mStylusView, View.ROTATION, curAngle, targetAngle);
        anim.setDuration(1000);
        anim.setInterpolator(new DecelerateInterpolator());
        mStylusView.setClickable(false);
        anim.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "mStylusView onAnimationStart");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "mStylusView onAnimationEnd");
                mStylusView.setClickable(true);
                if (mStylusView.getRotation() != 0) {
                } else {
                    mStylusLight.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                Log.d(TAG, "mStylusView onAnimationCancel");
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                Log.d(TAG, "mStylusView onAnimationRepeat");
            }

        });
        anim.start();
    }

    private void initTapeAnimations() {
        if (mAnimWheelPlayBack == null) {
            mAnimWheelPlayBack = ObjectAnimator.ofFloat(mAlbumAnimView,
                    "rotation", new float[]{0.0F, 360.0F});
            mAnimWheelPlayBack.setDuration(30000L);
            mAnimWheelPlayBack.setInterpolator(new LinearInterpolator());
            mAnimWheelPlayBack.setRepeatCount(-1);
            mAnimWheelPlayBack.setRepeatMode(1);
        }
    }

    public void startTapeAnimation() {
        if ((mAnimWheelPlayBack != null) && (!mAnimWheelPlayBack.isRunning())) {
            mAnimWheelPlayBack.start();
            mAnimWheelPlayBack.setCurrentPlayTime(currentWheelPlayTime);
        }
    }

    public void stopTapeAnimation(boolean reset) {
        if ((mAnimWheelPlayBack != null) && (mAnimWheelPlayBack.isRunning())) {
            if (reset) {
                currentWheelPlayTime = 0;
            } else {
                currentWheelPlayTime = mAnimWheelPlayBack.getCurrentPlayTime();
            }
            Log.d(TAG, "stopTapeAnimation currentWheelPlayTime=" + currentWheelPlayTime, new Throwable());
            mAnimWheelPlayBack.cancel();
        }
    }

    private ValueAnimator createTurntalbeAnim() {
        final View v = mTurntableLayout;
        final float centerX = mContext.getResources().getDimension(R.dimen.turntable_width_anim) / 2;
        final float centerY = mContext.getResources().getDimension(R.dimen.turntable_height_anim) / 2;
        ValueAnimator animator = ValueAnimator.ofInt(0, 360);
        animator.setDuration(30000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        v.setPivotX(centerX);
        v.setPivotY(centerY);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            private static final int MAX_COUNT = 900;
            private static final float STEP_LEN = ((float) 360 / (float) MAX_COUNT);

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (true || PLATFORM_ID == PLATFORM_ALIYUN) {
                    if (mTurntableAnimCount % MAX_COUNT == 0) {
                        mTurntableAnimCount = 0;
                    }
                    float value = STEP_LEN * mTurntableAnimCount;
                    mTurntableLayout.setRotation(value);
                } else {
                    int value = (Integer) animation.getAnimatedValue();
                    mTurntableLayout.setRotation(mTurntableRotationStart + value);
                }
                mTurntableAnimCount++;
                //Log.d(TAG, "mTurntableLayout onAnimationUpdate mTurntableAnimCount="+mTurntableAnimCount+";getAnimatedValue="+value);

                int state = mMusicControl.getState();
                if (state == MusicControl.STATE_PLAYING) {
                    long curTime = mMusicControl.getCurrTime();
                    long duration = mMusicControl.getDuration();
                    float angle = getStylusAngle(curTime, duration);
                    //mStylusView.setRotation(angle);
                }
            }

        });
        return animator;
    }

    private Bitmap scaleAlbum(Bitmap raw) {
        if (raw == null) {
            return null;
        }
        int rawW = raw.getWidth();
        int rawH = raw.getHeight();
        int w = (int) getResources().getDimension(R.dimen.album_width);
        int h = (int) getResources().getDimension(R.dimen.album_height);

        Bitmap dst = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas cvs = new Canvas();
        cvs.setBitmap(dst);

        BitmapDrawable mask = (BitmapDrawable) getResources().getDrawable(R.drawable.album_mask);

        float ratio = (float) w / (float) h;
        float albumRatio = (float) rawW / (float) rawH;
        int sx = 0;
        int sy = 0;
        int sw = 0;
        int sh = 0;
        if (ratio > albumRatio) {
            sw = rawW;
            sh = (int) (rawW / ratio);
            sx = 0;
            sy = (rawH - sh) / 2;
        } else {
            sh = rawH;
            sw = (int) (rawH * ratio);
            sx = (rawW - sw) / 2;
            sy = 0;
        }
        Rect srcR = new Rect(sx, sy, sx + sw, sy + sh);
        Rect dstR = new Rect(0, 0, w, h);
        cvs.drawBitmap(raw, srcR, dstR, null);

        // cvs.drawBitmap(raw, new Rect(0), 0, null);
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        cvs.drawBitmap(mask.getBitmap(), 0, 0, p);

        raw.recycle();
        return dst;
    }

    private void doStateChanged(int state) {
        if (state != mPlayerState) {
            if (state == MusicControl.STATE_COMPLETED) {
                if (mTurntableAnim != null) {
                    mTurntableAnim.cancel();
                    mTurntableAnimCount = 0;
                }
                stopTapeAnimation(true);
                mLyricListView.stop();
                mStylusLight.setVisibility(View.GONE);
            } else if (state != MusicControl.STATE_PLAYING) {
                if (mTurntableAnim != null) {
                    mTurntableAnim.cancel();
                }
                stopTapeAnimation(false);
                if (mStylusView.getRotation() != 0) {
                    startStylusAnim(state);
                }
                mLyricListView.pause();
                mStylusLight.setVisibility(View.GONE);
            } else {
                mLyricListView.resume();
                startStylusAnim(state);
            }
            mPlayerState = state;
        }
        if (state == MusicControl.STATE_PLAYING && !mWidgetPagePaused) {
            doResume();
        }
    }

    private void doResume() {
        if (mTurntableAnim == null) {
            //mTurntableAnim = createTurntableAnim();
            //mTurntableAnim = createTurntalbeAnim();
        }
        if (mTurntableAnim != null && !mTurntableAnim.isRunning()) {
            mTurntableRotationStart = mTurntableLayout.getRotation();
            mTurntableAnim.start();
        }
        startTapeAnimation();
        if (mStylusView.getRotation() == 0) {
            startStylusAnim(MusicControl.STATE_PLAYING);
        }
        if (mStylusLight.getVisibility() != View.VISIBLE) {
            mStylusLight.setVisibility(View.VISIBLE);
        }

    }

    //@Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        mWidgetPagePaused = true;
        int state = mMusicControl.getState();
        if (state == MusicControl.STATE_PLAYING) {
            if (mTurntableAnim != null) {
                mTurntableAnim.cancel();
            }
            stopTapeAnimation(false);
            mLyricListView.pause();
        }
    }

    //@Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        mWidgetPagePaused = false;
        int state = mMusicControl.getState();
        if (state == MusicControl.STATE_PLAYING) {
            doResume();
            mMusicControl.requestAlbumCover();
        } else {
            if (mStylusView.getRotation() != 0) {
                startStylusAnim(state);
            }
            mStylusLight.setVisibility(View.INVISIBLE);
        }
    }

    //@Override
    public void onPageBeginMoving() {
        Log.d(TAG, "onPageBeginMoving()");
    }

    //@Override
    public void enterWidgetPage(int page) {
        Log.d(TAG, "enterWidgetPage");
    }

    //@Override
    public void leaveWidgetPage(int page) {
        Log.d(TAG, "leaveWidgetPage");
    }

    public void updateLyrics(String path) {
        mLyricListView.setLrc(null, null);// reset
        if (path != null) {
            setLrc(path);
        } else {
        }
        updateState();
    }

    private void updateState() {
        boolean haveLyc = mLyricListView.isHaveLyc();
        if (haveLyc) {
            mLyricListView.start();
            mLyricListView.setVisibility(View.VISIBLE);
        } else {
            mLyricListView.stop();
            mLyricListView.setVisibility(View.GONE);
        }
    }

    private boolean setLrc(String lrcPath) {
        boolean ready = false;
        LRC lrc = null;
        try {

            lrc = LRCParser.parseFromFile(new File(lrcPath));
            ready = true;
        } catch (Exception e) {
            Log.e(TAG, "SET LYRIC ERROR:" + e.getMessage());
        }
        mLyricListView.setLrc(lrcPath, lrc);
        return ready;
    }

    private void checkMusicUses(Context context) {
        boolean exists = false;
        exists = serviceExists(context, new ComponentName(XIAMI_MUSIC_PKG, XIAMI_MUSIC_CLASS));
        if (exists) {
            MUSIC_USES = MUSIC_XIAMI_EXISTS;
        } else {
            exists = serviceExists(context, new ComponentName(SYSTEM_MUSIC_PKG, SYSTEM_MUSIC_CLASS));
            if (exists) {
                MUSIC_USES = MUSIC_SYSTEM_EXISTS;
            } else {
                Log.e(TAG, "Music services not found!");
            }
        }
    }

    private boolean serviceExists(Context context, ComponentName name) {
        try {
            return name != null && context.getPackageManager().getServiceInfo(name, 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void startMusicActivity() {
        //Intent intent = mMusicControl.getMusicActivityIntent();
        Intent intent = new Intent("android.intent.action.MUSIC_PLAYER§");
        if (intent != null) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                // | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                );
                mContext.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "startMusicActivity Exception! intent e=" + e);
            }
        }
    }

    public interface AnimRunnableCallback {
        void onAnimStart();

        void onAnimRun(int from, int to, int currValue);

        void onAnimEnd();
    }

    public class AnimRunnable implements Runnable {

        private Scroller mScroller;
        private AnimRunnableCallback mCallback;
        private boolean mIsRunning = false;
        private int mFrom;
        private int mTo;

        public void init(int from, int to, int duration, Interpolator interpolator, AnimRunnableCallback callback) {
            mFrom = from;
            mTo = to;
            mCallback = callback;
            if (mScroller == null) {
                mScroller = new Scroller(mContext, interpolator == null ? new LinearInterpolator() : interpolator);
            }
            mScroller.startScroll(0, from, 0, to - from, duration);
            mScroller.computeScrollOffset();
            if (mCallback != null) {
                mCallback.onAnimStart();
            }
            post(this);
            mIsRunning = true;
        }

        @Override
        public void run() {
            Log.d(TAG, "AnimRunnable run! mScroller.getCurrY()=" + mScroller.getCurrY() + ";mIsRunning=" + mIsRunning);
            if (mIsRunning) {
                if (mCallback != null) {
                    mCallback.onAnimRun(mFrom, mTo, mScroller.getCurrY());
                }
                if (mScroller.computeScrollOffset()) {
                    //scrollTo(mScroller.getCurrX(), 0);
                    post(this);
                } else {
                    if (mCallback != null) {
                        mCallback.onAnimEnd();
                    }
                    mIsRunning = false;
                }
            }
        }

        public boolean isRunning() {
            return mIsRunning;
        }

        public void abortRunning(boolean toEnd) {
            Log.d(TAG, "AnimRunnable abortRunning! mIsRunning=" + mIsRunning + ";toEnd=" + toEnd);
            if (mIsRunning) {
                LongPlayViewEx.this.removeCallbacks(this);
                mScroller.abortAnimation();
                if (toEnd && mCallback != null) {
                    mCallback.onAnimEnd();
                }
                mIsRunning = false;
            }
        }
    }

    public class TurntableInterpolator implements Interpolator {

        public float getInterpolation(float paramFloat) {
            return 0.3333333F + (float) (2.0D * Math.cos(Math.PI * (4.0F + 2.0F * paramFloat)
                    / 3.0D) / 3.0D);
        }
    }
}
