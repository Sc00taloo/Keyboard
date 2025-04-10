package com.example.keyboard

import android.view.KeyEvent
import android.view.View
import android.widget.Button
import com.example.keyboard.databinding.KeyboardSpecialCharactersLayoutBinding

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
        keyboarding.btnBaseKeyboard.setOnClickListener{
            parentService.setInputView(parentService.onCreateInputView())
        }
        keyboarding.btnNumbers.setOnClickListener{
            parentService.setInputView(parentService.keyboardNumbers.onCreateInputView())
        }
        return keyboarding.root
    }
}