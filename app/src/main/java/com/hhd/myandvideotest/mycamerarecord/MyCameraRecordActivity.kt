package com.hhd.myandvideotest.mycamerarecordrecord

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.hhd.myandvideotest.R
import com.hhd.myandvideotest.mycamerarecord.MyCrPipeLine
import com.hhd.myandvideotest.util.MyActivityUtil
import com.hhd.myandvideotest.util.MyUtil
import kotlinx.android.synthetic.main.my_camera_record_activity.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MyCameraRecordActivity : AppCompatActivity() {

    private val _VIDEO_BITRATE: Int = 6_000_000
    private val _VIDEO_FPS: Int = 1
    private val _VIDEO_HEIGHT = 720
    private val _VIDEO_IFRAME_INTERVAL: Int = 1
    private val _VIDEO_MIME_TYPE: String = "video/avc"
    private val _VIDEO_WIDTH = 1280 // dimensions for 720p video
    private val _pipeline = MyCrPipeLine()
    private var _isCameraStart: Boolean = false
    private var _isRecordStart: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyActivityUtil.setActionBarHide(this)
        MyActivityUtil.setKeepScreenOn(this)

        this.setContentView(R.layout.my_camera_record_activity)
        this.btn_camera_start_stop.setOnClickListener { _btn_camera_start_stop_click() }
        this.btn_record_start_stop.setOnClickListener { _btn_record_start_stop_click() }
    }


    private fun _btn_camera_start_stop_click() {
        if (_isCameraStart) {
            _camera_stop()
        } else {
            _camera_start()
        }
    }

    private fun _camera_start() {
        val isCamFacingFront = this.cb_camera_face_front.isChecked

        _pipeline.startCamera(
            this.sv.holder.surface,
            _VIDEO_WIDTH,
            _VIDEO_HEIGHT,
            _VIDEO_FPS,
            MyUtil.isScreenPortrait(this),
            isCamFacingFront
        )

        _isCameraStart = true
        this.btn_camera_start_stop.text = "camera stop"
    }

    private fun _camera_stop() {
        if (_isRecordStart) {
            _pipeline.stopEncoder()
            _isRecordStart = false
            this.btn_record_start_stop.text = "record start"
        }

        _pipeline.stopCamera()
        _isCameraStart = false
        this.btn_camera_start_stop.text = "camera start"
    }

    override fun onDestroy() {
        super.onDestroy()
        _camera_stop()
    }

    private fun _btn_record_start_stop_click() {
        if (_isRecordStart) {
            _record_stop()
        } else {
            _record_start()
        }
    }

    private fun _record_start() {
        val date = Date(System.currentTimeMillis())
        val sdf = SimpleDateFormat("yyMMdd_HHmmss")
        val outputFile =
            File(Environment.getExternalStorageDirectory(), "mycamerarecord_${sdf.format(date)}.mp4")
        val width: Int
        val height: Int

        if (MyUtil.isScreenPortrait(this)) {
            width = _VIDEO_HEIGHT
            height = _VIDEO_WIDTH
        } else {
            width = _VIDEO_WIDTH
            height = _VIDEO_HEIGHT
        }

        _pipeline.startEncoder(
            width,
            height,
            _VIDEO_BITRATE,
            _VIDEO_FPS,
            outputFile,
            _VIDEO_MIME_TYPE,
            _VIDEO_IFRAME_INTERVAL
        )

        _isRecordStart = true
        this.btn_record_start_stop.text = "record stop"
    }

    private fun _record_stop() {
        _pipeline.stopEncoder()
        _isRecordStart = false
        this.btn_record_start_stop.text = "record start"
    }

    override fun onPause() {
        super.onPause()
        _camera_stop()
    }
}