package com.way.music;

import android.content.Intent;

public interface MusicControl {
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_COMPLETED = 3;

    public String getTrackName();

    public String getArtistName();

    boolean open();

    void register();

    void unregister();

    long getCurrTime();

    long getDuration();

    int getState();

    void requestAlbumCover();

    void onKeyEvent(int key);

    Intent getMusicActivityIntent();
}