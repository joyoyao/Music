package com.andrew.apollo.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.andrew.apollo.ApolloApplication;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.way.downloadmanager.WDMSharPre;
import com.way.music.lrc.LRC;
import com.way.music.lrc.LRCParser;
import com.way.music.lrc.LyricListView;
import com.way.music.lrc.manager.LoaderLyricCallBack;
import com.way.music.lrc.manager.LyricFetcher;

import java.io.File;

public class LyricFragment extends Fragment implements LRC.PositionProvider,
        LoaderLyricCallBack, OnClickListener {
    private LyricListView mLyricListView;
    private TextView mEmptyView;
    // Broadcast receiver
    private PlaybackStatus mPlaybackStatus;
    private String mCurrentLrcPath = null;
    Runnable refresh = new Runnable() {

        @Override
        public void run() {
            if (mCurrentLrcPath != null) {
                setLrc(mCurrentLrcPath);
            }
            updateState();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.lyric_fragment_layout,
                null);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLyricListView = (LyricListView) view.findViewById(R.id.lyric_list);
        mEmptyView = (TextView) view.findViewById(R.id.nolrc_notifier);
        mEmptyView.setOnClickListener(this);
        mLyricListView.setPositionProvider(this);
        updateLyrics();
        mPlaybackStatus = new PlaybackStatus();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        getActivity().registerReceiver(mPlaybackStatus, filter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister the receiver
        try {
            getActivity().unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
        }
    }

    public void updateLyrics() {
        mCurrentLrcPath = null;
        mLyricListView.setLrc(mCurrentLrcPath, null);// reset
        mCurrentLrcPath = getLyricPath();
        if (mCurrentLrcPath != null) {
            setLrc(mCurrentLrcPath);
        } else {
            searchAndDownloadLyric(MusicUtils.getTrackName(),
                    MusicUtils.getArtistName());
        }
        updateState();
    }

    private void searchAndDownloadLyric(String trackName, String artistName) {
        // 如果有网络或者用户设置允许下载
        if (ApolloUtils.isOnline(getActivity())
                && PreferenceUtils.getInstance(getActivity())
                .downloadMissingLyricAndArtwork())
            LyricFetcher.getInstance(getActivity()).loadLyric(trackName,
                    artistName, this);
    }

    private String getLyricPath() {
        String fileName = LyricFetcher.getFileName(MusicUtils.getTrackName(),
                MusicUtils.getArtistName());
        String path = LyricFetcher.getLyricDirName() + File.separator + fileName;
        String downFileName = WDMSharPre.getSingle().getValue(path, "");
        File file = new File(path);
        Log.i("liweiping",
                "path = " + path + ", file.exists() = " + file.exists()
                        + ", downFileName = " + downFileName);
        if (file.exists() && TextUtils.equals(fileName, downFileName)) {
            return path;
        } else {
            return null;
        }
    }

    private void updateState() {
        boolean haveLyc = mLyricListView.isHaveLyc();
        if (haveLyc) {
            mLyricListView.start();
            mLyricListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.INVISIBLE);
        } else {
            mLyricListView.stop();
            mLyricListView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    private boolean setLrc(String lrcPath) {
        boolean ready = false;
        LRC lrc = null;
        try {
            lrc = LRCParser.parseFromFile(lrcPath, ApolloApplication
                    .getInstance().getCacheDir().toString());
            ready = true;
        } catch (Exception e) {
            Log.e("liweiping", "SET LYRIC ERROR:" + e.getMessage());
        }
        mLyricListView.setLrc(lrcPath, lrc);
        return ready;
    }

    @Override
    public void loadFinished(String path) {
        mCurrentLrcPath = path;
        if (getActivity() != null)
            getActivity().runOnUiThread(refresh);
    }

    @Override
    public long getPosition() {
        long position = 0;
        try {
            position = MusicUtils.mService.position();
        } catch (Exception e) {
            // Log.e("liweiping",
            // "MEDIA PLAY BACK GET POSITION ERROR:" + e.getMessage());
        }
        return position;
    }

    @Override
    public long getDuration() {
        long duration = 0;
        try {
            duration = MusicUtils.mService.duration();
        } catch (Exception e) {
            // Log.e("liweiping",
            // "MEDIA PLAY BACK GET DURATION ERROR:" + e.getMessage());
        }
        return duration;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nolrc_notifier:

                break;

            default:
                break;
        }
    }

    private class PlaybackStatus extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(MusicPlaybackService.META_CHANGED)) {
                LyricFragment.this.updateLyrics();
            } else if (action.equals(MusicPlaybackService.PLAYSTATE_CHANGED)) {
                // Set the play and pause image
                if (MusicUtils.isPlaying()) {
                    mLyricListView.resume();
                } else {
                    mLyricListView.pause();
                }
            }
        }

    }

}
