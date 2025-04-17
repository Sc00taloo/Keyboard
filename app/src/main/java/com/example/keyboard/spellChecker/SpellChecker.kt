package com.example.keyboard.spellChecker

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.keyboard.Language
import com.example.keyboard.R
import java.io.File

class SpellChecker(private val context: Context, private val inputConnection: android.view.inputmethod.InputConnection?, private val onSuggestionSelected: () -> Unit)
{
    private var hunspellEn: Long = 0
    private var hunspellRu: Long = 0

    init {
        // Копируем словари из assets в файлы приложения
        val dictDir = File(context.filesDir, "dictionaries")
        dictDir.mkdirs()
        copyAssetToFile("dictionaries/en_US.dic", File(dictDir, "en_US.dic"))
        copyAssetToFile("dictionaries/en_US.aff", File(dictDir, "en_US.aff"))
        copyAssetToFile("dictionaries/ru_RU.dic", File(dictDir, "ru_RU.dic"))
        copyAssetToFile("dictionaries/ru_RU.aff", File(dictDir, "ru_RU.aff"))

        // Инициализируем Hunspell
        hunspellEn = initHunspell(
            File(dictDir, "en_US.dic").absolutePath,
            File(dictDir, "en_US.aff").absolutePath
        )
        hunspellRu = initHunspell(
            File(dictDir, "ru_RU.dic").absolutePath,
            File(dictDir, "ru_RU.aff").absolutePath
        )
    }

    private fun copyAssetToFile(assetPath: String, destFile: File) {
        if (!destFile.exists()) {
            context.assets.open(assetPath).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private external fun initHunspell(dicPath: String, affPath: String): Long
    private external fun destroyHunspell(handle: Long)
    private external fun suggest(handle: Long, word: String): Array<String>
    private external fun spell(handle: Long, word: String): Boolean

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1, // удаление
                    dp[i][j - 1] + 1, // вставка
                    dp[i - 1][j - 1] + cost // замена
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun checkSpelling(rootView: View, text: String, language: Language, hasSpaceBefore: Boolean) {
        if (text.isEmpty()) {
            clearSuggestions(rootView)
            return
        }

        // Выбираем Hunspell в зависимости от языка
        val hunspellHandle = if (language == Language.EN) hunspellEn else hunspellRu
        // Проверяем, правильное ли слово
        val isCorrect = spell(hunspellHandle, text)
        Log.d("SpellChecker", "Input: $text, Is correct: $isCorrect, Has space: $hasSpaceBefore")

        if (isCorrect) {
            // Если слово правильное, показываем только слова, начинающиеся с введенного текста
            val suggestions = suggest(hunspellHandle, text)
                .filter { it.startsWith(text) && it != text && levenshteinDistance(text, it) <= 2 }
                .take(3)
            showSuggestions(rootView, suggestions, text, hasSpaceBefore)
        } else {
            // Если слово неправильное, фильтруем по префиксу, длине и расстоянию Левенштейна
            val suggestions = suggest(hunspellHandle, text)
                .filter {
                    (it.startsWith(text) || levenshteinDistance(text, it) <= 2) &&
                            it.length >= text.length - 1
                }
                .take(3)
            showSuggestions(rootView, suggestions, text, hasSpaceBefore)
        }
    }

    private fun showSuggestions(rootView: View, suggestions: List<String>, currentText: String, hasSpaceBefore: Boolean) {
        val suggestionContainer = rootView.findViewById<LinearLayout>(R.id.suggestion_container)
        suggestionContainer?.removeAllViews()
        Log.d("SpellChecker", "Showing suggestions: $suggestions")

        for (suggestion in suggestions) {
            val textView = TextView(context).apply {
                text = suggestion
                setPadding(16, 8, 16, 8)
                setTextColor(0xFF000000.toInt())
                textSize = 16f
                setBackgroundResource(android.R.drawable.btn_default_small)
                setOnClickListener {
                    // Удаляем текущее слово и пробел перед ним, если он есть
                    val charsToDelete = if (hasSpaceBefore && currentText.isNotEmpty()) currentText.length + 1 else currentText.length
                    inputConnection?.deleteSurroundingText(charsToDelete, 0)
                    // Вставляем предложение и пробел
                    inputConnection?.commitText("$suggestion ", 1)
                    suggestionContainer.removeAllViews()
                    onSuggestionSelected()
                    Log.d("SpellChecker", "Selected suggestion: $suggestion, Deleted chars: $charsToDelete")
                }
            }
            suggestionContainer?.addView(textView)
        }
    }

    fun clearSuggestions(rootView: View) {
        val suggestionContainer = rootView.findViewById<LinearLayout>(R.id.suggestion_container)
        suggestionContainer?.removeAllViews()
        Log.d("SpellChecker", "Cleared suggestions")
    }

    fun destroy() {
        if (hunspellEn != 0L) destroyHunspell(hunspellEn)
        if (hunspellRu != 0L) destroyHunspell(hunspellRu)
    }

    companion object {
        init {
            System.loadLibrary("hunspell")
        }
    }
}