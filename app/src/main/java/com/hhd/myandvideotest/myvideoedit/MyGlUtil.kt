package com.hhd.myandvideotest.myvideoedit

import android.opengl.GLES20

object MyGlUtil {

    fun createTextureHandle(target: Int): Int {
        return createTextureHandle(
            target,
            GLES20.GL_NEAREST,
            GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    fun createTextureHandle(
        target: Int,
        minFilter: Int,
        magFilter: Int,
        wrapS: Int,
        wrapT: Int
    ): Int {
        val handles = IntArray(1)
        GLES20.glGenTextures(1, handles, 0)
        checkGlError("glGenTextures")
        if (handles[0] <= 0) {
            throw Exception("Failed to gen textures")
        }
        val tex = handles[0]
        GLES20.glBindTexture(target, tex)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, minFilter)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, magFilter)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, wrapS)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, wrapT)
        checkGlError("setTextureParameters")
        return tex
    }

    fun checkGlError(op: String) {
        var error: Int
        if (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            val msg = op + " : glError " + getGlErrorStringWithHex(error)
            throw Exception(msg)
        }
    }

    fun getGlErrorStringWithHex(error: Int): String? {
        return getGlErrorString(error) + " (0x" + Integer.toHexString(error) + ")"
    }

    fun getGlErrorString(error: Int): String {
        return when (error) {
            GLES20.GL_NO_ERROR -> "GL_NO_ERROR"
            GLES20.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
            GLES20.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
            GLES20.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
            GLES20.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
            GLES20.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
            else -> "UNKNOWN"
        }
    }
}