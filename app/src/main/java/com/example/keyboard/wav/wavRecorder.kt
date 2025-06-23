package com.example.keyboard.wav

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sqrt

class wavRecorder(
    private val context: Context,
    private val onRecordingFinished: (FloatArray) -> Unit,
    private val onSilenceDetected: () -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    // Параметры для детектора тишины
    private val silenceThreshold = 5000.0
    private val silenceWindowSize = (sampleRate * 0.5).toInt()
    private val silenceDuration = 2.0
    private var silenceCounter = 0
    private val silenceWindowsRequired = (silenceDuration / 0.5).toInt()
    private var startTime = 0L

    @SuppressLint("MissingPermission")
    fun startRecording(filePath: String): FloatArray {
        val audioBuffer = ShortArray(bufferSize)
        val floatBuffer = ArrayList<Float>()
        val silenceBuffer = mutableListOf<Short>()

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        isRecording = true
        startTime = System.currentTimeMillis()
        audioRecord?.startRecording()
        Log.d("WavRecorder", "Запись началась, буфер: $bufferSize")

        while (isRecording) {
            // Минимальная длительность записи 4 секунды
            if (System.currentTimeMillis() - startTime < 4000) {
                Thread.sleep(100)
                continue
            }
            val read = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
            if (read > 0) {
                Log.d("WavRecorder", "Прочитано сэмплов: $read")
                for (i in 0 until read) {
                    floatBuffer.add(audioBuffer[i] / 32768f)
                    silenceBuffer.add(audioBuffer[i])
                }

                // Проверка тишины каждые 0.5 секунды
                while (silenceBuffer.size >= silenceWindowSize) {
                    val window = silenceBuffer.take(silenceWindowSize).toShortArray()
                    silenceBuffer.subList(0, silenceWindowSize).clear()
                    val energy = computeEnergy(window)
                    Log.d("WavRecorder", "Энергия окна: $energy")
                    if (energy < silenceThreshold) {
                        silenceCounter++
                        Log.d("WavRecorder", "Тишина обнаружена, счётчик: $silenceCounter")
                        if (silenceCounter >= silenceWindowsRequired) {
                            Log.d("WavRecorder", "2 секунды тишины, остановка записи")
                            stopRecording()
                            onSilenceDetected()
                            break
                        }
                    } else {
                        silenceCounter = 0
                    }
                }
            } else {
                Log.e("WavRecorder", "Ошибка чтения аудио: $read")
            }
        }

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val audioData = floatBuffer.toFloatArray()
        Log.d("WavRecorder", "Запись завершена, сэмплов: ${audioData.size}, сохранено в $filePath")
        saveToWavFile(audioData, filePath)
        onRecordingFinished(audioData)
        return audioData
    }

    fun stopRecording() {
        isRecording = false
    }

    private fun computeEnergy(buffer: ShortArray): Double {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sum / buffer.size)
    }

    private fun saveToWavFile(audioData: FloatArray, filePath: String) {
        try {
            FileOutputStream(File(filePath)).use { fos ->
                val totalAudioLen = audioData.size * 2
                val totalDataLen = totalAudioLen + 36
                val byteRate = sampleRate * 2

                fos.write("RIFF".toByteArray())
                fos.write(intToByteArray(totalDataLen))
                fos.write("WAVE".toByteArray())
                fos.write("fmt ".toByteArray())
                fos.write(intToByteArray(16))
                fos.write(shortToByteArray(1))
                fos.write(shortToByteArray(1))
                fos.write(intToByteArray(sampleRate))
                fos.write(intToByteArray(byteRate))
                fos.write(shortToByteArray(2))
                fos.write(shortToByteArray(16))
                fos.write("data".toByteArray())
                fos.write(intToByteArray(totalAudioLen))

                audioData.forEach { sample ->
                    val shortSample = (sample * 32767).toInt().toShort()
                    fos.write(shortToByteArray(shortSample))
                }
            }
        } catch (e: IOException) {
            Log.e("WavRecorder", "Ошибка сохранения WAV: ${e.message}", e)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 0).toByte(), (value shr 8).toByte(),
            (value shr 16).toByte(), (value shr 24).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() shr 0).toByte(), (value.toInt() shr 8).toByte()
        )
    }
}