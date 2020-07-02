package com.hhd.myandvideotest.myplayvideo

import android.view.Surface
import com.hhd.myandvideotest.util.MyUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.io.File

class MyPvPipeLine {

    var isOpen: Boolean = false
        get() {
            if (_myAudioPlayer == null)
                return false

            if (_myVideoPlayer == null)
                return false

            return true
        }
        private set

    var isPlay: Boolean
        get() {
            if (_myAudioPlayer == null)
                return false

            if (_myVideoPlayer == null)
                return false

            return _myAudioPlayer!!.isPlay
        }
        set(value) {
            _myAudioPlayer!!.isPlay = value
            _myVideoPlayer!!.isPlay = value
        }


    var speedRatio: Double
        get() {
            return _mySpeedControlAudio.speedRatio
        }
        set(value) {
            _mySpeedControlAudio.speedRatio = value
            _mySpeedControlVideo.speedRatio = value
        }

    var durationUs: Long = -1L
        get() {
            if (_myAudioPlayer == null)
                return -1L

            return _myAudioPlayer!!.durationUs
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
    private var _myAudioPlayer: MyAudioPlayer? = null
    private val _pipeline = PublishSubject.create<Array<Any>>()
    private val _mySpeedControlAudio = MySpeedControl()
    private val _mySpeedControlVideo = MySpeedControl()

    private enum class _Commands {
        NONE,
        OPEN_VIDEO,
        OPEN_AUDIO,
        CLOSE_VIDEO,
        CLOSE_AUDIO,
        SEEK_VIDEO,
        SEEK_AUDIO,
        DECODE_RENDER_VIDEO,
        DECODE_RENDER_AUDIO,
        DECODE_RENDER_1FRAME_VIDEO,
        DECODE_RENDER_REVERSE_VIDEO,
        DECODE_RENDER_1FRAME_REVERSE_VIDEO,
        RENDER_IDX_OUT_BUF_VIDEO,
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
            .observeOn(MyUtil.newNamedScheduler("T_AUDIO"))
            .map {
                val paramArray = it as Array<*>
                val cmd = paramArray[0] as _Commands

                when (cmd) {
                    _Commands.OPEN_AUDIO -> {
                        val srcFile = paramArray[1] as File
                        _myAudioPlayer = MyAudioPlayer()
                        _myAudioPlayer!!.prepare(srcFile)
                        _mySpeedControlAudio.init()
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.CLOSE_AUDIO -> {
                        if (_myAudioPlayer == null)
                            return@map null

                        _myAudioPlayer!!.stop()
                        _myAudioPlayer = null
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.SEEK_AUDIO -> {
                        if (_myAudioPlayer == null)
                            return@map paramArray

                        val pts = paramArray[1] as Long
                        _myAudioPlayer!!.seek(pts)
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_AUDIO -> {
                        if (_myAudioPlayer == null)
                            return@map paramArray

                        _mySpeedControlAudio.waitBeforeRenderFrame(_myAudioPlayer!!.curPts)
                        val res = _myAudioPlayer!!.decodeRender(true)

                        if (!res)
                            return@map paramArray

                        _myAudioPlayer!!.advance()
                        _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_AUDIO))
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                }

                return@map paramArray
            }
            .observeOn(MyUtil.newNamedScheduler("T_VIDEO"))
            .map {
                val paramArray = it as Array<*>
                val cmd = paramArray[0] as _Commands

                when (cmd) {
                    _Commands.OPEN_VIDEO -> {
                        val srcFile = paramArray[1] as File
                        val renderSurface = paramArray[2] as Surface
                        _myVideoPlayer = MyVideoPlayer()
                        _myVideoPlayer!!.prepare(srcFile, renderSurface)
                        _mySpeedControlVideo.init()
                        _myVideoPlayer!!.seek(0)
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.CLOSE_VIDEO -> {
                        if (_myVideoPlayer == null)
                            return@map null

                        _myVideoPlayer!!.stop()
                        _myVideoPlayer = null
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.SEEK_VIDEO -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        val pts = paramArray[1] as Long
                        _myVideoPlayer!!.seek(pts)
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_VIDEO -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        if (!_myVideoPlayer!!.isPlay)
                            return@map paramArray

                        _mySpeedControlVideo.waitBeforeRenderFrame(_myVideoPlayer!!.curPts)
                        val res = _myVideoPlayer!!.decodeRender(true)

                        if (!res)
                            return@map paramArray

                        _myVideoPlayer!!.advance()
                        _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_VIDEO))
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_1FRAME_VIDEO -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        _myVideoPlayer!!.decodeRender(true)
                        _myVideoPlayer!!.advance()
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_1FRAME_REVERSE_VIDEO -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        _myVideoPlayer!!.seekPrevFrame()
                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_REVERSE_VIDEO -> {
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
                            _pipeline.onNext(arrayOf(_Commands.RENDER_IDX_OUT_BUF_VIDEO, idx, pts))
                        }

                        if (prevPts >= 0)
                            _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE_VIDEO, prevPts))

                        _pipeline.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.RENDER_IDX_OUT_BUF_VIDEO -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        if (!_myVideoPlayer!!.isPlay)
                            return@map paramArray

                        val idx = paramArray[1] as Int
                        val pts = paramArray[2] as Long

                        _mySpeedControlVideo.waitBeforeRenderFrame(_myVideoPlayer!!.curPts)
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
                _Commands.OPEN_AUDIO,
                srcFile
            )
        )

        _pipeline.onNext(
            arrayOf(
                _Commands.OPEN_VIDEO,
                srcFile,
                renderSurface
            )
        )
    }

    fun close() {
        _pipeline.onNext(arrayOf(_Commands.CLOSE_AUDIO))
        _pipeline.onNext(arrayOf(_Commands.CLOSE_VIDEO))
    }

    fun seek(pts: Long) {
        _pipeline.onNext(arrayOf(_Commands.SEEK_AUDIO, pts))
        _pipeline.onNext(arrayOf(_Commands.SEEK_VIDEO, pts))
    }

    fun play() {
        if (_myAudioPlayer == null)
            return

        if (_myVideoPlayer == null)
            return

        _myAudioPlayer!!.isPlay = true
        _myVideoPlayer!!.isPlay = true

        if (_myVideoPlayer!!.isReverse) {
            if (this.curPts == 0L) {
                _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE_VIDEO, -1L))
            } else {
                _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE_VIDEO, this.curPts))
            }
        } else {
            _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_AUDIO))
            _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_VIDEO))
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
        _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_1FRAME_VIDEO))
    }

    fun decodeRenderPrevFrame() {
        this.pause()
        _pipeline.onNext(arrayOf(_Commands.DECODE_RENDER_1FRAME_REVERSE_VIDEO))
    }
}

