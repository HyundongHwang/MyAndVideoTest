package com.hhd.myandvideotest.arfit_thread_proto

import android.graphics.Canvas
import com.naver.videocelltech.logsloth.LogSloth
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

class ArfitPipeLine {

    private val _channel_main = Channel<List<Any>>()

    private val _t_calc = CoroutineScope(newSingleThreadContext("T_CALC"))
    private val _channel_calc = Channel<List<Any>>()

    private val _cam = MyCameraX()
    private val _moveNet = MyMoveNet()
    private val _poseAnalyzer = MyPoseAnalyzer()
    private val _touchAnalyzer = MyTouchAnalyzer()
    private val _renderer = MyRenderer()

    private var _touchResults: MyTouchResults? = null
    private var _poseInfo: MyPoseInfo? = null
    private var _landMarks: MyLandMarks? = null

    // https://stackoverflow.com/questions/44589669/correctly-implementing-wait-and-notify-in-kotlin
    // val _lock = ReentrantLock()
    // val _lock_condition = _lock.newCondition()

    ////////////////////////////////////////////////////////////////////////////////
    // 1.
    // 파이프라인의 초기화
    // 메인스레드의 소비자루프, 계산스레드의 소비자루프가 초기화됨.
    init {
        MainScope().launch {
            _channel_main.consumeEach {
                val cmd = it[0] as _Cmds

                when (cmd) {
                    _Cmds.OPEN_CAM -> {
                        ////////////////////////////////////////////////////////////////////////////////
                        // 3.
                        // 카메라 열기는 여기(메인스레드의 채널 소비자 루프) 수행되므로 시각적 직관적으로도 알기쉽고 메인스레드 실행이 강제됨.
                        val canvas = it[1] as Canvas
                        _openCam(canvas)
                    }
                    _Cmds.CLOSE_CAM -> {
                        _cam.close()
                    }
                    _Cmds.IMG_RECV -> {
                        val canvas = it[1] as Canvas
                        val image = it[2] as MyImage
                        _draw(canvas, image)
                    }
                }
            }
        }

        _t_calc.launch {
            _channel_calc.consumeEach {
                val cmd = it[0] as _Cmds

                when (cmd) {
                    _Cmds.IMG_RECV -> {
                        val canvas = it[1] as Canvas
                        val image = it[2] as MyImage
                        _calc(image)
                    }
                }
            }
        }
    }

    fun openCam(canvas: Canvas) {
        ////////////////////////////////////////////////////////////////////////////////
        // 2.
        // 카메라 열기 명령어 전송
        // 카메라 열기는 CameraX의 제한사항상 꼭 메인스레드에서만 수행해야 함.
        // 하지만 외부의 호출자는 그런걸 알수 없음.
        // 그래서 외부의 호출자가 아무 스레드에서 막 호출해도 문제 없도록 메인스레드의 채널로 명령어와 인자들만 전송함.
        // 채널에 전송하는 함수 자체도 suspend 이라서 사용하기 불편하니 GlobalScope.launch 이용해서 스레드풀 내부에서 스레드 하나 얻어서 블로킹도 없으면서 자원도 아껴서 수행함.
        GlobalScope.launch {
            _channel_main.send(listOf(_Cmds.OPEN_CAM, canvas))
        }
    }

    fun closeCam() {
        GlobalScope.launch {
            _channel_main.send(listOf(_Cmds.CLOSE_CAM))
        }
    }

    private fun _calc(image: MyImage) {
        _landMarks = _moveNet.calcLandMarks(image)
        _poseInfo = _poseAnalyzer.calcPoseInfo(_landMarks!!)
        _touchResults = _touchAnalyzer.calcTouchResults(_poseInfo!!)
    }

    private fun _draw(
        canvas: Canvas,
        image: MyImage
    ) {
        _renderer.draw(
            canvas,
            image,
            _landMarks,
            _poseInfo,
            _touchResults
        )
    }

    private fun _openCam(canvas: Canvas) {
        _cam.open {
            ////////////////////////////////////////////////////////////////////////////////
            // 4.
            // 여기가 중요함.
            // 이미지 수신 콜백은 CameraX 내부 스레드이지만 호출자도 파이프라인 개발자도 서로 쉽게 알수 없는 상태
            // 스레드 분석 할 필요도 없이, 다음 요구사항인 분석작업들과 렌더링작업을 처리할 수 있다.
            //
            // 요구사항과 제약사항은 아래와 같음.
            // 분석작업은 이미지프레임에 순서종속되어 각 단계가 끝까지 순차적으로 처리되야 하고 메인스레드가 아니라도 상관없음.
            // 렌더링작업은 이미지프레임에 순서종속되지만 렌더러 한단계 뿐이고 메인스레드에서 처리되야 하고, 특히 빨리 처리되야 함.
            //
            // 그러므로 이미지프레임을 수신하는 즉시 메인스레드에 렌더링을 명령요청하고, 랜드마크/포즈정보/터치정보 등 데이타들은 최신캐시를 그냥 사용함.
            // 그리고 분석계산작업들은 이미지프레임 수신하는 속도보다 느리니깐 이미지프레임 수신할때마다 분석계산채널을 계속 비우면서 마지막 한개의 요청만 전달해서 마지막 요청만 처리할 수 있도록 함.
            GlobalScope.launch {
                val img = it
                _channel_main.send(listOf(_Cmds.IMG_RECV, canvas, img))

                while (!_channel_calc.isEmpty) {
                    LogSloth.d("_channel_calc.receive() ...")
                    _channel_calc.receive()
                }

                _channel_calc.send(listOf(_Cmds.IMG_RECV, canvas, img))
            }
        }
    }



    private enum class _Cmds {
        OPEN_CAM,
        CLOSE_CAM,
        IMG_RECV,
    }
}