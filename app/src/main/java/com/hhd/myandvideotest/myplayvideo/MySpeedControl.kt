package com.hhd.myandvideotest.myplayvideo

import com.hhd.myandvideotest.util.LogEx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.max

class MySpeedControl {
    var speedRatio: Double = 1.0

    private var _ptsOld: Long = -1L
    private var _timeStampOldUs: Long = -1L

    fun init() {
        _ptsOld = -1L
        _timeStampOldUs = -1L
    }

    fun waitBeforeRenderFrame(ptsNew: Long) {
        val nowUs = System.nanoTime() / 1_000

        val initialized = when {
            _ptsOld < 0L -> false
            _timeStampOldUs < 0L -> false
            else -> true
        }

        if (initialized) {
            var waitTimeUs = (abs(ptsNew - _ptsOld) / speedRatio) - (nowUs - _timeStampOldUs)
            waitTimeUs = max(waitTimeUs, 0.0)
            runBlocking { delay(waitTimeUs.toLong() / 1_000) }
        }

        _ptsOld = ptsNew
        _timeStampOldUs = nowUs
    }
}