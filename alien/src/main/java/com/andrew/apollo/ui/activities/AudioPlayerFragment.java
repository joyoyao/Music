package com.andrew.apollo.ui.activities;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore.Audio.Playlists;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.adapters.PagerAdapter;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.ui.fragments.EmptyFragment;
import com.andrew.apollo.ui.fragments.LyricFragment;
import com.andrew.apollo.ui.fragments.QueueFragment;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.BlurTransformation;
import com.andrew.apollo.utils.MaterialColorMapUtils;
import com.andrew.apollo.utils.MaterialColorMapUtils.MaterialPalette;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.ThemeUtils;
import com.andrew.apollo.widgets.LetterTileDrawable;
import com.andrew.apollo.widgets.PlayPauseButton;
import com.andrew.apollo.widgets.RepeatButton;
import com.andrew.apollo.widgets.RepeatingImageButton;
import com.andrew.apollo.widgets.ShuffleButton;
import com.viewpagerindicator.CirclePageIndicator;

import java.lang.ref.WeakReference;

import static com.andrew.apollo.utils.MusicUtils.mService;

public class AudioPlayerFragment extends Fragment implements
        OnSeekBarChangeListener, DeleteDialog.DeleteDialogCallback {
    // Message to refresh the time
    private static final int REFRESH_TIME = 1;
    /**
     * Used to scan backwards through the track
     */
    private final RepeatingImageButton.RepeatListener mRewindListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };
    /**
     * Used to scan ahead through the track
     */
    private final RepeatingImageButton.RepeatListener mFastForwardListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanForward(repcnt, howlong);
        }
    };
    /**
     * Switches from the large album art screen to show the queue and lyric
     * fragments, then back again
     */
    private final OnClickListener mToggleHiddenPanel = new OnClickListener() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onClick(final View v) {
            // if (mPageContainer.getVisibility() == View.VISIBLE) {
            // // Open the current album profile
            // mAudioPlayerHeader.setOnClickListener(mOpenAlbumProfile);
            // // Show the artwork, hide the queue
            // showAlbumArt();
            // } else {
            // // Scroll to the current track
            // mAudioPlayerHeader.setOnClickListener(mScrollToCurrentSong);
            // // Show the queue, hide the artwork
            // hideAlbumArt();
            // }
        }
    };
    /**
     * Opens to the current album profile
     */
    private final OnClickListener mOpenAlbumProfile = new OnClickListener() {

        @Override
        public void onClick(final View v) {
            NavUtils.openAlbumProfile(getActivity(), MusicUtils.getAlbumName(),
                    MusicUtils.getArtistName(), MusicUtils.getCurrentAlbumId());
        }
    };
    /**
     * Scrolls the queue to the currently playing song
     */
    private final OnClickListener mScrollToCurrentSong = new OnClickListener() {

        @Override
        public void onClick(final View v) {
            ((QueueFragment) mPagerAdapter.getFragment(0))
                    .scrollToCurrentSong();
        }
    };
    OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            if (position == 1)
                mAlbumArt.setAlpha(1f);
            else
                mAlbumArt.setAlpha(0f);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
//			Log.i("liweiping", "position = " + position + ", positionOffset = "
//					+ positionOffset + ", positionOffsetPixels = "
//					+ positionOffsetPixels);
            if (position == 0)
                mAlbumArt.setAlpha(positionOffset);
            else if (position == 1)
                mAlbumArt.setAlpha(1 - positionOffset);

        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };
    // The service token
    private ServiceToken mToken;
    // Play and pause button
    private PlayPauseButton mPlayPauseButton;
    // Repeat button
    private RepeatButton mRepeatButton;
    // Shuffle button
    private ShuffleButton mShuffleButton;
    // Previous button
    private RepeatingImageButton mPreviousButton;
    // Next button
    private RepeatingImageButton mNextButton;
    // Track name
    private TextView mTrackName;
    // Artist name
    private TextView mArtistName;
    // Album art
    private ImageView mAlbumArt;
    private ImageView mAlbumArtBackground;
    // Tiny artwork
    private ImageView mAlbumArtSmall;
    // Current time
    private TextView mCurrentTime;
    // Total time
    private TextView mTotalTime;
    // Queue switch
    private ImageView mQueueSwitch;

    // ViewPager container
    // private FrameLayout mPageContainer;
    // Progess
    private SeekBar mProgress;
    private SeekBar mProgressParent;
    // Broadcast receiver
    private PlaybackStatus mPlaybackStatus;
    // Handler used to update the current time
    private TimeHandler mTimeHandler;
    // View pager
    private ViewPager mViewPager;
    // Pager adpater
    private PagerAdapter mPagerAdapter;
    // Header
    private LinearLayout mAudioPlayerHeader;
    // Image cache
    private ImageFetcher mImageFetcher;
    // Theme resources
    private ThemeUtils mResources;
    private long mPosOverride = -1;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private long mLastShortSeekEventTime;
    private boolean mIsPaused = false;
    private boolean mFromTouch = false;
    private MaterialColorMapUtils mMaterialColorMapUtils;
    private boolean mHasComputedThemeColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mResources = new ThemeUtils(getActivity());
        mMaterialColorMapUtils = new MaterialColorMapUtils(getResources());
        mToken = ((BaseActivity) getActivity()).getServiceToken();
        // Initialize the image fetcher/cache
        mImageFetcher = ApolloUtils.getImageFetcher(getActivity());

        // Initialize the handler used to update the current time
        mTimeHandler = new TimeHandler(this);

        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus(this);
    }

    public void onServiceConnected(IApolloService service) {
        mService = service;
        // Check whether we were asked to start any playback
        startPlayback();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Update the favorites icon
        // invalidateOptionsMenu();

        // Refresh the queue
        ((QueueFragment) mPagerAdapter.getFragment(0)).refreshQueue();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_player_base, container,
                false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initPlaybackControls(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Refresh the queue
        ((QueueFragment) mPagerAdapter.getFragment(0)).refreshQueue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        getActivity().registerReceiver(mPlaybackStatus, filter);
        // Refresh the current time
        final long next = refreshCurrentTime();
        queueNextRefresh(next);
        // MusicUtils.notifyForegroundStateChanged(this, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();
        // MusicUtils.notifyForegroundStateChanged(this, false);
        mImageFetcher.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH_TIME);

        // Unregister the receiver
        try {
            getActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }
    }

    /**
     * Initializes the items in the now playing screen
     */
    private void initPlaybackControls(View view) {

        // Now playing header
        mAudioPlayerHeader = (LinearLayout) view
                .findViewById(R.id.audio_player_header);
        // Opens the currently playing album profile
        mAudioPlayerHeader.setOnClickListener(mOpenAlbumProfile);

        // Used to hide the artwork and show the queue
        final FrameLayout mSwitch = (FrameLayout) view
                .findViewById(R.id.audio_player_switch);
        mSwitch.setOnClickListener(mToggleHiddenPanel);

        // Initialize the pager adapter
        mPagerAdapter = new PagerAdapter(getActivity());
        // Queue
        mPagerAdapter.add(QueueFragment.class, null);
        mPagerAdapter.add(EmptyFragment.class, null);
        mPagerAdapter.add(LyricFragment.class, null);

        // Initialize the ViewPager
        mViewPager = (ViewPager) view.findViewById(R.id.audio_player_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen pager loading limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        //mViewPager.setOnPageChangeListener(onPageChangeListener);
        CirclePageIndicator circlePageIndicator = (CirclePageIndicator) view.findViewById(R.id.circle_page_indicator);
        circlePageIndicator.setViewPager(mViewPager);
        circlePageIndicator.setOnPageChangeListener(onPageChangeListener);

        // Play and pause button
        mPlayPauseButton = (PlayPauseButton) view
                .findViewById(R.id.action_button_play);
        // Shuffle button
        mShuffleButton = (ShuffleButton) view
                .findViewById(R.id.action_button_shuffle);
        // Repeat button
        mRepeatButton = (RepeatButton) view
                .findViewById(R.id.action_button_repeat);
        // Previous button
        mPreviousButton = (RepeatingImageButton) view
                .findViewById(R.id.action_button_previous);
        // Next button
        mNextButton = (RepeatingImageButton) view
                .findViewById(R.id.action_button_next);
        // Track name
        mTrackName = (TextView) view.findViewById(R.id.audio_player_track_name);
        // Artist name
        mArtistName = (TextView) view
                .findViewById(R.id.audio_player_artist_name);
        // Album art
        mAlbumArt = (ImageView) view.findViewById(R.id.audio_player_album_art);
        mAlbumArtBackground = (ImageView) view.findViewById(R.id.background);
        // Small album art
        mAlbumArtSmall = (ImageView) view
                .findViewById(R.id.audio_player_switch_album_art);
        // Current time
        mCurrentTime = (TextView) view
                .findViewById(R.id.audio_player_current_time);
        // Total time
        mTotalTime = (TextView) view.findViewById(R.id.audio_player_total_time);
        // Used to show and hide the queue fragment
        mQueueSwitch = (ImageView) view
                .findViewById(R.id.audio_player_switch_queue);
        // Theme the queue switch icon
        mQueueSwitch.setImageDrawable(mResources
                .getDrawable("btn_switch_queue"));
        // Progress
        mProgress = (SeekBar) view.findViewById(android.R.id.progress);
        mProgressParent = (SeekBar) getActivity()
                .findViewById(R.id.progressBar);

        // Set the repeat listner for the previous button
        mPreviousButton.setRepeatListener(mRewindListener);
        // Set the repeat listner for the next button
        mNextButton.setRepeatListener(mFastForwardListener);
        // Update the progress
        mProgress.setOnSeekBarChangeListener(this);
        mProgressParent.setOnSeekBarChangeListener(this);

        mViewPager.setCurrentItem(1);
    }

    /**
     * Sets the track name, album name, and album art.
     */
    public void updateNowPlayingInfo() {
        // Set the track name
        mTrackName.setText(MusicUtils.getTrackName());
        // Set the artist name
        mArtistName.setText(MusicUtils.getArtistName());
        // Set the total time
        mTotalTime.setText(MusicUtils.makeTimeString(getActivity(),
                MusicUtils.duration() / 1000));
        // Set the album art
        mImageFetcher.loadCurrentArtistImage(mAlbumArt,
                new LoaderArtCallBack() {

                    @Override
                    public void loadFinished(Bitmap bitmap) {
                        updateArtImage(bitmap);
                    }
                });
//		if(isAdded())
//			mAlbumArtBackground.setImageDrawable(new ColorDrawable(pickColor(MusicUtils
//					.getArtistName())));
        // Set the small artwork
        // mImageFetcher.loadCurrentArtistImage(mAlbumArtSmall);
        // Update the current time
        queueNextRefresh(1);
        //((LyricFragment)mPagerAdapter.getItem(2)).updateLyrics();
    }

    public void updateArtImage(Bitmap bitmap) {
        if (bitmap != null) {
            mAlbumArt.setImageBitmap(bitmap);
            bitmap = scaleBitmap(bitmap);
            bitmap = BlurTransformation.transform(getActivity(), bitmap, 4f);
            if (bitmap != null)
                mAlbumArtBackground.setImageBitmap(bitmap);
        } else {
//			mAlbumArt.setImageDrawable(new ColorDrawable(pickColor(MusicUtils
//					.getArtistName())));
            if (isAdded())
                mAlbumArtBackground.setImageDrawable(new ColorDrawable(pickColor(MusicUtils
                        .getArtistName())));
        }
        //extractAndApplyTintFromPhotoViewAsynchronously();
    }

    private Bitmap scaleBitmap(Bitmap myBitmap) {

        int width = (int) (myBitmap.getWidth() / 8);
        int height = (int) (myBitmap.getHeight() / 8);

        return Bitmap.createScaledBitmap(myBitmap, width, height, false);
    }

    private int pickColor(final String identifier) {
        if (TextUtils.isEmpty(identifier))
            return R.color.material_blue;
        TypedArray sColors = getActivity().getResources().obtainTypedArray(
                R.array.letter_tile_colors);
        final int color = Math.abs(identifier.hashCode()) % sColors.length();
        return sColors.getColor(color, R.color.material_blue);
    }

    /**
     * Checks whether the passed intent contains a playback request, and starts
     * playback if that's the case
     */
    private void startPlayback() {
        Intent intent = getActivity().getIntent();

        if (intent == null || mService == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (uri != null && uri.toString().length() > 0) {
            MusicUtils.playFile(getActivity(), uri);
            handled = true;
        } else if (Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = intent.getLongExtra("playlistId", -1);
            if (id < 0) {
                String idString = intent.getStringExtra("playlist");
                if (idString != null) {
                    try {
                        id = Long.parseLong(idString);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
            if (id >= 0) {
                MusicUtils.playPlaylist(getActivity(), id);
                handled = true;
            }
        }

        if (handled) {
            // Make sure to process intent only once
            getActivity().setIntent(new Intent());
            // Refresh the queue
            ((QueueFragment) mPagerAdapter.getFragment(0)).refreshQueue();
        }
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private void updatePlaybackControls() {
        // Set the play and pause image
        mPlayPauseButton.updateState();
        // Set the shuffle image
        mShuffleButton.updateShuffleState();
        // Set the repeat image
        mRepeatButton.updateRepeatState();
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);
            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    /**
     * Used to scan backwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta  The long press duration
     */
    private void scanBackward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(getActivity());
                final long duration = MusicUtils.duration();
                mStartSeekPos += duration;
                newpos += duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta  The long press duration
     */
    private void scanForward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos + delta;
            final long duration = MusicUtils.duration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                mStartSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    private void refreshCurrentTimeText(final long pos) {
        mCurrentTime.setText(MusicUtils.makeTimeString(getActivity(),
                pos / 1000));
    }

    /* Used to update the current time string */
    private long refreshCurrentTime() {
        if (mService == null) {
            return 500;
        }
        try {
            final long pos = mPosOverride < 0 ? MusicUtils.position()
                    : mPosOverride;
            if (pos >= 0 && MusicUtils.duration() > 0) {
                refreshCurrentTimeText(pos);
                final int progress = (int) (1000 * pos / MusicUtils.duration());
                mProgress.setProgress(progress);
                mProgressParent.setProgress(progress);
                if (mFromTouch) {
                    return 500;
                } else if (MusicUtils.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    final int vis = mCurrentTime.getVisibility();
                    mCurrentTime
                            .setVisibility(vis == View.INVISIBLE ? View.VISIBLE
                                    : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
                mProgressParent.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            final long remaining = 1000 - pos % 1000;
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) {
                width = 320;
            }
            final long smoothrefreshtime = MusicUtils.duration() / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }
            if (smoothrefreshtime < 20) {
                return 20;
            }
            return smoothrefreshtime;
        } catch (final Exception ignored) {

        }
        return 500;
    }

    /**
     * @param v     The view to animate
     * @param alpha The alpha to apply
     */
    private void fade(final View v, final float alpha) {
        final ObjectAnimator fade = ObjectAnimator.ofFloat(v, "alpha", alpha);
        fade.setInterpolator(AnimationUtils.loadInterpolator(getActivity(),
                android.R.anim.accelerate_decelerate_interpolator));
        fade.setDuration(400);
        fade.start();
    }

    /**
     * Called to show the album art and hide the queue
     */
    private void showAlbumArt() {
        // mPageContainer.setVisibility(View.INVISIBLE);
        mAlbumArtSmall.setVisibility(View.GONE);
        mQueueSwitch.setVisibility(View.VISIBLE);
        // Fade out the pager container
        // fade(mPageContainer, 0f);
        // Fade in the album art
        // fade(mAlbumArt, 1f);
    }

    /**
     * Called to hide the album art and show the queue
     */
    public void hideAlbumArt() {
        // mPageContainer.setVisibility(View.VISIBLE);
        mQueueSwitch.setVisibility(View.GONE);
        mAlbumArtSmall.setVisibility(View.VISIBLE);
        // Fade out the artwork
        // fade(mAlbumArt, 0f);
        // Fade in the pager container
        // fade(mPageContainer, 1f);
    }

    ;

    /**
     * /** Used to shared what the user is currently listening to
     */
    private void shareCurrentTrack() {
        if (MusicUtils.getTrackName() == null
                || MusicUtils.getArtistName() == null) {
            return;
        }
        final Intent shareIntent = new Intent();
        final String shareMessage = getString(R.string.now_listening_to,
                MusicUtils.getTrackName(), MusicUtils.getArtistName());

        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent,
                getString(R.string.share_track_using)));
    }

    /**
     * Asynchronously extract the most vibrant color from the PhotoView. Once
     * extracted, apply this tint to {@link MultiShrinkScroller}. This operation
     * takes about 20-30ms on a Nexus 5.
     */
    private void extractAndApplyTintFromPhotoViewAsynchronously() {
        // if (mScroller == null) {
        // return;
        // }
        final Drawable imageViewDrawable = mAlbumArt.getDrawable();
        if (imageViewDrawable == null)
            return;
        new AsyncTask<Void, Void, MaterialPalette>() {
            @Override
            protected MaterialPalette doInBackground(Void... params) {

                if (imageViewDrawable instanceof BitmapDrawable) {
                    // Perform the color analysis on the thumbnail instead of
                    // the full sized
                    // image, so that our results will be as similar as possible
                    // to the Bugle
                    // app.
                    final Bitmap bitmap = ((BitmapDrawable) imageViewDrawable)
                            .getBitmap();
                    try {
                        final int primaryColor = colorFromBitmap(bitmap);
                        if (primaryColor != 0) {
                            return mMaterialColorMapUtils
                                    .calculatePrimaryAndSecondaryColor(primaryColor);
                        }
                    } finally {
                        // bitmap.recycle();
                    }
                }
                if (imageViewDrawable instanceof LetterTileDrawable) {
                    final int primaryColor = ((LetterTileDrawable) imageViewDrawable)
                            .getColor();
                    return mMaterialColorMapUtils
                            .calculatePrimaryAndSecondaryColor(primaryColor);
                }
                return MaterialColorMapUtils
                        .getDefaultPrimaryAndSecondaryColors(getResources());
            }

            @Override
            protected void onPostExecute(MaterialPalette palette) {
                super.onPostExecute(palette);
                if (mHasComputedThemeColor) {
                    // If we had previously computed a theme color from the
                    // contact photo,
                    // then do not update the theme color. Changing the theme
                    // color several
                    // seconds after QC has started, as a result of an
                    // updated/upgraded photo,
                    // is a jarring experience. On the other hand, changing the
                    // theme color after
                    // a rotation or onNewIntent() is perfectly fine.
                    return;
                }
                // Check that the Photo has not changed. If it has changed, the
                // new tint
                // color needs to be extracted
                if (imageViewDrawable == mAlbumArt.getDrawable()) {
                    mHasComputedThemeColor = true;
                    setThemeColor(palette);
                }
            }
        }.execute();
    }

    private void setThemeColor(MaterialPalette palette) {
        // If the color is invalid, use the predefined default
        final int primaryColor = palette.mPrimaryColor;
        // getActionBar().setBackgroundDrawable(new
        // ColorDrawable(primaryColor));
        //new ThemeUtils(getActivity()).transWindows(getActivity(), primaryColor);
        //mAlbumArtBackground.setBackgroundColor(primaryColor);
    }

    private int colorFromBitmap(Bitmap bitmap) {
        // Author of Palette recommends using 24 colors when analyzing profile
        // photos.
        final int NUMBER_OF_PALETTE_COLORS = 24;
        final Palette palette = Palette.generate(bitmap,
                NUMBER_OF_PALETTE_COLORS);
        if (palette != null && palette.getVibrantSwatch() != null) {
            return palette.getVibrantSwatch().getRgb();
        }
        return 0;
    }

    @Override
    public void onDelete(long[] id) {
        // TODO Auto-generated method stub
        ((QueueFragment) mPagerAdapter.getFragment(0)).refreshQueue();
        if (MusicUtils.getQueue().length == 0) {
            // NavUtils.goHome(this);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (!fromUser || mService == null) {
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            MusicUtils.seek(mPosOverride);
            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1;
            }
        } else if (now - mLastShortSeekEventTime > 5) {
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            refreshCurrentTimeText(mPosOverride);
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        mLastSeekEventTime = 0;
        mFromTouch = true;
        mCurrentTime.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        if (mPosOverride != -1) {
            MusicUtils.seek(mPosOverride);
        }
        mPosOverride = -1;
        mFromTouch = false;
    }

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {

        private final WeakReference<AudioPlayerFragment> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        public TimeHandler(final AudioPlayerFragment player) {
            mAudioPlayer = new WeakReference<AudioPlayerFragment>(player);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REFRESH_TIME:
                    final long next = mAudioPlayer.get().refreshCurrentTime();
                    mAudioPlayer.get().queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Used to monitor the state of playback
     */
    private static final class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<AudioPlayerFragment> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final AudioPlayerFragment activity) {
            mReference = new WeakReference<AudioPlayerFragment>(activity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action.equals(MusicPlaybackService.META_CHANGED)) {
                // Current info
                mReference.get().updateNowPlayingInfo();
                // Update the favorites icon
                // Reference.get().invalidateOptionsMenu();
            } else if (action.equals(MusicPlaybackService.PLAYSTATE_CHANGED)) {
                // Set the play and pause image
                mReference.get().mPlayPauseButton.updateState();
            } else if (action.equals(MusicPlaybackService.REPEATMODE_CHANGED)
                    || action.equals(MusicPlaybackService.SHUFFLEMODE_CHANGED)) {
                // Set the repeat image
                mReference.get().mRepeatButton.updateRepeatState();
                // Set the shuffle image
                mReference.get().mShuffleButton.updateShuffleState();
            }
        }
    }
}
