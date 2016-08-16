package com.way.music;

import android.graphics.Bitmap;

public interface PlayerEventListener {
    void onStateChanged(long songId, int state);

    void onAlbumCoverChanged(Bitmap albumCover);

    void onServiceDisconnected();

    void onSongChanged();

    void onLyricChanged(String lrcPath, String staticTxt);

    void onServciceConnected();
}
