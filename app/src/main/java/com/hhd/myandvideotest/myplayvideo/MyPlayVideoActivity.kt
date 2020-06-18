package com.hhd.myandvideotest.myplayvideo

import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.hhd.myandvideotest.R
import com.hhd.myandvideotest.util.MyActivityUtil
import kotlinx.android.synthetic.main.my_play_video_activity.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MyPlayVideoActivity : AppCompatActivity() {

    private var _isPlaying: Boolean = false
    private val _pipeline = MyPvPipeLine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyActivityUtil.setActionBarHide(this)
        MyActivityUtil.setKeepScreenOn(this)

        this.setContentView(R.layout.my_play_video_activity)

        val date = Date(System.currentTimeMillis())
        val sdf = SimpleDateFormat("yyMMdd_HHmmss")
        val outputFile =
            File(
                Environment.getExternalStorageDirectory(),
                "mycamerarecord_${sdf.format(date)}.mp4"
            )

        val mp4FileNameList = Environment
            .getExternalStorageDirectory()
            .listFiles()
            .filter { it.name.endsWith(".mp4") }
            .map { it.name }

        this.sp_video_file.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                mp4FileNameList
            )

        this.btn_play_stop.setOnClickListener { _btn_play_stop_click() }
    }

    private fun _btn_play_stop_click() {
        if (_isPlaying) {
            _stop()
        } else {
            _play()
        }
    }

    private fun _play() {
        val srcFile = File(
            Environment.getExternalStorageDirectory(),
            this.sp_video_file.selectedItem as String
        )

        _pipeline.play(srcFile, this.sv.holder.surface)
        this.btn_play_stop.text = "stop"
        _isPlaying = true
    }

    override fun onPause() {
        super.onPause()
        _stop()
    }

    private fun _stop() {
        _pipeline.stop()
        this.btn_play_stop.text = "play"
        _isPlaying = false
    }
}



