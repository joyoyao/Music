package com.way.downloadmanager;

import android.util.Log;

import com.way.downloadmanager.lib.FileUtil;
import com.way.downloadmanager.net.exception.DataErrorEnum;
import com.way.downloadmanager.net.exception.DataException;
import com.way.downloadmanager.net.http.HttpConnectionBuilder;
import com.way.downloadmanager.net.http.HttpConstant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;

public class DownloadTask extends Thread {
    private static final String TAG = "DownloadTask";

    private static final long RESERVED_SPACE = 30 * 1024 * 1024;

    private DownloadRequest downloadRequest;

    private volatile boolean stop = false;
    private volatile boolean pause = false;
    private long updateTime = 100;
    private IDownloadControl iDownloadControl;

    public DownloadTask(DownloadRequest downloadRequest, IDownloadControl iDownloadControl) {
        this.downloadRequest = downloadRequest;
        this.iDownloadControl = iDownloadControl;
    }

    @Override
    public void run() {
        if (downloadRequest == null) {
            return;
        }
        if (this.iDownloadControl != null) {
            this.iDownloadControl.onStart(downloadRequest);
        }
        stop = false;
        pause = false;
        long startPosition = downloadRequest.getDownloadedSize();
        long currentUpdateTime = 0;
        String filePath = downloadRequest.getFilePath();
        String fileName = downloadRequest.getFileName();
        File tempFilePath = new File(filePath, fileName + ".tmp");
        if (tempFilePath.exists()) {
            startPosition = tempFilePath.length();
            if (startPosition != downloadRequest.getDownloadedSize()) {
                downloadRequest.setDownloadedSize(startPosition);
            }
        } else {
            File folder = new File(filePath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

        }
        HttpURLConnection connection = null;
        try {
            connection = new HttpConnectionBuilder(downloadRequest.getUrl(), HttpConstant.GET).setReadTimeout(20 * 1000)
                    .setProperty("User-Agent", "NetFox").build();

            if (startPosition > 0) {
                String start = "bytes=" + startPosition + "-";
                connection.setRequestProperty("RANGE", start);
            }

            long fileLength = connection.getContentLength();
            if (fileLength <= 0) {
                this.iDownloadControl.onError(this.downloadRequest, DataErrorEnum.DOWNLOAD_FILE_NOT_EXISTS);
                connection.disconnect();
                return;
            }
            try {
                if (FileUtil.hasSdcard()) {
                    if (fileLength > FileUtil.getSDCardIdleSpace() - RESERVED_SPACE) {
                        this.iDownloadControl.onError(this.downloadRequest, DataErrorEnum.DOWNLOAD_STORAGE_FAILED);
                        connection.disconnect();
                        return;
                    }
                } else {
                    if (fileLength > FileUtil.getDataIdleSapce() - RESERVED_SPACE) {
                        Log.e(TAG,
                                "data storage is full: " + "fileLength = " + fileLength + ", idle space = "
                                        + (FileUtil.getDataIdleSapce() - RESERVED_SPACE));
                        this.iDownloadControl.onError(this.downloadRequest, DataErrorEnum.DOWNLOAD_STORAGE_FAILED);
                        connection.disconnect();
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "storage error 0.");
                if (this.iDownloadControl != null) {
                    this.iDownloadControl.onError(this.downloadRequest, DataErrorEnum.DOWNLOAD_STORAGE_FAILED);
                }
                connection.disconnect();
                return;
            }

            if (startPosition > 0) {
                downloadRequest.setTotalSize(startPosition + connection.getContentLength());
            } else {
                downloadRequest.setTotalSize(connection.getContentLength());
            }

        } catch (DataException e) {
            Log.e(TAG, "net error.", e);
            if (this.iDownloadControl != null) {
                this.iDownloadControl.onError(this.downloadRequest, DataErrorEnum.DOWNLOAD_NET_FAILED);
            }
            return;
        }

        InputStream in = null;
        RandomAccessFile randomAccessFile = null;
        try {
            byte[] buffer = new byte[4096];
            int length = 0;

            if (!tempFilePath.exists()) {
                tempFilePath.createNewFile();
            }
            in = connection.getInputStream();
            randomAccessFile = new RandomAccessFile(tempFilePath, "rwd");
            randomAccessFile.seek(startPosition);
            while (!stop && (length = in.read(buffer)) > 0) {
                randomAccessFile.write(buffer, 0, length);
                downloadRequest.setDownloadedSize(downloadRequest.getDownloadedSize() + length);
                int downloadProgress = (int) (downloadRequest.getDownloadedSize() * 100 / downloadRequest.getTotalSize());
                if (System.currentTimeMillis() - currentUpdateTime > this.updateTime || downloadProgress == 100) {
                    if (this.iDownloadControl != null) {
                        this.iDownloadControl.onProgress(this.downloadRequest, downloadProgress);
                    }
                    currentUpdateTime = System.currentTimeMillis();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "storage error.");
            if (this.iDownloadControl != null) {
                this.iDownloadControl.onError(this.downloadRequest, DataErrorEnum.DOWNLOAD_NET_FAILED);
            }
            return;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "storage error 2.");
                return;
            }
        }
        if (stop && !pause) {
            this.iDownloadControl.onCancel(this.downloadRequest);
        } else if (stop && pause) {
            this.iDownloadControl.onPause(this.downloadRequest);
        } else {
            String originFileName = downloadRequest.getOriginFileName();
            if (originFileName != null) {
                File localFile = new File(filePath, originFileName);
                if (localFile.exists()) {
                    localFile.delete();
                }
            }
            File localFile = new File(filePath, fileName);
            tempFilePath.renameTo(localFile);
            String value = "" + downloadRequest.getResId();
            Log.i("liweiping", "value = " + value + ",  localFile.getAbsolutePath() = " + localFile.getAbsolutePath());
            WDMSharPre.getSingle().setValue(localFile.getAbsolutePath(), value);
            if (this.iDownloadControl != null) {
                this.iDownloadControl.onFinished(this.downloadRequest);
            }
        }
    }

    public DownloadRequest getRequest() {
        return this.downloadRequest;
    }

    public void cancel() {
        stop = true;
        String filePath = downloadRequest.getFilePath();
        String fileName = downloadRequest.getFileName();
        File tempFilePath = new File(filePath, fileName + ".tmp");
        FileUtil.delete(tempFilePath);
        iDownloadControl.onCancel(downloadRequest);
    }

    public void pause() {
        stop = true;
        pause = true;
    }

    public int getProgress() {
        if (downloadRequest.getTotalSize() <= 0)
            return 0;
        else
            return (int) (downloadRequest.getDownloadedSize() * 100 / downloadRequest.getTotalSize());
    }
}
