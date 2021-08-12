package com.hhd.myandvideotest.arfit_thread_proto

import android.graphics.Canvas
import com.naver.videocelltech.logsloth.LogSloth
import kotlinx.coroutines.*

////////////////////////////////////////////////////////////////////////////////
// 카메라, 랜드마크추출, 포즈분석, 터치분석 등 arfit의 서비스를 구성하는 살점 모듈들
// 파이프라인으로 뼈대를 이루고 그 뼈대에서만 스레드를 다루고 살점들은 콜백없는 단일스레드 동작인게 좋음.
// 만일 CameraX, MlKit 처럼 내부에 스레드가 있다면 호출자와 콜백에 ReenteranceLock 등으로 단일스레드 동작처럼 되도록 조정해 놓는게 속이 편안함.

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