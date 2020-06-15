package com.hhd.myandvideotest.mycamerarecord

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.hhd.myandvideotest.util.LogEx

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.ByteBuffer

class MyEncoder {

    var inputEglSurface: EglSurfaceEx? = null
        private set

    private var _encoder: MediaCodec? = null
    private var _inputSurface: Surface? = null
    private var _muxer: MediaMuxer? = null
    private var _muxerVideoTrackIdx: Int = -1
    private var _outputFile: File? = null

    fun start(width: Int,
              height: Int,
              bitRate: Int,
              frameRate: Int,
              outputFile: File,
              mimeType: String,
              iFrameInterval: Int) {
        _outputFile = outputFile
        val format = MediaFormat.createVideoFormat(mimeType, width, height)

        // 몇 가지 속성을 설정하십시오. 이들 중 일부를 지정하지 않으면 MediaCodec이 발생할 수 있습니다.
        //  도움이되지 않는 예외를 발생시키기위한 configure () 호출.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        LogEx.d("format[$format]")

        // MediaCodec 인코더를 작성하고 형식으로 구성하십시오. 표면을 얻으십시오
        //  입력에 사용하고 EGL 작업을 처리하는 클래스로 랩핑 할 수 있습니다.
        _encoder = MediaCodec.createEncoderByType(mimeType)
        _encoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        _inputSurface = _encoder!!.createInputSurface()
        _encoder!!.start()
        inputEglSurface = EglSurfaceEx(_inputSurface!!, false)
    }

    fun stop() {
        _encoder!!.stop()
        _encoder!!.release()
        _encoder = null

        _muxer!!.stop()
        _muxer!!.release()
        _muxer = null

        _inputSurface!!.release()
        _inputSurface = null

        this.inputEglSurface!!.release()
        this.inputEglSurface = null

        _outputFile = null
        _muxerVideoTrackIdx = -1
    }

    fun drain(): MutableList<Pair<ByteBuffer, MediaCodec.BufferInfo>> {
        val res = mutableListOf<Pair<ByteBuffer, MediaCodec.BufferInfo>>()
        val bufInfo = MediaCodec.BufferInfo()
        var encoderOutputBuffers: Array<ByteBuffer?> = _encoder!!.getOutputBuffers()

        while (true) {
            val encoderStatus: Int = _encoder!!.dequeueOutputBuffer(bufInfo, 0)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 아직 출력이 없습니다
                break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // 엔코더에 기대되지 않음
                encoderOutputBuffers = _encoder!!.getOutputBuffers()
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 버퍼를 받기 전에 발생해야하며 한 번만 발생해야합니다.
                //  MediaFormat에는 csd-0 및 csd-1 키가 포함되어 있습니다.
                //  MediaMuxer 용. MediaMuxer가 원하는 다른 것이 확실하지 않으므로
                //  코덱 별 데이터를 추출하고 새로운 데이터를 재구성하는 대신
                //  나중에 MediaFormat, 우리는 여기를 잡고 그것을 유지합니다.
                val outFormat = _encoder!!.getOutputFormat()
                LogEx.d("encoder output format changed: $outFormat")

                if (_muxer == null) {
                    _muxer = MediaMuxer(_outputFile!!.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    _muxerVideoTrackIdx = _muxer!!.addTrack(outFormat)
                    _muxer!!.start()
                }

            } else if (encoderStatus < 0) {
                LogEx.w("unexpected result from encoder.dequeueOutputBuffer encoderStatus[$encoderStatus]")
                // 무시하자
            } else {
                val encoderOutBuf = encoderOutputBuffers[encoderStatus]

                if (encoderOutBuf == null)
                    throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                if (bufInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // 코덱 구성 데이터는
                    //  INFO_OUTPUT_FORMAT_CHANGED 상태 MediaMuxer는 수락하지 않습니다
                    //  단일 큰 얼룩-별도의 csd-0csd-1 덩어리를 원합니다.
                    //  단순히 이것을 저장하면 작동하지 않습니다.
                    LogEx.d("ignoring BUFFER_FLAG_CODEC_CONFIG")
                    bufInfo.size = 0
                }

                if (bufInfo.size > 0) {
                    val newBuf = ByteBuffer.allocate(encoderOutBuf.remaining())
                    newBuf.put(encoderOutBuf)
                    newBuf.rewind()
                    val newBufInfo = MediaCodec.BufferInfo()
                    newBufInfo.offset = newBuf.position()
                    newBufInfo.size = newBuf.limit()
                    newBufInfo.flags = bufInfo.flags
                    newBufInfo.presentationTimeUs = bufInfo.presentationTimeUs
                    res.add(Pair(newBuf, newBufInfo))
                }

                _encoder!!.releaseOutputBuffer(encoderStatus, false)

                if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    LogEx.d("reached end of stream unexpectedly")
                    break // out of while
                }
            }
        }

        return res
    }

    fun writeToFile(bufPairList: MutableList<Pair<ByteBuffer, MediaCodec.BufferInfo>>) {
        bufPairList.forEach {
            if (_muxer == null)
                return@forEach

            val buf = it.first
            val bufInfo = it.second
            _muxer!!.writeSampleData(_muxerVideoTrackIdx, buf, bufInfo)
        }
    }
}