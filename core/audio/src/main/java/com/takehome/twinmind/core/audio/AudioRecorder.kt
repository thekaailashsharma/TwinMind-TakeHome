package com.takehome.twinmind.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.takehome.twinmind.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AudioRecorder @Inject constructor(
    private val dispatchers: DispatcherProvider,
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var currentWriter: WavWriter? = null
    @Volatile private var isPaused = false

    private val _amplitudes = MutableSharedFlow<Float>(extraBufferCapacity = 64)
    val amplitudes: SharedFlow<Float> = _amplitudes.asSharedFlow()

    private val _chunkReady = MutableSharedFlow<ChunkResult>(extraBufferCapacity = 8)
    val chunkReady: SharedFlow<ChunkResult> = _chunkReady.asSharedFlow()

    private val _silenceDetected = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    val silenceDetected: SharedFlow<Boolean> = _silenceDetected.asSharedFlow()

    val isRecording: Boolean get() = recordingJob?.isActive == true

    private val overlapQueue = ArrayDeque<ByteArray>()
    private var lastSignificantAudioMs = 0L

    @SuppressLint("MissingPermission")
    fun start(outputDir: File, sessionId: String, scope: CoroutineScope) {
        if (isRecording) return
        isPaused = false
        lastSignificantAudioMs = System.currentTimeMillis()
        overlapQueue.clear()

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

            try {
                currentWriter = WavWriter(chunkFile)

                while (isActive) {
                    if (isPaused) {
                        delay(100)
                        continue
                    }
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        try {
                            currentWriter?.write(buffer, 0, read)
                        } catch (ioe: IOException) {
                            Timber.e(ioe, "AudioRecorder write failed, likely due to low storage")
                            break
                        }
                        val amplitude = computeAmplitude(buffer, read)
                        _amplitudes.tryEmit(amplitude)

                        trackOverlap(buffer, read)

                        if (amplitude > SILENCE_THRESHOLD) {
                            lastSignificantAudioMs = System.currentTimeMillis()
                            _silenceDetected.tryEmit(false)
                        } else if (System.currentTimeMillis() - lastSignificantAudioMs > SILENCE_TIMEOUT_MS) {
                            _silenceDetected.tryEmit(true)
                        }

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
                            writeOverlapToChunk()
                        }
                    }
                }
            } catch (ioe: IOException) {
                Timber.e(ioe, "AudioRecorder I/O error, stopping recording (likely low storage)")
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

    fun pause() {
        isPaused = true
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Timber.w(e, "Error pausing AudioRecord")
        }
    }

    fun resume() {
        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            Timber.w(e, "Error resuming AudioRecord")
        }
        lastSignificantAudioMs = System.currentTimeMillis()
        isPaused = false
    }

    suspend fun stop() {
        isPaused = false
        recordingJob?.cancelAndJoin()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Timber.w(e, "Error stopping AudioRecord")
        }
        audioRecord = null
        overlapQueue.clear()
        Timber.d("AudioRecorder stopped")
    }

    private fun trackOverlap(buffer: ByteArray, readSize: Int) {
        overlapQueue.addLast(buffer.copyOf(readSize))
        while (overlapQueue.size > MAX_OVERLAP_BUFFERS) {
            overlapQueue.removeFirst()
        }
    }

    private fun writeOverlapToChunk() {
        for (buf in overlapQueue) {
            currentWriter?.write(buf, 0, buf.size)
        }
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
        const val CHUNK_DURATION_MS = 10_000L
        private const val OVERLAP_MS = 2_000L
        private val OVERLAP_BYTES =
            (WavWriter.SAMPLE_RATE * WavWriter.CHANNELS * (WavWriter.BITS_PER_SAMPLE / 8) * OVERLAP_MS / 1000).toInt()
        private val MAX_OVERLAP_BUFFERS = OVERLAP_BYTES / BUFFER_SIZE + 1
        private const val SILENCE_THRESHOLD = 0.01f
        private const val SILENCE_TIMEOUT_MS = 10_000L
    }
}
