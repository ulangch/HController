package com.ulangch.hcontroller.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by xyzc on 18-3-15.
 */

public class HUtils {

    private static final boolean DEBUG = true;

    public static void logD(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void logE(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void logE(String tag, String msg, Throwable t) {
        Log.e(tag, msg, t);
    }

    public static void showWarning(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showWarning(Context ctx, int resId) {
        Toast.makeText(ctx, resId, Toast.LENGTH_SHORT).show();
    }

    public static boolean isAddress(String addr) {
        if (!TextUtils.isEmpty(addr)) {
            String[] ele = addr.split(":");
            return ele.length == 6;
        }
        return false;
    }
}
