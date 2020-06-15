package com.hhd.myandvideotest.mycamerarecord

import android.graphics.SurfaceTexture
import android.opengl.*
import android.view.Surface

object EglCoreUtil {
    //생성자 플래그 : 표면을 기록 할 수 있어야합니다.
    //이는 EGL이 비디오 인코더에서 사용 가능한 것으로 효율적으로 변환 할 수없는 픽셀 형식을 사용하지 못하게합니다.
    const val FLAG_RECORDABLE = 0x01

    //생성자 플래그 : GLES3을 요청하고, 사용할 수없는 경우 GLES2로 폴백합니다.
    //이 플래그가 없으면 GLES2가 사용됩니다.
    const val FLAG_TRY_GLES3 = 0x02

    // Android-specific extension.
    private const val EGL_RECORDABLE_ANDROID = 0x3142

    private var _eglCfg: EGLConfig? = null
    private var _eglCtx = EGL14.EGL_NO_CONTEXT
    private var _eglDisp = EGL14.EGL_NO_DISPLAY
    private var _eglVer: Int = -1

    fun init() {
        init(null, 0)
    }

    fun init(shEglCtx: EGLContext?, flags: Int) {
        if (_eglDisp != EGL14.EGL_NO_DISPLAY)
            throw Exception("_eglDisplay != EGL14.EGL_NO_DISPLAY \n init condition error")

        var eglCtx = shEglCtx ?: EGL14.EGL_NO_CONTEXT

        _eglDisp = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

        if (_eglDisp == EGL14.EGL_NO_DISPLAY)
            throw Exception("_eglDisp == EGL14.EGL_NO_DISPLAY \n eglGetDisplay fail")

        val verAr = IntArray(2)
        var res = EGL14.eglInitialize(_eglDisp, verAr, 0, verAr, 1)

        if (!res) {
            _eglDisp = null
            throw Exception("eglInitialize fail")
        }

        var cfg: EGLConfig?

        if (eglCtx == EGL14.EGL_NO_CONTEXT && flags and FLAG_TRY_GLES3 != 0) {
            cfg = _getConfig(flags, 3)
            if (cfg != null) {
                val attrb3Ar = intArrayOf(
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                        EGL14.EGL_NONE
                )

                eglCtx = EGL14.eglCreateContext(_eglDisp, cfg, eglCtx, attrb3Ar, 0)
                _chkEglErr("eglCreateContext")
                _eglCfg = cfg
                _eglCtx = eglCtx
                _eglVer = 3
            }
        }

        if (eglCtx == EGL14.EGL_NO_CONTEXT) {
            cfg = _getConfig(flags, 2)

            if (cfg == null)
                throw Exception("cfg == null \n _getConfig(flags, 2)")

            val attrb2Ar = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            )

            eglCtx = EGL14.eglCreateContext(_eglDisp, cfg, eglCtx, attrb2Ar, 0)
            _chkEglErr("eglCreateContext")
            _eglCfg = cfg
            _eglCtx = eglCtx
            _eglVer = 2
        }
    }

    fun release() {
        if (_eglDisp != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(_eglDisp, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(_eglDisp, _eglCtx)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(_eglDisp)
        }

        _eglCfg = null
        _eglCtx = EGL14.EGL_NO_CONTEXT
        _eglDisp = EGL14.EGL_NO_DISPLAY
        _eglVer = -1
    }

    fun releaseSurface(surface: EGLSurface) {
        EGL14.eglDestroySurface(_eglDisp, surface)
    }

    fun createWindowSurface(surface: Any): EGLSurface? {
        if (surface !is Surface && surface !is SurfaceTexture)
            throw RuntimeException("invalid surface: $surface")

        val surfaceAttrbAr = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(_eglDisp, _eglCfg, surface, surfaceAttrbAr, 0)
        _chkEglErr("eglCreateWindowSurface \n eglCreateWindowSurface")

        if (eglSurface == null)
            throw Exception("eglSurface == null")

        return eglSurface
    }

    fun createOffscreenSurface(width: Int, height: Int): EGLSurface? {
        val surfaceAr = intArrayOf(EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreatePbufferSurface(_eglDisp, _eglCfg, surfaceAr, 0)
        _chkEglErr("eglCreatePbufferSurface")

        if (eglSurface == null)
            throw Exception("eglSurface == null \n eglCreatePbufferSurface")

        return eglSurface
    }

    fun makeCurrent(eglSurface: EGLSurface) {
        val res = EGL14.eglMakeCurrent(_eglDisp, eglSurface, eglSurface, _eglCtx)

        if (!res)
            throw Exception("if (!res) \n eglMakeCurrent")
    }

    fun makeCurrent(drawSurface: EGLSurface, readSurface: EGLSurface) {
        val res = EGL14.eglMakeCurrent(_eglDisp, drawSurface, readSurface, _eglCtx)

        if (!res)
            throw Exception("if (!res) \n eglMakeCurrent")
    }

    fun makeNothingCurrent() {
        val res = EGL14.eglMakeCurrent(_eglDisp, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, _eglCtx)

        if (!res)
            throw Exception("if (!res) \n eglMakeCurrent \n makeNothingCurrent")
    }

    private fun _chkEglErr(msg: String) {
        val err = EGL14.eglGetError()

        if (err != EGL14.EGL_SUCCESS)
            throw Exception("${msg} : \n EGL error : 0x${Integer.toHexString(err)}")
    }

    fun swapBuffers(eglSurface: EGLSurface): Boolean {
        val res = EGL14.eglSwapBuffers(_eglDisp, eglSurface)
        return res
    }

    fun setPresentationTime(eglSurface: EGLSurface, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(_eglDisp, eglSurface, nsecs)
    }

    fun isCurrent(eglSurface: EGLSurface): Boolean {
        val res = _eglCtx == EGL14.eglGetCurrentContext() &&
                eglSurface == EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)

        return res
    }

    fun querySurface(eglSurface: EGLSurface, what: Int): Int {
        val intAr = IntArray(1)
        val res = EGL14.eglQuerySurface(_eglDisp, eglSurface, what, intAr, 0)
        return intAr[0]
    }

    fun queryString(what: Int): String {
        val res = EGL14.eglQueryString(_eglDisp, what)
        return res
    }

    fun getGlVersion(): Int {
        return _eglVer
    }

    fun getLogCurrent(msg: String): String {
        val disp = EGL14.eglGetCurrentDisplay()
        val ctx = EGL14.eglGetCurrentContext()
        val surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
        val resStr = "Current EGL \n msg[${msg}] \n disp[${disp}] \n ctx[${ctx}] \n surface[${surface}]"
        return resStr
    }

    private fun _getConfig(flags: Int, version: Int): EGLConfig? {
        val renderableType = EGL14.EGL_OPENGL_ES2_BIT

        val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,

                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        )

        val FLAG_RECORDABLE = 0x01
        val EGL_RECORDABLE_ANDROID = 0x3142

        if (flags and FLAG_RECORDABLE != 0) {
            attribList[attribList.size - 3] = EGL_RECORDABLE_ANDROID
            attribList[attribList.size - 2] = 1
        }

        val cfgAr = arrayOfNulls<EGLConfig>(1)
        val numCfgAr = IntArray(1)

        val res = EGL14.eglChooseConfig(
                _eglDisp,
                attribList,
                0,
                cfgAr,
                0,
                cfgAr.size,
                numCfgAr,
                0)

        if (!res)
            throw Exception("eglChooseConfig fail")

        val cfg = cfgAr[0]
        return cfg
    }
}

