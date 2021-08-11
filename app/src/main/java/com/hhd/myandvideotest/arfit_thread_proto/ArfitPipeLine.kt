package com.hhd.myandvideotest.arfit_thread_proto

import android.graphics.Canvas
import com.naver.videocelltech.logsloth.LogSloth
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

object ArfitPipeLine {
    private var _touchResults: MyTouchResults? = null
    private var _poseInfo: MyPoseInfo? = null
    private var _landMarks: MyLandMarks? = null

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
        _t_calc.launch {
            _channel_calc.consumeEach {
                val cmd = it[0] as String

                if (cmd == "IMAGE_RECV") {
                    val canvas = it[1] as Canvas
                    val image = it[2] as MyImage
                    _landMarks = _moveNet.calcLandMarks(image)
                    _poseInfo = _poseAnalyzer.calcPoseInfo(_landMarks!!)
                    _touchResults = _touchAnalyzer.calcTouchResults(_poseInfo!!)
                }
            }
        }
    }

    fun openCamera(canvas: Canvas) {
        _t_main.launch {
            _cam.open {
                val image = it
                _onImgRecv(canvas, image)
            }
        }
    }

    fun closeCamera() {
        _t_main.launch {
            _cam.close()
        }
    }


    private fun _onImgRecv(canvas: Canvas, image: MyImage) {
        _t_main.launch {
            _renderer.draw(
                canvas,
                image,
                _landMarks,
                _poseInfo,
                _touchResults
            )
        }

        GlobalScope.launch {

            while (!_channel_calc.isEmpty) {
                LogSloth.d("_channel_calc.receive() ...")
                _channel_calc.receive()
            }

            _channel_calc.send(listOf("IMAGE_RECV", canvas, image))
        }
    }
}