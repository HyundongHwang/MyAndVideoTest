package com.hhd.myandvideotest.myplayvideo

import android.media.*
import android.os.Build
import com.hhd.myandvideotest.util.LogEx
import java.io.File
import java.lang.Exception

class MyAudioPlayer {

    var curPts: Long = -1L
        private set

    var durationUs: Long = -1L
        private set

    /**
     * AudioFormat.ENCODING_PCM_*
     */
    var pcmEncoding: Int = -1
        private set

    var channelCount: Int = -1
        private set

    var sampleRate: Int = -1
        private set

    var lastRenderTimeUs: Long = -1L
        private set

    var isOpen = false
    var isPlay = false
    var speedRatio = 1.0


    private var _extractor: MediaExtractor? = null
    private var _decorder: MediaCodec? = null
    private var _audioTrack: AudioTrack? = null
    private var _ptsList: MutableList<Long>? = null


    fun prepare(srcFile: File) {
        _extractor = MediaExtractor()
        _extractor!!.setDataSource(srcFile.toString())

        for (i in 0 until _extractor!!.trackCount) {
            _extractor!!.unselectTrack(i)
        }

        var fmt: MediaFormat? = null
        var mime: String? = null
        var idxAudioTrack = -1

        for (i in 0 until _extractor!!.trackCount) {
            fmt = _extractor!!.getTrackFormat(i)
            mime = fmt.getString(MediaFormat.KEY_MIME)

            if (mime.contains("audio/")) {
                idxAudioTrack = i
                break
            }
        }

        if (idxAudioTrack >= 0) {
            this.isOpen = true
        } else {
            this.stop()
            return
        }

        LogEx.value("fmt", fmt)
        _extractor!!.selectTrack(idxAudioTrack)
        this.sampleRate = fmt!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        this.channelCount = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        this.durationUs = fmt.getLong(MediaFormat.KEY_DURATION)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                this.pcmEncoding = fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                this.pcmEncoding = fmt.getInteger("pcm-encoding")
            }
        } catch (_: Exception) {
        }

        if (this.pcmEncoding < 0) {
            try {
                val bits_per_sample = fmt.getInteger("bits-per-sample")

                this.pcmEncoding = when (bits_per_sample) {
                    8 -> AudioFormat.ENCODING_PCM_8BIT
                    16 -> AudioFormat.ENCODING_PCM_16BIT
                    32 -> AudioFormat.ENCODING_PCM_FLOAT
                    else -> AudioFormat.ENCODING_PCM_16BIT
                }
            } catch (_: Exception) {
            }
        }

        if (this.pcmEncoding < 0)
            this.pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

        _decorder = MediaCodec.createDecoderByType(mime!!)
        _decorder!!.configure(fmt, null, null, 0)
        _decorder!!.start()

        _ptsList = mutableListOf()

        while (true) {
            _ptsList!!.add(_extractor!!.sampleTime)
            val res = _extractor!!.advance()

            if (!res)
                break
        }

        _extractor!!.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val chCfg = when (this.channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            else -> AudioFormat.CHANNEL_OUT_MONO
        }

        val minBufSize = AudioTrack.getMinBufferSize(sampleRate, chCfg, this.pcmEncoding)

        _audioTrack = AudioTrack(
            AudioAttributes
                .Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat
                .Builder()
                .setSampleRate(this.sampleRate)
                .setEncoding(this.pcmEncoding)
                .build(),
            minBufSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        _audioTrack!!.play()
    }

    fun seek(pts: Long) {
        val lastPts = _ptsList!!.last()

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

        _extractor!!.seekTo(caliPts, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
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

        for (_x_ in 0..10) {
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
            this.lastRenderTimeUs = System.nanoTime() / 1_000
            this.curPts = pts

            if (idx >= 0) {
                val outBuf = _decorder!!.getOutputBuffer(idx)
                val outByteArray = ByteArray(outBuf!!.remaining())
                outBuf.get(outByteArray)
                _audioTrack!!.write(outByteArray, 0, outByteArray.size)
            }
        }

        if (idx >= 0)
            _decorder!!.releaseOutputBuffer(idx, render)

        return true
    }

    fun stop() {
        this.isOpen = false
        this.curPts = -1L
        this.durationUs = -1L
        this.isPlay = false

        if (_extractor != null) {
            _extractor!!.release()
            _extractor = null
        }

        this.lastRenderTimeUs = -1L

        if (_decorder != null) {
            _decorder!!.stop()
            _decorder!!.release()
            _decorder = null
        }

        _ptsList = null
    }

    fun advance() {
        _extractor!!.advance()
    }
}