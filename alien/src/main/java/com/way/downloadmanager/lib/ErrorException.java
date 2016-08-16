package com.way.downloadmanager.lib;

import android.util.Log;

public final class ErrorException extends Exception {

    private static final long serialVersionUID = 4918321648798599467L;

    public ErrorException(Class<?> throwClass) {
        this(throwClass, null, null);
    }

    public ErrorException(Class<?> throwClass, String message) {
        this(throwClass, message, null);
    }

    public ErrorException(Class<?> throwClass, Throwable throwable) {
        this(throwClass, null, throwable);
    }

    public ErrorException(Class<?> throwClass, String message, Throwable throwable) {
        super(message, throwable);
        printException(throwClass, message, throwable);
    }

    public static void printException(Class<?> throwClass, String message, Throwable throwable) {
        if (!StringUtil.isEmpty(message)) {
            Log.w(LogConst.TAG_SDK, "DatabaseErrorException: " + message);
        }
        if (throwable != null) {
            Log.w(LogConst.TAG_SDK, "DatabaseErrorException: " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }
}
