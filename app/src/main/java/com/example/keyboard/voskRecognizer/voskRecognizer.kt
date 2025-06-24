package com.example.keyboard.voskRecognizer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

class VoskRecognizer(private val context: Context) {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var lastRecognizedText: String? = null
    private var isModelInitialized = false

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelPath = copyVoskModelToInternalStorage(context)
                model = Model(modelPath)
                isModelInitialized = true
                Log.d("VoskRecognizer", "Модель Vosk загружена из: $modelPath")
            } catch (e: Exception) {
                throw RuntimeException("Ошибка загрузки модели Vosk", e)
            }
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!isModelInitialized) {
            Log.e("VoskRecognizer", "Модель не инициализирована")
            return
        }
        val recognizer = org.vosk.Recognizer(model, 16000.0f)
        speechService = SpeechService(recognizer, 16000.0f)
        speechService?.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {
                hypothesis?.let {
                    val text = parseVoskResult(hypothesis)
                    if (text.isNotBlank() && text != "Не распознано") {
                        lastRecognizedText = text
                        onResult(text)
                    }
                }
            }

            override fun onResult(hypothesis: String?) {
                hypothesis?.let {
                    val text = parseVoskResult(hypothesis)
                    if (text.isNotBlank() && text != "Не распознано") {
                        lastRecognizedText = text
                        onResult(text)
                    }
                }
            }

            override fun onFinalResult(hypothesis: String?) {
                hypothesis?.let {
                    val text = parseVoskResult(hypothesis)
                    if (text.isNotBlank() && text != "Не распознано") {
                        lastRecognizedText = text
                        onResult(text)
                    }
                }
            }

            override fun onError(exception: Exception?) {
                Log.e("VoskRecognizer", "Ошибка распознавания: ${exception?.message}")
            }

            override fun onTimeout() {
                stopListening()
            }
        })
    }

    fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    private fun parseVoskResult(result: String): String {
        return try {
            val json = JSONObject(result)
            json.optString("text", "Не распознано")
        } catch (e: Exception) {
            Log.e("VoskRecognizer", "Ошибка парсинга результата: $result", e)
            "Не распознано"
        }
    }

    private fun copyVoskModelToInternalStorage(context: Context): String {
        val modelDir = File(context.filesDir, "vosk-model-small-ru-0.22")
        if (!modelDir.exists()) {
            try {
                modelDir.mkdirs()
                copyAssetsToInternalStorage(context, "models/vosk-model-small-ru-0.22", modelDir)
                Log.d("VoskRecognizer", "Модель скопирована в: ${modelDir.absolutePath}")
            } catch (e: Exception) {
                throw RuntimeException("Ошибка копирования модели Vosk", e)
            }
        }
        return modelDir.absolutePath
    }

    private fun copyAssetsToInternalStorage(context: Context, assetPath: String, destDir: File) {
        val assetList = context.assets.list(assetPath) ?: emptyArray()
        if (assetList.isEmpty()) throw RuntimeException("Нет файлов в assets/$assetPath")
        for (fileName in assetList) {
            val assetSubPath = "$assetPath/$fileName"
            val subItems = context.assets.list(assetSubPath) ?: emptyArray()
            if (subItems.isNotEmpty()) {
                val subDir = File(destDir, fileName)
                subDir.mkdirs()
                copyAssetsToInternalStorage(context, assetSubPath, subDir)
            } else {
                context.assets.open(assetSubPath).use { input ->
                    File(destDir, fileName).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    fun destroy() {
        stopListening()
        model?.close()
        model = null
        isModelInitialized = false
    }
}