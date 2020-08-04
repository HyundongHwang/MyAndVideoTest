package com.hhd.myandvideotest.myvideoedit

import android.opengl.GLES20
import android.util.Log
import com.hhd.myandvideotest.util.LogEx

class MyFrameBuffer {

    companion object {
        fun create(id: String): MyFrameBuffer? {
            return create(id, 0, 0)
        }

        fun create(
            id: String,
            width: Int,
            height: Int
        ): MyFrameBuffer? {
            return this.create(
                id,
                width,
                height,
                MyTexture.Format.RGBA
            )
        }

        fun create(
            id: String,
            width: Int,
            height: Int,
            format: MyTexture.Format
        ): MyFrameBuffer? {
            var renderTarget: MyTexture? = null
            if (width > 0 && height > 0) {
                // Create a default RenderTarget with same ID.
                renderTarget = MyTexture.create(format, width, height, null)
                if (renderTarget == null) {
                    LogEx.e("Failed to create render target for frame buffer.")
                    return null
                }
            }

            // Create the frame buffer
            val handle = intArrayOf(0)
            GLES20.glGenFramebuffers(1, handle, 0)
            MyGlUtil.checkGlError("glGenFramebuffers")
            //hhdtodo
//            val frameBuffer: MyFrameBuffer =
//                MyFrameBuffer(
//                    id,
//                    width,
//                    height,
//                    handle[0]
//                )
//
//            // Create the render target array for the new frame buffer
//            val `val` = intArrayOf(0)
//            GLES20.glGetIntegerv(
//                MyFrameBuffer.GL_MAX_COLOR_ATTACHMENTS,
//                `val`,
//                0
//            )
//            if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
//                `val`[0] = 1
//            }
//            val maxRenderTargets = Math.max(1, `val`[0])
//            frameBuffer.mRenderTargets = arrayOfNulls<RenderTarget>(maxRenderTargets)
//            if (renderTarget != null) {
//                frameBuffer.setRenderTarget(renderTarget, 0)
//            }
//            frameBuffer.unbindWithDetach()
//            return frameBuffer

            return null
        }
    }

}