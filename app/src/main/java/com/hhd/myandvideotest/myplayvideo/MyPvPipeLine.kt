package com.hhd.myandvideotest.myplayvideo

import android.view.Surface
import com.hhd.myandvideotest.util.MyUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File

class MyPvPipeLine {

    var isOpen: Boolean = false
        get() {
            if (_myVideoPlayer == null)
                return false

            return true
        }
        private set

    var isPlay: Boolean = false
        get() {
            if (_myVideoPlayer == null)
                return false

            return _myVideoPlayer!!.isPlay
        }
        private set

    var speedRatio: Double
        get() {
            if (_myVideoPlayer == null)
                return 1.0

            return _myVideoPlayer!!.speedRatio
        }
        set(value) {
            if (_myVideoPlayer == null)
                return

            _myVideoPlayer!!.speedRatio = value
        }

    var durationUs: Long = -1L
        get() {
            if (_myVideoPlayer == null)
                return -1L

            return _myVideoPlayer!!.durationUs
        }
        private set

    var videoWidth: Int = -1
        get() {
            if (_myVideoPlayer == null)
                return -1

            return _myVideoPlayer!!.width
        }
        private set

    var videoHeight: Int = -1
        get() {
            if (_myVideoPlayer == null)
                return -1

            return _myVideoPlayer!!.height
        }
        private set

    var videoRotation: Int = 0
        get() {
            if (_myVideoPlayer == null)
                return 0

            return _myVideoPlayer!!.rotation
        }
        private set

    var curPts: Long = -1L
        get() {
            if (_myVideoPlayer == null)
                return -1L

            return _myVideoPlayer!!.curPts
        }
        private set

    var isReverse: Boolean
        get() {
            if (_myVideoPlayer == null)
                return false

            return _myVideoPlayer!!.isReverse
        }
        set(value) {
            if (_myVideoPlayer == null)
                return

            _myVideoPlayer!!.isReverse = value
        }


    private var _myVideoPlayer: MyVideoPlayer? = null
    private val _pipeline = PublishSubject.create<Array<Any>>()

    private enum class _Commands {
        NONE,
        OPEN,
        CLOSE,
        SEEK,
        DECODE_RENDER,
        DECODE_RENDER_1FRAME,
        DECODE_RENDER_REVERSE,
        DECODE_RENDER_1FRAME_REVERSE,
        RENDER_IDX_OUT_BUF,
        UPDATE_UI,
        TEST,
    }


    constructor(updateUiCb: (MyPvPipeLine) -> Unit) {
        _pipeline
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                val paramArray = it as Array<*>
                val cmd = paramArray[0] as _Commands

                when (cmd) {
                    _Commands.UPDATE_UI -> {
                        updateUiCb(this)
                    }
                }
                return@map paramArray
            }
            .observeOn(MyUtil.newNamedScheduler("T_PLAYER"))
            .map {
                val paramArray = it as Array<*>
                val cmd = paramArray[0] as _Commands

                when (cmd) {
                    _Commands.OPEN -> {
                        val srcFile = paramArray[1] as File
                        val renderSurface = paramArray[2] as Surface
                        _myVideoPlayer = MyVideoPlayer()
                        _myVideoPlayer!!.prepare(srcFile, renderSurface)
                        _myVideoPlayer!!.seek(0)
                        _myVideoPlayer!!.decodeRender(true)
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.CLOSE -> {
                        if (_myVideoPlayer == null)
                            return@map null

                        _myVideoPlayer!!.stop()
                        _myVideoPlayer = null
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.SEEK -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        val pts = paramArray[1] as Long
                        _myVideoPlayer!!.seek(pts)
                        _myVideoPlayer!!.decodeRender(true)
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        if (!_myVideoPlayer!!.isPlay)
                            return@map paramArray

                        val res = _myVideoPlayer!!.decodeRender(true)

                        if (!res)
                            return@map paramArray

                        val waitTimeMs = _myVideoPlayer!!.getWaitTimeMs()

                        if (waitTimeMs > 0)
                            runBlocking { delay(waitTimeMs) }

                        _myVideoPlayer!!.advance()
                        _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER))
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_1FRAME -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        _myVideoPlayer!!.decodeRender(true)
                        _myVideoPlayer!!.advance()
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_1FRAME_REVERSE -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        _myVideoPlayer!!.seekPrevFrame()
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_REVERSE -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        if (!_myVideoPlayer!!.isPlay)
                            return@map paramArray

                        val curPts = paramArray[1] as Long
                        val pair = _myVideoPlayer!!.decodeReverse(curPts)
                        val outBufIdxPtsList = pair.first
                        val prevPts = pair.second

                        for (pair in outBufIdxPtsList) {
                            val idx = pair.first
                            val pts = pair.second
                            _pipeline.onNext(arrayOf(_Commands.RENDER_IDX_OUT_BUF, idx, pts))
                        }

                        if (prevPts >= 0)
                            _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE, prevPts))

                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.RENDER_IDX_OUT_BUF -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        if (!_myVideoPlayer!!.isPlay)
                            return@map paramArray

                        val idx = paramArray[1] as Int
                        val pts = paramArray[2] as Long
                        val waitTimeMs = _myVideoPlayer!!.getWaitTimeMs()

                        if (waitTimeMs > 0)
                            runBlocking { delay(waitTimeMs) }

                        _myVideoPlayer!!.renderIdxOutBuf(idx, pts)
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.TEST -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        _myVideoPlayer!!.test()
                    }
                }
                return@map paramArray
            }
            .subscribe()

    }


    fun open(srcFile: File, renderSurface: Surface) {
        _pipeline.onNext(
            arrayOf(
                _Commands.OPEN,
                srcFile,
                renderSurface
            )
        )
    }

    fun close() {
        _pipeline.onNext(arrayOf(_Commands.CLOSE))
    }

    fun seek(pts: Long) {
        _pipeline.onNext(arrayOf(_Commands.SEEK, pts))
    }

    fun play() {
        if (_myVideoPlayer == null)
            return

        _myVideoPlayer!!.isPlay = true

        if (_myVideoPlayer!!.isReverse) {
            if (this.curPts == 0L) {
                _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE, -1L))
            } else {
                _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE, this.curPts))
            }
        } else {
            _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER))
        }
    }

    fun pause() {
        if (_myVideoPlayer == null)
            return

        _myVideoPlayer!!.isPlay = false
    }

    fun test() {
        _pipeline.onNext(arrayOf(_Commands.TEST))
    }

    fun decodeRenderNextFrame() {
        this.pause()
        _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_1FRAME))
    }

    fun decodeRenderPrevFrame() {
        this.pause()
        _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_1FRAME_REVERSE))
    }
}

