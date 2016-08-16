package com.way.downloadmanager;

import android.content.Context;
import android.content.SharedPreferences;

public final class WDMSharPre {
    public static final String PREFS = "WDM_itemID_filepath";
    private static final Object[] mLock = new Object[0];
    public static WDMSharPre single = null;
    private SharedPreferences sf = null;

    private WDMSharPre(Context c) {
        super();
        sf = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static WDMSharPre init(Context c) {
        synchronized (mLock) {
            return single == null ? single = new WDMSharPre(c) : single;
        }
    }

    public static WDMSharPre getSingle() {
        return single;
    }

    public String getValue(String name, String defValue) {
        String value = sf.getString(name, defValue);
        return value;
    }

    public String setValue(String name, String value) {
        sf.edit().putString(name, value).commit();
        return value;
    }
}