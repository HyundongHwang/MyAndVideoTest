package com.hhd.myandvideotest.arfit_thread_proto

import android.graphics.Canvas
import com.naver.videocelltech.logsloth.LogSloth
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

////////////////////////////////////////////////////////////////////////////////
// 구식 복잡한 콜백 모델
class ArfitOldComplexCallbackModel {
    private val _cam = MyCameraX()
    private val _moveNet = MyMoveNet()
    private val _poseAnalyzer = MyPoseAnalyzer()
    private val _touchAnalyzer = MyTouchAnalyzer()
    private val _renderer = MyRenderer()

    ////////////////////////////////////////////////////////////////////////////////
    // 1.
    // 카메라 열기
    fun openCam(canvas: Canvas) {
        LogSloth.enter()


        _cam.open {
            ////////////////////////////////////////////////////////////////////////////////
            // 2.
            // 카메라 오픈후에 이미지가 한장씩 수신됨
            // 카메라 모듈 CCD에서 데이타를 읽어서 색공간변환, 화질개선처리 등 진행하느라 약간 시간이 걸리고
            // 이때문에 CameraX에서는 내부에서 스레드를 만들어서 콜백호출해줌
            val img = it
            _onImgRecv(img, canvas)
        }

        LogSloth.leave()
    }

    ////////////////////////////////////////////////////////////////////////////////
    // 5.
    // 카메라 닫기
    // 카메라가 닫히면 내부의 스레드가 취소상태로 바뀌면서 루프탈출로 종료된다.
    // 당연히 이미지 콜백도 더는 안오게 되서 마지막 분석, 렌더링 작업까지 정상적으로 마칠수 있음.
    fun closeCam() {
        LogSloth.enter()
        _cam.close()
        LogSloth.leave()
    }

    private fun _onImgRecv(
        img: MyImage,
        canvas: Canvas
    ) {
        LogSloth.enter()

        ////////////////////////////////////////////////////////////////////////////////
        // 3.
        // 랜드마크추출, 포즈분석, 터치분석, 그외 경기기록 조회/작성 등 갖가지 작업들
        // 카메라가 리턴해준 스레드에서 처리해도 무방하지만 MlKit 같은 경우는 랜드마크 추출시에 내부에서 또 스레드를 만들어서 콜백으로 리턴을 해줬음.
        // 각 작업들은 이미지프레임부터 터치분석까지 순서가 종속되어 직렬적으로 수행되야 함.
        // 시간이 오래 걸리고 (약 80ms) 그 때문에 콜백의 리턴을 늦게 하니깐 CameraX의 다음콜백도 늦게 오고 다음콜백의 프레임은 바로 다음이 아닌 세번쯤 건너뛴 프레임이 옴.
        val landMarks = _moveNet.calcLandMarks(img)
        val poseInfo = _poseAnalyzer.calcPoseInfo(landMarks)
        val touchResults = _touchAnalyzer.calcTouchResults(poseInfo)

        ////////////////////////////////////////////////////////////////////////////////
        // 4.
        // 렌더링자체는 메인스레드에 작업요청을 하기 때문에 오히려 블록이 안되지만,
        // 어차피 벌써 위에서 시간 다 끌어 버렸다.
        MainScope().launch {
            _draw(canvas, img, landMarks, poseInfo, touchResults)
        }

        LogSloth.leave()
    }

    private fun _draw(
        canvas: Canvas,
        img: MyImage,
        landMarks: MyLandMarks,
        poseInfo: MyPoseInfo,
        touchResults: MyTouchResults
    ) {
        LogSloth.enter()

        _renderer.draw(
            canvas,
            img,
            landMarks,
            poseInfo,
            touchResults
        )

        LogSloth.leave()
    }
}