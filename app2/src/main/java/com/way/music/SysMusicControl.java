package com.way.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.KeyEvent;

import com.andrew.apollo.IApolloService;
import com.way.longplay.LongPlayView;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

//import com.android.music.IMediaPlaybackService;

public class SysMusicControl implements MusicControl {
    public static final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    public static final String META_CHANGED = "com.android.music.metachanged";
    public static final String QUEUE_CHANGED = "com.android.music.queuechanged";
    public static final String PLAYBACK_COMPLETE = "com.android.music.playbackcomplete";
    public static final String QUIT_PLAYBACK = "com.android.music.quitplayback";
    private static final String TAG = LongPlayView.TAG + ".SysMusicControl";
    private static final int MSG_REFRESH_ALBUM = 10;
    private static final Uri sArtworkUri = Uri
            .parse("content://media/external/audio/albumart");
    private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();

    static {
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        sBitmapOptions.inDither = false;
    }

    private static Bitmap mCachedBit = null;
    private static Bitmap mDefaultBackgroundBitmap;
    private Context mContext;
    private IntentFilter mFilter;
    private MusicCallback mCallback;
    private BroadcastReceiver mPlayReceiver = new BroadcastReceiver() {
        private static final String KEY_PLAYING = "playing";
        private static final String KEY_ARTIST = "artist";
        private static final String KEY_TRACK = "track";
        private static final String KEY_ALBUM = "album";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive action=" + action);
            if (PLAYSTATE_CHANGED.equals(action)) {
                if (intent.hasExtra(KEY_PLAYING)) {
                    boolean isplaying = intent.getBooleanExtra(KEY_PLAYING, false);
                    if (mCallback != null) {
                        mCallback.onStateChanged(isplaying ? MusicControl.STATE_PLAYING : MusicControl.STATE_NONE);
                    }
                }
            }
            if (/*PLAYSTATE_CHANGED.equals(action)
                    || */
                    META_CHANGED.equals(action)
                //|| QUEUE_CHANGED.equals(action)
                //|| PLAYBACK_COMPLETE.equals(action)
                //|| QUIT_PLAYBACK.equals(action)
                    ) {
                /*if (PLAYSTATE_CHANGED.equals(action) && intent.hasExtra(KEY_PLAYING)) {
					boolean isplaying = intent.getBooleanExtra(KEY_PLAYING, false);
					if (mCallback != null) {
						mCallback.onStateChanged(isplaying?MusicControl.STATE_PLAYING:MusicControl.STATE_NONE);
					}
				}*/
                if (intent.hasExtra(KEY_ARTIST)) {
                    String artist = intent.getStringExtra(KEY_ARTIST);
                    if (mCallback != null) {
                        mCallback.onArtistChanged(artist);
                    }
                }
                if (intent.hasExtra(KEY_TRACK)) {
                    String track = intent.getStringExtra(KEY_TRACK);
                    if (mCallback != null) {
                        mCallback.onTitleChanged(track);
                    }
                }
                if (intent.hasExtra(KEY_ALBUM)) {
                    String album = intent.getStringExtra(KEY_ALBUM);
                    Log.d(TAG, "onReceive KEY_ALBUM album=" + album);
                }
                mHandler.sendEmptyMessageDelayed(MSG_REFRESH_ALBUM, 1500);
            }
        }
    };
    private boolean bClickAndBind = false;
    private IApolloService mService;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            Log.d(TAG, "mHandler handleMessage(what=" + what + ")");
            switch (what) {
                case MSG_REFRESH_ALBUM:
                    removeMessages(what);
                    if (mCallback != null && mService != null) {
                        try {
                            boolean playing = mService.isPlaying();
                            mCallback.onStateChanged(playing ? MusicControl.STATE_PLAYING : MusicControl.STATE_NONE);
                        } catch (Exception e) {
                            Log.e(TAG, "MSG_REFRESH_ALBUM isplaying?", e);
                        }
                        try {
                            long song_id = mService.getAudioId();
                            long album_id = mService.getAlbumId();
                            mCallback.onAlbumCoverChanged(getArtwork(mContext, song_id, album_id, true));
                        } catch (Exception e) {
                            Log.e(TAG, "MSG_REFRESH_ALBUM", e);
                        }
                    } else {
                        Log.e(TAG, "MSG_REFRESH_ALBUM mCallback=" + mCallback + ";mService=" + mService);
                    }
                    break;
            }
        }

        ;
    };
    private boolean mServiceConnected;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = IApolloService.Stub.asInterface(service);
            mServiceConnected = true;
            if (bClickAndBind) {
                Log.d(TAG, "onServciceConnected-bClickAndBind");
                try {
                    boolean playing = mService.isPlaying();
                    if (!playing) {
                        mService.play();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onServiceConnected", e);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mServiceConnected = false;
            mService = null;
            mContext.unbindService(this);
        }
    };

    public SysMusicControl(Context context, MusicCallback callback) {
        mContext = context;
        mCallback = callback;

        mFilter = new IntentFilter();
        mFilter.addAction(PLAYSTATE_CHANGED);
        mFilter.addAction(META_CHANGED);
        mFilter.addAction(QUEUE_CHANGED);
        mFilter.addAction(PLAYBACK_COMPLETE);
        mFilter.addAction(QUIT_PLAYBACK);

    }

    public static Bitmap getArtwork(Context context, long song_id,
                                    long album_id, boolean allowdefault) {

        if (album_id < 0) {
            // This is something that is not in the database, so get the album
            // art directly
            // from the file.
            if (song_id >= 0) {
                Bitmap bm = getArtworkFromFile(context, song_id, -1);
                if (bm != null) {
                    return bm;
                }
            }
            if (allowdefault) {
                return getDefaultArtwork(context);
            }
            return getDefaultArtwork(context);
        }

        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                return BitmapFactory.decodeStream(in, null, sBitmapOptions);
            } catch (FileNotFoundException ex) {
                // The album art thumbnail does not actually exist. Maybe the
                // user deleted it, or
                // maybe it never existed to begin with.
                Bitmap bm = getArtworkFromFile(context, song_id, album_id);
                if (bm != null) {
                    if (bm.getConfig() == null) {
                        bm = bm.copy(Bitmap.Config.RGB_565, false);
                        if (bm == null && allowdefault) {
                            return getDefaultArtwork(context);
                        }
                    }
                } else if (allowdefault) {
                    bm = getDefaultArtwork(context);
                }
                return bm;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                }
            }
        }

        return getDefaultArtwork(context);
    }

    private static Bitmap getArtworkFromFile(Context context, long songid,
                                             long albumid) {
        Bitmap bm = null;
        ParcelFileDescriptor pfd = null;

        if (albumid < 0 && songid < 0) {
            throw new IllegalArgumentException(
                    "Must specify an album or a song id");
        }

        try {
            if (albumid < 0) {
                Uri uri = Uri.parse("content://media/external/audio/media/"
                        + songid + "/albumart");
                pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            } else {
                Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
                pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            }
        } catch (IllegalStateException ex) {
        } catch (FileNotFoundException ex) {
        } finally {
            try {
                if (pfd != null)
                    pfd.close();
            } catch (IOException e) {
            }
        }
        if (bm != null) {
            mCachedBit = bm;
        }
        return bm;
    }

    public static Bitmap getDefaultArtwork(Context context) {
        Log.d(TAG, "getDefaultArtwork....");
        if (mDefaultBackgroundBitmap == null) {
            //mDefaultBackgroundBitmap = BitmapFactory.decodeResource(context.getResources(),
            //		R.drawable.music_bg);
        }
        return mDefaultBackgroundBitmap;
    }

    @Override
    public String getTrackName() {
        if (mService != null) {
            try {
                return mService.getTrackName();
            } catch (Exception e) {
                //Log.e(TAG, "getCurrTime", e);
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public String getArtistName() {
        if (mService != null) {
            try {
                return mService.getArtistName();
            } catch (Exception e) {
                //Log.e(TAG, "getCurrTime", e);
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public boolean open() {
        return bindServices(false);
    }

    @Override
    public void register() {
        mContext.registerReceiver(mPlayReceiver, mFilter);
    }

    @Override
    public void unregister() {
        mContext.unregisterReceiver(mPlayReceiver);
    }

    @Override
    public long getCurrTime() {
        if (mService != null) {
            try {
                return mService.position();
            } catch (Exception e) {
                //Log.e(TAG, "getCurrTime", e);
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mService != null) {
            try {
                mService.duration();
            } catch (Exception e) {
                //Log.e(TAG, "getDuration", e);
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public int getState() {
        if (mService != null) {
            boolean isplaying = false;
            try {
                isplaying = mService.isPlaying();
            } catch (Exception e) {
                Log.e(TAG, "getState", e);
                e.printStackTrace();
            }
            if (isplaying) {
                return MusicControl.STATE_PLAYING;
            }
        }
        return MusicControl.STATE_NONE;
    }

    @Override
    public void requestAlbumCover() {

    }

    @Override
    public void onKeyEvent(int key) {
        boolean isplaying = false;
        try {
            switch (key) {
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    mService.prev();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    if (!mServiceConnected || mService == null) {
                        Log.d(TAG, "system service is disconnect, rebind it");

                        bClickAndBind = true;
                        bindServices(true);
                        return;
                    }
                    isplaying = mService.isPlaying();
                    if (isplaying) {
                    } else {
                        mService.play();
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    if (!mServiceConnected || mService == null) {
                        Log.d(TAG, "system service is disconnect, rebind it");
                        bClickAndBind = true;
                        bindServices(true);
                        return;
                    }
                    isplaying = mService.isPlaying();
                    if (isplaying) {
                        mService.pause();
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    Log.d(TAG, "mStylusView onClick");
                    if (!mServiceConnected || mService == null) {
                        Log.d(TAG, "system service is disconnect, rebind it");
                        bindServices(true);
                        return;
                    }
                    isplaying = mService.isPlaying();
                    if (isplaying) {
                        mService.pause();
                    } else {
                        mService.play();
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    mService.next();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "onKeyEvent", e);
        }
    }

    private boolean bindServices(boolean force) {
        Log.d(TAG, "bindServices mServiceConnected=" + mServiceConnected);
        if (!force && mServiceConnected) return mServiceConnected;
        Intent intent = new Intent();
        intent.setClassName("com.andrew.apollo", "com.andrew.apollo.MusicPlaybackService");
        mContext.startService(intent);
        return mContext.bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public Intent getMusicActivityIntent() {
        Intent intent = null;
        try {
            intent = Intent.parseUri("intent:#Intent;action=android.intent.action.MAIN;component=com.android.music/.MusicBrowserActivity;category=android.intent.category.LAUNCHER;end", 0);
        } catch (Exception e) {
            Log.e(TAG, "getMusicActivityIntent", e);
        }
        return intent;
    }
}