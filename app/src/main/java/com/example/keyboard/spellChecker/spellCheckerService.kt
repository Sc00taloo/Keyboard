package com.example.keyboard.spellChecker

import android.service.textservice.SpellCheckerService
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo

class spellCheckerService : SpellCheckerService() {
    override fun createSession(): Session {
        return spellCheckerSession()
    }
}

class spellCheckerSession : SpellCheckerService.Session() {
    private val dictionaryEn = listOf("the", "and", "to", "hello", "world")
    private val dictionaryRu = listOf("и", "в", "не", "привет", "мир")

    override fun onCreate() {}

    override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo {
        val word = textInfo.text.lowercase()
        val dictionary = if (getLocale().toString().startsWith("ru")) dictionaryRu else dictionaryEn
        val suggestions = mutableListOf<String>()

        if (!dictionary.contains(word)) {
            suggestions.addAll(
                dictionary.filter { it.startsWith(word.take(2)) }.take(suggestionsLimit)
            )
        }

        return SuggestionsInfo(
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO,
            suggestions.toTypedArray()
        )
    }
}