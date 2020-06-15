package com.hhd.myandvideotest.mycamerarecord

class FullFrameRect {
    private val mRectDrawable = Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE)
    private var mProgram: Texture2dProgram? = null

    //객체를 준비합니다.
    //
    //@param program 사용할 프로그램.
    //FullFrameRect는 소유권을 가져옵니다.
    //더 이상 필요하지 않으면 프로그램을 해제합니다.
    //fun FullFrameRect(program: Texture2dProgram?) {
    constructor(program: Texture2dProgram?) {
        mProgram = program
    }


    //자원을 해제합니다.
    //
    //적절한 EGL 컨텍스트 전류로 호출해야합니다.
    //(즉, 생성자가 호출되었을 때 최신 상태 임).
    //EGL 컨텍스트를 파괴하려는 경우,
    //이 정리를 수행하기 위해 발신자를 현재 상태로 만드는 것은 가치가 없습니다.
    //따라서이 함수가 EGL 컨텍스트 특정 정리를 건너 뛰도록 지시하는 플래그를 전달할 수 있습니다.}
    fun release(doEglCleanup: Boolean) {
        if (mProgram != null) {
            if (doEglCleanup) {
                mProgram!!.release()
            }
            mProgram = null
        }
    }

    //현재 사용중인 프로그램을 반환합니다.
    fun getProgram(): Texture2dProgram? {
        return mProgram
    }

    //프로그램을 변경합니다.
    //이전 프로그램이 릴리스됩니다.
    //적절한 EGL 컨텍스트가 최신 상태 여야합니다.
    fun changeProgram(program: Texture2dProgram?) {
        mProgram!!.release()
        mProgram = program
    }

    //drawFrame ()과 함께 사용하기에 적합한 텍스처 객체를 만듭니다.
    fun createTextureObject(): Int {
        return mProgram!!.createTextureObject()
    }

    //지정된 텍스처 오브젝트로 텍스처링하여 뷰포트를 채우는 rect를 그립니다.
    fun drawFrame(textureId: Int, texMatrix: FloatArray?) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mProgram!!.draw(
                GlUtil.IDENTITY_MATRIX,
                mRectDrawable.getVertexArray(),
                0,
                mRectDrawable.getVertexCount(),
                mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix,
                mRectDrawable.getTexCoordArray(),
                textureId,
                mRectDrawable.getTexCoordStride())
    }
}

