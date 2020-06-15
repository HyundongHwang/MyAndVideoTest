package com.hhd.myandvideotest.mycamerarecord

import android.opengl.GLES20

class MyExtraRenderer : IMyExtraRenderer {
    override fun render(viewWidth: Int, viewHeight: Int, frameNum: Int) {
        // We "draw" with the scissor rect and clear calls.  Note this uses window coordinates.
        val colorSwitch = frameNum % 3
        when (colorSwitch) {
            0 -> GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
            1 -> GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
            2 -> GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f)
        }
        val xpos = (viewWidth * (frameNum % 100 / 100.0f)).toInt()
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
        GLES20.glScissor(xpos, 0, viewWidth / 32, viewHeight / 32)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
    }
}