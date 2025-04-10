package com.example.keyboard

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
            private var isLongPress = false
            private val swipeThreshold = 50

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = event.x
                        v.postDelayed({
                            isLongPress = true
                        }, 300)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isLongPress) {
                            val deltaX = event.x - initialX
                            if (abs(deltaX) > swipeThreshold) {
                                val newLanguage = if (parentService.getCurrentLanguage() == Language.EN) Language.RU else Language.EN
                                parentService.setCurrentLanguage(newLanguage)
                                parentService.setInputView(parentService.onCreateInputView())
                                isLongPress = false
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isLongPress) {
                            parentService.currentInputConnection?.commitText(" ", 1)
                        }
                        isLongPress = false
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
        keyboarding.btnDelete.setOnClickListener{
            val input = parentService.currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            return@setOnClickListener
        }
        keyboarding.btnBaseKeyboard.setOnClickListener{
            parentService.setInputView(parentService.onCreateInputView())
        }
        keyboarding.btnNumbers.setOnClickListener{
            parentService.setInputView(parentService.keyboardNumbers.onCreateInputView())
        }
        return keyboarding.root
    }
}