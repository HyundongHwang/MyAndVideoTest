package com.hhd.myandvideotest.myplayvideo

import android.view.Surface
import com.hhd.myandvideotest.util.LogEx
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

    var duration: Long = -1L
        get() {
            if (_myVideoPlayer == null)
                return -1L

            return _myVideoPlayer!!.videoDurationUs
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
        DECODE_RENDER_REVERSE,
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
                    _Commands.DECODE_RENDER_REVERSE -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        if (!_myVideoPlayer!!.isPlay)
                            return@map paramArray

                        val keyFramePts = paramArray[1] as Long

                        val resKeyFramePts = _myVideoPlayer!!.decodeRenderReverse(keyFramePts, {
                            _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                        })

                        if (resKeyFramePts >= 0)
                            _pipeline.onNext(
                                arrayOf(
                                    _Commands.DECODE_RENDER_REVERSE,
                                    resKeyFramePts
                                )
                            )

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
            _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE, -1L))
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
}

