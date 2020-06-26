package com.hhd.myandvideotest.myplayvideo

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import com.hhd.myandvideotest.util.LogEx
import java.io.File

class MyVideoPlayer {

    var curPts: Long = -1L
    var fps: Int = -1
    var isPlay = false
    var speedRatio = 1.0
    var videoDurationUs: Long = -1L

    private var _extractor: MediaExtractor? = null
    private var _keyFramePtsList: MutableList<Long>? = null
    private var _lastRenderTimeUs: Long = -1L
    private var _vCodec: MediaCodec? = null

    fun prepare(srcFile: File, renderSurface: Surface) {
        _extractor = MediaExtractor()
        _extractor!!.setDataSource(srcFile.toString())

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
                this.videoDurationUs = fmt.getLong(MediaFormat.KEY_DURATION)
                this.fps = fmt.getInteger(MediaFormat.KEY_FRAME_RATE)
                break
            }
        }

        _keyFramePtsList = mutableListOf<Long>()

        while (true) {
            if (_extractor!!.sampleFlags == MediaExtractor.SAMPLE_FLAG_SYNC)
                _keyFramePtsList!!.add(_extractor!!.sampleTime)

            val res = _extractor!!.advance()

            if (!res)
                break
        }

        _vCodec!!.start()
    }

    fun seek(pts: Long) {
        _vCodec!!.flush()
        _extractor!!.seekTo(pts, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    fun decodeRender(): Boolean {

        if (_vCodec == null)
            return false

        if (_extractor == null)
            return false

        val isEos =
            _extractor!!.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM ==
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM

        if (isEos) {
            LogEx.i("EOS")
            this.isPlay = false
            return false
        }

        var idx = 0

        while (true) {
            idx = _vCodec!!.dequeueInputBuffer(0)
            LogEx.d("dequeueInputBuffer idx[${idx}]", idx)

            if (idx >= 0)
                break
        }

        val buf = _vCodec!!.getInputBuffer(idx)
        val readSize = _extractor!!.readSampleData(buf!!, 0)
        val flags = _extractor!!.sampleFlags
        val pts = _extractor!!.sampleTime
        LogEx.d("queueInputBuffer idx[${idx}] readSize[${readSize}] flags[${flags}] pts[${pts}]")
        _vCodec!!.queueInputBuffer(idx, 0, readSize, pts, flags)
        val info = MediaCodec.BufferInfo()

        while (true) {
            idx = _vCodec!!.dequeueOutputBuffer(info, 0)

            if (idx >= 0)
                break
        }

        LogEx.d(
            "dequeueOutputBuffer " +
                    "idx[${idx}] " +
                    "info.presentationTimeUs[${info.presentationTimeUs}] " +
                    "info.flags[${info.flags}] " +
                    "info.size[${info.size}]"
        )

        this.curPts = info.presentationTimeUs
        _vCodec!!.releaseOutputBuffer(idx, true)
        return true
    }


    fun stop() {
        _vCodec!!.stop()
        _vCodec!!.release()
        _vCodec = null
        _extractor = null
        _lastRenderTimeUs = 0
        this.speedRatio = 1.0
        this.curPts = -1L
        this.fps = -1
        this.isPlay = false
        this.speedRatio = 1.0
        this.videoDurationUs = -1L
    }

    fun getWaitTimeMs(): Long {
        val nowUs = System.nanoTime() / 1_000
        val waitTimeUs = (1_000_000 / this.fps) * (1 / this.speedRatio) - (nowUs - _lastRenderTimeUs)
        _lastRenderTimeUs = nowUs
        val waitTimeMs = (waitTimeUs / 1_000).toLong()
        LogEx.value("waitTimeMs", waitTimeMs)
        return waitTimeMs
    }

    fun advance() {
        _extractor!!.advance()
    }


//    fun __decodeRender() {
//        LogEx.d("")
//        val isEos =
//            _extractor!!.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM ==
//                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
//
//        if (isEos) {
//            LogEx.i("EOS")
//            return
//        }
//
//        val idxQueue = ArrayDeque<Int>()
//
//        while (true) {
//            val idx = _vCodec!!.dequeueInputBuffer(0)
//
//            if (idx >= 0) {
//                idxQueue.add(idx)
//                LogEx.d("idxQueue.add(idx[${idx}])")
//            } else {
//                LogEx.w("dequeueInputBuffer idx[$idx] break")
//                break
//            }
//        }
//
//        for (i in 0 until idxQueue.size) {
//            val index = idxQueue.remove()
//            val buf = _vCodec!!.getInputBuffer(index)
//            var readSize = _extractor!!.readSampleData(buf!!, 0)
//            var flags = _extractor!!.sampleFlags
//            var pts = _extractor!!.sampleTime
//
//            if (readSize <= 0) {
//                readSize = 0
//                pts = 0
//                flags = flags or MediaCodec.BUFFER_FLAG_END_OF_STREAM
//            }
//
//            LogEx.d("queueInputBuffer index[${index}] readSize[${readSize}] pts[${pts}]")
//            _vCodec!!.queueInputBuffer(index, 0, readSize, pts, flags)
//            _extractor!!.advance()
//        }
//
//        val idxBufferInfoQueue = ArrayDeque<Pair<Int, MediaCodec.BufferInfo>>()
//
//        while (true) {
//            val info = MediaCodec.BufferInfo()
//            val idx = _vCodec!!.dequeueOutputBuffer(info, 0)
//
//
//            if (idx >= 0) {
//                LogEx.d("dequeueOutputBuffer idx[$idx] pts[${info.presentationTimeUs}]")
//                idxBufferInfoQueue.add(Pair(idx, info))
//                LogEx.d("idxBufferInfoQueue.add(idx[${idx}], pts[${info.presentationTimeUs}])")
//            } else {
////                    LogEx.w("dequeueOutputBuffer idx[$idx] break")
//                break
//            }
//        }
//
//        for (i in 0 until idxBufferInfoQueue.size) {
//            var pair: Pair<Int, MediaCodec.BufferInfo>? = null
//
//            if (this.speedRatio > 0) {
//                pair = idxBufferInfoQueue.removeFirst()
//            } else {
//                pair = idxBufferInfoQueue.removeLast()
//            }
//
//            val idx = pair.first
//            val bufInfo = pair.second
//            LogEx.d("releaseOutputBuffer idx[${idx}], pts[${bufInfo.presentationTimeUs}]")
//            val waitTimeMs = getWaitTimeMs()
//
//            if (waitTimeMs > 0) {
//                runBlocking { delay(waitTimeMs) }
//            }
//
//            _curPts = bufInfo.presentationTimeUs
//            _vCodec!!.releaseOutputBuffer(idx, true)
//        }
//    }

}