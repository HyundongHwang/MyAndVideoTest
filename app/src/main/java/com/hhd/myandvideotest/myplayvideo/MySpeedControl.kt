package com.hhd.myandvideotest.myplayvideo

import com.hhd.myandvideotest.util.LogEx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class MySpeedControl {
    var speedRatio: Double = 1.0

    private var _ptsOld: Long = -1L
    private var _timeStampOldUs: Long = -1L
    private var _waitTimeSumUs = 0L

    fun init() {
        _ptsOld = -1L
        _timeStampOldUs = -1L
        _waitTimeSumUs = 0L
    }

    fun waitBeforeRenderFrame(ptsNew: Long) {
        var nowUs = System.nanoTime() / 1_000

        val initialized = when {
            _ptsOld < 0L -> false
            _timeStampOldUs < 0L -> false
            else -> true
        }

        if (initialized) {
            val waitTimeUs = ((ptsNew - _ptsOld) / speedRatio) - (nowUs - _timeStampOldUs)
            _waitTimeSumUs += waitTimeUs.toLong()

            if (_waitTimeSumUs > 0L) {
                runBlocking { delay(_waitTimeSumUs.toLong() / 1_000) }
                _waitTimeSumUs = 0L
            }
        }

        _ptsOld = ptsNew
        nowUs = System.nanoTime() / 1_000
        _timeStampOldUs = nowUs
    }
}