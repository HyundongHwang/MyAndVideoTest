package com.hhd.myandvideotest.util;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;


public class LogEx {

    public interface IValueFormatter {
        boolean canFormat(Object value);
        String format(Object value);
    }

    private static final String TAG = LogEx.class.getSimpleName();

    public static void enter() {
        LogEx.writeLog(Log.DEBUG, "↘↘↘");
    }

    public static void leave() {
        LogEx.writeLog(Log.DEBUG, "↗↗↗");
    }

    public static void value(String valueDesc, Object value) {

        String valueStr = "";

        if (value == null) {
            valueStr = "null";
        } else if (value instanceof Integer ||
                value instanceof Float ||
                value instanceof Double ||
                value instanceof Boolean ||
                value instanceof Long ||
                value instanceof Short ||
                value instanceof Character ||
                value instanceof CharSequence) {

            valueStr = value.toString();
        } else {
            try {
                valueStr = MyUtil.toJsonStr(value);
            } catch (Exception ex) {
                valueStr = String.format("%s %s", value.getClass().getSimpleName(), ex.getClass().getSimpleName());
            }
        }

        String logStr = String.format("%s:::%s", valueDesc, valueStr);
        LogEx.writeLog(Log.DEBUG, logStr);
    }

    public static void caller(Object callParam) {
        String callParamStr;

        if (callParam == null) {
            callParamStr = "null";
        } else if (callParam instanceof Integer ||
                callParam instanceof Float ||
                callParam instanceof Double ||
                callParam instanceof Boolean ||
                callParam instanceof Long ||
                callParam instanceof Short ||
                callParam instanceof Character ||
                callParam instanceof CharSequence) {
            callParamStr = callParam.toString();
        } else {
            try {
                callParamStr = MyUtil.toJsonStr(callParam);
            } catch (Exception ex) {
                callParamStr = String.format("%s %s", callParam.getClass().getSimpleName(), ex.getClass().getSimpleName());
            }
        }

        String logStr = String.format("→→→%s→→→", callParamStr);
        LogEx.writeLog(Log.DEBUG, logStr);
    }

    public static void callee(Object callParam) {
        String callParamStr;

        if (callParam == null) {
            callParamStr = "null";
        } else if (callParam instanceof Integer ||
                callParam instanceof Float ||
                callParam instanceof Double ||
                callParam instanceof Boolean ||
                callParam instanceof Long ||
                callParam instanceof Short ||
                callParam instanceof Character ||
                callParam instanceof CharSequence) {
            callParamStr = callParam.toString();
        } else {
            try {
                callParamStr = MyUtil.toJsonStr(callParam);
            } catch (Exception ex) {
                callParamStr = String.format("%s %s", callParam.getClass().getSimpleName(), ex.getClass().getSimpleName());
            }
        }

        String logStr = String.format("←←←%s←←←", callParamStr);
        LogEx.writeLog(Log.DEBUG, logStr);
    }

    public static void d(String strFormat, Object... args) {
        LogEx.writeLog(Log.DEBUG, strFormat, args);
    }

    public static void w(String strFormat, Object... args) {
        LogEx.writeLog(Log.WARN, strFormat, args);
    }

    public static void e(String strFormat, Object... args) {
        LogEx.writeLog(Log.ERROR, strFormat, args);
    }

    public static void i(String strFormat, Object... args) {
        LogEx.writeLog(Log.INFO, strFormat, args);
    }

    public static void v(String strFormat, Object... args) {
        LogEx.writeLog(Log.VERBOSE, strFormat, args);
    }

    public static void exception(Exception ex) {
        LogEx.writeLog(Log.ERROR, Log.getStackTraceString(ex));
    }

    public static void empty() {
        LogEx.writeLog(Log.DEBUG, "");
    }

    public static void writeLog(
            int level,
            String strFormat,
            Object... args) {

        StackTraceElement[] stList = Thread.currentThread().getStackTrace();

        if (stList.length < 5)
            return;

        StackTraceElement st = stList[4];

        writeLog(level,
                st.getFileName(),
                st.getLineNumber(),
                st.getMethodName(),
                strFormat,
                args);
    }


    @SuppressLint("DefaultLocale")
    public static void writeLog(
            int level,
            String strFileName,
            int nLineNum,
            String strFuncName,
            String strFormat,
            Object... args) {

        String strLog = "";

        if (args != null && args.length > 0) {
            strLog = String.format(strFormat, args);
        } else {
            strLog = strFormat == null ? "" : strFormat;
        }

//        strLog = strLog.replace("\n", "↓↓↓");

        String strFormatedLog = String.format("%s:%d:%s %s %s",
                strFileName,
                nLineNum,
                strFuncName,
                Thread.currentThread().getName(),
                strLog);

        switch (level) {
            case Log.VERBOSE:
                Log.v(TAG, strFormatedLog);
                break;
            case Log.DEBUG:
                Log.d(TAG, strFormatedLog);
                break;
            case Log.INFO:
                Log.i(TAG, strFormatedLog);
                break;
            case Log.WARN:
                Log.w(TAG, strFormatedLog);
                break;
            case Log.ERROR:
                Log.e(TAG, strFormatedLog);
                break;
            case Log.ASSERT:
                Log.e(TAG, strFormatedLog);
                break;
            default:
                Log.d(TAG, strFormatedLog);
                break;
        }
    }


}
