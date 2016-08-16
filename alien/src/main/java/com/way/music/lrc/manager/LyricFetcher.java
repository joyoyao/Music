package com.way.music.lrc.manager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.ApolloApplication;
import com.andrew.apollo.cache.ImageCache;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.way.downloadmanager.DownloadManager;
import com.way.downloadmanager.DownloadRequest;
import com.way.downloadmanager.IDownloadListener;
import com.way.downloadmanager.net.exception.DataErrorEnum;
import com.way.music.lrc.StringConstant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class LyricFetcher {
    private static final String BASE_URL = "http://geci.me/api/lyric/";
    private static LyricFetcher sInstance = null;

    /**
     * Creates a new instance of {@link com.way.music.lrc.manager.LyricFetcher}.
     *
     * @param context The {@link android.content.Context} to use.
     */
    private LyricFetcher(final Context context) {
    }

    /**
     * Used to create a singleton of the lyric fetcher
     *
     * @param context The {@link android.content.Context} to use
     * @return A new instance of this class.
     */
    public static final LyricFetcher getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new LyricFetcher(context.getApplicationContext());
        }
        return sInstance;
    }

    public static File getLyricDir() {
        File file = ImageCache.getDiskCacheDir(ApolloApplication.getInstance(), "lyric");
        if (!file.exists())
            file.mkdirs();
        return file;
    }

    public static String getLyricDirName() {
        File file = ImageCache.getDiskCacheDir(ApolloApplication.getInstance(), "lyric");
        if (!file.exists())
            file.mkdirs();
        return file.getAbsolutePath();
    }

    public static String getFileName(String trackName, String artistName) {
        return trackName + StringConstant.LINK_FACTOR + artistName
                + StringConstant.LYRIC_SUFFIX;
    }

    private static String getAbsoluteUrl(String trackName, String artistName) {
        String subString = trackName + File.separator + artistName;
        return BASE_URL + subString;
    }

    private static boolean isEnglishLetter(final char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    private boolean isHasSpecial(String name) {
        if (TextUtils.isEmpty(name))
            return true;
        if (name.contains("_") || name.contains("(") || name.contains(")")
                || name.contains("<") || name.contains(">"))
            return true;
        return false;
    }

    public void loadLyric(final String trackName, final String artistName,
                          final LoaderLyricCallBack callBack) {
        if (isHasSpecial(trackName) || isHasSpecial(artistName)) {
            if (callBack != null)
                callBack.loadFinished(null);
            return;
        }
        String url = getAbsoluteUrl(trackName, artistName);
        url = url.replaceAll(" ", "%20");
        Log.i("liweiping", "url = " + url);
        new AsyncHttpClient().get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, JSONObject response) {
                super.onSuccess(statusCode, response);
                Log.i("liweiping", "onSuccess response = " + response);
                try {
                    int count = response.getInt("count");
                    if (count > 0) {
                        JSONArray lyricJSONArray = response
                                .getJSONArray("result");
                        JSONObject jsonObject = lyricJSONArray.getJSONObject(0);
                        String lyricUrl = jsonObject.getString("lrc");
                        Log.i("liweiping", "lyricUrl = " + lyricUrl);
                        downloadLyric(trackName, artistName, lyricUrl, callBack);
                    } else {
                        if (callBack != null)
                            callBack.loadFinished(null);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (callBack != null)
                        callBack.loadFinished(null);
                }
            }

            @Override
            public void onFailure(Throwable error, String content) {
                super.onFailure(error, content);
                Log.i("liweiping", "onFailure  content= " + content
                        + ",  error = " + error.getMessage());
                if (callBack != null)
                    callBack.loadFinished(null);
            }
        });

    }

    protected void downloadLyric(String trackName, String artistName,
                                 String lyricUrl, LoaderLyricCallBack callBack) {
        if (TextUtils.isEmpty(lyricUrl)) {
            if (callBack != null)
                callBack.loadFinished(null);
            return;
        }
        String fileName = getFileName(trackName, artistName);
        String dirName = getLyricDirName();
        DownloadRequest downloadRequest = new DownloadRequest(fileName,
                fileName, dirName, lyricUrl);
        DownloadManager.instance().registerListener(fileName,
                new LyricIDownloadListener(callBack));
        DownloadManager.instance().start(downloadRequest);

    }

    class LyricIDownloadListener implements IDownloadListener {
        private LoaderLyricCallBack mCallBack;

        public LyricIDownloadListener(LoaderLyricCallBack callBack) {
            // TODO Auto-generated constructor stub
            mCallBack = callBack;
        }

        @Override
        public void downloadWait(DownloadRequest downloadRequest) {
            // TODO Auto-generated method stub
            Log.i("liweiping", "downloadWait...");
        }

        @Override
        public void downloadStart(DownloadRequest downloadRequest) {
            // TODO Auto-generated method stub
            Log.i("liweiping", "downloadStart...");
        }

        @Override
        public void downloadFinish(DownloadRequest downloadRequest) {
            // TODO Auto-generated method stub
            if (mCallBack != null) {
                File file = new File(downloadRequest.getFilePath(),
                        downloadRequest.getFileName());
                Log.i("liweiping", "downloadFinish path = " + file.toString());
                mCallBack.loadFinished(file.getAbsolutePath());

            }
        }

        @Override
        public void downloadCancel(DownloadRequest downloadRequest) {
            // TODO Auto-generated method stub
            Log.i("liweiping", "downloadCancel...");
        }

        @Override
        public void downloadPause(DownloadRequest downloadRequest) {
            // TODO Auto-generated method stub
            Log.i("liweiping", "downloadPause...");
        }

        @Override
        public void downloadProgress(DownloadRequest downloadRequest,
                                     int downloadProgress) {
            // TODO Auto-generated method stub
            Log.i("liweiping", "downloadProgress... + downloadProgress = "
                    + downloadProgress);
        }

        @Override
        public void downloadError(DownloadRequest downloadRequest,
                                  DataErrorEnum error) {
            // TODO Auto-generated method stub
            Log.i("liweiping", "downloadError... + error = " + error);
            if (mCallBack != null) {
                mCallBack.loadFinished(null);
            }
        }

    }
}
