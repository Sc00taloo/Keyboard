package com.example.keyboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.example.keyboard.databinding.KeyboardNumbersLayoutBinding
import kotlin.math.abs

class KeyboardNumbers(private val parentService: Keyboard) {
    //private val keyboardSpecialCharacters = KeyboardSpecialCharacters(parentService)

    fun onCreateInputView(): View {
        val keyboarding = KeyboardNumbersLayoutBinding.inflate(parentService.layoutInflater)
        val buttonList = arrayOf(
            R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn0,
            R.id.btnDash, R.id.btnSlash, R.id.btnDoublePoint, R.id.btnPointWithComma, R.id.btnBracketLeft, R.id.btnBracketRight,
            R.id.btnCurrency, R.id.btnAnd, R.id.btnDog, R.id.btnQuotes, R.id.btnPoint, R.id.btnComma, R.id.btnQuestion,
            R.id.btnExclamation, R.id.btnMiniQuotes
        )
        for (buttonId in buttonList) {
            val button = keyboarding.root.findViewById<Button>(buttonId)
            button.setOnClickListener {
                val input = parentService.currentInputConnection
                input?.commitText(button.text.toString(), 1)
            }
        }

        keyboarding.btnSpace.text = if (parentService.getCurrentLanguage() == Language.EN) "< English >" else "< Русский >"
        keyboarding.btnCurrency.text = parentService.resources.getString(
            if (parentService.getCurrentLanguage() == Language.EN) R.string.currency else R.string.currencyRU
        )

        keyboarding.btnSpace.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0f
            private var isSwiping = false
            private val swipeThreshold = 50f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = event.x
                        isSwiping = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.x - initialX
                            if (abs(deltaX) > swipeThreshold) {
                                isSwiping = true
                                val newLanguage = if (parentService.getCurrentLanguage() == Language.EN) Language.RU else Language.EN
                                parentService.setCurrentLanguage(newLanguage)
                                parentService.setInputView(parentService.onCreateInputView())
                                return true
                            }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isSwiping) {
                            parentService.currentInputConnection?.commitText(" ", 1)
                        }
                        isSwiping = false
                        return true
                    }
                }
                return false
            }
        })
        //// NEED!!!!!!!!!!!!!!!!!!!!!
        keyboarding.btnVoiceInput.apply {
            isClickable = true
            isFocusable = true
            isEnabled = true
            Log.d("KeyboardNumbers", "Кнопка голосового ввода инициализирована для языка: ${if (parentService.getCurrentLanguage() == Language.EN) "Английский" else "Русский"}")

            setOnClickListener {
                Log.d("KeyboardNumbers", "Кнопка голосового ввода нажата: ${if (parentService.getCurrentLanguage() == Language.EN) "en-US" else "ru-RU"}")
                Log.d("KeyboardNumbers", "Текущее соединение ввода: ${if (parentService.currentInputConnection == null) "отсутствует" else "есть"}")
                Log.d("KeyboardNumbers", "root элемент: ${if (keyboarding.root.parent != null) "присутствует" else "отсутствует"}")

                try {
                    if (ContextCompat.checkSelfPermission(parentService, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if (keyboarding.root.parent != null) {
                            Log.d("KeyboardNumbers", "Разрешение на запись получено")
                            parentService.voiceInputAnimation.showVoiceInputField(keyboarding.root.parent as ViewGroup, parentService.currentInputConnection)
                            parentService.microphone.startVoiceInput(if (parentService.getCurrentLanguage() == Language.EN) "en-US" else "ru-RU")
                        } else {
                            Log.e("KeyboardNumbers", "Ошибка, отсутствует родительский элемент")
                        }
                    } else {
                        Log.d("KeyboardNumbers", "Разрешение на запись не получено")
                        parentService.requestMicrophone.requestMicrophonePermission()
                    }
                } catch (e: Exception) {
                    Log.e("KeyboardNumbers", "Ошибка при нажатии кнопки голосового ввода: ${e.message}", e)
                }
            }

            setOnTouchListener { _, event ->
                Log.d("KeyboardNumbers", "Кнопка голосового ввода нажата: действие ${event.action}")
                false
            }
        }
        //// NEED!!!!!!!!!!!!!!!!!!!!!
        keyboarding.btnEnter.setOnClickListener{
            val input = parentService.currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            return@setOnClickListener
        }
        val delete = keyboarding.root.findViewById<ImageButton>(R.id.btnDelete)
        delete.setOnTouchListener(object : View.OnTouchListener {
            private var isDeleting = false
            private val handler = Handler(Looper.getMainLooper())
            private val deleteRunnable = object : Runnable {
                override fun run() {
                    if (isDeleting) {
                        val input = parentService.currentInputConnection
                        input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        handler.postDelayed(this, 200)
                    }
                }
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDeleting = true
                        handler.post(deleteRunnable)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDeleting = false
                        handler.removeCallbacks(deleteRunnable)
                        return true
                    }
                }
                return false
            }
        })
        keyboarding.btnBaseKeyboard.setOnClickListener {
            parentService.setInputView(parentService.onCreateInputView())
        }
        keyboarding.btnSpecialCharacters.setOnClickListener{
            parentService.setInputView(parentService.getKeyboardSpecialCharacters().onCreateInputView())
        }
        return keyboarding.root
    }
}