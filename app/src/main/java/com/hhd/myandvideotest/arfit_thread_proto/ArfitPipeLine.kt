package com.hhd.myandvideotest.arfit_thread_proto

import android.graphics.Canvas
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

object ArfitPipeLine {
    private var _canvas: Canvas? = null
    private var _touchResults: MyTouchResults? = null
    private var _poseInfo: MyPoseInfo? = null
    private var _landMarks: MyLandMarks? = null
    private var _image: MyImage? = null

    val _t_cam_render = CoroutineScope(newSingleThreadContext("T_CAM_RENDER"))
    val _channel_cam_render = Channel<List<Any>>()

    val _t_calc = CoroutineScope(newSingleThreadContext("T_CALC"))
    val _channel_calc = Channel<List<Any>>()

    val _t_main = CoroutineScope(Dispatchers.Main)

    val _cam = MyCameraX()
    val _moveNet = MyMoveNet()
    val _poseAnalyzer = MyPoseAnalyzer()
    val _touchAnalyzer = MyTouchAnalyzer()
    val _renderer = MyRenderer()

    // https://stackoverflow.com/questions/44589669/correctly-implementing-wait-and-notify-in-kotlin
    // val _lock = ReentrantLock()
    // val _lock_condition = _lock.newCondition()

    init {
        _t_cam_render.launch {
            _channel_cam_render.consumeEach {
                val cmd = it[0] as String

                if (cmd == "IMAGE_RECV") {
                    _t_main.launch {
                        _renderer.draw(
                            _canvas,
                            _image,
                            _landMarks,
                            _poseInfo,
                            _touchResults
                        )
                    }
                }
            }
        }

        _t_calc.launch {
            _channel_calc.consumeEach {
                val cmd = it[0] as String

                if (cmd == "IMAGE_RECV") {
                    _image = it[1] as MyImage
                    _landMarks = _moveNet.calc(_image!!)
                    _poseInfo = _poseAnalyzer.calc(_landMarks!!)
                    _touchResults = _touchAnalyzer.calc(_poseInfo!!)
                }
            }
        }
    }

    fun openCamera(canvas: Canvas) {
        _canvas = canvas

        _cam.open {
            GlobalScope.launch {
                val image = it
                _channel_cam_render.send(listOf("IMAGE_RECV", image))
                _channel_calc.send(listOf("IMAGE_RECV", image))
            }
        }
    }

}