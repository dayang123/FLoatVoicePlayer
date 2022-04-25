package com.yang.floatvoiceplayer.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

public class Utils {

    public static String formatVideoTime(int timestamp) {     // 时间格式   00:13    s
        String secondString;
        String minuteString;
        int second = timestamp % 60;
        int minute = (int)Math.floor(timestamp / 60);
        if(second == 0) {
            secondString = "00";
        } else if(second < 10) {
            secondString = "0" + second;
        } else  {
            secondString = "" + second;
        }
        if(minute == 0) {
            minuteString = "00";
        } else if(minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = "" + minute;
        }
        return minuteString + ":" + secondString;
    }

    public static boolean checkFloatPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return Settings.canDrawOverlays(context);
        } else {
            if (Settings.canDrawOverlays(context)) return true;
            try {
                WindowManager mgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (mgr == null) return false; //getSystemService might return null
                View viewToAdd = new View(context);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(0, 0, android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSPARENT);
                viewToAdd.setLayoutParams(params);
                mgr.addView(viewToAdd, params);
                mgr.removeView(viewToAdd);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
