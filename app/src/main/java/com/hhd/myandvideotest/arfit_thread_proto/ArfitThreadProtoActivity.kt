package com.hhd.myandvideotest.arfit_thread_proto

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.hhd.myandvideotest.util.MyActivityUtil
import com.hhd.myandvideotest.util.MyUtil
import com.naver.videocelltech.logsloth.LogSloth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArfitThreadProtoActivity : AppCompatActivity() {
    val _canvasView:Canvas = Canvas()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MyActivityUtil.setActionBarHide(this)
        MyActivityUtil.setKeepScreenOn(this)
        val context = this as Context
        val thisObj = this
        val sv = MyUtil.createScrollViewMpWc(context)
        val fl = MyUtil.createFlexboxLayoutMpWc(context)
        this.setContentView(sv)
        sv.addView(fl)

        this.javaClass.methods
            .filter { it.name.startsWith("_") }
            .filterNot { it.name.contains("$") }
            .forEach {
                val method = it
                val btn = Button(context)
                fl.addView(btn)
                btn.isAllCaps = false
                btn.text = method.name
                btn.setOnClickListener { method.invoke(thisObj) }
            }
    }

    val _t_main = CoroutineScope(Dispatchers.Main)
    val _cam = MyCameraX()
    val _moveNet = MyMoveNet()
    val _poseAnalyzer = MyPoseAnalyzer()
    val _touchAnalyzer = MyTouchAnalyzer()
    val _renderer = MyRenderer()

    fun _old_complex_callback() {
        LogSloth.enter()

        _cam.open {
            LogSloth.enter()
            val img = it
            val landMarks = _moveNet.calcLandMarks(img)
            val poseInfo = _poseAnalyzer.calcPoseInfo(landMarks)
            val touchResults = _touchAnalyzer.calcTouchResults(poseInfo)

            _t_main.launch {
                LogSloth.enter()
                _renderer.draw(
                    _canvasView,
                    img,
                    landMarks,
                    poseInfo,
                    touchResults
                )
                LogSloth.leave()
            }
            LogSloth.leave()
        }
        LogSloth.leave()
    }

    fun _new_coroutine_open_camera() {
        ArfitPipeLine.openCamera(_canvasView)
    }

    fun _new_coroutine_close_camera() {
        ArfitPipeLine.closeCamera()
    }
}

