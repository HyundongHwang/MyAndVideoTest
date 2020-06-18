package com.hhd.myandvideotest.myplayvideo

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import com.hhd.myandvideotest.util.LogEx
import java.io.File
import java.util.ArrayDeque

class MyVideoPlayer {

    private var _availInBufIdxArray: ArrayDeque<Int>? = null
    private var _availOutBufIdxArray: ArrayDeque<Int>? = null
    private var _extractor: MediaExtractor? = null
    private var _isPlaying = false
    private var _vCodec: MediaCodec? = null

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
                _extractor!!.selectTrack(i)
                _vCodec = MediaCodec.createDecoderByType(mime)
                _vCodec!!.configure(fmt, renderSurface, null, 0)
                break
            }
        }

        _vCodec!!.start()
        _availInBufIdxArray = ArrayDeque<Int>()
        _availOutBufIdxArray = ArrayDeque<Int>()
    }

    enum class ReturnCodes {
        CONTINUE,
        EOS,
        STOP,
    }

    fun processCodecInputBuffer() : ReturnCodes {
        if (!_isPlaying)
            return ReturnCodes.STOP

        LogEx.d("")
        val isEos =
            _extractor!!.sampleFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM ==
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM

        if (isEos) {
            LogEx.e("EOS")
            return ReturnCodes.EOS
        }

        while (true) {
            if (!_isPlaying)
                return ReturnCodes.STOP

            val idx = _vCodec!!.dequeueInputBuffer(0)
//                LogEx.i("dequeueInputBuffer idx[$idx]")

            if (idx >= 0) {
                _availInBufIdxArray!!.add(idx)
                LogEx.d("availInBufIdxArray.add(idx[${idx}])")
            } else {
//                    LogEx.w("dequeueInputBuffer idx[$idx] break")
                break
            }
        }

        repeat(_availInBufIdxArray!!.size) {
            if (!_isPlaying)
                return ReturnCodes.STOP

            val index = _availInBufIdxArray!!.remove()
            val buf = _vCodec!!.getInputBuffer(index)
            var readSize = _extractor!!.readSampleData(buf!!, 0)
            var flags = _extractor!!.sampleFlags
            var pts = _extractor!!.sampleTime

            if (readSize <= 0) {
                readSize = 0
                pts = 0
                flags = flags or MediaCodec.BUFFER_FLAG_END_OF_STREAM
            }

            LogEx.i("queueInputBuffer index[${index}] readSize[${readSize}] pts[${pts}]")
            _vCodec!!.queueInputBuffer(index, 0, readSize, pts, flags)
            _extractor!!.advance()
        }

        return ReturnCodes.CONTINUE
    }

    fun processCodecOutputBuffer() {
        while (true) {
            if (!_isPlaying)
                return

            val info = MediaCodec.BufferInfo()
            val idx = _vCodec!!.dequeueOutputBuffer(info, 0)

//                LogEx.i("dequeueOutputBuffer idx[$idx] pts[${info.presentationTimeUs}]")

            if (idx >= 0) {
                _availOutBufIdxArray!!.add(idx)
                LogEx.d("availOutBufIdxArray.add(idx[${idx}])")
            } else {
//                    LogEx.w("dequeueOutputBuffer idx[$idx] break")
                break
            }
        }

        repeat(_availOutBufIdxArray!!.size) {
            if (!_isPlaying)
                return

            val index = _availOutBufIdxArray!!.remove()
            LogEx.i("releaseOutputBuffer index[${index}]")
            _vCodec!!.releaseOutputBuffer(index, true)
        }
    }

    fun stop() {
        if (!_isPlaying)
            return

        _isPlaying = false
        _vCodec!!.stop()
        _vCodec!!.release()
        _vCodec = null
        _availInBufIdxArray = null
        _availOutBufIdxArray = null
        _extractor = null
    }
}