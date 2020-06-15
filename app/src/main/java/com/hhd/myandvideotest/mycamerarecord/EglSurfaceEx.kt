package com.hhd.myandvideotest.mycamerarecord

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES20
import android.view.Surface
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EglSurfaceEx {
    private var _releaseWinSurface: Boolean = false
    private var _winSurface: Surface? = null
    private var _eglSurface = EGL14.EGL_NO_SURFACE
    private var _width = -1;
    private var _height = -1;

    constructor(offscreenWidth : Int, offscreenHeight : Int) {
        this.createOffscreenSurface(offscreenWidth, offscreenHeight)
    }

    constructor(winSurface: Surface, releaseWinSurface : Boolean) {
        _winSurface = winSurface
        _releaseWinSurface = releaseWinSurface
        this.createWindowSurface(winSurface)
    }

    constructor(st : SurfaceTexture) {
        this.createWindowSurface(st)
    }

    fun createWindowSurface(surface : Any) {
        if (_eglSurface != EGL14.EGL_NO_SURFACE)
            throw Exception("if (_eglSurface != EGL14.EGL_NO_SURFACE)")

        _eglSurface = EglCoreUtil.createWindowSurface(surface)
    }

    fun createOffscreenSurface(width: Int, height: Int) {
        if (_eglSurface != EGL14.EGL_NO_SURFACE)
            throw Exception("if (_eglSurface != EGL14.EGL_NO_SURFACE)")

        _eglSurface = EglCoreUtil.createOffscreenSurface(width, height)
        _width = width
        _height = height
    }

    fun getWidth(): Int {
        if (_width < 0) {
            val currentWidth = EglCoreUtil.querySurface(_eglSurface, EGL14.EGL_WIDTH)
            return currentWidth
        }

        return _width
    }

    fun getHeight(): Int {
        if (_height < 0) {
            val currentHeight = EglCoreUtil.querySurface(_eglSurface, EGL14.EGL_HEIGHT)
            return currentHeight
        }

        return _height
    }

    fun release() {
        EglCoreUtil.releaseSurface(_eglSurface)
        _eglSurface = EGL14.EGL_NO_SURFACE
        _width = -1
        _height = -1

        if (_winSurface != null) {
            if (_releaseWinSurface) {
                _winSurface!!.release()
            }

            _winSurface = null
        }
    }

    fun makeCurrent() {
        EglCoreUtil.makeCurrent(_eglSurface)
    }

    fun makeCurrentReadFrom(readSurface : EglSurfaceEx) {
        EglCoreUtil.makeCurrent(_eglSurface, readSurface._eglSurface)
    }

    fun swapBuffers(): Boolean {
        val res = EglCoreUtil.swapBuffers(_eglSurface)
        return res
    }

    fun setPresentationTime(nsec : Long) {
        EglCoreUtil.setPresentationTime(_eglSurface, nsec)
    }

    fun saveFrame(file : File) {
        if (!EglCoreUtil.isCurrent(_eglSurface))
            throw Exception("if (!EglCoreUtil.isCurrent(_eglSurface))")

        val width = this.getWidth()
        val height = this.getHeight()
        val buf = ByteBuffer.allocateDirect(width * height * 4)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
        GlUtil.checkGlErr("GLES20.glReadPixels")
        buf.rewind()

        val bos = BufferedOutputStream(FileOutputStream(file))

        bos.use {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buf)
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos)
            bmp.recycle()
        }
    }
}

