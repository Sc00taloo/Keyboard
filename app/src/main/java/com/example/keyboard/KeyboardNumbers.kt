package com.example.keyboard

import android.view.KeyEvent
import android.view.View
import android.widget.Button
import com.example.keyboard.databinding.KeyboardNumbersLayoutBinding

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
        keyboarding.btnSpace.setOnClickListener{
            val input = parentService.currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
            return@setOnClickListener
        }
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
        keyboarding.btnBaseKeyboard.setOnClickListener {
            parentService.setInputView(parentService.onCreateInputView())
        }
        keyboarding.btnSpecialCharacters.setOnClickListener{
            parentService.setInputView(parentService.getKeyboardSpecialCharacters().onCreateInputView())
        }
        return keyboarding.root
    }
}