package com.hhd.myandvideotest.myplayvideo

import android.annotation.SuppressLint
import android.view.Surface
import com.hhd.myandvideotest.util.LogEx
import com.hhd.myandvideotest.util.MyUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.PublishSubject
import java.io.File

class MyPvPipeLine {

    var isOpen: Boolean = false
        get() {
            when {
                _myAudioPlayer.isOpen || _myVideoPlayer.isOpen -> return true
                else -> return false
            }
        }
        private set

    var isPlay: Boolean
        get() {
            if (_myAudioPlayer.isOpen)
                return _myAudioPlayer.isPlay

            if (_myVideoPlayer.isOpen)
                return _myVideoPlayer.isPlay

            return false
        }
        set(value) {
            if (_myAudioPlayer.isOpen)
                _myAudioPlayer.isPlay = value

            if (_myVideoPlayer.isOpen)
                _myVideoPlayer.isPlay = value
        }


    var speedRatio: Double
        get() {
            if (_myAudioPlayer.isOpen)
                return _mySpeedControlAudio.speedRatio

            if (_myVideoPlayer.isOpen)
                return _mySpeedControlVideo.speedRatio

            return 1.0
        }
        set(value) {
            _mySpeedControlAudio.speedRatio = value
            _mySpeedControlVideo.speedRatio = value
        }

    var durationUs: Long = -1L
        get() {
            if (_myAudioPlayer.isOpen)
                return _myAudioPlayer.durationUs

            if (_myVideoPlayer.isOpen)
                return _myVideoPlayer.durationUs

            return -1L
        }
        private set

    var videoWidth: Int = -1
        get() {
            if (_myVideoPlayer.isOpen)
                return _myVideoPlayer.width

            return -1
        }
        private set

    var videoHeight: Int = -1
        get() {
            if (_myVideoPlayer.isOpen)
                return _myVideoPlayer.height

            return -1
        }
        private set

    var videoRotation: Int = 0
        get() {
            if (_myVideoPlayer.isOpen)
                return _myVideoPlayer.rotation

            return -1
        }
        private set

    var curPts: Long = -1L
        get() {
            if (_myVideoPlayer.isOpen)
                return _myVideoPlayer.curPts

            if (_myAudioPlayer.isOpen)
                return _myAudioPlayer.curPts

            return -1
        }
        private set

    var isReverse: Boolean
        get() {
            if (_myVideoPlayer.isOpen)
                return _myVideoPlayer.isReverse

            return false
        }
        set(value) {
            if (_myVideoPlayer.isOpen)
                _myVideoPlayer.isReverse = value
        }


    private val _myVideoPlayer = MyVideoPlayer()
    private val _myAudioPlayer = MyAudioPlayer()
    private val _mySpeedControlAudio = MySpeedControl()
    private val _mySpeedControlVideo = MySpeedControl()

    private val _ps_audio = PublishSubject.create<Array<Any>>()
    private val _ps_video = PublishSubject.create<Array<Any>>()
    private val _ps_ui = PublishSubject.create<Array<Any>>()

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
    }


    @SuppressLint("CheckResult")
    constructor(updateUiCb: (MyPvPipeLine) -> Unit) {

        _ps_audio
            .observeOn(MyUtil.newNamedScheduler("T_AUDIO"))
            .map {
                val paramArray = it as Array<Any>
                val cmd = paramArray[0] as _Commands

                when (cmd) {
                    _Commands.OPEN_AUDIO -> {
                        val srcFile = paramArray[1] as File
                        _myAudioPlayer.prepare(srcFile)
                        _mySpeedControlAudio.init()
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.CLOSE_AUDIO -> {
                        _myAudioPlayer.stop()
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.SEEK_AUDIO -> {
                        if (!_myAudioPlayer.isOpen)
                            return@map it

                        val pts = paramArray[1] as Long
                        _myAudioPlayer.seek(pts)
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_AUDIO -> {
                        if (!_myAudioPlayer.isOpen)
                            return@map it

                        if (!_myAudioPlayer.isPlay)
                            return@map it

                        _mySpeedControlAudio.waitBeforeRenderFrame(_myAudioPlayer.curPts)
                        val res = _myAudioPlayer.decodeRender(true)


                        if (!res)
                            return@map it

                        _myAudioPlayer.advance()
                        _ps_audio.onNext(arrayOf(_Commands.DECODE_RENDER_AUDIO))
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                }

                return@map it
            }
            .subscribe()

        _ps_video
            .observeOn(MyUtil.newNamedScheduler("T_VIDEO"))
            .map {
                val paramArray = it as Array<Any>
                val cmd = paramArray[0] as _Commands

                when (cmd) {
                    _Commands.OPEN_VIDEO -> {
                        val srcFile = paramArray[1] as File
                        val renderSurface = paramArray[2] as Surface
                        _myVideoPlayer.prepare(srcFile, renderSurface)
                        _mySpeedControlVideo.init()
                        _myVideoPlayer.seek(0)
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.CLOSE_VIDEO -> {
                        _myVideoPlayer.stop()
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.SEEK_VIDEO -> {
                        if (!_myVideoPlayer.isOpen)
                            return@map it

                        val pts = paramArray[1] as Long
                        _myVideoPlayer.seek(pts)
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_VIDEO -> {
                        if (!_myVideoPlayer.isOpen)
                            return@map it

                        if (!_myVideoPlayer.isPlay)
                            return@map it

                        _myVideoPlayer.syncToAudioPts(_myAudioPlayer.curPts)
                        _mySpeedControlVideo.waitBeforeRenderFrame(_myVideoPlayer.curPts)
                        val res = _myVideoPlayer.decodeRender(true)

                        if (!res)
                            return@map it

                        _myVideoPlayer.advance()
                        _ps_video.onNext(arrayOf(_Commands.DECODE_RENDER_VIDEO))
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_1FRAME_VIDEO -> {
                        if (!_myVideoPlayer.isOpen)
                            return@map it

                        _myVideoPlayer.decodeRender(true)
                        _myVideoPlayer.advance()
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_1FRAME_REVERSE_VIDEO -> {
                        if (!_myVideoPlayer.isOpen)
                            return@map it

                        _myVideoPlayer.seekPrevFrame()
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.DECODE_RENDER_REVERSE_VIDEO -> {
                        if (!_myVideoPlayer.isOpen)
                            return@map it

                        if (!_myVideoPlayer.isPlay)
                            return@map it

                        val curPts = paramArray[1] as Long
                        val pair = _myVideoPlayer.decodeReverse(curPts)
                        val outBufIdxPtsList = pair.first
                        val prevPts = pair.second

                        for (pair in outBufIdxPtsList) {
                            val idx = pair.first
                            val pts = pair.second
                            _ps_video.onNext(
                                arrayOf(
                                    _Commands.RENDER_IDX_OUT_BUF_VIDEO,
                                    idx,
                                    pts
                                )
                            )
                        }

                        if (prevPts >= 0)
                            _ps_video.onNext(
                                arrayOf(
                                    _Commands.DECODE_RENDER_REVERSE_VIDEO,
                                    prevPts
                                )
                            )

                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                    _Commands.RENDER_IDX_OUT_BUF_VIDEO -> {
                        if (!_myVideoPlayer.isOpen)
                            return@map it

                        if (!_myVideoPlayer.isPlay)
                            return@map it

                        val idx = paramArray[1] as Int
                        val pts = paramArray[2] as Long

                        _mySpeedControlVideo.waitBeforeRenderFrame(_myVideoPlayer.curPts)
                        _myVideoPlayer.renderIdxOutBuf(idx, pts)
                        _ps_ui.onNext(arrayOf(_Commands.UPDATE_UI))
                    }
                }

                return@map it
            }
            .subscribe()

        _ps_ui
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                val paramArray = it as Array<Any>
                val cmd = paramArray[0] as _Commands

                when (cmd) {
                    _Commands.UPDATE_UI -> {
                        updateUiCb(this)
                    }
                }

                return@map it
            }
            .subscribe()
    }


    fun open(srcFile: File, renderSurface: Surface) {
        _ps_audio.onNext(arrayOf(_Commands.OPEN_AUDIO, srcFile))
        _ps_video.onNext(arrayOf(_Commands.OPEN_VIDEO, srcFile, renderSurface))
    }

    fun close() {
        _ps_audio.onNext(arrayOf(_Commands.CLOSE_AUDIO))
        _ps_video.onNext(arrayOf(_Commands.CLOSE_VIDEO))
    }

    fun seek(pts: Long) {
        _ps_audio.onNext(arrayOf(_Commands.SEEK_AUDIO, pts))
        _ps_video.onNext(arrayOf(_Commands.SEEK_VIDEO, pts))
    }

    fun play() {
        _myAudioPlayer.isPlay = true
        _myVideoPlayer.isPlay = true

        if (_myVideoPlayer.isReverse) {
            if (this.curPts == 0L) {
                _ps_video.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE_VIDEO, -1L))
            } else {
                _ps_video.onNext(arrayOf(_Commands.DECODE_RENDER_REVERSE_VIDEO, this.curPts))
            }
        } else {
            _ps_audio.onNext(arrayOf(_Commands.DECODE_RENDER_AUDIO))
            _ps_video.onNext(arrayOf(_Commands.DECODE_RENDER_VIDEO))
        }
    }

    fun pause() {
        _myVideoPlayer.isPlay = false
        _myAudioPlayer.isPlay = false
    }

    fun decodeRenderNextFrame() {
        this.pause()
        _ps_video.onNext(arrayOf(_Commands.DECODE_RENDER_1FRAME_VIDEO))
    }

    fun decodeRenderPrevFrame() {
        this.pause()
        _ps_video.onNext(arrayOf(_Commands.DECODE_RENDER_1FRAME_REVERSE_VIDEO))
    }
}