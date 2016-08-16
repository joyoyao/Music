package com.way.downloadmanager.net.exception;

public enum DataErrorEnum {

    DOWNLOAD_NET_FAILED(2001, "下载时网络连接失败"),

    DOWNLOAD_STORAGE_FAILED(2002, "下载时存储失败"),

    DOWNLOAD_FILE_NOT_EXISTS(2003, "下载文件不存在");

    private int code;

    private String message;

    private DataErrorEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
