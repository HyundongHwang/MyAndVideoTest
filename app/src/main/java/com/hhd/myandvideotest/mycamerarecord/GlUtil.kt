package com.hhd.myandvideotest.mycamerarecord

import android.opengl.GLES20
import android.opengl.Matrix
import com.hhd.myandvideotest.util.LogEx

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object GlUtil {
    fun checkGlErr(op: String) {
        val err = GLES20.glGetError()

        if (err == GLES20.GL_NO_ERROR)
            return

        val msg = "op[${op}] \n GLES20.glGetError[0x${Integer.toHexString(err)}]"
        throw Exception(msg)
    }

    private const val SIZEOF_FLOAT = 4

    //직접 부동 버퍼를 할당하고 부동 배열 데이터로 채 웁니다.
    fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        val bb = ByteBuffer.allocateDirect(coords.size * GlUtil.SIZEOF_FLOAT)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(coords)
        fb.position(0)
        return fb
    }

    //GLES 오류가 발생했는지 확인합니다.
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
            LogEx.e(msg)
            throw RuntimeException(msg)
        }
    }

    //우리가 얻은 위치가 유효한지 확인합니다.
    //라벨을 찾을 수 없지만 GL 오류를 설정하지 않은 경우 GLES는 -1을 반환합니다.
    //위치가 유효하지 않은 경우 RuntimeException을 Throw합니다.
    fun checkLocation(location: Int, label: String) {
        if (location < 0) {
            throw RuntimeException("Unable to locate '$label' in program")
        }
    }

    //제공된 버텍스 및 프래그먼트 셰이더에서 새 프로그램을 만듭니다.
    //@return 프로그램 핸들 또는 실패시 0.
    fun createProgram(vertexSource: String?, fragmentSource: String?): Int {
        val vertexShader: Int = GlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader: Int = GlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            return 0
        }
        var program = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program == 0) {
            LogEx.e("Could not create program")
        }
        GLES20.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        GLES20.glAttachShader(program, pixelShader)
        checkGlError("glAttachShader")
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LogEx.e("Could not link program: ")
            LogEx.e("GLES20.glGetProgramInfoLog(program)")
            GLES20.glDeleteProgram(program)
            program = 0
        }
        return program
    }

    //제공된 셰이더 소스를 컴파일합니다.
    //@return 셰이더에 대한 핸들 또는 실패시 0.
    fun loadShader(shaderType: Int, source: String?): Int {
        var shader = GLES20.glCreateShader(shaderType)
        checkGlError("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            LogEx.e("Could not compile shader $shaderType:")
            LogEx.e(GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    //일반적인 사용을위한 단위 행렬.
    //수정하지 않으면 인생이 이상해집니다.
    lateinit var IDENTITY_MATRIX: FloatArray

    init {
        IDENTITY_MATRIX = FloatArray(16)
        Matrix.setIdentityM(IDENTITY_MATRIX, 0)
    }
}