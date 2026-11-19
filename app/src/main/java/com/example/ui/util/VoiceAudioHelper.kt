package com.example.ui.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
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

data class VoiceNoteRecording(
    val base64: String,
    val durationSec: Long
)

class VoiceAudioController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordStartMs: Long = 0L
    private var recordTickerJob: Job? = null

    private var player: MediaPlayer? = null
    private var playbackFile: File? = null
    private var playbackTickerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0L)
    val recordingSeconds: StateFlow<Long> = _recordingSeconds.asStateFlow()

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun startRecording(onError: (String) -> Unit): Boolean {
        stopPlayback()
        cancelRecording()
        return try {
            val outFile = File(context.cacheDir, "vn_rec_${System.currentTimeMillis()}.m4a")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(16000)
            mr.setAudioEncodingBitRate(24000)
            mr.setMaxDuration(35_000)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare()
            mr.start()

            recorder = mr
            recordingFile = outFile
            recordStartMs = System.currentTimeMillis()
            _recordingSeconds.value = 0L
            _isRecording.value = true

            recordTickerJob?.cancel()
            recordTickerJob = scope.launch(Dispatchers.Main) {
                while (isActive && _isRecording.value) {
                    _recordingSeconds.value = ((System.currentTimeMillis() - recordStartMs) / 1000L).coerceAtLeast(0L)
                    delay(250L)
                }
            }
            true
        } catch (e: Exception) {
            cleanupRecorder()
            onError(e.localizedMessage ?: "Microphone unavailable on this device.")
            false
        }
    }

    fun stopAndEncodeRecording(onError: (String) -> Unit): VoiceNoteRecording? {
        if (!_isRecording.value) return null
        val elapsedMs = System.currentTimeMillis() - recordStartMs
        val durationSec = (elapsedMs / 1000L).coerceIn(1L, 180L)
        val file = recordingFile
        recordTickerJob?.cancel()
        _isRecording.value = false
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val bytes = file?.takeIf { it.exists() }?.readBytes()
            file?.delete()
            recordingFile = null
            if (bytes == null || bytes.isEmpty()) {
                onError("Recording was empty. Hold mic for at least 1 second and speak clearly.")
                null
            } else {
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (b64.length > 135_000) {
                    onError("Voice note exceeded max length (keep under ~35 seconds).")
                    null
                } else {
                    VoiceNoteRecording(base64 = b64, durationSec = durationSec)
                }
            }
        } catch (e: Exception) {
            cleanupRecorder()
            onError(
                if (elapsedMs < 600L) "Hold for at least 1 second before sending a voice note."
                else "Could not finish voice recording."
            )
            null
        }
    }

    fun cancelRecording() {
        recordTickerJob?.cancel()
        _isRecording.value = false
        _recordingSeconds.value = 0L
        cleanupRecorder()
    }

    fun togglePlayback(
        messageId: String,
        base64Audio: String?,
        onError: (String) -> Unit
    ) {
        if (_playingMessageId.value == messageId) {
            stopPlayback()
            return
        }
        stopPlayback()
        if (base64Audio.isNullOrBlank()) {
            onError("Voice note audio data unavailable.")
            return
        }
        try {
            val bytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val tmpFile = File(context.cacheDir, "vn_play_${messageId.take(16)}.m4a")
            tmpFile.writeBytes(bytes)
            playbackFile = tmpFile

            val mp = MediaPlayer()
            mp.setDataSource(tmpFile.absolutePath)
            mp.prepare()
            mp.setOnCompletionListener {
                stopPlayback()
            }
            mp.setOnErrorListener { _, _, _ ->
                stopPlayback()
                onError("Unable to play voice note.")
                true
            }
            mp.start()

            player = mp
            _playingMessageId.value = messageId
            _playbackProgress.value = 0f

            playbackTickerJob = scope.launch(Dispatchers.Main) {
                while (isActive && _playingMessageId.value == messageId) {
                    val currentPlayer = player
                    if (currentPlayer != null && currentPlayer.duration > 0) {
                        _playbackProgress.value = (currentPlayer.currentPosition.toFloat() / currentPlayer.duration.toFloat()).coerceIn(0f, 1f)
                    }
                    delay(120L)
                }
            }
        } catch (e: Exception) {
            stopPlayback()
            onError(e.localizedMessage ?: "Failed to play voice note.")
        }
    }

    fun stopPlayback() {
        playbackTickerJob?.cancel()
        _playingMessageId.value = null
        _playbackProgress.value = 0f
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            player = null
        }
        try {
            playbackFile?.delete()
        } catch (_: Exception) {
        } finally {
            playbackFile = null
        }
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }

    private fun cleanupRecorder() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        } finally {
            recorder = null
        }
        try {
            recordingFile?.delete()
        } catch (_: Exception) {
        } finally {
            recordingFile = null
        }
    }
}

fun formatCallOrVoiceDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val mins = safe / 60L
    val rem = safe % 60L
    return "%d:%02d".format(mins, rem)
}
