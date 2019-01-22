package com.hhd.myandvideotest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.concurrent.CountDownLatch;

public class MyTestActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = this.getBaseContext();
        ViewGroup.LayoutParams lpMpMp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ScrollView svRoot = new ScrollView(context);
        svRoot.setLayoutParams(lpMpMp);
        this.setContentView(svRoot);

        LinearLayout llRoot = MyTestUiUtil.createTestLinearLayout(this.getBaseContext(), this);
        svRoot.addView(llRoot);
    }


    public void _test_CameraManager_Char(MyTestUiUtil.ILogger logger) {
        try {
            logger.clear();
            CameraManager mgr = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            String camId = mgr.getCameraIdList()[0];
            CameraCharacteristics chars = mgr.getCameraCharacteristics(camId);
            Integer level = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            boolean is_INFO_SUPPORTED_HARDWARE_LEVEL_FULL = level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
            logger.write("is_INFO_SUPPORTED_HARDWARE_LEVEL_FULL[%b]", is_INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void _test_check_camera(MyTestUiUtil.ILogger logger) {
        Context context = this.getBaseContext();
        boolean has_FEATURE_CAMERA = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        logger.clear();
        logger.write("has_FEATURE_CAMERA[%b]", has_FEATURE_CAMERA);
    }

    public void _test_hello(MyTestUiUtil.ILogger logger) {
        logger.clear();
        logger.write("hello %d", 0);
        logger.write("hello %d", 1);
        logger.write("hello %d", 2);
    }

    private CountDownLatch _latch;

    public void _test_CountDownLatch(MyTestUiUtil.ILogger logger) {
        logger.clear();
        _latch = new CountDownLatch(1);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                logger.write("_latch.getCount()[%d] 001", _latch.getCount());
                _latch.countDown();
                logger.write("_latch.getCount()[%d] 002", _latch.getCount());
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        try {
            logger.write("_latch.await before 003");
            _latch.await();
            logger.write("_latch.await before 004");
        } catch (Exception e) {
            logger.write(e.toString());
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

                logger.write("_latch.getCount()[%d] 100", _latch.getCount());
                _latch.countDown();
                logger.write("_latch.getCount()[%d] 101", _latch.getCount());
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        try {
            logger.write("_latch.await before 103");
            _latch.await();
            logger.write("_latch.await before 104");
        } catch (Exception e) {
            logger.write(e.toString());
        }

    }


}



