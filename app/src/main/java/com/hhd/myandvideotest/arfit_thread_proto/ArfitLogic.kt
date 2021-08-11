package com.hhd.myandvideotest.arfit_thread_proto

import android.graphics.Canvas
import com.naver.videocelltech.logsloth.LogSloth
import kotlinx.coroutines.*

class MyCameraX {
    private var _t_cam_inter: CoroutineScope? = null
    private var _pts = 0L

    fun open(imgRecvCallback: (MyImage) -> Unit) {
        _t_cam_inter = CoroutineScope(newSingleThreadContext("T_CAM_INTER"))

        _t_cam_inter!!.launch {
            _pts = 0L

            while (true) {
                if (!this.isActive) {
                    LogSloth.d("if (!this.isActive) !!!")
                    break
                }

                Thread.sleep(1_000)
                imgRecvCallback(MyImage(_pts))
                _pts += 1
            }
        }
    }

    fun close() {
        _t_cam_inter?.cancel()
        _t_cam_inter = null
    }
}

data class MyImage(val pts : Long)

class MyMoveNet {
    fun calcLandMarks(img: MyImage): MyLandMarks {
        LogSloth.enter()
        LogSloth.d("img.pts:${img.pts}")
        Thread.sleep(1_000)
        LogSloth.leave()
        return MyLandMarks()
    }
}

class MyLandMarks

class MyPoseAnalyzer {
    fun calcPoseInfo(landMarks: MyLandMarks): MyPoseInfo {
        LogSloth.enter()
        Thread.sleep(2_000)
        LogSloth.leave()
        return MyPoseInfo()
    }
}

class MyPoseInfo

class MyTouchAnalyzer {
    fun calcTouchResults(poseInfo: MyPoseInfo): MyTouchResults {
        LogSloth.enter()
        Thread.sleep(3_000)
        LogSloth.leave()
        return MyTouchResults()
    }
}

class MyTouchResults

class MyRenderer {
    fun draw(
        canvas: Canvas?,
        img: MyImage?,
        landMarks: MyLandMarks?,
        poseInfo: MyPoseInfo?,
        touchResults: MyTouchResults?
    ) {
        LogSloth.enter()
        LogSloth.d("img.pts:${img!!.pts}")
        Thread.sleep(10)
        LogSloth.leave()
    }
}