package com.takehome.twinmind.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.takehome.twinmind.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class AudioRecorder @Inject constructor(
    private val dispatchers: DispatcherProvider,
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var currentWriter: WavWriter? = null

    private val _amplitudes = MutableSharedFlow<Float>(extraBufferCapacity = 64)
    val amplitudes: SharedFlow<Float> = _amplitudes.asSharedFlow()

    private val _chunkReady = MutableSharedFlow<ChunkResult>(extraBufferCapacity = 8)
    val chunkReady: SharedFlow<ChunkResult> = _chunkReady.asSharedFlow()

    val isRecording: Boolean get() = recordingJob?.isActive == true

    @SuppressLint("MissingPermission")
    fun start(outputDir: File, sessionId: String, scope: CoroutineScope) {
        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(
            WavWriter.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(BUFFER_SIZE)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            WavWriter.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        ).also { it.startRecording() }

        recordingJob = scope.launch(dispatchers.io) {
            val buffer = ByteArray(BUFFER_SIZE)
            var chunkIndex = 0
            var chunkFile = newChunkFile(outputDir, sessionId, chunkIndex)
            currentWriter = WavWriter(chunkFile)

            try {
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        currentWriter?.write(buffer, 0, read)
                        _amplitudes.tryEmit(computeAmplitude(buffer, read))

                        if ((currentWriter?.durationMs ?: 0) >= CHUNK_DURATION_MS) {
                            val duration = currentWriter?.durationMs ?: 0L
                            currentWriter?.close()
                            currentWriter = null

                            _chunkReady.tryEmit(
                                ChunkResult(
                                    filePath = chunkFile.absolutePath,
                                    chunkIndex = chunkIndex,
                                    durationMs = duration,
                                    sessionId = sessionId,
                                ),
                            )

                            chunkIndex++
                            chunkFile = newChunkFile(outputDir, sessionId, chunkIndex)
                            currentWriter = WavWriter(chunkFile)
                        }
                    }
                }
            } finally {
                val duration = currentWriter?.durationMs ?: 0L
                currentWriter?.close()
                currentWriter = null
                if (duration > 0) {
                    _chunkReady.tryEmit(
                        ChunkResult(
                            filePath = chunkFile.absolutePath,
                            chunkIndex = chunkIndex,
                            durationMs = duration,
                            sessionId = sessionId,
                        ),
                    )
                }
            }
        }
        Timber.d("AudioRecorder started for session $sessionId")
    }

    suspend fun stop() {
        recordingJob?.cancelAndJoin()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Timber.d("AudioRecorder stopped")
    }

    private fun computeAmplitude(buffer: ByteArray, readSize: Int): Float {
        var sum = 0.0
        val sampleCount = readSize / 2
        for (i in 0 until sampleCount) {
            val sample = (buffer[i * 2].toInt() and 0xFF) or
                (buffer[i * 2 + 1].toInt() shl 8)
            val normalized = sample.toShort().toDouble()
            sum += normalized * normalized
        }
        val rms = sqrt(sum / sampleCount.coerceAtLeast(1))
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    private fun newChunkFile(dir: File, sessionId: String, index: Int): File {
        val chunksDir = File(dir, "sessions/$sessionId").also { it.mkdirs() }
        return File(chunksDir, "chunk_${index.toString().padStart(4, '0')}.wav")
    }

    data class ChunkResult(
        val filePath: String,
        val chunkIndex: Int,
        val durationMs: Long,
        val sessionId: String,
    )

    companion object {
        private const val BUFFER_SIZE = 4096
        const val CHUNK_DURATION_MS = 30_000L
    }
}
