package com.hhd.myandvideotest.mycamerarecord

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.view.Surface
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.nio.ByteBuffer

class MyPipeLine {

    private val _pipeline = PublishSubject.create<Array<Any>>()
    private var _camera: MyCamera? = null
    private var _encoder: MyEncoder? = null
    private var _renderer: MyRenderer? = null

    private enum class _Commands {
        NONE,
        START_CAMERA,
        STOP_CAMERA,
        FRAME_AVAILABLE,
        START_ENCODER,
        STOP_ENCODER,
        WRITE_TO_FILE,
    }

    constructor() {
        _pipeline
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    val paramArray = it as Array<*>
                    val cmd = paramArray[0] as _Commands

                    when (cmd) {
                        _Commands.START_CAMERA -> {
                            val surface = paramArray[1] as Surface
                            val videoWidth = paramArray[2] as Int
                            val videoHeight = paramArray[3] as Int
                            val videoFps = paramArray[4] as Int
                            val isDisplayPortrait = paramArray[5] as Boolean
                            val isCamFacingFront = paramArray[6] as Boolean

                            _camera = MyCamera()
                            _renderer = MyRenderer()
                            _renderer!!.extraRendererList.add(MyExtraRenderer())

                            _camera!!.start(
                                    surface,
                                    videoWidth,
                                    videoHeight,
                                    videoFps,
                                    isDisplayPortrait,
                                    isCamFacingFront)

                            _renderer!!.start()

                            _camera!!.startPreview(
                                    _renderer!!.textureId,
                                    { st ->
                                        _pipeline.onNext(
                                                arrayOf(
                                                        _Commands.FRAME_AVAILABLE,
                                                        st
                                                ))
                                    })

                            _renderer!!.eglSurfaceList.add(_camera!!.dispEglSurface!!)
                        }
                        _Commands.STOP_CAMERA -> {
                            _camera!!.stop()
                            _camera = null
                            _renderer!!.stop()
                            _renderer = null
                        }
                        _Commands.FRAME_AVAILABLE -> {
                            val st = paramArray[1] as SurfaceTexture
                            _renderer!!.render(st)
                        }
                    }

                    return@map paramArray
                }
                .observeOn(Schedulers.io())
                .map {
                    val paramArray = it as Array<*>
                    val cmd = paramArray[0] as _Commands

                    when (cmd) {
                        _Commands.START_ENCODER -> {

                            val width = paramArray[1] as Int
                            val height = paramArray[2] as Int
                            val bitRate = paramArray[3] as Int
                            val frameRate = paramArray[4] as Int
                            val outputFile = paramArray[5] as File
                            val mimeType = paramArray[6] as String
                            val iFrameInterval = paramArray[7] as Int

                            _encoder = MyEncoder()

                            _encoder!!.start(
                                    width,
                                    height,
                                    bitRate,
                                    frameRate,
                                    outputFile,
                                    mimeType,
                                    iFrameInterval)

                            _renderer!!.eglSurfaceList.add(_encoder!!.inputEglSurface!!)
                        }
                        _Commands.STOP_ENCODER -> {
                            _renderer!!.eglSurfaceList.remove(_encoder!!.inputEglSurface)
                            _encoder!!.stop()
                            _encoder = null
                        }
                        _Commands.FRAME_AVAILABLE -> {
                            if (_encoder == null) {
                            } else {
                                val bufPairArray = _encoder!!.drain()
                                return@map arrayOf(_Commands.WRITE_TO_FILE, bufPairArray)
                            }
                        }
                    }

                    return@map paramArray
                }
                .observeOn(Schedulers.io())
                .map {
                    val paramArray = it as Array<*>
                    val cmd = paramArray[0] as _Commands

                    when (cmd) {
                        _Commands.WRITE_TO_FILE -> {
                            if (_encoder == null)
                                return@map paramArray

                            val bufParrArray = paramArray[1] as MutableList<Pair<ByteBuffer, MediaCodec.BufferInfo>>
                            _encoder!!.writeToFile(bufParrArray)
                        }
                    }

                    return@map paramArray
                }
                .subscribe()
    }


    fun startCamera(surface: Surface,
                    videoWidth: Int,
                    videoHeight: Int,
                    videoFps: Int,
                    isDisplayPortrait: Boolean,
                    isCamFacingFront: Boolean
    ) {
        _pipeline.onNext(
                arrayOf(
                        _Commands.START_CAMERA,
                        surface,
                        videoWidth,
                        videoHeight,
                        videoFps,
                        isDisplayPortrait,
                        isCamFacingFront))

    }

    fun stopCamera() {
        _pipeline.onNext(arrayOf(_Commands.STOP_CAMERA))
    }

    fun startEncoder(width: Int,
                     height: Int,
                     bitRate: Int,
                     frameRate: Int,
                     outputFile: File,
                     mimeType: String,
                     iFrameInterval: Int) {


        _pipeline.onNext(
                arrayOf(
                        _Commands.START_ENCODER,
                        width,
                        height,
                        bitRate,
                        frameRate,
                        outputFile,
                        mimeType,
                        iFrameInterval))

    }

    fun stopEncoder() {
        _pipeline.onNext(arrayOf(_Commands.STOP_ENCODER))
    }
}

