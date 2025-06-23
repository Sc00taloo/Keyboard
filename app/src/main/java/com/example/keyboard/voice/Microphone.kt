package com.example.keyboard.voice

import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.inputmethod.InputConnection
import androidx.lifecycle.MutableLiveData
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.mfcc.MFCC
import com.example.keyboard.R
import com.example.keyboard.sentenceClassifier.SentenceClassifier
import com.example.keyboard.voskRecognizer.VoskRecognizer
import com.example.keyboard.wav.wavRecorder
import java.io.File
import java.util.Locale
import kotlin.math.min


class Microphone(
    private val context: Context,
    private val inputConnection: InputConnection?,
    private val onTextRecognized: (String) -> Unit,
    private val classifier: SentenceClassifier
) {
    private val voskRecognizer = VoskRecognizer(context)
    private var wavRecorder: wavRecorder? = null
    private var lastText: String? = null
    val isRecording = MutableLiveData<Boolean>(false)
    private var audioData: FloatArray? = null

    fun startVoiceInput(language: String = "ru-RU") {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Требуется разрешение на микрофон.", Toast.LENGTH_LONG).show()
            return
        }

        if (isRecording.value == true) return
        isRecording.postValue(true)
        lastText = null
        audioData = null

        val wavFilePath = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav").absolutePath
        Log.d("Microphone", "WAV file saved at: $wavFilePath")
        wavRecorder = wavRecorder(context, { data ->
            audioData = data
            processAudio()
        }, {
            stopVoiceInput()
        })

        Thread {
            wavRecorder?.startRecording(wavFilePath)
        }.start()

        voskRecognizer.startListening { text ->
            lastText = text
            Log.d("Microphone", "Распознанный текст: $text")
            onTextRecognized(text.capitalizeFirst())
        }

    }

    fun stopVoiceInput() {
        if (isRecording.value == false) return
        isRecording.postValue(false)
        voskRecognizer.stopListening()
        wavRecorder?.stopRecording()
        wavRecorder = null
        Thread.sleep(100)
        processAudio()
    }

    private fun processAudio() {
        val text = lastText ?: context.getString(R.string.not_recognized)
        val data = audioData ?: return
        try {
            val mfccFeatures = extractMFCC(data)
            Log.d("Microphone", "MFCC shape: ${mfccFeatures.size}")
            val resultText = classifier.classify(text, mfccFeatures)
            val finalText = resultText.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            onTextRecognized(finalText)
        } catch (e: Exception) {
            Log.e("Microphone", "Ошибка классификации: ${e.message}", e)
            onTextRecognized(text.capitalizeFirst())
        }
    }

    private fun extractMFCC(audioData: FloatArray): FloatArray {
        val windowSize = 400
        val hopSize = 160
        val nMfcc = 40
        val mfccLength = 174
        val sampleRate = 16000
        val mfccList = mutableListOf<FloatArray>()

        val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val processor = MFCC(windowSize, sampleRate.toFloat(), nMfcc, 20, 100f, 5000f)

        var i = 0
        while (i + windowSize <= audioData.size) {
            val buffer = audioData.sliceArray(i until i + windowSize)
            val event = AudioEvent(format)
            event.setFloatBuffer(buffer)
            processor.process(event)
            processor.mfcc?.let { mfccList.add(it) }
            i += hopSize
        }

        Log.d("Microphone", "MFCC frames: ${mfccList.size}")
        val result = FloatArray(nMfcc * mfccLength) { 0f }
        for (frameIndex in 0 until min(mfccLength, mfccList.size)) {
            val frame = mfccList[frameIndex]
            for (j in 0 until min(nMfcc, frame.size)) {
                result[frameIndex * nMfcc + j] = frame[j]
            }
        }
        return result
    }

    fun destroy() {
        voskRecognizer.destroy()
        wavRecorder?.stopRecording()
        wavRecorder = null
        lastText = null
        audioData = null
        isRecording.postValue(false)
    }

    private fun String.capitalizeFirst(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}