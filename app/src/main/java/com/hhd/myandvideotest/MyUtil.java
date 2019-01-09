package com.hhd.myandvideotest;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MyUtil {
    private static Float _DP1;
    private static Float _SP1;

    public static float get1DP(Context context) {
        if (_DP1 == null) {
            _DP1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());
        }

        return _DP1;
    }

    public static float get1SP(Context context) {
        if (_SP1 == null) {
            _SP1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1, context.getResources().getDisplayMetrics());
        }

        return _SP1;
    }

    public static boolean isStringNullOrEmpty(String str) {
        if (str == null)
            return true;

        if (str.isEmpty())
            return true;

        return false;
    }

    public static void writeTestBtnLog(View v, String format, Object... args) {
        Log.d("MYUTIL", String.format(format, args));

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                TextView tv = (TextView) v.getTag();
                String oldLog = tv.getText().toString();
                String newLog = String.format(format, args);
                String log;

                if (MyUtil.isStringNullOrEmpty(oldLog)) {
                    log = newLog;
                } else {
                    log = String.format("%s\n%s", oldLog, newLog);
                }

                tv.setText(log);
            }
        });
    }

    public static void clearTestBtnLog(View v) {
        TextView tv = (TextView) v.getTag();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                tv.setText("");
            }
        });
    }

    public static void showToast(Context context, String format, Object... args) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String msg = String.format(format, args);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

}
