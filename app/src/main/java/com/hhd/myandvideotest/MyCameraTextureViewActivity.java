package com.hhd.myandvideotest;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MyCameraTextureViewActivity extends AppCompatActivity {
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
        TextureView textureView = new TextureView(context);
        textureView.setLayoutParams(lpMpMp);
        textureView.setKeepScreenOn(true);
        this.setContentView(textureView);
        _latch_pictureTaken = new CountDownLatch(0);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                _onSurfaceTextureAvailable(surface, width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                _onSurfaceTextureDestroyed(surface);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _onClick_surfaceView();
            }
        });
    }

    private void _onSurfaceTextureDestroyed(SurfaceTexture surface) {
        _camera.stopPreview();
        _camera.release();
    }

    private void _onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            _camera = Camera.open();
            _camera.setPreviewTexture(surface);
            Camera.Parameters params = _camera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();

            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            params.setJpegQuality(100);

            if (params.isZoomSupported()) {
                int curZoom = params.getZoom();
                int maxZoom = params.getMaxZoom();
                int zoom = maxZoom > (curZoom + 2) ? curZoom + 1 : curZoom;
                params.setZoom(zoom);
            }

            _camera.setParameters(params);
            _camera.startPreview();
        } catch (Exception ex) {

        }
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
            _latch_pictureTaken = new CountDownLatch(1);
            _camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    }


}
