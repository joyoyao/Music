package com.way.music;

import android.graphics.Bitmap;

public interface MusicCallback {
    void onTitleChanged(String title);

    void onArtistChanged(String artist);

    void onStateChanged(int state);

    void onLyricChanged(String lrcPath, String staticTxt);

    void onAlbumCoverChanged(Bitmap albumCover);
}