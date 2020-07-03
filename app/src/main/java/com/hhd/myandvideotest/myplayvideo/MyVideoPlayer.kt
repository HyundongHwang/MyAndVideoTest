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
    var isOpen = false
    var isPlay = false
    var isReverse = false

    private var _extractor: MediaExtractor? = null
    private var _lastRenderTimeUs: Long = -1L
    private var _maxCodecBufSize = 0
    private var _decorder: MediaCodec? = null
    private var _ptsGroupByGopList: MutableList<MutableList<Long>>? = null


    fun prepare(srcFile: File, renderSurface: Surface) {
        _extractor = MediaExtractor()
        _extractor!!.setDataSource(srcFile.toString())

        for (i in 0 until _extractor!!.trackCount) {
            _extractor!!.unselectTrack(i)
        }

        var fmt: MediaFormat? = null
        var mime: String? = null
        var idxVideoTrack: Int = -1

        for (i in 0 until _extractor!!.trackCount) {
            fmt = _extractor!!.getTrackFormat(i)
            mime = fmt.getString(MediaFormat.KEY_MIME)

            if (mime.contains("video/")) {
                idxVideoTrack = i
                break
            }
        }

        if (idxVideoTrack >= 0) {
            this.isOpen = true
        } else {
            this.stop()
            return
        }

        LogEx.value("fmt", fmt)
        _extractor!!.selectTrack(idxVideoTrack)
        _decorder = MediaCodec.createDecoderByType(mime!!)
        _decorder!!.configure(fmt, renderSurface, null, 0)
        this.durationUs = fmt!!.getLong(MediaFormat.KEY_DURATION)
        this.width = fmt.getInteger(MediaFormat.KEY_WIDTH)
        this.height = fmt.getInteger(MediaFormat.KEY_HEIGHT)

        try {
            this.rotation = fmt.getInteger(MediaFormat.KEY_ROTATION)
        } catch (ex: Exception) {
            this.rotation = 0
        }

        this.fps = fmt.getInteger(MediaFormat.KEY_FRAME_RATE)

        _ptsGroupByGopList = mutableListOf()
        var videoFrameIndexListGop = mutableListOf<Long>()

        while (true) {
            if (_extractor!!.sampleFlags == MediaExtractor.SAMPLE_FLAG_SYNC) {
                videoFrameIndexListGop = mutableListOf<Long>()
                _ptsGroupByGopList!!.add(videoFrameIndexListGop)
            }

            videoFrameIndexListGop.add(_extractor!!.sampleTime)
            val res = _extractor!!.advance()

            if (!res)
                break
        }

        _decorder!!.start()

        repeat(100) {
            val idx = _decorder!!.dequeueInputBuffer(0)

            if (idx >= 0)
                _maxCodecBufSize++
        }

        _decorder!!.flush()
        _extractor!!.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    fun seekPrevFrame() {
        val ptsInGopList = _ptsGroupByGopList!!.first { this.curPts <= it.last() }
        val idxPts = ptsInGopList.indexOf(this.curPts)
        var prevPts = 0L

        if (idxPts > 0) {
            prevPts = ptsInGopList[idxPts - 1]
        } else {
            val idxList = _ptsGroupByGopList!!.indexOf(ptsInGopList)

            if (idxList > 0) {
                prevPts = _ptsGroupByGopList!![idxList - 1].last()
            }
        }

        this.seek(prevPts)
    }

    fun seek(pts: Long) {
        val lastPts = _ptsGroupByGopList!!.last().last()

        var caliPts = when {
            pts < 0 -> 0L
            pts > lastPts -> lastPts
            else -> pts
        }

        val ptsInGopList = _ptsGroupByGopList!!.first { caliPts <= it.last() }

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

        this.decodeRender(true)
    }

    fun decodeRender(render: Boolean): Boolean {
        if (_decorder == null)
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
            idx = _decorder!!.dequeueInputBuffer(0)

            if (idx >= 0)
                break
        }

        val buf = _decorder!!.getInputBuffer(idx)
        val readSize = _extractor!!.readSampleData(buf!!, 0)
        val flags = _extractor!!.sampleFlags
        val pts = _extractor!!.sampleTime
        _decorder!!.queueInputBuffer(idx, 0, readSize, pts, flags)

        LogEx.d(
            "INBUF " +
                    "idx[${idx}] " +
                    "pts[${pts}] " +
                    "readSize[${readSize}] " +
                    "flags[${flags}] "
        )

        val info = MediaCodec.BufferInfo()

        while (true) {
            idx = _decorder!!.dequeueOutputBuffer(info, 0)

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

        _decorder!!.releaseOutputBuffer(idx, render)
        return true
    }

    fun stop() {
        this.isOpen = false
        this.curPts = -1L
        this.width = -1
        this.height = -1
        this.durationUs = -1L
        this.fps = -1
        this.isPlay = false
        this.isReverse = false

        if (_extractor != null) {
            _extractor!!.release()
            _extractor = null
        }

        _lastRenderTimeUs = -1L
        _maxCodecBufSize = 0

        if (_decorder != null) {
            _decorder!!.stop()
            _decorder!!.release()
            _decorder = null
        }

        _ptsGroupByGopList = null
    }

    fun advance() {
        _extractor!!.advance()
    }

    fun decodeReverse(pts: Long): Pair<MutableList<Pair<Int, Long>>, Long> {
        var targetPts = -1L

        if (pts >= 0) {
            targetPts = pts
        } else {
            targetPts = _ptsGroupByGopList!!.last().last()
        }

        val ptsInGopList =
            _ptsGroupByGopList!!.first { it.first() <= targetPts && targetPts <= it.last() }
        val keyFramePts = ptsInGopList.first()
        val targetIdx = ptsInGopList.indexOf(targetPts)
        val subGopFirstIdx = max(targetIdx - _maxCodecBufSize + 1, 0)
        val subGopSize = targetIdx - subGopFirstIdx + 1

        _extractor!!.seekTo(keyFramePts, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        repeat(subGopFirstIdx) {
            this.decodeRender(false)
            _extractor!!.advance()
        }

        val inBufIdxList = mutableListOf<Int>()

        repeat(subGopSize) {
            val idx = _decorder!!.dequeueInputBuffer(0)

            if (idx >= 0) {
                inBufIdxList.add(idx)
                LogEx.d("dequeueInputBuffer idx[${idx}]")
            }
        }

        val inBufIdxPtsList = mutableListOf<Pair<Int, Long>>()

        for (idx in inBufIdxList) {
            val buf = _decorder!!.getInputBuffer(idx)
            var readSize = _extractor!!.readSampleData(buf!!, 0)
            var flags = _extractor!!.sampleFlags
            var pts = _extractor!!.sampleTime

            if (readSize <= 0) {
                readSize = 0
                pts = 0
                flags = flags or MediaCodec.BUFFER_FLAG_END_OF_STREAM
            }

            LogEx.d("queueInputBuffer idx[${idx}] pts[${pts}] readSize[${readSize}]")
            _decorder!!.queueInputBuffer(idx, 0, readSize, pts, flags)
            _extractor!!.advance()
            inBufIdxPtsList.add(Pair(idx, pts))
        }

        val outBufIdxList = mutableListOf<Int>()

        while (true) {
            repeat(subGopSize) {
                val info = MediaCodec.BufferInfo()
                val idx = _decorder!!.dequeueOutputBuffer(info, 0)

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
            val curPtsGopIdx = _ptsGroupByGopList!!.indexOf(ptsInGopList)

            if (curPtsGopIdx > 0)
                prevPts = _ptsGroupByGopList!![curPtsGopIdx - 1].last()
        }

        return Pair(outBufIdxPtsList, prevPts)
    }

    fun renderIdxOutBuf(idxOutBuf: Int, pts: Long) {
        LogEx.d("releaseOutputBuffer idx[$idxOutBuf] pts[${pts}]")
        _lastRenderTimeUs = System.nanoTime() / 1_000
        this.curPts = pts
        _decorder!!.releaseOutputBuffer(idxOutBuf, true)
    }

    fun syncToAudioPts(audioPts: Long) {
        val ptsDiff = _extractor!!.sampleTime - audioPts
        LogEx.value("ptsDiff", ptsDiff)

        if (ptsDiff > 1_000_000) {
            LogEx.d("delay")
//            runBlocking { delay(ptsDiff / 1_000) }
        } else if (ptsDiff < -1_000_000) {
            while (true) {
                if (_extractor!!.sampleTime >= audioPts)
                    break

                val isEos =
                    _extractor!!.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM ==
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM

                if (isEos)
                    break

                LogEx.d("decodeRender advance")
                this.decodeRender(false)
                this.advance()
            }
        }
    }
}

