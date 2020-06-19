package com.hhd.myandvideotest.myplayvideo

import android.media.MediaCodec
import android.view.Surface
import com.hhd.myandvideotest.util.MyUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.io.File

class MyPvPipeLine {

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

    private var _myVideoPlayer: MyVideoPlayer? = null
    private val _pipeline = PublishSubject.create<Array<Any>>()

    private enum class _Commands {
        NONE,
        PLAY,
        PROCESS_INPUT_BUFFER,
        PROCESS_OUTPUT_BUFFER,
        STOP,
        PAUSE,
        RESUME,
    }


    constructor() {
        _pipeline
            .observeOn(MyUtil.newNamedScheduler("T_START_IN_BUF"))
            .map {
                val paramArray = it as Array<*>
                val cmd = paramArray[0] as _Commands
                when (cmd) {
                    _Commands.PLAY -> {
                        val srcFile = paramArray[1] as File
                        val renderSurface = paramArray[2] as Surface
                        _myVideoPlayer = MyVideoPlayer()
                        _myVideoPlayer!!.play(srcFile, renderSurface)
                        _pipeline.onNext(arrayOf(_Commands.PROCESS_INPUT_BUFFER))
                    }
                    _Commands.STOP -> {
                        if (_myVideoPlayer == null)
                            return@map paramArray

                        _myVideoPlayer!!.stop()
                    }
                    _Commands.PROCESS_INPUT_BUFFER -> {
                        val res = _myVideoPlayer!!.processCodecInputBuffer()

                        when (res) {
                            MyVideoPlayer.ReturnCodes.CONTINUE ->
                                _pipeline.onNext(arrayOf(_Commands.PROCESS_OUTPUT_BUFFER))

                            MyVideoPlayer.ReturnCodes.EOS ->
                                _pipeline.onNext(arrayOf(_Commands.STOP))

                            MyVideoPlayer.ReturnCodes.STOP -> {
                            }
                        }
                    }
                }
                return@map paramArray
            }
            .observeOn(MyUtil.newNamedScheduler("T_OUT_BUF"))
            .map {
                val paramArray = it as Array<*>
                val cmd = paramArray[0] as _Commands
                when (cmd) {
                    _Commands.PROCESS_OUTPUT_BUFFER -> {
                        _myVideoPlayer!!.processCodecOutputBuffer()
                        _pipeline.onNext(arrayOf(_Commands.PROCESS_INPUT_BUFFER))
                    }
                }
                return@map paramArray
            }
            .subscribe()

    }


    fun play(srcFile: File, renderSurface: Surface) {
        _pipeline.onNext(
            arrayOf(
                _Commands.PLAY,
                srcFile,
                renderSurface
            )
        )
    }

    fun stop() {
        _pipeline.onNext(arrayOf(_Commands.STOP))
    }
}

