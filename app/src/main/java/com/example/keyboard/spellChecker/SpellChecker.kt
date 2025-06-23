package com.example.keyboard.spellChecker

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.keyboard.Language
import com.example.keyboard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SpellChecker(private val context: Context, private val inputConnection: android.view.inputmethod.InputConnection?, private val onSuggestionSelected: () -> Unit)
{
    private var hunspellEn: Long = 0
    private var hunspellRu: Long = 0
    private var isInitialized = false

    private var lastInput: String? = null
    private var lastLanguage: Language? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var suggestionJob: Job? = null
    private val suggestionCache = mutableMapOf<String, List<String>>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeDictionaries()
            withContext(Dispatchers.Main) {
                isInitialized = true
            }
        }
    }

    private fun initializeDictionaries() {
        val dictDir = File(context.filesDir, "dictionaries")
        dictDir.mkdirs()
        copyAssetToFile("dictionaries/en_US.dic", File(dictDir, "en_US.dic"))
        copyAssetToFile("dictionaries/en_US.aff", File(dictDir, "en_US.aff"))
        copyAssetToFile("dictionaries/ru_RU.dic", File(dictDir, "ru_RU.dic"))
        copyAssetToFile("dictionaries/ru_RU.aff", File(dictDir, "ru_RU.aff"))

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
    private external fun suggest(handle: Long, word: String): Array<String>
    private external fun spell(handle: Long, word: String): Boolean

    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1.length > 10 || s2.length > 10) return Int.MAX_VALUE
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
        if (!isInitialized || text.isEmpty()) {
            clearSuggestions(rootView)
            return
        }

        // Проверяем только последнее слово для оптимизации
        val lastWord = text.trim().split(" ").lastOrNull() ?: return
        if (lastWord == lastInput && language == lastLanguage) {
            return // Пропускаем, если ввод не изменился
        }
        lastInput = lastWord
        lastLanguage = language

        // Отменяем предыдущую задачу
        suggestionJob?.cancel()
        suggestionJob = scope.launch {
            try {
                val hunspellHandle = if (language == Language.EN) hunspellEn else hunspellRu
                val cacheKey = "$lastWord:$language"
                val suggestions = suggestionCache[cacheKey] ?: run {
                    val isCorrect = spell(hunspellHandle, lastWord)
                    Log.d("SpellChecker", "Input: $lastWord, Is correct: $isCorrect, Has space: $hasSpaceBefore")
                    val rawSuggestions = suggest(hunspellHandle, lastWord).take(30)
                    val filteredSuggestions = if (isCorrect) {
                        rawSuggestions
                            .filter { it.startsWith(lastWord) && it != lastWord && levenshteinDistance(lastWord, it) <= 2 }
                            .take(7)
                    } else {
                        rawSuggestions
                            .filter { (it.startsWith(lastWord) || levenshteinDistance(lastWord, it) <= 3) &&
                                    (it.length >= lastWord.length - 1 && it.length >= 3) }
                            .take(7)
                    }
                    suggestionCache[cacheKey] = filteredSuggestions
                    filteredSuggestions
                }

                withContext(Dispatchers.Main) {
                    delay(100)
                    showSuggestions(rootView, suggestions, text, hasSpaceBefore)
                }
            } catch (e: Exception) {
                Log.e("SpellChecker", "Failed to process suggestions: ${e.message}")
                withContext(Dispatchers.Main) {
                    clearSuggestions(rootView)
                }
            }
        }
    }

    private fun showSuggestions(rootView: View, suggestions: List<String>, currentText: String, hasSpaceBefore: Boolean) {
        val suggestionContainer = rootView.findViewById<LinearLayout>(R.id.suggestion_container)
        suggestionContainer?.removeAllViews()
        Log.d("SpellChecker", "Showing suggestions: $suggestions")

        val textViewPool = mutableListOf<TextView>()
        for (i in 0 until suggestionContainer.childCount) {
            val view = suggestionContainer.getChildAt(i)
            if (view is TextView) textViewPool.add(view)
        }

        for (suggestion in suggestions) {
            val textView = textViewPool.firstOrNull() ?: TextView(context).apply {
                setPadding(16, 8, 16, 8)
                setTextColor(0xFF000000.toInt())
                textSize = 16f
                setBackgroundResource(android.R.drawable.btn_default_small)
            }.also { textViewPool.remove(it) }

            textView.text = suggestion
            textView.setOnClickListener {
                try {
                    val charsToDelete = if (hasSpaceBefore && currentText.isNotEmpty()) currentText.length + 1 else currentText.length
                    inputConnection?.deleteSurroundingText(charsToDelete, 0)
                    inputConnection?.commitText("$suggestion ", 1)
                    suggestionContainer.removeAllViews()
                    onSuggestionSelected()
                } catch (e: Exception) {
                    Log.e("SpellChecker", "Failed to commit suggestion: ${e.message}")
                }
            }
            suggestionContainer.addView(textView)
        }
    }

    fun clearSuggestions(rootView: View) {
        val suggestionContainer = rootView.findViewById<LinearLayout>(R.id.suggestion_container)
        suggestionContainer?.removeAllViews()
    }

    fun destroy() {
        scope.cancel()
        suggestionCache.clear()
    }

    companion object {
        init {
            System.loadLibrary("hunspell")
        }
    }
}