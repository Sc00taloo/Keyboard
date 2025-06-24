package com.example.keyboard.animation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputConnection
import com.example.keyboard.R
import com.example.keyboard.databinding.VoiceInputLayoutBinding

class voiceInputAnimation(
    private val context: Context,
    private var inputConnection: InputConnection?
) {
    private var voiceInputView: View? = null
    var voiceInputBinding: VoiceInputLayoutBinding? = null
    private var currentText: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showVoiceInputField(parent: ViewGroup, newInputConnection: InputConnection?) {
        inputConnection = newInputConnection
        currentText = ""
        try {
            if (voiceInputView == null) {
                voiceInputBinding = VoiceInputLayoutBinding.inflate(android.view.LayoutInflater.from(context))
                voiceInputView = voiceInputBinding?.root
                if (parent.isAttachedToWindow) {
                    parent.addView(voiceInputView, 0)
                    voiceInputView?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_up))
                    Log.d("VoiceInputAnimation", "Новое поле голосового ввода создано")
                } else {
                    Log.e("VoiceInputAnimation", "Родительский элемент не прикреплён к окну")
                    return
                }

                //OK
                voiceInputBinding?.confirmButton?.setOnClickListener {
                    val text = voiceInputBinding?.voiceInputEditText?.text?.toString() ?: currentText
                    if (text.isNotEmpty() && text.trimEnd('.', '!', '?') != context.getString(R.string.not_recognized)) {
                        inputConnection?.commitText(text, 1)
                        Log.d("VoiceInputAnimation", "Текст отправлен в InputConnection: $text")
                    } else {
                        Log.d("VoiceInputAnimation", "Текст пустой или не распознан, ничего не отправлено")
                    }
                    hideVoiceInputField(parent)
                }
            } else {
                Log.d("VoiceInputAnimation", "Используется существующее поле голосового ввода")
            }

            mainHandler.post {
                voiceInputBinding?.voiceInputEditText?.apply {
                    setText("")
                    hint = context.getString(R.string.speak_hint)
                }
                Log.d("VoiceInputAnimation", "Состояние EditText сброшено, hint: '${voiceInputBinding?.voiceInputEditText?.hint}'")
            }
        } catch (e: Exception) {
            Log.e("VoiceInputAnimation", "Ошибка добавления поля: ${e.message}", e)
        }
    }

    fun hideVoiceInputField(parent: ViewGroup) {
        mainHandler.post {
            voiceInputView?.let { view ->
                try {
                    if (view.parent != null && parent.isAttachedToWindow) {
                        parent.removeView(view)
                    }
                    voiceInputView = null
                    voiceInputBinding = null
                    Log.d("VoiceInputAnimation", "Поле ввода удалено")
                } catch (e: Exception) {
                    Log.e("VoiceInputAnimation", "Ошибка удаления поля: ${e.message}")
                }
            }
        }
    }

    fun setRecognizedText(text: String) {
        val cleanedText = text.trimEnd('.', '!', '?')
        currentText = if (cleanedText == context.getString(R.string.not_recognized)) "" else text
        mainHandler.post {
            voiceInputBinding?.voiceInputEditText?.apply {
                setText(currentText)
                hint = if (currentText.isEmpty()) {
                    if (cleanedText == context.getString(R.string.not_recognized)) {
                        context.getString(R.string.not_recognized)
                    } else {
                        context.getString(R.string.speak_hint)
                    }
                } else {
                    null
                }
            }
            Log.d("VoiceInputAnimation", "Текст установлен: '$currentText', hint: '${voiceInputBinding?.voiceInputEditText?.hint}'")
        }
    }

    fun updateInputConnection(newInputConnection: InputConnection?) {
        inputConnection = newInputConnection
        Log.d("VoiceInputAnimation", "InputConnection обновлён: ${if (newInputConnection == null) "null" else "valid"}")
    }
}