package com.takehome.twinmind.core.audio

import java.io.File
import java.io.RandomAccessFile

class WavWriter(
    private val file: File,
    private val sampleRate: Int = SAMPLE_RATE,
    private val channels: Int = CHANNELS,
    private val bitsPerSample: Int = BITS_PER_SAMPLE,
) : AutoCloseable {

    private val raf = RandomAccessFile(file, "rw")
    private var dataSize = 0L

    init {
        writeHeader()
    }

    fun write(pcmData: ByteArray, offset: Int = 0, length: Int = pcmData.size) {
        raf.write(pcmData, offset, length)
        dataSize += length
    }

    val durationMs: Long
        get() {
            val bytesPerSecond = sampleRate * channels * (bitsPerSample / 8)
            return if (bytesPerSecond > 0) (dataSize * 1000L) / bytesPerSecond else 0L
        }

    override fun close() {
        finalizeHeader()
        raf.close()
    }

    private fun writeHeader() {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        raf.writeBytes("RIFF")
        raf.writeIntLE(0) // placeholder for file size
        raf.writeBytes("WAVE")

        raf.writeBytes("fmt ")
        raf.writeIntLE(16) // chunk size
        raf.writeShortLE(1) // PCM format
        raf.writeShortLE(channels)
        raf.writeIntLE(sampleRate)
        raf.writeIntLE(byteRate)
        raf.writeShortLE(blockAlign)
        raf.writeShortLE(bitsPerSample)

        raf.writeBytes("data")
        raf.writeIntLE(0) // placeholder for data size
    }

    private fun finalizeHeader() {
        raf.seek(4)
        raf.writeIntLE((36 + dataSize).toInt())
        raf.seek(40)
        raf.writeIntLE(dataSize.toInt())
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }
}
