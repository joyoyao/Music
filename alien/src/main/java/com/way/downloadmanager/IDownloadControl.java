package com.way.downloadmanager;

import com.way.downloadmanager.net.exception.DataErrorEnum;

public interface IDownloadControl {

    public void onStart(DownloadRequest request);

    public void onProgress(DownloadRequest request, int progress);

    public void onFinished(DownloadRequest request);

    public void onError(DownloadRequest request, DataErrorEnum error);

    public void onCancel(DownloadRequest request);

    public void onPause(DownloadRequest request);


}