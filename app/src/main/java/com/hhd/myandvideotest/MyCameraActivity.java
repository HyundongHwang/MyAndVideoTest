package com.hhd.myandvideotest;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;

public class MyCameraActivity extends AppCompatActivity {
    private Camera _camera;
    private CountDownLatch _latch_pictureTaken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        int numberOfCameras = Camera.getNumberOfCameras();

        if (numberOfCameras == 0)
            return;

        _camera = Camera.open(0);
        Context context = this.getBaseContext();
        ViewGroup.LayoutParams lpMpMp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        SurfaceView surfaceView = new SurfaceView(context);
        surfaceView.setLayoutParams(lpMpMp);
        surfaceView.setKeepScreenOn(true);
        this.setContentView(surfaceView);
        _latch_pictureTaken = new CountDownLatch(0);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                _surfaceCreated(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                _surfaceChanged(holder, format, width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                _surfaceDestroyed(holder);
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _onClick_surfaceView();
            }
        });
    }

    private void _onClick_surfaceView() {
        if (_camera == null)
            return;

        _camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                _jpeg_onPictureTaken(data, camera);
            }
        });
    }

    private void _jpeg_onPictureTaken(byte[] data, Camera camera) {
        if (_camera == null)
            return;

        try {
            _latch_pictureTaken.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        _latch_pictureTaken = new CountDownLatch(1);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Context context = getBaseContext();

                try {
                    File extDir = Environment.getExternalStorageDirectory();
                    File mytestDir = new File(extDir.getAbsolutePath() + "/mytest");
                    mytestDir.mkdirs();
                    String fileName = String.format("%d.jpg", System.currentTimeMillis());
                    File outFile = new File(mytestDir, fileName);
                    FileOutputStream fos = new FileOutputStream(outFile);
                    fos.write(data);
                    fos.flush();
                    fos.close();
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(outFile));
                    context.sendBroadcast(scanIntent);
                    MyUtil.showToast(context, "outFile[%s] data.length[%d]", outFile.getAbsoluteFile(), data.length);
                } catch (Exception e) {
                    MyUtil.showToast(context, e.toString());
                }
                _latch_pictureTaken.countDown();
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        _camera.startPreview();
    }


    private void _surfaceCreated(SurfaceHolder holder) {
        try {
            if (_camera == null)
                return;

            _camera.setPreviewDisplay(holder);
            _camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void _surfaceDestroyed(SurfaceHolder holder) {
        if (_camera == null)
            return;

        _camera.stopPreview();
    }



    private void _surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            if (_camera == null)
                return;

            _camera.stopPreview();
            _camera.setPreviewDisplay(holder);
            _camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
