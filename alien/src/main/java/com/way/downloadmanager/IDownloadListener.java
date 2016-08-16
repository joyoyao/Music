package com.way.downloadmanager;

import com.way.downloadmanager.net.exception.DataErrorEnum;

public interface IDownloadListener {
    void downloadWait(DownloadRequest downloadRequest);

    void downloadStart(DownloadRequest downloadRequest);

    void downloadFinish(DownloadRequest downloadRequest);

    void downloadCancel(DownloadRequest downloadRequest);

    void downloadPause(DownloadRequest downloadRequest);

    void downloadProgress(DownloadRequest downloadRequest, int downloadProgress);

    void downloadError(DownloadRequest downloadRequest, DataErrorEnum error);

}
