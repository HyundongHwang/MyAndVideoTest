package com.hhd.myandvideotest.mycamerarecord

import android.graphics.SurfaceTexture
import android.opengl.GLES20


class MyRenderer {

    val eglSurfaceList = mutableListOf<EglSurfaceEx>()
    val extraRendererList = mutableListOf<IMyExtraRenderer>()

    var textureId: Int = 0
        private set

    private var _frameNum: Int = 0
    private var _fullFrameRect: FullFrameRect? = null

    fun start() {
        _fullFrameRect = FullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
        textureId = _fullFrameRect!!.createTextureObject()
    }

    fun stop() {
        _fullFrameRect!!.release(false)
        _fullFrameRect = null

        _frameNum = 0
        this.textureId = -1
    }

    fun render(st: SurfaceTexture) {
        val tmpMatrix = FloatArray(16)
        st.getTransformMatrix(tmpMatrix)

        this.eglSurfaceList.forEach {
            val eglSurface = it
            val width = eglSurface.getWidth()
            val height = eglSurface.getHeight()
            eglSurface.makeCurrent()
            st.updateTexImage()
            GLES20.glViewport(0, 0, width, height)
            _fullFrameRect!!.drawFrame(this.textureId, tmpMatrix)

            this.extraRendererList.forEach {
                val extraRenderer = it
                extraRenderer.render(width, height, _frameNum)
            }

            eglSurface.swapBuffers()
        }

        _frameNum++
    }
}