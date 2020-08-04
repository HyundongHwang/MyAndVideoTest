package com.hhd.myandvideotest.myvideoedit

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.hhd.myandvideotest.mycamerarecord.GlUtil
import java.nio.Buffer

class MyTexture {
    enum class Type(
        val value: Int
    ) {
        TEXTURE_2D(GLES20.GL_TEXTURE_2D),
        TEXTURE_EXT(GLES11Ext.GL_TEXTURE_EXTERNAL_OES),
        TEXTURE_CUBE(GLES20.GL_TEXTURE_CUBE_MAP),
    }

    enum class Format(
        val value: Int
    ) {
        UNKNOWN(0),
        RGB(GLES20.GL_RGB),
        RGB888(GLES20.GL_RGB),  // equivalent to RGB
        RGBA(GLES20.GL_RGBA),
        RGBA8888(GLES20.GL_RGBA),  // equivalent to RGBA
        LUMINANCE(GLES20.GL_LUMINANCE),
        LUMINANCE_ALPHA(GLES20.GL_LUMINANCE_ALPHA),
        ALPHA(GLES20.GL_ALPHA);
    }

    var mHandle = 0
        private set

    var mFormat = Format.UNKNOWN
        private set

    var mInternalFormat = Format.UNKNOWN // This MUST match 'format'
        private set

    var mType: Type = Type.TEXTURE_2D
        private set

    var mWidth = 0
        private set

    var mHeight = 0
        private set

//    private val mWrapS: renderengine.Texture.Wrap =
//        renderengine.Texture.Wrap.CLAMP
//    private val mWrapT: renderengine.Texture.Wrap =
//        renderengine.Texture.Wrap.CLAMP
//    private val mMinFilter: renderengine.Texture.Filter =
//        renderengine.Texture.Filter.LINEAR
//    private val mMagFilter: renderengine.Texture.Filter =
//        renderengine.Texture.Filter.LINEAR
//    private val mCondition: EGLSyncKHR = EGL14Ext.EGL_NO_SYNC_KHR

    companion object {

        fun create(
            format: Format,
            width: Int,
            height: Int,
            data: Buffer?
        ): MyTexture {
            return create(
                Type.TEXTURE_2D,
                format,
                width,
                height,
                data
            )
        }

        fun create(
            type: Type,
            width: Int,
            height: Int
        ): MyTexture {
            return create(
                type,
                Format.RGBA,
                width,
                height,
                null
            )
        }

        fun create(
            type: Type,
            format: Format,
            width: Int,
            height: Int,
            data: Buffer?
        ): MyTexture {
            val textureId: Int = MyGlUtil.createTextureHandle(type.value)
            if (type == Type.TEXTURE_2D) {
                GLES20.glTexImage2D(
                    type.value,
                    0,
                    format.value,
                    width,
                    height,
                    0,
                    format.value,
                    GLES20.GL_UNSIGNED_BYTE, data
                )
                GlUtil.checkGlError("glTexImage2D")
            }
            val texture = MyTexture()
            texture.mHandle = textureId
            texture.mFormat = format
            texture.mInternalFormat = format
            texture.mType = type
            texture.mWidth = width
            texture.mHeight = height
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            MyGlUtil.checkGlError("glBindTexture")
            return texture
        }
    }

    fun test() {
        val t = Type.TEXTURE_2D
    }
}

