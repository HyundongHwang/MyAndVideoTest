package com.hhd.myandvideotest;

import android.util.Log;

public class LogSloth {

    public static void enter() {
        LogSloth.getInstance().writeLog("d", "↘↘↘");
    }

    public static void enter(String strTag,
                             String strFuncName) {
        LogSloth.getInstance().writeLog("d", strTag, strFuncName, "↘↘↘");
    }

    public static void leave() {
        LogSloth.getInstance().writeLog("d", "↗↗↗");
    }

    public static void leave(String strTag,
                             String strFuncName) {
        LogSloth.getInstance().writeLog("d", strTag, strFuncName, "↗↗↗");
    }

    public static void call(String strFuncCalleeName) {
        LogSloth.getInstance().writeLog("d", "→→→%s→→→", strFuncCalleeName);
    }

    public static void call(String strTag,
                            String strFuncName,
                            String strFuncCalleeName) {
        LogSloth.getInstance().writeLog("d", strTag, strFuncName, "→→→%s→→→", strFuncCalleeName);
    }

    public static void d(String strFormat,
                         Object... args) {
        LogSloth.getInstance().writeLog("d", strFormat, args);
    }

    public static void d(String strTag,
                         String strFuncName,
                         String strFormat,
                         Object... args) {
        LogSloth.getInstance().writeLog("d", strTag, strFuncName, strFormat, args);
    }

    public static void w(String strTag,
                         String strFuncName,
                         String strFormat,
                         Object... args) {
        LogSloth.getInstance().writeLog("w", strTag, strFuncName, strFormat, args);
    }

    public static void e(String strTag,
                         String strFuncName,
                         String strFormat,
                         Object... args) {
        LogSloth.getInstance().writeLog("e", strTag, strFuncName, strFormat, args);
    }

    public static void i(String strTag,
                         String strFuncName,
                         String strFormat,
                         Object... args) {
        LogSloth.getInstance().writeLog("i", strTag, strFuncName, strFormat, args);
    }

    public static void v(String strTag,
                         String strFuncName,
                         String strFormat,
                         Object... args) {
        LogSloth.getInstance().writeLog("v", strTag, strFuncName, strFormat, args);
    }


    public void writeLog(
            String strLogLevel,
            String strFormat,
            Object... args) {

        long tid = Thread.currentThread().getId();
        long tsEpochMs = System.currentTimeMillis();
        StackTraceElement[] stList = Thread.currentThread().getStackTrace();

        if (stList.length < 5)
            return;


        StackTraceElement st = stList[4];

        writeLog("d",
                tsEpochMs,
                tid,
                st.getFileName(),
                st.getLineNumber(),
                st.getMethodName(),
                strFormat,
                args);
    }


    public void writeLog(
            String strLogLevel,
            String strTag,
            String strFuncName,
            String strFormat,
            Object... args) {

        if (!BuildConfig.DEBUG)
            return;


        long tid = Thread.currentThread().getId();
        long tsEpochMs = System.currentTimeMillis();
        String strFileName = String.format("%s.java", strTag);

        writeLog(strLogLevel,
                tsEpochMs,
                tid,
                strFileName,
                0,
                strFuncName,
                strFormat,
                args);
    }

    public void writeLog(
            String strLogLevel,
            long uTimeStampEpochMs,
            long uTid,
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

        String strTag = strFileName.toUpperCase();

        if (strTag.length() > 10) {
            strTag = strTag.substring(0, 10);
        }

        String strFormatedLog = String.format("%s:%d:%s %s",
                strFileName,
                nLineNum,
                strFuncName,
                strLog);

        if (strLogLevel.equals("v"))
            Log.v(strTag, strFormatedLog);
        else if (strLogLevel.equals("d"))
            Log.d(strTag, strFormatedLog);
        else if (strLogLevel.equals("i"))
            Log.i(strTag, strFormatedLog);
        else if (strLogLevel.equals("w"))
            Log.w(strTag, strFormatedLog);
        else if (strLogLevel.equals("e"))
            Log.e(strTag, strFormatedLog);
        else
            Log.d(strTag, strFormatedLog);

    }


    private final static Object _lk_this = new Object();
    private static LogSloth _this;

    public static LogSloth getInstance() {
        synchronized (_lk_this) {
            if (_this == null)
                _this = new LogSloth();

            return _this;
        }
    }
}
