package com.hhd.myandvideotest.myplayvideo

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import com.hhd.myandvideotest.util.LogEx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

class MyVideoPlayer {

    private val _FRAME_SKIP_MS: Int = 1_000
    private var _extractor: MediaExtractor? = null
    private var _fps: Int = 0
    private var _isPlaying = false
    private var _lastRenderUs: Long = 0
    private var _vCodec: MediaCodec? = null
    private var _videoDurationUs: Long = 0
    var speedRatio = 1.0

    fun play(file: File, renderSurface: Surface) {
        if (_isPlaying) {
            LogEx.w("already playing ...")
            return
        }

        _isPlaying = true
        _extractor = MediaExtractor()
        _extractor!!.setDataSource(file.toString())

        for (i in 0 until _extractor!!.trackCount) {
            _extractor!!.unselectTrack(i)
        }

        for (i in 0 until _extractor!!.trackCount) {
            val fmt = _extractor!!.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME)

            if (mime.contains("video/")) {
                LogEx.value("fmt", fmt)
                _extractor!!.selectTrack(i)
                _vCodec = MediaCodec.createDecoderByType(mime)
                _vCodec!!.configure(fmt, renderSurface, null, 0)
                _videoDurationUs = fmt.getLong(MediaFormat.KEY_DURATION)
                _fps = fmt.getInteger(MediaFormat.KEY_FRAME_RATE)
                break
            }
        }

        _vCodec!!.start()
    }

    enum class ReturnCodes {
        CONTINUE,
        EOS,
        STOP,
    }

    fun processCodecInputBuffer(): ReturnCodes {
        if (!_isPlaying)
            return ReturnCodes.STOP

        LogEx.d("")
        val isEos =
            _extractor!!.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM ==
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM

        if (isEos) {
            LogEx.i("EOS")
            return ReturnCodes.EOS
        }

        val idxQueue = ArrayDeque<Int>()

        while (true) {
            if (!_isPlaying)
                return ReturnCodes.STOP

            val idx = _vCodec!!.dequeueInputBuffer(0)

            if (idx >= 0) {
                idxQueue.add(idx)
                LogEx.d("idxQueue.add(idx[${idx}])")
            } else {
//                    LogEx.w("dequeueInputBuffer idx[$idx] break")
                break
            }
        }

        for (i in 0 until idxQueue.size) {
            if (!_isPlaying)
                return ReturnCodes.STOP

            val index = idxQueue.remove()
            val buf = _vCodec!!.getInputBuffer(index)
            var readSize = _extractor!!.readSampleData(buf!!, 0)
            var flags = _extractor!!.sampleFlags
            var pts = _extractor!!.sampleTime

            if (readSize <= 0) {
                readSize = 0
                pts = 0
                flags = flags or MediaCodec.BUFFER_FLAG_END_OF_STREAM
            }

            LogEx.d("queueInputBuffer index[${index}] readSize[${readSize}] pts[${pts}]")
            _vCodec!!.queueInputBuffer(index, 0, readSize, pts, flags)
            _extractor!!.advance()
        }

        return ReturnCodes.CONTINUE
    }

    fun processCodecOutputBuffer() {
        val idxBufferInfoQueue = ArrayDeque<Pair<Int, MediaCodec.BufferInfo>>()

        while (true) {
            if (!_isPlaying)
                return

            val info = MediaCodec.BufferInfo()
            val idx = _vCodec!!.dequeueOutputBuffer(info, 0)


            if (idx >= 0) {
                LogEx.d("dequeueOutputBuffer idx[$idx] pts[${info.presentationTimeUs}]")
                idxBufferInfoQueue.add(Pair(idx, info))
                LogEx.d("idxBufferInfoQueue.add(idx[${idx}], info[${info}])")
            } else {
//                    LogEx.w("dequeueOutputBuffer idx[$idx] break")
                break
            }
        }


        for (i in 0 until idxBufferInfoQueue.size) {
            if (!_isPlaying)
                return

            val pair = idxBufferInfoQueue.remove()
            val idx = pair.first
            val bufInfo = pair.second
            LogEx.d("releaseOutputBuffer idx[${idx}], bufInfo[${bufInfo}]")
            val waitTimeMs = _getWaitTimeMs(bufInfo)

            if (waitTimeMs > 0) {
                runBlocking { delay(waitTimeMs) }
            }

            _vCodec!!.releaseOutputBuffer(idx, true)
        }

    }

    fun stop() {
        if (!_isPlaying)
            return

        _isPlaying = false
        _vCodec!!.stop()
        _vCodec!!.release()
        _vCodec = null
        _extractor = null
        _lastRenderUs = 0
        _videoDurationUs = 0
        this.speedRatio = 1.0
    }

    private fun _getWaitTimeMs(lastFrameBufferInfo: MediaCodec.BufferInfo): Long {
        val nowUs = System.nanoTime() / 1_000
        val waitTimeUs = (1_000_000 / _fps) * (1 / this.speedRatio) - (nowUs - _lastRenderUs)
        _lastRenderUs = nowUs
        val waitTimeMs = (waitTimeUs / 1_000).toLong()
        LogEx.value("waitTimeMs", waitTimeMs)
        return waitTimeMs
    }
}