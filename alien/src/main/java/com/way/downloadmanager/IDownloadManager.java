package com.way.downloadmanager;

import java.util.List;

public interface IDownloadManager {
    //do request
    public void start(DownloadRequest downloadRequest);

    public void pause(Object resId);

    public void cancel(Object resId);

    //request information
    public boolean isWaiting(Object resId);

    public boolean isDownloading(Object resId);

    public int getDownloadProgress(Object resId);

    //request listener
    public void registerListener(Object resId, IDownloadListener downloadListener);

    public void removeListener(Object resId, IDownloadListener listener);

    public void removeListener(IDownloadListener listener);

    public void removeListener(Object resId);

    //request List
    public List<DownloadRequest> getDownloadingList();

    public List<DownloadRequest> getDownloadWaitingList();


}