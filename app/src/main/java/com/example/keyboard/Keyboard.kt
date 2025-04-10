package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import com.example.keyboard.databinding.KeyboardLayoutBinding

class Keyboard : InputMethodService() {
    override fun onCreateInputView(): View {
        val keyboarding = KeyboardLayoutBinding.inflate(layoutInflater)
        val buttonList = arrayOf(
            R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn0,
            R.id.btnq, R.id.btnw, R.id.btne, R.id.btnr, R.id.btnt, R.id.btny, R.id.btnu, R.id.btni, R.id.btno, R.id.btnp,
            R.id.btna, R.id.btns, R.id.btnd, R.id.btnf, R.id.btng, R.id.btnh, R.id.btnj, R.id.btnk, R.id.btnl, R.id.btnz,
            R.id.btnx, R.id.btnc, R.id.btnv, R.id.btnb, R.id.btnn, R.id.btnm, R.id.btnComma, R.id.btnPoint
        )
        for(buttonId in buttonList){
            val button = keyboarding.root.findViewById<Button>(buttonId)
            button.setOnClickListener{
                val input = currentInputConnection
                input?.commitText(button.text.toString(),1)
            }
        }
        keyboarding.btnSpace.setOnClickListener{
            val input = currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
            return@setOnClickListener
        }
        keyboarding.btnUp.setOnClickListener{
            val input = currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAPS_LOCK))
            return@setOnClickListener
        }
        keyboarding.btnNumbers.setOnClickListener{
            val input = currentInputConnection
            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            return@setOnClickListener
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
}















