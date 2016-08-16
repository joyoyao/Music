package com.way.downloadmanager.lib;

import android.content.Context;

public class SdkLib {

    private static SdkLib appLib = null;
    private Context applicationContext;

    public static SdkLib instance() {
        if (null == appLib) {
            appLib = new SdkLib();
        }
        return appLib;
    }

    public Context getApplicationContext() {
        return this.applicationContext;
    }

    public void setApplicationContext(Context context) {
        this.applicationContext = context;
    }
}
