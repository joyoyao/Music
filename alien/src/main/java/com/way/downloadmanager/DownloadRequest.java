package com.way.downloadmanager;

public class DownloadRequest {
    private String fileName;
    private String originFileName;
    private String filePath;
    private String url;
    private long downloadedSize;
    private long totalSize;
    private Object resId;

    public DownloadRequest(Object resId, String fileName, String filePath, String url) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.url = url;
        this.resId = resId;
    }

    public Object getResId() {
        return resId;
    }

    public void setResId(Object resId) {
        this.resId = resId;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    /**
     * 0~100
     *
     * @return progress
     */
    public int getProgress() {
        if (totalSize == 0)
            return 0;
        return (int) (downloadedSize * 100 / totalSize);
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginFileName() {
        return originFileName;
    }

    public void setOriginFileName(String fileName) {
        this.originFileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
