package com.example.keyboard.sentenceClassifier

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SentenceClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var isInitialized = false

    private val labelMap = mapOf(
        0 to "statement/Regular",
        1 to "question/Interrogative",
        2 to "exclamation/Exclamatory"
    )
    private val punctuationMap = mapOf(
        "statement/Regular" to ".",
        "question/Interrogative" to "?",
        "exclamation/Exclamatory" to "!"
    )

    init {
        try {
            val model = loadModelFile("models/intonation_model.tflite")
            interpreter = Interpreter(model)
            isInitialized = true
            Log.d("TFLiteClassifier", "TFLite загрузилась")
        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "Ошибка загрузки модели: ${e.message}")
            isInitialized = false
        }
    }

    fun classify(text: String, features: FloatArray): String {
        if (!isInitialized || interpreter == null) {
            Log.e("TFLiteClassifier", "Классификатор не ициниализирован")
            return text.trim().ifEmpty { "" } + "."
        }
        if (text.isEmpty()) {
            Log.w("TFLiteClassifier", "Текст пустой")
            return "."
        }

        try {
            val expectedInputSize = interpreter?.getInputTensor(0)?.shape()?.reduce { a, b -> a * b } ?: 0
            if (features.size != expectedInputSize) {
                return text.trim() + "."
            }

            val input = ByteBuffer.allocateDirect(features.size * 4)
                .order(ByteOrder.nativeOrder())
            features.forEach { input.putFloat(it) }
            input.rewind()

            val output = Array(1) { FloatArray(3) }
            interpreter!!.run(input, output)

            val predictedIdx = output[0].indices.maxByOrNull { output[0][it] } ?: 0
            val predictedLabel = labelMap[predictedIdx] ?: "statement/Regular"
            val punctuation = punctuationMap[predictedLabel] ?: "."

            val cleanedText = text.trim().removeSuffix(".").removeSuffix("?").removeSuffix("!")
            val result = "$cleanedText$punctuation"

            Log.d("TFLiteClassifier", "Вход: \"$text\" -> Предикт: $predictedLabel → Выход: \"$result\"")
            return result
        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "Ошибка классификатора: ${e.message}", e)
            return text.trim() + "."
        }
    }

    private fun loadModelFile(assetPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetPath)
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }
}