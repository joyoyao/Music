package com.way.downloadmanager.lib;

import android.util.Log;

public final class ValueUtil {

    public static int convertToInt(Object value) {
        try {
            return Integer.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToInt");
            return 0;
        }
    }

    public static int convertToInt(Object value, int defaultValue) {
        try {
            return Integer.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToInt");
            return defaultValue;
        }
    }

    public static long convertToLong(Object value) {
        try {
            return Long.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToLong");
            return 0;
        }
    }

    public static long convertToLong(Object value, long defaultValue) {
        try {
            return Long.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToLong");
            return defaultValue;
        }
    }

    public static float convertToFloat(Object value) {
        try {
            return Float.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToFloat");
            return 0;
        }
    }

    public static float convertToFloat(Object value, float defaultValue) {
        try {
            return Float.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToFloat2");
            return defaultValue;
        }
    }

    public static double convertToDouble(Object value) {
        try {
            return Double.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToDouble");
            return 0;
        }
    }

    public static double convertToDouble(Object value, double defaultValue) {
        try {
            return Double.valueOf(value.toString());
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToDouble2");
            return defaultValue;
        }
    }

    public static boolean convertToBoolean(Object value) {
        try {
            String strValue = StringUtil.trimTailSpaces(value.toString());
            if (StringUtil.isEmpty(strValue)) {
                return false;
            }
            if ("true".equalsIgnoreCase(strValue)) {
                return true;
            }
            if (Integer.valueOf(strValue) != 0) {
                return true;
            }
        } catch (Exception e) {
            Log.v(LogConst.TAG_VALUE, "ValueUtil.convertToBoolean");
            return false;
        }
        return false;
    }
}
