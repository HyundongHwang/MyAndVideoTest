package com.hhd.myandvideotest.arfit_thread_proto

import android.graphics.Canvas
import com.naver.videocelltech.logsloth.LogSloth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

class MyCameraX {
    val _t_cam_inter = CoroutineScope(newSingleThreadContext("T_CAM_INTER"))

    fun open(imgRecvCallback: (MyImage) -> Unit) {
        _t_cam_inter.launch {
            while (true) {
                Thread.sleep(1_000)
                imgRecvCallback(MyImage())
            }
        }
    }
}

class MyImage

class MyMoveNet {
    fun calc(img: MyImage): MyLandMarks {
        LogSloth.enter()
        Thread.sleep(1_000)
        LogSloth.leave()
        return MyLandMarks()
    }
}

class MyLandMarks

class MyPoseAnalyzer {
    fun calc(landMarks: MyLandMarks): MyPoseInfo {
        LogSloth.enter()
        Thread.sleep(2_000)
        LogSloth.leave()
        return MyPoseInfo()
    }
}

class MyPoseInfo

class MyTouchAnalyzer {
    fun calc(poseInfo: MyPoseInfo): MyTouchResults {
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
        Thread.sleep(10)
        LogSloth.leave()
    }
}