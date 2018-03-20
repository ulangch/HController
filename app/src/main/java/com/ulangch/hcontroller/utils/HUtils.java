package com.ulangch.hcontroller.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by xyzc on 18-3-15.
 */

public class HUtils {

    private static final boolean DEBUG = true;

    private static final char[] ALPHABET = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f',
    };

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

    public static byte[] hexToBytes(String str) {
        if (str == null || (str.length() & 1) == 1) {
            throw new NumberFormatException("Odd length hex string: " + str.length());
        }
        byte[] data = new byte[str.length() >> 1];
        int position = 0;
        for (int n = 0; n < str.length(); n += 2) {
            data[position] =
                    (byte) (((fromHex(str.charAt(n)) & 0x0F) << 4) |
                            (fromHex(str.charAt(n + 1)) & 0x0F));
            position++;
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] buf = new char[bytes.length * 2];
        int c = 0;
        for (byte b : bytes) {
            buf[c++] = ALPHABET[(b >> 4) & 0xf];
            buf[c++] = ALPHABET[b & 0xf];
        }
        return new String(buf);
    }

    public static String byteToHex(byte b) {
        char[] buf = new char[2];
        buf[0] = ALPHABET[(b >> 4) & 0xf];
        buf[1] = ALPHABET[b & 0xf];
        return new String(buf);
    }

    public static int fromHex(char ch) throws NumberFormatException {
        if (ch <= '9' && ch >= '0') {
            return ch - '0';
        } else if (ch >= 'a' && ch <= 'f') {
            return ch + 10 - 'a';
        } else if (ch <= 'F' && ch >= 'A') {
            return ch + 10 - 'A';
        } else {
            throw new NumberFormatException("Bad hex-character: " + ch);
        }
    }

    public static boolean isHexString(String hex) {
        hex = hex.toLowerCase();
        for (char c : hex.toCharArray()) {
            if (c <= '9' && c >= '0' || c >= 'a' && c <= 'f') {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    public static String formatTime(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        return String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c);
    }

    public static String formatCurrentTime() {
        return formatTime(System.currentTimeMillis());
    }
}
