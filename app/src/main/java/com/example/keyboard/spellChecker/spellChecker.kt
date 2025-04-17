package com.example.keyboard.spellChecker

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import com.example.keyboard.Language
import com.example.keyboard.R

private val dictionaryEn = listOf("the", "and", "to", "hello", "world")
private val dictionaryRu = listOf("и", "в", "не", "привет", "мир")

class SpellChecker(private val context: Context, private val inputConnection: android.view.inputmethod.InputConnection?, private val onSuggestionSelected: () -> Unit)
{
    fun checkSpelling(rootView: View, text: String, language: Language, hasSpaceBefore: Boolean) {
        if (text.isEmpty()) {
            clearSuggestions(rootView)
            return
        }

        val predictiveSuggestions = if (language == Language.EN) {
            dictionaryEn.filter { it.lowercase().startsWith(text.lowercase()) }
        } else {
            dictionaryRu.filter { it.lowercase().startsWith(text.lowercase()) }
        }
        //Log.d("SpellChecker", "Input: $text, Has space: $hasSpaceBefore, Predictive suggestions: $predictiveSuggestions")

        val textServicesManager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        val locale = if (language == Language.EN) java.util.Locale("en_US") else java.util.Locale("ru_RU")
        val session = textServicesManager.newSpellCheckerSession(null, locale, object : android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener {
            override fun onGetSuggestions(suggestions: Array<android.view.textservice.SuggestionsInfo>?) {
                val suggestionList = mutableListOf<String>()
                suggestions?.forEach { info ->
                    for (i in 0 until info.suggestionsCount) {
                        info.getSuggestionAt(i)?.let { suggestionList.add(it) }
                    }
                }
                //Log.d("SpellChecker", "Spelling suggestions: $suggestionList")
                showSuggestions(rootView, (suggestionList + predictiveSuggestions).distinct().take(3), text, hasSpaceBefore)
            }

            override fun onGetSentenceSuggestions(suggestions: Array<android.view.textservice.SentenceSuggestionsInfo>?) {}
        }, true)

        if (session == null) {
            //Log.w("SpellChecker", "SpellCheckerSession is null for locale: $locale")
            showSuggestions(rootView, predictiveSuggestions.distinct().take(3), text, hasSpaceBefore)
        } else {
            //Log.d("SpellChecker", "Created SpellCheckerSession for locale: $locale")
            session.getSuggestions(TextInfo(text), 3)
        }
    }

    private fun showSuggestions(rootView: View, suggestions: List<String>, currentText: String, hasSpaceBefore: Boolean) {
        val suggestionContainer = rootView.findViewById<LinearLayout>(R.id.suggestion_container)
        suggestionContainer?.removeAllViews()
        //Log.d("SpellChecker", "Showing suggestions: $suggestions")

        for (suggestion in suggestions.take(3)) {
            val textView = TextView(context).apply {
                text = suggestion
                setPadding(16, 8, 16, 8)
                setTextColor(0xFF000000.toInt())
                textSize = 16f
                setBackgroundResource(android.R.drawable.btn_default_small)
                setOnClickListener {
                    val charsToDelete = if (hasSpaceBefore) currentText.length + 1 else currentText.length
                    inputConnection?.deleteSurroundingText(charsToDelete, 0)
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
        rootView.findViewById<LinearLayout>(R.id.suggestion_container)?.removeAllViews()
    }
}