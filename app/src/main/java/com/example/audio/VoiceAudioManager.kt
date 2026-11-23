package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

data class RecordedVoiceNote(
    val base64Audio: String,
    val durationSec: Int,
    val waveform: List<Int>
)

data class PlaybackUiState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val speed: Float = 1.0f
)

class VoiceAudioManager(private val context: Context) {
    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var currentRecordFile: File? = null
    private var recordStartMs: Long = 0L
    private var amplitudeJob: Job? = null
    private val recordedAmplitudes = mutableListOf<Int>()

    private val _liveAmplitudes = MutableStateFlow<List<Int>>(emptyList())
    val liveAmplitudes: StateFlow<List<Int>> = _liveAmplitudes.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private var player: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    fun startRecording(scope: CoroutineScope): Boolean {
        stopRecordingInternal(discard = true)
        return try {
            val outFile = File(appContext.cacheDir, "nabd_rec_${System.currentTimeMillis()}.m4a")
            currentRecordFile = outFile
            recordedAmplitudes.clear()
            _liveAmplitudes.value = emptyList()
            _recordingDurationSec.value = 0

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(24000)
                setMaxDuration(120_000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            recordStartMs = System.currentTimeMillis()
            amplitudeJob = scope.launch(Dispatchers.Main) {
                while (isActive && recorder != null) {
                    val amp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    val normalized = ((amp / 32767.0f) * 100f).roundToInt().coerceIn(8, 100)
                    recordedAmplitudes.add(normalized)
                    _liveAmplitudes.value = recordedAmplitudes.takeLast(28)
                    _recordingDurationSec.value =
                        ((System.currentTimeMillis() - recordStartMs) / 1000L).toInt()
                    delay(80L)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("VoiceAudio", "Failed to start recording", e)
            stopRecordingInternal(discard = true)
            false
        }
    }

    fun cancelRecording() {
        stopRecordingInternal(discard = true)
    }

    fun stopAndEncode(): RecordedVoiceNote? {
        val file = currentRecordFile
        val durationSec = ((System.currentTimeMillis() - recordStartMs) / 1000L)
            .toInt()
            .coerceAtLeast(1)
        val waveformSnapshot = downsampleWaveform(recordedAmplitudes, 28)
        stopRecordingInternal(discard = false)

        if (file == null || !file.exists()) return null
        return try {
            val bytes = file.readBytes()
            file.delete()
            if (bytes.isEmpty() || bytes.size > 490_000) {
                Log.w("VoiceAudio", "Audio note size invalid or >490KB: ${bytes.size}")
                return null
            }
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            RecordedVoiceNote(
                base64Audio = base64,
                durationSec = durationSec,
                waveform = waveformSnapshot
            )
        } catch (e: Exception) {
            Log.e("VoiceAudio", "Failed encoding voice note", e)
            null
        }
    }

    private fun stopRecordingInternal(discard: Boolean) {
        amplitudeJob?.cancel()
        amplitudeJob = null
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        if (discard) {
            currentRecordFile?.delete()
            currentRecordFile = null
        }
        _liveAmplitudes.value = emptyList()
        _recordingDurationSec.value = 0
    }

    fun togglePlayVoice(messageId: String, base64Audio: String, scope: CoroutineScope) {
        val current = _playbackState.value
        if (current.activeMessageId == messageId && player != null) {
            if (current.isPlaying) {
                player?.pause()
                _playbackState.value = current.copy(isPlaying = false)
            } else {
                player?.start()
                applySpeed(current.speed)
                _playbackState.value = current.copy(isPlaying = true)
                startProgressLoop(messageId, scope)
            }
            return
        }

        stopPlayback()
        try {
            val decoded = Base64.decode(base64Audio, Base64.DEFAULT)
            val tempFile = File(appContext.cacheDir, "nabd_play_${messageId.take(12)}.m4a")
            tempFile.writeBytes(decoded)

            val mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    tempFile.delete()
                }
                start()
            }
            player = mediaPlayer
            val speed = _playbackState.value.speed
            applySpeed(speed)
            _playbackState.value = PlaybackUiState(
                activeMessageId = messageId,
                isPlaying = true,
                progress = 0f,
                speed = speed
            )
            startProgressLoop(messageId, scope)
        } catch (e: Exception) {
            Log.e("VoiceAudio", "Playback failed", e)
            stopPlayback()
        }
    }

    fun cyclePlaybackSpeed(): Float {
        val next = when (_playbackState.value.speed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackState.value = _playbackState.value.copy(speed = next)
        if (_playbackState.value.isPlaying) {
            applySpeed(next)
        }
        return next
    }

    private fun applySpeed(speed: Float) {
        try {
            player?.let { mp ->
                mp.playbackParams = PlaybackParams().setSpeed(speed)
            }
        } catch (e: Exception) {
            Log.w("VoiceAudio", "Speed adjustment unsupported: ${e.message}")
        }
    }

    private fun startProgressLoop(messageId: String, scope: CoroutineScope) {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Main) {
            while (isActive && player != null && _playbackState.value.activeMessageId == messageId) {
                val mp = player ?: break
                if (mp.isPlaying) {
                    val dur = mp.duration.coerceAtLeast(1)
                    val pos = mp.currentPosition.coerceAtLeast(0)
                    _playbackState.value = _playbackState.value.copy(
                        progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    )
                }
                delay(80L)
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        _playbackState.value = _playbackState.value.copy(
            activeMessageId = null,
            isPlaying = false,
            progress = 0f
        )
    }

    private fun downsampleWaveform(samples: List<Int>, targetCount: Int): List<Int> {
        if (samples.isEmpty()) return List(targetCount) { 25 }
        if (samples.size <= targetCount) return samples
        val step = samples.size.toFloat() / targetCount
        return List(targetCount) { i ->
            val idx = (i * step).toInt().coerceIn(0, samples.lastIndex)
            samples[idx]
        }
    }
}
