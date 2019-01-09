package com.hhd.myandvideotest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class MyTestActivity extends AppCompatActivity {


    public class CmdInfo {
        public String btnTxt;
        public View.OnClickListener clickListener;

        public CmdInfo() {
        }

        public CmdInfo(String btnTxt, View.OnClickListener clickListener) {
            this.btnTxt = btnTxt;
            this.clickListener = clickListener;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = this.getBaseContext();
        ViewGroup.LayoutParams lpMpMp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ScrollView svRoot = new ScrollView(context);
        svRoot.setLayoutParams(lpMpMp);
        this.setContentView(svRoot);
        LinearLayout llRoot = new LinearLayout(context);
        llRoot.setLayoutParams(lpMpMp);
        llRoot.setOrientation(LinearLayout.VERTICAL);
        svRoot.addView(llRoot);

        ArrayList<CmdInfo> cmdInfoList = new ArrayList<CmdInfo>();

        cmdInfoList.add(new CmdInfo("hello", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _hello(v);
            }
        }));

        cmdInfoList.add(new CmdInfo("check camera", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _check_camera(v);
            }
        }));

        cmdInfoList.add(new CmdInfo("CountDownLatch", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _CountDownLatch(v);
            }
        }));

        for (CmdInfo cmdInfo : cmdInfoList) {
            LinearLayout.LayoutParams lpMpWc = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            Button btn = new Button(context);
            btn.setLayoutParams(lpMpWc);
            btn.setTextColor(0xff000000);
            btn.setText(cmdInfo.btnTxt);
            btn.setOnClickListener(cmdInfo.clickListener);
            llRoot.addView(btn);

            TextView tv = new TextView(context);
            tv.setTextColor(0xff000000);
            tv.setLayoutParams(lpMpWc);
            llRoot.addView(tv);

            btn.setTag(tv);
        }
    }


    private void _check_camera(View v) {
        Context context = this.getBaseContext();
        boolean has_FEATURE_CAMERA = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        MyUtil.clearTestBtnLog(v);
        MyUtil.writeTestBtnLog(v, "has_FEATURE_CAMERA[%b]", has_FEATURE_CAMERA);
    }

    private void _hello(View v) {
        MyUtil.clearTestBtnLog(v);
        MyUtil.writeTestBtnLog(v, "hello %d", 0);
        MyUtil.writeTestBtnLog(v, "hello %d", 1);
        MyUtil.writeTestBtnLog(v, "hello %d", 2);
    }


    private CountDownLatch _latch;

    private void _CountDownLatch(View v) {
        MyUtil.clearTestBtnLog(v);
        _latch = new CountDownLatch(1);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                MyUtil.writeTestBtnLog(v, "_latch.getCount()[%d] 001", _latch.getCount());
                _latch.countDown();
                MyUtil.writeTestBtnLog(v, "_latch.getCount()[%d] 002", _latch.getCount());
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        try {
            MyUtil.writeTestBtnLog(v, "_latch.await before 003");
            _latch.await();
            MyUtil.writeTestBtnLog(v, "_latch.await after 004");
        } catch (Exception e) {
            MyUtil.writeTestBtnLog(v, e.toString());
        }


        _latch = new CountDownLatch(1);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                MyUtil.writeTestBtnLog(v, "_latch.getCount()[%d] 100", _latch.getCount());
                _latch.countDown();
                MyUtil.writeTestBtnLog(v, "_latch.getCount()[%d] 101", _latch.getCount());
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        try {
            MyUtil.writeTestBtnLog(v, "_latch.await before 103");
            _latch.await();
            MyUtil.writeTestBtnLog(v, "_latch.await after 104");
        } catch (Exception e) {
            MyUtil.writeTestBtnLog(v, e.toString());
        }

    }


}
