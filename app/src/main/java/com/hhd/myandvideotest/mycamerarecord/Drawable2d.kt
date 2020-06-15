package com.hhd.myandvideotest.mycamerarecord

import java.nio.FloatBuffer

class Drawable2d {
    private val SIZEOF_FLOAT = 4

    /**
     * Simple equilateral triangle (1.0 per side).  Centered on (0,0).
     */
    private val TRIANGLE_COORDS = floatArrayOf(
            0.0f, 0.577350269f,  // 0 top
            -0.5f, -0.288675135f,  // 1 bottom left
            0.5f, -0.288675135f // 2 bottom right
    )
    private val TRIANGLE_TEX_COORDS = floatArrayOf(
            0.5f, 0.0f,  // 0 top center
            0.0f, 1.0f,  // 1 bottom left
            1.0f, 1.0f)
    private val TRIANGLE_BUF: FloatBuffer = GlUtil.createFloatBuffer(TRIANGLE_COORDS)
    private val TRIANGLE_TEX_BUF: FloatBuffer = GlUtil.createFloatBuffer(TRIANGLE_TEX_COORDS)


    //단순 사각형으로 삼각형 스트립으로 지정됩니다.
    //정사각형은 (0,0)을 중심으로하며 크기는 1x1입니다.
    //삼각형은 0-1-2 및 2-1-3 (시계 반대 방향 감기)입니다.
    private val RECTANGLE_COORDS = floatArrayOf(
            -0.5f, -0.5f,  // 0 bottom left
            0.5f, -0.5f,  // 1 bottom right
            -0.5f, 0.5f,  // 2 top left
            0.5f, 0.5f)
    private val RECTANGLE_TEX_COORDS = floatArrayOf(
            0.0f, 1.0f,  // 0 bottom left
            1.0f, 1.0f,  // 1 bottom right
            0.0f, 0.0f,  // 2 top left
            1.0f, 0.0f // 3 top right
    )
    private val RECTANGLE_BUF: FloatBuffer = GlUtil.createFloatBuffer(RECTANGLE_COORDS)
    private val RECTANGLE_TEX_BUF: FloatBuffer = GlUtil.createFloatBuffer(RECTANGLE_TEX_COORDS)

    //"전체"정사각형으로, 두 차원 모두에서 -1에서 +1까지입니다.
    //modelviewprojection 행렬이 동일하면 뷰포트를 정확하게 덮습니다.
    //텍스처 좌표는 RECTANGLE을 기준으로 Y 반전됩니다.
    //(이것은 SurfaceTexture의 외부 텍스처에서 올바르게 작동하는 것 같습니다.)
    private val FULL_RECTANGLE_COORDS = floatArrayOf(
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f)
    private val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(
            0.0f, 0.0f,  // 0 bottom left
            1.0f, 0.0f,  // 1 bottom right
            0.0f, 1.0f,  // 2 top left
            1.0f, 1.0f // 3 top right
    )
    private val FULL_RECTANGLE_BUF: FloatBuffer = GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS)
    private val FULL_RECTANGLE_TEX_BUF: FloatBuffer = GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS)


    private var mVertexArray: FloatBuffer? = null
    private var mTexCoordArray: FloatBuffer? = null
    private var mVertexCount = 0
    private var mCoordsPerVertex = 0
    private var mVertexStride = 0
    private var mTexCoordStride = 0
    private var mPrefab: Prefab? = null

    /**
     * Enum values for constructor.
     */
    enum class Prefab {
        TRIANGLE, RECTANGLE, FULL_RECTANGLE
    }

    //"조립식"형상 정의에서 드로어 블을 준비합니다.
    //EGLGL 조작이 없으므로 언제든지 수행 할 수 있습니다.}
//    fun Drawable2d(shape: Prefab) {
    constructor(shape: Prefab) {
        when (shape) {
            Prefab.TRIANGLE -> {
                mVertexArray = TRIANGLE_BUF
                mTexCoordArray = TRIANGLE_TEX_BUF
                mCoordsPerVertex = 2
                mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT
                mVertexCount = TRIANGLE_COORDS.size / mCoordsPerVertex
            }
            Prefab.RECTANGLE -> {
                mVertexArray = RECTANGLE_BUF
                mTexCoordArray = RECTANGLE_TEX_BUF
                mCoordsPerVertex = 2
                mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT
                mVertexCount = RECTANGLE_COORDS.size / mCoordsPerVertex
            }
            Prefab.FULL_RECTANGLE -> {
                mVertexArray = FULL_RECTANGLE_BUF
                mTexCoordArray = FULL_RECTANGLE_TEX_BUF
                mCoordsPerVertex = 2
                mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT
                mVertexCount = FULL_RECTANGLE_COORDS.size / mCoordsPerVertex
            }
            else -> throw RuntimeException("Unknown shape $shape")
        }
        mTexCoordStride = 2 * SIZEOF_FLOAT
        mPrefab = shape
    }

    //꼭짓점의 배열을 반환합니다.
    //할당을 피하기 위해 내부 상태를 반환합니다. 호출자는 수정해서는 안됩니다.}
    fun getVertexArray(): FloatBuffer? {
        return mVertexArray
    }

    //텍스처 좌표의 배열을 반환합니다.
    //할당을 피하기 위해 내부 상태를 반환합니다.
    //호출자는 수정해서는 안됩니다.
    fun getTexCoordArray(): FloatBuffer? {
        return mTexCoordArray
    }

    /**
     * Returns the number of vertices stored in the vertex array.
     */
    fun getVertexCount(): Int {
        return mVertexCount
    }

    /**
     * Returns the width, in bytes, of the data for each vertex.
     */
    fun getVertexStride(): Int {
        return mVertexStride
    }

    /**
     * Returns the width, in bytes, of the data for each texture coordinate.
     */
    fun getTexCoordStride(): Int {
        return mTexCoordStride
    }

    /**
     * Returns the number of position coordinates per vertex.  This will be 2 or 3.
     */
    fun getCoordsPerVertex(): Int {
        return mCoordsPerVertex
    }

    override fun toString(): String {
        return if (mPrefab != null) {
            "[Drawable2d: $mPrefab]"
        } else {
            "[Drawable2d: ...]"
        }
    }
}