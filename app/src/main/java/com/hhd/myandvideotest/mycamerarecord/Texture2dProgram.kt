package com.hhd.myandvideotest.mycamerarecord

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.hhd.myandvideotest.util.LogEx

import java.nio.FloatBuffer

class Texture2dProgram {
    enum class ProgramType {
        TEXTURE_2D, TEXTURE_EXT, TEXTURE_EXT_BW, TEXTURE_EXT_FILT
    }

    // Simple vertex shader, used for all programs.
    private val VERTEX_SHADER = """
uniform mat4 uMVPMatrix;
uniform mat4 uTexMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = (uTexMatrix * aTextureCoord).xy;
}
"""

    // Simple fragment shader for use with "normal" 2D textures.
    private val FRAGMENT_SHADER_2D = """
precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTextureCoord);
}
"""

    //외부 2D 텍스처와 함께 사용하기위한 간단한 조각 쉐이더
    //(예 : SurfaceTexture에서 얻는 것).
    private val FRAGMENT_SHADER_EXT = """
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTextureCoord);
}
"""

    //간단한 변환으로 색상을 흑백으로 변환하는 조각 쉐이더.
    private val FRAGMENT_SHADER_EXT_BW = """
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
void main() {
    vec4 tc = texture2D(sTexture, vTextureCoord);
    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;
    gl_FragColor = vec4(color, color, color, 1.0);
}
"""

    //컨벌루션 필터가있는 조각 쉐이더.
    //왼쪽 상단이 정상적으로 그려집니다.
    //오른쪽 아래 절반에 필터가 적용됩니다.
    //테두리에 얇은 빨간색 선이 그려집니다.
    //이것은 성능에 최적화되지 않았습니다.
    //.
    //더 빨라질 수있는 것들 :
    //.
    //-조건을 제거하십시오.
    //그들은 중간에 빨간 줄무늬가있는 반 &amp; 반보기를 제공하는 데 사용됩니다.
    //그러나 이것은 데모에만 유용합니다.
    //.
    //-루프를 풉니 다.
    //이상적으로 컴파일러는 유익 할 때이 작업을 수행합니다.
    //.
    //-필터 커널을 셰이더에 굽고
    //균일 한 배열을 통해 전달하는 대신.
    //루프 언 롤링과 결합하면 메모리 액세스가 줄어 듭니다.
    val KERNEL_SIZE = 9
    private val FRAGMENT_SHADER_EXT_FILT = """
#extension GL_OES_EGL_image_external : require
#define KERNEL_SIZE $KERNEL_SIZE
precision highp float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform float uKernel[KERNEL_SIZE];
uniform vec2 uTexOffset[KERNEL_SIZE];
uniform float uColorAdjust;
void main() {
    int i = 0;
    vec4 sum = vec4(0.0);
    if (vTextureCoord.x < vTextureCoord.y - 0.005) {
        for (i = 0; i < KERNEL_SIZE; i++) {
            vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);
            sum += texc * uKernel[i];
        }
        sum += uColorAdjust;
    } else if (vTextureCoord.x > vTextureCoord.y + 0.005) {
        sum = texture2D(sTexture, vTextureCoord);
    } else {
        sum.r = 1.0;
    }
    gl_FragColor = sum;
}
"""

    private var mProgramType: ProgramType? = null

    // GL 프로그램 및 다양한 구성 요소를 처리합니다.
    private var mProgramHandle = 0
    private var muMVPMatrixLoc = 0
    private var muTexMatrixLoc = 0
    private var muKernelLoc = 0
    private var muTexOffsetLoc = 0
    private var muColorAdjustLoc = 0
    private var maPositionLoc = 0
    private var maTextureCoordLoc = 0

    private var mTextureTarget = 0

    private val mKernel = FloatArray(KERNEL_SIZE)
    private lateinit var mTexOffset: FloatArray
    private var mColorAdjust = 0f


    //현재 EGL 컨텍스트에서 프로그램을 준비합니다.
    constructor(programType: ProgramType) {
        mProgramType = programType
        when (programType) {
            ProgramType.TEXTURE_2D -> {
                mTextureTarget = GLES20.GL_TEXTURE_2D
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D)
            }
            ProgramType.TEXTURE_EXT -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT)
            }
            ProgramType.TEXTURE_EXT_BW -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW)
            }
            ProgramType.TEXTURE_EXT_FILT -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT)
            }
            else -> throw RuntimeException("Unhandled type $programType")
        }
        if (mProgramHandle == 0) {
            throw RuntimeException("Unable to create program")
        }
        LogEx.e("Created program $mProgramHandle ($programType)")

        // get locations of attributes and uniforms
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition")
        GlUtil.checkLocation(maPositionLoc, "aPosition")
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord")
        GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord")
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix")
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix")
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix")
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix")
        muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel")
        if (muKernelLoc < 0) {
            // no kernel in this one
            muKernelLoc = -1
            muTexOffsetLoc = -1
            muColorAdjustLoc = -1
        } else {
            // has kernel, must also have tex offset and color adj
            muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset")
            GlUtil.checkLocation(muTexOffsetLoc, "uTexOffset")
            muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust")
            GlUtil.checkLocation(muColorAdjustLoc, "uColorAdjust")

            // initialize default values
            setKernel(floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f), 0f)
            setTexSize(256, 256)
        }
    }

    //프로그램을 해제합니다.
    //적절한 EGL 컨텍스트는 최신이어야합니다.
    //(즉, 프로그램을 만드는 데 사용 된 것).
    fun release() {
        LogEx.d("deleting program $mProgramHandle")
        GLES20.glDeleteProgram(mProgramHandle)
        mProgramHandle = -1
    }

    //프로그램 유형을 반환합니다.
    fun getProgramType(): ProgramType? {
        return mProgramType
    }

    //이 프로그램과 함께 사용하기에 적합한 텍스처 객체를 만듭니다.
    //종료하면 텍스처가 바인딩됩니다.
    fun createTextureObject(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GlUtil.checkGlError("glGenTextures")
        val texId = textures[0]
        GLES20.glBindTexture(mTextureTarget, texId)
        GlUtil.checkGlError("glBindTexture $texId")
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE)
        GlUtil.checkGlError("glTexParameter")
        return texId
    }

    //Configures the convolution filter values.
    //@param values Normalized filter values;
    //must be KERNEL_SIZE elements.
    fun setKernel(values: FloatArray, colorAdj: Float) {
        require(values.size == KERNEL_SIZE) {
            "Kernel size is " + values.size +
                    " vs. " + KERNEL_SIZE
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE)
        mColorAdjust = colorAdj
        //Log.d(TAG, "filt kernel: " + Arrays.toString(mKernel) + ", adj=" + colorAdj);
    }

    //텍스처의 크기를 설정합니다.
    //필터링 할 때 인접한 텍셀을 찾는 데 사용됩니다.
    fun setTexSize(width: Int, height: Int) {
        val rw = 1.0f / width
        val rh = 1.0f / height

        //여기에 새 배열을 만들 필요는 없지만 구문 상 편리합니다.
        mTexOffset = floatArrayOf(
                -rw, -rh, 0f, -rh, rw, -rh,
                -rw, 0f, 0f, 0f, rw, 0f,
                -rw, rh, 0f, rh, rw, rh
        )
        //Log.d(TAG, "filt size: " + width + "x" + height + ": " + Arrays.toString(mTexOffset));
    }

    //드로우 콜을 발행합니다. 모든 통화에서 전체 설정을 수행합니다.
    //@param mvpMatrix 4x4 투영 행렬입니다.
    //@param vertexBuffer 정점 위치 데이터가있는 버퍼.
    //@param firstVertex vertexBuffer에서 사용할 첫 번째 정점의 인덱스.
    //@param vertexCount vertexBuffer의 꼭짓점 수입니다.
    //@param coordsPerVertex 꼭짓점 당 좌표 수입니다 (예 : x, y는 2).
    //@param vertexStride 각 정점에 대한 위치 데이터의 폭 (바이트) (종종 vertexCount sizeof (float)).
    //@param texMatrix 텍스처 좌표를위한 4x4 변환 매트릭스. (주로 SurfaceTexture와 함께 사용하도록 설계되었습니다.)
    //@param texBuffer 정점 텍스처 데이터가있는 버퍼.
    //@param texStride 각 꼭짓점에 대한 텍스처 데이터의 폭 (바이트).
    fun draw(mvpMatrix: FloatArray?, vertexBuffer: FloatBuffer?, firstVertex: Int,
             vertexCount: Int, coordsPerVertex: Int, vertexStride: Int,
             texMatrix: FloatArray?, texBuffer: FloatBuffer?, textureId: Int, texStride: Int) {
        GlUtil.checkGlError("draw start")

        // Select the program.
        GLES20.glUseProgram(mProgramHandle)
        GlUtil.checkGlError("glUseProgram")

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(mTextureTarget, textureId)

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GlUtil.checkGlError("glUniformMatrix4fv")

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0)
        GlUtil.checkGlError("glUniformMatrix4fv")

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc)
        GlUtil.checkGlError("glEnableVertexAttribArray")

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)
        GlUtil.checkGlError("glVertexAttribPointer")

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc)
        GlUtil.checkGlError("glEnableVertexAttribArray")

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer)
        GlUtil.checkGlError("glVertexAttribPointer")

        // Populate the convolution kernel, if present.
        if (muKernelLoc >= 0) {
            GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0)
            GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0)
            GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust)
        }

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount)
        GlUtil.checkGlError("glDrawArrays")

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc)
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc)
        GLES20.glBindTexture(mTextureTarget, 0)
        GLES20.glUseProgram(0)
    }
}