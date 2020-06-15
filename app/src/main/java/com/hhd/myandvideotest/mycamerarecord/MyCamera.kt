package com.hhd.myandvideotest.mycamerarecord

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.Surface

class MyCamera {


    var dispEglSurface: EglSurfaceEx? = null
        private set

    private var _cam: Camera? = null
    private var _camSurfaceTexture: SurfaceTexture? = null

    fun start(surface: Surface,
              videoWidth: Int,
              videoHeight: Int,
              videoFps: Int,
              isDisplayPortrait: Boolean,
              isCamFacingFront: Boolean
    ) {

        val numCam = Camera.getNumberOfCameras()

        val camFacing = if (isCamFacingFront)
            Camera.CameraInfo.CAMERA_FACING_FRONT else
            Camera.CameraInfo.CAMERA_FACING_BACK

        for (i in 0..numCam) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == camFacing) {
                _cam = Camera.open(i)
                break
            }
        }

        if (_cam == null)
            throw Exception("cam == null")


        val camParam = _cam!!.parameters
        val ppsfv = _cam!!.parameters.preferredPreviewSizeForVideo

        if (ppsfv == null)
            throw Exception("ppsfv == null")

        for (size in camParam.supportedVideoSizes) {
            if (size.width == videoWidth && size.height == videoHeight) {
                camParam.setPreviewSize(size.width, size.height)
                break
            }
        }

        camParam.setPreviewSize(ppsfv.width, ppsfv.height)

        val fps_1k = videoFps * 1_000

        for (entry in camParam.supportedPreviewFpsRange) {
            if (entry[0] == entry[1] && entry[0] == fps_1k) {
                camParam.setPreviewFpsRange(fps_1k, fps_1k)
                break
            }
        }

        camParam.setRecordingHint(true)
        _cam!!.parameters = camParam

        if (isDisplayPortrait) {
            _cam!!.setDisplayOrientation(90)
        } else {
            _cam!!.setDisplayOrientation(180)
        }

        EglCoreUtil.init(null, EglCoreUtil.FLAG_RECORDABLE)
        this.dispEglSurface = EglSurfaceEx(surface, false)
        this.dispEglSurface!!.makeCurrent()
    }

    fun startPreview(textureId: Int, frameAvailableCb: (SurfaceTexture) -> Unit) {
        _camSurfaceTexture = SurfaceTexture(textureId)
        _camSurfaceTexture!!.setOnFrameAvailableListener { st -> frameAvailableCb(st) }
        _cam!!.setPreviewTexture(_camSurfaceTexture)
        _cam!!.startPreview()
    }

    fun stop() {
        _cam!!.stopPreview()
        _cam!!.release()
        _cam = null

        _camSurfaceTexture!!.release()
        _camSurfaceTexture = null

        this.dispEglSurface!!.release()
        this.dispEglSurface = null

        EglCoreUtil.release()
    }
}

