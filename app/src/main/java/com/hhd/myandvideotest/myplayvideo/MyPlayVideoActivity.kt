package com.hhd.myandvideotest.myplayvideo

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.hhd.myandvideotest.R
import com.hhd.myandvideotest.util.LogEx
import com.hhd.myandvideotest.util.MyActivityUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.my_play_video_activity.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class MyPlayVideoActivity : AppCompatActivity() {

    private val _pipeline = MyPvPipeLine({ _updateUi(it) })
    private val _sb_ps = PublishSubject.create<Int>()


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
                listOf(
                    "x1",
                    "x2",
                    "x5",
                    "x10",
                    "x0.5",
                    "x0.3",
                    "x0.1"
                )
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

        this.btn_open_close.setOnClickListener { _btn_open_close_click() }
        this.btn_play_pause.setOnClickListener { _btn_play_pause_click() }
        this.btn_test.setOnClickListener { _btn_test_click() }

        this.sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    _sb_progress_change_fromUser(progress, seekBar!!.max)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        this.btn_frame_prev.setOnClickListener { _btn_frame_prev_click() }
        this.btn_frame_next.setOnClickListener { _btn_frame_next_click() }

        _sb_ps
            .debounce(200, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                val progress = it
                _sb_ps_debounce(progress)
                return@map Any()
            }
            .subscribe()
    }

    private fun _btn_frame_prev_click() {
        _pipeline.decodeRenderPrevFrame()
    }

    private fun _sb_ps_debounce(progress: Int) {
        LogEx.value("progress", progress)
        _pipeline.pause()
        val pts = progress * 1_000L
        _pipeline.seek(pts)
    }

    private fun _btn_frame_next_click() {
        _pipeline.decodeRenderNextFrame()
    }

    private fun _btn_test_click() {
        _pipeline.test()
    }

    private fun _sb_progress_change_fromUser(progress: Int, max: Int) {
        _sb_ps.onNext(progress)
    }

    private fun _btn_play_pause_click() {
        if (_pipeline.isPlay) {
            _pipeline.pause()
        } else {
            _pipeline.speedRatio =
                (this.sp_speed_ratio.selectedItem as String).trimStart('x').toDouble()
            _pipeline.isReverse = this.cb_reverse.isChecked
            _pipeline.play()
        }
    }


    private fun _btn_open_close_click() {
        if (_pipeline.isOpen) {
            _close()
        } else {
            _open()
        }
    }

    private fun _open() {
        val srcFile = File(
            Environment.getExternalStorageDirectory(),
            this.sp_video_file.selectedItem as String
        )

        _pipeline.open(srcFile, this.sv.holder.surface)
    }

    private fun _close() {
        _pipeline.close()
    }

    private fun _sp_speed_ratio_itemselected() {
        _pipeline.speedRatio =
            (this.sp_speed_ratio.selectedItem as String).trimStart('x').toDouble()
    }


    override fun onPause() {
        super.onPause()
        _close()
    }

    private fun _updateUi(pipeline: MyPvPipeLine) {
        val winMgr = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val winSize = Point()
        winMgr.defaultDisplay.getSize(winSize)

        var vw = -1
        var vh = -1

        when (_pipeline.videoRotation) {
            90, 270 -> {
                vw = _pipeline.videoHeight
                vh = _pipeline.videoWidth
            }
            else -> {
                vw = _pipeline.videoWidth
                vh = _pipeline.videoHeight
            }
        }

        if (vw > 0 && vh > 0) {
            if (vw > vh) { // landscape
                this.sv.layoutParams.width = winSize.x
                this.sv.layoutParams.height = winSize.x * vh / vw
            } else { // portrait
                this.sv.layoutParams.width = winSize.x * 2 / 3
                this.sv.layoutParams.height = (winSize.x * 2 / 3) * vh / vw
            }
        }

        var curPts_s = pipeline.curPts.toDouble() / 1_000_000
        var duration_s = pipeline.durationUs.toDouble() / 1_000_000

        this.tv_seek.text =
            "${String.format("%.1f", curPts_s)}s : " +
                    "${String.format("%.1f", duration_s)}s " +
                    "${String.format("%.0f", curPts_s * 100 / duration_s)} %"

        this.sb.max = (pipeline.durationUs / 1_000).toInt()
        this.sb.progress = (pipeline.curPts / 1_000).toInt()

        if (pipeline.isOpen) {
            this.btn_open_close.text = "close"
        } else {
            this.btn_open_close.text = "open"
        }

        if (pipeline.isPlay) {
            this.btn_play_pause.text = "pause"
        } else {
            this.btn_play_pause.text = "play"
        }
    }
}