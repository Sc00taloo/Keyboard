package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import com.example.keyboard.databinding.KeyboardLayoutBinding

class Keyboard : InputMethodService() {
    private var capsLockFull = false
    private var capsLockOne = false
    private var lastClick = 0L
    private val doubleClickThreshold = 500

    val keyboardNumbers = KeyboardNumbers(this)
    private val keyboardSpecialCharacters = KeyboardSpecialCharacters(this)

    override fun onCreateInputView(): View {
        val keyboarding = KeyboardLayoutBinding.inflate(layoutInflater)
        val buttonList = arrayOf(
            R.id.btnq, R.id.btnw, R.id.btne, R.id.btnr, R.id.btnt, R.id.btny, R.id.btnu, R.id.btni, R.id.btno, R.id.btnp,
            R.id.btna, R.id.btns, R.id.btnd, R.id.btnf, R.id.btng, R.id.btnh, R.id.btnj, R.id.btnk, R.id.btnl, R.id.btnz,
            R.id.btnx, R.id.btnc, R.id.btnv, R.id.btnb, R.id.btnn, R.id.btnm, R.id.btnComma, R.id.btnPoint
        )
        for(buttonId in buttonList){
            val button = keyboarding.root.findViewById<Button>(buttonId)
            button.setOnClickListener{
                val input = currentInputConnection
                var text = button.text.toString()
                text = if (capsLockFull || capsLockOne) text.uppercase() else text.lowercase()
                input?.commitText(text, 1)

                if (capsLockOne && !capsLockFull) {
                    capsLockOne = false
                    updateButtonLabels(keyboarding, buttonList)
                }
            }
        }
        keyboarding.btnSpace.setOnClickListener{
            val input = currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
            return@setOnClickListener
        }
        keyboarding.btnUp.setOnClickListener{
            val currentTime = System.currentTimeMillis()
            val timeSinceLastClick = currentTime - lastClick

            if (timeSinceLastClick <= doubleClickThreshold && !capsLockFull) {
                capsLockFull = true
                capsLockOne = false
            } else if (capsLockFull) {
                capsLockFull = false
                capsLockOne = false
            } else {
                capsLockOne = true
                capsLockFull = false
            }
            updateButtonLabels(keyboarding, buttonList)
            lastClick = currentTime
        }
        keyboarding.btnNumbers.setOnClickListener{
            setInputView(keyboardNumbers.onCreateInputView())
        }
        //// NEED!!!!!!!!!!!!!!!!!!!!!
        keyboarding.btnVoiceInput.setOnClickListener{
            val input = currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
            return@setOnClickListener
        }
        //// NEED!!!!!!!!!!!!!!!!!!!!!
        keyboarding.btnEnter.setOnClickListener{
            val input = currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            return@setOnClickListener
        }
        keyboarding.btnDelete.setOnClickListener{
            val input = currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            return@setOnClickListener
        }
        return keyboarding.root
    }

    private fun updateButtonLabels(keyboarding: KeyboardLayoutBinding, buttonList: Array<Int>) {
        for (buttonId in buttonList) {
            val button = keyboarding.root.findViewById<Button>(buttonId)
            val baseText = button.text.toString().lowercase()
            button.text = if (capsLockFull || capsLockOne) baseText.uppercase() else baseText
        }
    }

    fun getKeyboardSpecialCharacters(): KeyboardSpecialCharacters = keyboardSpecialCharacters
}















