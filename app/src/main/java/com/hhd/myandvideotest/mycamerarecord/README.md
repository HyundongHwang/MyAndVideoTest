# MyCameraRecord

## 소개

- grafika 예제중 [ContinuousCapture](https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/ContinuousCaptureActivity.java) 를 RxJava를 사용하여 파이프라인, 멀티쓰레드 구현을 우아하게? 재구현 했음.
- ContinuousCapture는 카메라, MediaCodec, GL, SurfaceTexture, MediaMuxer를 모두 사용하는 나름 종합적인 예제라서 선택함
- Kotlin을 사용했기 때문에 정확히는 RxKotlin을 사용했음.



## 개선사항

- 컴포넌트 분리 구현

    - 기존 
        - 뷰에 카메라, 렌더러, 추가렌더러가 뭉쳐져 있으며, 
        - 인코더가 circular buffer를 포함한 복잡한 구조였음.
    - 개선
        - view, 카메라, 렌더러, 추가렌더러, 인코더를 모두 분리함

- RxJava로 파이프라인, 멀티쓰레드 구현 개선

    - 기존
        - 뷰에 많은 코드들이 뭉쳐져 있는 문제도 있었고,
        - MediaCodec의 start/stop/drain, MediaMuxer의 write등 지역적으로 쓰레드가 생성되고 핸들러로 메시지 보내서 통신하는 방식이라 시퀀스 이해가 어려움.
    - 개선
        - 모든 컴포넌트의 내부 구현에서는 스레드 관련 코드는 전혀 없이 모두 단일스레드 구현 이며,
        - 파이프라인 클래스 내부에서 이를 RxJava의 PublishSubject로 각 컴포넌트를 연결하고 동작지점마다 스케줄러로 스레드를 명시함.

- 시간무한 레코딩

    - 기존
        - 기존에는 6초? 정도의 큰 메모리를 미리 생성하고 이를 이용해서 codec drain -> muxer write 로 활용하는 구조였는데
    - 개선
        - RxJava의 파이프라이닝 기능을 이용해서 원형버퍼 구현을 없앨수 있어서 시간무한 레코딩이 가능함.

- 렌더링 사이즈 오류 수정

    - 기존
        - 뷰의 EglSurface와 인코더의 EglSurface가 크기를 따로 관리해야 해서 약간 까다로움
        - 이때문에 ContinuousCapture 예제를 portrait로 촬영/인코딩 하면 화면이 종횡비 깨짐
    - 개선
        - 뷰와 인코더의 렌더링시에 동적으로 각 EglSurface로 부터 정확한 크기를 얻어서 GL 렌더링을 해서 뷰와 인코더 크기가 달라도 문제 없음.

- EGL관련 코드 리펙토링

    - EglCore관련 코드들은 필드로 유지할 필요가 없어서 static util로 단순화
    - EglSurface관련 코드들은 pbuffer/window 타입 공통으로 클래스 한개로 단순화

- RuntimePermission 문제 수정

    


## 데모

<iframe width="560" height="315" src="https://www.youtube.com/embed/dNJWjYlF2uc" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

## 파이프라인 구성

<!--
@startuml

rectangle MyPipeLine {
	rectangle MyCamera {
	}
	rectangle MyRenderer {
		collections MyExtraRenderer
	}
	rectangle MyEncoder {
		rectangle MediaCodec
    	rectangle MediaMuxer
	}
}

MyCamera --> MyRenderer
MyExtraRenderer <--> MyExtraRenderer
MyRenderer --> MyEncoder
MediaCodec <--> MediaCodec
MediaCodec --> MediaMuxer
@enduml
-->

![](https://i.postimg.cc/8PpBwff9/screenshot-13.png)

## 시퀀스 다이어그램



<!--
@startuml
participant MyCameraRecordActivity
participant MyPipeLine
participant "MyPipeLine::_pipeline" as _pipeline
participant MyCamera
participant MyRenderer
participant MyExtraRenderer
participant MyEncoder

== _camera_start ==

MyCameraRecordActivity -> MyPipeLine : startCamera
MyPipeLine -> _pipeline : onNext(START_CAMERA, ...)
_pipeline -> MyCamera : 생성
_pipeline -> MyRenderer : 생성
_pipeline -> MyRenderer : add(MyExtraRenderer 생성)
_pipeline -> MyCamera : start
_pipeline -> MyRenderer : eglSurfaceList.add(camera.dispEglSurface)
_pipeline -> MyRenderer : start
_pipeline -> MyCamera : startPreview

MyCamera -> MyPipeLine : onFrameAvailable
MyPipeLine -> _pipeline : onNext(FRAME_AVAILABLE, st)
_pipeline -> MyRenderer : render(st)
MyRenderer -> MyExtraRenderer : render(w, h, frameNum)

== _record_start ==

MyCameraRecordActivity -> MyPipeLine : startEncoder
MyPipeLine -> _pipeline : onNext(START_ENCODER, ...)
_pipeline -> MyEncoder : 생성
_pipeline -> MyEncoder : start
_pipeline -> MyRenderer : eglSurfaceList.add(encoder.inputEglSurface)
MyCamera -> MyPipeLine : onFrameAvailable
MyPipeLine -> _pipeline : onNext(FRAME_AVAILABLE, st)
_pipeline -> MyEncoder : drain
_pipeline -> _pipeline : onNext(WRITE_TO_FILE, bufPairArray)
_pipeline -> MyEncoder : writeToFile(bufParrArray)

@enduml
-->


![](https://i.postimg.cc/nzg7xTHw/screenshot-13.png)

## 핵심코드

```kotlin
_pipeline = PublishSubject.create<Array<Any>>()

_pipeline
.observeOn(AndroidSchedulers.mainThread())
.map {
    val paramArray = it as Array<*>
    val cmd = paramArray[0] as _Commands

    when (cmd) {
        _Commands.START_CAMERA -> {
            val surface = paramArray[1] as Surface
            val videoWidth = paramArray[2] as Int
            val videoHeight = paramArray[3] as Int

            _camera!!.start(
                    surface,
                    videoWidth,
                    videoHeight,

            _renderer!!.start()

            _camera!!.startPreview(
                    _renderer!!.textureId,
                    { st ->
                        _pipeline.onNext(
                                arrayOf(
                                        _Commands.FRAME_AVAILABLE,
                                        st
                                ))
                    })

            _renderer!!.eglSurfaceList.add(_camera!!.dispEglSurface!!)
        }
        _Commands.FRAME_AVAILABLE -> {
            val st = paramArray[1] as SurfaceTexture
            _renderer!!.render(st)
        }
    }

    return@map paramArray
}
.observeOn(Schedulers.io())
.map {
    val paramArray = it as Array<*>
    val cmd = paramArray[0] as _Commands

    when (cmd) {
        _Commands.START_ENCODER -> {
            val width = paramArray[1] as Int
            val height = paramArray[2] as Int
            _encoder = MyEncoder()

            _encoder!!.start(
                    width,
                    height,

            _renderer!!.eglSurfaceList.add(_encoder!!.inputEglSurface!!)
        }
        _Commands.STOP_ENCODER -> {
            _renderer!!.eglSurfaceList.remove(_encoder!!.inputEglSurface)
            _encoder!!.stop()
            _encoder = null
        }
        _Commands.FRAME_AVAILABLE -> {
            val bufPairArray = _encoder!!.drain()
            return@map arrayOf(_Commands.WRITE_TO_FILE, bufPairArray)
        }
    }

    return@map paramArray
}
.observeOn(Schedulers.io())
.map {
    val paramArray = it as Array<*>
    val cmd = paramArray[0] as _Commands

    when (cmd) {
        _Commands.WRITE_TO_FILE -> {
            val bufParrArray = paramArray[1] as MutableList<Pair<ByteBuffer, MediaCodec.BufferInfo>>
            _encoder!!.writeToFile(bufParrArray)
        }
    }

    return@map paramArray
}
.subscribe()
```