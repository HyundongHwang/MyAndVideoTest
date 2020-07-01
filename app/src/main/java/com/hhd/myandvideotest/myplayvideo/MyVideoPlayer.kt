package com.hhd.myandvideotest.myplayvideo

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import com.hhd.myandvideotest.util.LogEx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.max


class MyVideoPlayer {


    var curPts: Long = -1L
        private set

    var width: Int = -1
        private set

    var height: Int = -1
        private set

    var rotation: Int = 0
        private set

    var durationUs: Long = -1L
    var fps: Int = -1
    var isPlay = false
    var isReverse = false
    var speedRatio = 1.0


    private var _extractor: MediaExtractor? = null
    private var _lastRenderTimeUs: Long = -1L
    private var _maxVcodecBufSize = 0
    private var _vCodec: MediaCodec? = null
    private var _videoFrameIndexMap: MutableList<MutableList<Long>>? = null


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
                this.durationUs = fmt.getLong(MediaFormat.KEY_DURATION)
                this.width = fmt.getInteger(MediaFormat.KEY_WIDTH)
                this.height = fmt.getInteger(MediaFormat.KEY_HEIGHT)

                try {
                    this.rotation = fmt.getInteger(MediaFormat.KEY_ROTATION)
                } catch (ex: Exception) {
                    this.rotation = 0
                }

                this.fps = fmt.getInteger(MediaFormat.KEY_FRAME_RATE)
                break
            }
        }

        _videoFrameIndexMap = mutableListOf()
        var videoFrameIndexListGop = mutableListOf<Long>()

        while (true) {
            if (_extractor!!.sampleFlags == MediaExtractor.SAMPLE_FLAG_SYNC) {
                videoFrameIndexListGop = mutableListOf<Long>()
                _videoFrameIndexMap!!.add(videoFrameIndexListGop)
            }

            videoFrameIndexListGop.add(_extractor!!.sampleTime)
            val res = _extractor!!.advance()

            if (!res)
                break
        }

        _vCodec!!.start()

        repeat(100) {
            val idx = _vCodec!!.dequeueInputBuffer(0)

            if (idx >= 0)
                _maxVcodecBufSize++
        }

        _vCodec!!.flush()
        _extractor!!.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    fun seekPrevFrame() {
        val ptsInGopList = _videoFrameIndexMap!!.first { this.curPts <= it.last() }
        val idxPts = ptsInGopList.indexOf(this.curPts)
        var prevPts = 0L

        if (idxPts > 0) {
            prevPts = ptsInGopList[idxPts - 1]
        } else {
            val idxList = _videoFrameIndexMap!!.indexOf(ptsInGopList)

            if (idxList > 0) {
                prevPts = _videoFrameIndexMap!![idxList - 1].last()
            }
        }

        this.seek(prevPts)
    }

    fun seek(pts: Long) {
        val lastPts = _videoFrameIndexMap!!.last().last()

        var caliPts = when {
            pts < 0 -> {
                0L
            }
            pts > lastPts -> {
                lastPts
            }
            else -> {
                pts
            }
        }

        val ptsInGopList = _videoFrameIndexMap!!.first { caliPts <= it.last() }
        var idxNear = 0

        if (caliPts <= ptsInGopList.first()) {
            caliPts = ptsInGopList.first()
        } else if (caliPts >= ptsInGopList.last()) {
            caliPts = ptsInGopList.last()
        } else {
            for (i in 0 until ptsInGopList.size - 1) {
                if (ptsInGopList[i] <= caliPts && caliPts <= ptsInGopList[i + 1]) {
                    caliPts = ptsInGopList[i]
                    break
                }
            }
        }

        val keyFramePts = ptsInGopList.first()
        val idx = ptsInGopList.indexOf(caliPts)
        _extractor!!.seekTo(keyFramePts, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        if (idx > 0) {
            repeat(idx - 1) {
                decodeRender(false)
                _extractor!!.advance()
            }
        }

        decodeRender(true)
    }

    fun decodeRender(render: Boolean): Boolean {
        if (_vCodec == null)
            return false

        if (_extractor == null)
            return false

        val isEos =
            _extractor!!.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM ==
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM

        if (isEos) {
            LogEx.d("EOS pts[${_extractor!!.sampleTime}]")
            this.isPlay = false
            return false
        }

        var idx = 0

        while (true) {
            idx = _vCodec!!.dequeueInputBuffer(0)

            if (idx >= 0)
                break
        }

        val buf = _vCodec!!.getInputBuffer(idx)
        val readSize = _extractor!!.readSampleData(buf!!, 0)
        val flags = _extractor!!.sampleFlags
        val pts = _extractor!!.sampleTime
        _vCodec!!.queueInputBuffer(idx, 0, readSize, pts, flags)

        LogEx.d(
            "INBUF " +
                    "idx[${idx}] " +
                    "pts[${pts}] " +
                    "readSize[${readSize}] " +
                    "flags[${flags}] "
        )

        val info = MediaCodec.BufferInfo()

        while (true) {
            idx = _vCodec!!.dequeueOutputBuffer(info, 0)

            if (idx >= 0)
                break
        }

        LogEx.d(
            "OUTBUF " +
                    "idx[${idx}] " +
                    "inBufPts[${pts}] " +
                    "outBufPts[${info.presentationTimeUs}] " +
                    "size[${info.size}]" +
                    "flags[${info.flags}] " +
                    "render[${render}]"
        )

        if (render) {
            _lastRenderTimeUs = System.nanoTime() / 1_000
            this.curPts = pts
        }

        _vCodec!!.releaseOutputBuffer(idx, render)
        return true
    }

    fun stop() {
        this.curPts = -1L
        this.width = -1
        this.height = -1
        this.durationUs = -1L
        this.fps = -1
        this.isPlay = false
        this.isReverse = false
        this.speedRatio = 1.0

        if (_extractor != null) {
            _extractor!!.release()
            _extractor = null
        }

        _lastRenderTimeUs = -1L
        _maxVcodecBufSize = 0

        if (_vCodec != null) {
            _vCodec!!.stop()
            _vCodec!!.release()
            _vCodec = null
        }

        _videoFrameIndexMap = null
    }

    fun getWaitTimeMs(): Long {
        val nowUs = System.nanoTime() / 1_000
        val waitTimeUs =
            (1_000_000 / this.fps) * (1 / this.speedRatio) - (nowUs - _lastRenderTimeUs)
        val waitTimeMs = (waitTimeUs / 1_000).toLong()
        LogEx.value("waitTimeMs", waitTimeMs)
        return waitTimeMs
    }

    fun advance() {
        _extractor!!.advance()
    }

    fun decodeReverse(pts: Long): Pair<MutableList<Pair<Int, Long>>, Long> {
        var targetPts = -1L

        if (pts >= 0) {
            targetPts = pts
        } else {
            targetPts = _videoFrameIndexMap!!.last().last()
        }

        val ptsInGopList =
            _videoFrameIndexMap!!.first { it.first() <= targetPts && targetPts <= it.last() }
        val keyFramePts = ptsInGopList.first()
        val targetIdx = ptsInGopList.indexOf(targetPts)
        val subGopFirstIdx = max(targetIdx - _maxVcodecBufSize + 1, 0)
        val subGopSize = targetIdx - subGopFirstIdx + 1

        _extractor!!.seekTo(keyFramePts, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        repeat(subGopFirstIdx) {
            this.decodeRender(false)
            _extractor!!.advance()
        }

        val inBufIdxList = mutableListOf<Int>()

        repeat(subGopSize) {
            val idx = _vCodec!!.dequeueInputBuffer(0)

            if (idx >= 0) {
                inBufIdxList.add(idx)
                LogEx.d("dequeueInputBuffer idx[${idx}]")
            }
        }

        val inBufIdxPtsList = mutableListOf<Pair<Int, Long>>()

        for (idx in inBufIdxList) {
            val buf = _vCodec!!.getInputBuffer(idx)
            var readSize = _extractor!!.readSampleData(buf!!, 0)
            var flags = _extractor!!.sampleFlags
            var pts = _extractor!!.sampleTime

            if (readSize <= 0) {
                readSize = 0
                pts = 0
                flags = flags or MediaCodec.BUFFER_FLAG_END_OF_STREAM
            }

            LogEx.d("queueInputBuffer idx[${idx}] pts[${pts}] readSize[${readSize}]")
            _vCodec!!.queueInputBuffer(idx, 0, readSize, pts, flags)
            _extractor!!.advance()
            inBufIdxPtsList.add(Pair(idx, pts))
        }

        val outBufIdxList = mutableListOf<Int>()

        while (true) {
            repeat(subGopSize) {
                val info = MediaCodec.BufferInfo()
                val idx = _vCodec!!.dequeueOutputBuffer(info, 0)

                if (idx >= 0) {
                    LogEx.d("dequeueOutputBuffer idx[$idx] pts[${info.presentationTimeUs}]")
                    outBufIdxList.add(idx)
                }
            }

            if (outBufIdxList.size == subGopSize) {
                break
            } else {
                runBlocking { delay(10) }
            }
        }

        val outBufIdxPtsList = mutableListOf<Pair<Int, Long>>()

        for (i in outBufIdxList.size - 1 downTo 0) {
            val idx = outBufIdxList[i]
            val pts = inBufIdxPtsList[i].second
            outBufIdxPtsList.add(Pair(idx, pts))
        }

        var prevPts = -1L

        if (subGopFirstIdx > 0) {
            prevPts = ptsInGopList[subGopFirstIdx - 1]
        } else {
            val curPtsGopIdx = _videoFrameIndexMap!!.indexOf(ptsInGopList)

            if (curPtsGopIdx > 0)
                prevPts = _videoFrameIndexMap!![curPtsGopIdx - 1].last()
        }

        return Pair(outBufIdxPtsList, prevPts)
    }

    fun renderIdxOutBuf(idxOutBuf: Int, pts: Long) {
        LogEx.d("releaseOutputBuffer idx[$idxOutBuf] pts[${pts}]")
        _lastRenderTimeUs = System.nanoTime() / 1_000
        this.curPts = pts
        _vCodec!!.releaseOutputBuffer(idxOutBuf, true)
    }

    fun test() {
        _vCodec!!.flush()
    }
}