package com.way.music.lrc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.List;

/**
 * LyricListView
 *
 * @author lisc
 */
public class LyricListView extends ListView implements OnItemLongClickListener {

    public static final int SHOW_TYPE_DEFAULT = 0;
    private int showType = SHOW_TYPE_DEFAULT;
    public static final int SHOW_TYPE_SHOW_AWALAYS = 1;
    public static final int SHOW_TYPE_HIDE_AWALAYS = 2;
    private static final String TAG = "LyricListView";
    private int mNormalColor;
    private int mHighLightColor;
    private String mPath;
    private LRC mLRC;
    private int mLyricListItem;
    private int mItemHeight;
    private boolean mIsScrolling;
    private int mDelay;
    private int mOffset;
    private int mCurLrcPosition;
    private int mLyricCount;
    private int mFieldId;
    private boolean mInitialized;
    private LyricsAdapter mAdapter;
    private boolean mIsLyricModified;
    private LRC.PositionProvider mPosProvider;
    private boolean mScreenOn = true;
    private Handler mLyricHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case LyricConstants.LYRIC_SCROLL:
                    int allItems = getLastVisiblePosition()
                            - getFirstVisiblePosition();
                    if (mIsScrolling && mScreenOn) {
                        int off = computeOffset();
                        Log.i("way", "getFirstVisiblePosition() = "
                                + getFirstVisiblePosition()
                                + ", getLastVisiblePosition() = "
                                + getLastVisiblePosition() + ", mCurLrcPosition = "
                                + mCurLrcPosition);
                        if (off != 0) {
                            if (mCurLrcPosition < getFirstVisiblePosition())
                                smoothScrollToPosition(Math.max(
                                        (mCurLrcPosition - allItems / 2), 0));
                            else
                                smoothScrollToPosition(Math.min(
                                        (mCurLrcPosition + allItems / 2),
                                        (mLyricCount - 1)));
                            // offsetChildrenTopAndBottom(off);
                            invalidateViews();
                        }
                        sendEmptyMessageDelayed(LyricConstants.LYRIC_SCROLL, mDelay);
                    }
                    break;

                case LyricConstants.LYRIC_AJUST:
                    int off = computeOffset();
                    // offsetChildrenTopAndBottom(off);
                    invalidateViews();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * LyricListView
     *
     * @param context
     */
    public LyricListView(Context context) {
        this(context, null);
    }

    /**
     * LyricListView
     *
     * @param context
     * @param attrs
     */
    public LyricListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * LyricListView
     *
     * @param context
     * @param attrs
     * @param defStyle
     */
    public LyricListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mOffset = LyricConstants.DEFAULT_OFFSET;
        mDelay = LyricConstants.DEFAULT_DELAY;
        mIsScrolling = false;
        mCurLrcPosition = -1;
        mLyricCount = 0;
        mFieldId = R.id.content;
        mInitialized = false;
        mIsLyricModified = false;

        mNormalColor = getResources().getColor(R.color.action_bar_title);
//		mHighLightColor = getResources()
//				.getColor(R.color.line_one);
        mHighLightColor = PreferenceUtils.getInstance(getContext())
                .getDefaultThemeColor(getContext());
        // setOnItemLongClickListener(this);
        this.setSelector(android.R.color.transparent);

    }

    // Add start, name:kuangcheng, date:20130110,bugID:114791
    // optimize performance, because the loop to update the view.
    @Override
    protected void onDetachedFromWindow() {
        removeLycScroll();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != View.VISIBLE) {
            removeLycScroll();
        }
    }

    private void removeLycScroll() {
        Log.v(TAG, "removeLycScroll");
        mIsScrolling = false;
        mLyricHandler.removeMessages(LyricConstants.LYRIC_SCROLL);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!mInitialized && mLyricCount > 0) {
            mInitialized = true;
            // NullPointerException========
            if (getChildAt(1) != null) {
                mItemHeight = getChildAt(1).getHeight();
            }
            // NullPointerException========
        }
    }

    private boolean checkRequirments() {
        if (mLRC == null) {
            // throw new
            // IllegalStateException("LRC object is null, you should invoke setLrc() first.");
            Log.i(TAG,
                    "LRC object is null, you should invoke setLrc() first.");
            return false;
        }
        if (mPosProvider == null) {
            // throw new
            // IllegalStateException("PositionProvider object is null, you should invoke setPositionProvider() first.");
            Log.i(TAG,
                    "PositionProvider object is null, you should invoke setPositionProvider() first.");
            return false;
        }
        return true;
    }

    /**
     * reset
     */
    public void reset() {
        mCurLrcPosition = -1;
        mInitialized = false;
    }

    /**
     * start
     */
    public void start() {
        resume();
    }

    /**
     * resume
     */
    public void resume() {
        if (checkRequirments()) {
            Log.d(TAG, "isScrolling=" + mIsScrolling);
            if (!mIsScrolling) {
                setVisibility(View.VISIBLE);
                mIsScrolling = true;
                reset();
                mLyricHandler.sendEmptyMessage(LyricConstants.LYRIC_SCROLL);
            }
        }
    }

    /**
     * pause
     */
    public void pause() {
        // if (isScrolling) {
        removeLycScroll();
        // }
    }

    /**
     * stop
     */
    public void stop() {
        // if (isScrolling) {
        removeLycScroll();
        removeAllViewsInLayout();
        mLRC = null;
        // }
    }

    private int computeOffset() {
        // ++curLrc;
        mCurLrcPosition = 0;
        long pos = mPosProvider.getPosition();
        if (mLRC == null) {
            return 0;
        }
        List<LRC.Offset> ofs = mLRC.getOffsets();
        while (mCurLrcPosition < mLyricCount
                && pos > ofs.get(mCurLrcPosition).time) {
            mCurLrcPosition++;
        }

        int off = 0;
        long start = 0;
        long end = 0;
        if (mCurLrcPosition >= mLyricCount) {
            off = mLyricCount * mItemHeight;
        } else {
            start = ofs.get(mCurLrcPosition).time;
            if (pos < start && mCurLrcPosition > 0) {
                start = mLRC.getOffsets().get(--mCurLrcPosition).time;
            } else if (mCurLrcPosition <= 0) {
                return 0;
            }
            end = mCurLrcPosition == mLyricCount - 1 ? mPosProvider
                    .getDuration()
                    : mLRC.getOffsets().get(mCurLrcPosition + 1).time;
            off = (int) (1.0 * mItemHeight / (end - start) * (pos - start));
            off += mCurLrcPosition * mItemHeight;
        }
        int result = mOffset - off;
        mOffset = off;
        return result;
    }

    public void setScreenOnFlag(boolean screenOn) {
        mScreenOn = screenOn;
        if (screenOn) {
            mLyricHandler.sendEmptyMessage(LyricConstants.LYRIC_SCROLL);
        }
    }

    /**
     * @return boolean
     */
    public boolean isLyricModified() {
        return mIsLyricModified;
    }

    /**
     * @return String
     */
    public String getLrcPath() {
        return mPath;
    }

    /**
     * getLrc
     *
     * @return LRC
     */
    public LRC getLrc() {
        return mLRC;
    }

    /**
     * setLrc
     */
    public void setLrc(String path, LRC lrc) {
        setLrc(path, lrc, R.layout.lyric_list_item);
    }

    /**
     * user want the lyc to show or hide
     */
    public int getShowType() {
        return showType;
    }

    public void setShowType(int showType) {
        this.showType = showType;
    }

    /**
     * lrc scroll is pause?
     */
    public boolean isLycScroll() {
        return mIsScrolling;
    }

    /**
     * have lyc or not?
     */
    public boolean isHaveLyc() {
        return (mLRC != null) && (mLyricCount != 0);
    }

    /**
     * setLrc
     */
    public void setLrc(String path, LRC lrc, int item) {
        if (lrc == null) {
            this.mLRC = null;
            return;
        }
        stop();
        this.mPath = path;
        this.mLRC = lrc;
        this.mLyricListItem = item;
        this.mLyricCount = lrc.getOffsets().size();
        populateViews();
    }

    private void populateViews() {
        String[] lyrics = mLRC.listLyrics();
        // int len = lyrics.length;
        int len = 0;
        if (lyrics != null) {
            len = lyrics.length;
        }
        String[] newLyrics = new String[len + 2];
        newLyrics[0] = newLyrics[len + 1] = "";
        System.arraycopy(lyrics, 0, newLyrics, 1, len);
        if (mAdapter == null) {
            mAdapter = new LyricsAdapter(getContext(), mLyricListItem,
                    newLyrics);
            setAdapter(mAdapter);
        } else {
            mAdapter.setLyrics(newLyrics);
        }
        /*
         * The content of the adapter has changed but ListView did not receive a
		 * notification. so we call notifyDataSetChanged to send the
		 * notification.
		 */
        mAdapter.notifyDataSetChanged();
    }

    /**
     * ajustLyrics
     */
    public void ajustLyrics(int row, boolean after, boolean advance, int time) {
        mIsLyricModified = true;
        mLRC.ajust(row, after, advance, time);
        populateViews();
        mLyricHandler.sendEmptyMessage(LyricConstants.LYRIC_AJUST);
    }

    /**
     * setPositionProvider
     */
    public void setPositionProvider(LRC.PositionProvider provider) {
        Log.d(TAG, "setPositionProvider");
        mPosProvider = provider;
    }

    /**
     * onItemLongClick
     *
     * @return boolean
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   final int pos, long id) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.lyrics_edit_title)
                .setItems(R.array.lyrics_edit_item,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                switch (which) {
                                    case 0: // current lyric advance 0.5 second
                                        ajustLyrics(pos - 1, false, true,
                                                LyricConstants.DEFAULT_AJUST_TIME);
                                        break;
                                    case 1: // current lyric delayed 0.5 second
                                        ajustLyrics(pos - 1, false, false,
                                                LyricConstants.DEFAULT_AJUST_TIME);
                                        break;
                                    case 2: // the after lyrics advance 0.5 second
                                        ajustLyrics(pos - 1, true, true,
                                                LyricConstants.DEFAULT_AJUST_TIME);
                                        break;
                                    case 3: // the after lyrics delayed 0.5 second
                                        ajustLyrics(pos - 1, true, false,
                                                LyricConstants.DEFAULT_AJUST_TIME);
                                        break;
                                    case 4: // all lyrics advance 0.5 second
                                        ajustLyrics(-1, false, true,
                                                LyricConstants.DEFAULT_AJUST_TIME);
                                        break;
                                    case 5: // all lyrics delayed 0.5 second
                                        ajustLyrics(-1, false, false,
                                                LyricConstants.DEFAULT_AJUST_TIME);
                                        break;
                                    case 6:
                                        break; // do nothing
                                    default:
                                        if (mIsLyricModified) {
                                            boolean ret = LRCParser.saveToFile(
                                                    getLrcPath(), getLrc());
                                            // Toast.makeText(
                                            // getContext(),
                                            // ret ? R.string.lyrics_update_success
                                            // : R.string.lyrics_update_fail,
                                            // Toast.LENGTH_SHORT).show();
                                            mIsLyricModified = !ret;
                                        } else {
                                            // Toast.makeText(getContext(),
                                            // R.string.lyrics_no_update,
                                            // Toast.LENGTH_SHORT).show();
                                        }
                                }
                            }
                        }).show();
        // /*
        // * the call back function has dealt with the long click event ,so it
        // returns true.
        // */
        return true;
    }

    /**
     * getOffset
     *
     * @return int
     */
    public int getOffset() {
        return mOffset;
    }

    /**
     * setOffset
     */
    public void setOffset(int offset) {
        this.mOffset = offset;
    }

    /**
     * getDelay
     *
     * @return int
     */
    public int getDelay() {
        return mDelay;
    }

    /**
     * setDelay
     */
    public void setDelay(int delay) {
        this.mDelay = delay;
    }

    private class LyricsAdapter extends BaseAdapter {

        private Context mContext;

        private int mResId;

        private String[] mLyrics;

        private LayoutInflater mLayoutInflater;

        LyricsAdapter(Context c, int r, String[] l) {
            mContext = c;
            mResId = r;
            mLyrics = l;
            mLayoutInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setLyrics(String[] ls) {
            mLyrics = ls;
        }

        public int getCount() {
            return mLyrics.length;
        }

        public Object getItem(int position) {
            return mLyrics[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            TextView text;

            if (convertView == null) {
                view = mLayoutInflater.inflate(mResId, parent, false);
            } else {
                view = convertView;
            }

            try {
                if (mFieldId == 0) {
                    // If no custom field is assigned, assume the whole resource
                    // is a TextView
                    text = (TextView) view;
                } else {
                    // Otherwise, find the TextView field within the layout
                    text = (TextView) view.findViewById(mFieldId);
                }
            } catch (ClassCastException e) {
                Log.e("ArrayAdapter",
                        "You must supply a resource ID for a TextView");
                throw new IllegalStateException(
                        "ArrayAdapter requires the resource ID to be a TextView",
                        e);
            }

            String item = (String) getItem(position);
            text.setText(item);
            text.setTextColor((mCurLrcPosition + 1) == position ? mHighLightColor
                    : mNormalColor);

            return view;
        }
    }

}
