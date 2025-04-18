package com.example.keyboard

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import com.example.keyboard.databinding.KeyboardSpecialCharactersLayoutBinding
import kotlin.math.abs

class KeyboardSpecialCharacters(private val parentService: Keyboard) {
    //private val keyboardNumbers = KeyboardNumbers(parentService)

    fun onCreateInputView(): View {
        val keyboarding = KeyboardSpecialCharactersLayoutBinding.inflate(parentService.layoutInflater)
        val buttonList = arrayOf(
            R.id.btnQuadraBracketLeft, R.id.btnQuadraBracketRight, R.id.btnCurlyBracketLeft, R.id.btnCurlyBracketRight,
            R.id.btnLattice, R.id.btnPercent, R.id.btnDegree, R.id.btnStar, R.id.btnPlus, R.id.btnEqual, R.id.btnUnderscores,
            R.id.btnSlashRight, R.id.btnStick, R.id.btnFloatingStick, R.id.btnAngleBracketLeft, R.id.btnAngleBracketRight,
            R.id.btnCurrencySwap, R.id.btnCurrencyEuro, R.id.btnCurrencyFunt, R.id.btnCentrePoint, R.id.btnPoint, R.id.btnComma,
            R.id.btnQuestion, R.id.btnExclamation, R.id.btnMiniQuotes
        )
        for (buttonId in buttonList) {
            val button = keyboarding.root.findViewById<Button>(buttonId)
            button.setOnClickListener {
                val input = parentService.currentInputConnection
                input?.commitText(button.text.toString(), 1)
            }
        }

        keyboarding.btnSpace.text = if (parentService.getCurrentLanguage() == Language.EN) "< English >" else "< Русский >"
        keyboarding.btnCurrencySwap.text = parentService.resources.getString(
            if (parentService.getCurrentLanguage() == Language.EN) R.string.currency_swap else R.string.currency_swapEN
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
        keyboarding.btnVoiceInput.setOnClickListener{
            val input = parentService.currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
            return@setOnClickListener
        }
        //// NEED!!!!!!!!!!!!!!!!!!!!!

        keyboarding.btnEnter.setOnClickListener{
            val input = parentService.currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            return@setOnClickListener
        }
        keyboarding.btnDelete.setOnTouchListener(object : View.OnTouchListener {
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
        keyboarding.btnBaseKeyboard.setOnClickListener{
            parentService.setInputView(parentService.onCreateInputView())
        }
        keyboarding.btnNumbers.setOnClickListener{
            parentService.setInputView(parentService.keyboardNumbers.onCreateInputView())
        }
        return keyboarding.root
    }
}