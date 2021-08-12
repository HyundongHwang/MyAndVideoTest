package com.hhd.myandvideotest.arfit_thread_proto

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.hhd.myandvideotest.util.MyActivityUtil
import com.hhd.myandvideotest.util.MyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArfitThreadProtoActivity : AppCompatActivity() {
    private val _canvasView: Canvas = Canvas()
    private val _newPipeLine = ArfitPipeLine()
    private val _oldModel = ArfitOldComplexCallbackModel()

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
                btn.setOnClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        method.invoke(thisObj)
                    }
                }
            }
    }

    fun _old_model_open_cam() {
        _oldModel.openCam(_canvasView)
    }

    fun _old_model_close_cam() {
        _oldModel.closeCam()
    }

    fun _new_pipeline_open_cam() {
        _newPipeLine.openCam(_canvasView)
    }

    fun _new_pipeline_close_cam() {
        _newPipeLine.closeCam()
    }
}


