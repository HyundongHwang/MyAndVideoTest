package com.hhd.myandvideotest.myplayvideo

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.hhd.myandvideotest.R
import com.hhd.myandvideotest.util.MyActivityUtil
import kotlinx.android.synthetic.main.my_play_video_activity.*
import java.io.File

class MyPlayVideoActivity : AppCompatActivity() {

    private var _isPlaying: Boolean = false
    private val _pipeline = MyPvPipeLine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyActivityUtil.setActionBarHide(this)
        MyActivityUtil.setKeepScreenOn(this)

        this.setContentView(R.layout.my_play_video_activity)

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

        this.sp_speed_ratio.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("x1", "x2", "x5", "x10", "x0.5", "x0.3", "x0.1")
            )

        this.sp_speed_ratio.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                _sp_speed_ratio_itemselected()
            }
        })

        this.btn_play_stop.setOnClickListener { _btn_play_stop_click() }
    }

    private fun _sp_speed_ratio_itemselected() {
        _pipeline.speedRatio = (this.sp_speed_ratio.selectedItem as String).trimStart('x').toDouble()
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



