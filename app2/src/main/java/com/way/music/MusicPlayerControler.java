
package com.way.music;


public interface MusicPlayerControler {

    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_COMPLETED = 3;

    void play();

    void prev();

    void next();

    void pause();

    void resume();

    int getState();


    void seek(int pos);

    long getCachePercent();

    long getDuration();

    long getCurrTime();

    //Bitmap getAlbumCover();
    void requestAlbumCover();

    void setPlayerEventListener(PlayerEventListener listener);

    void open();

    void close();

    boolean isOpened();
}
