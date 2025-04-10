package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import com.example.keyboard.databinding.KeyboardLayoutBinding
import com.example.keyboard.databinding.KeyboardRuLayoutBinding
import kotlin.math.abs

class Keyboard : InputMethodService() {
    private var capsLockFull = false
    private var capsLockOne = false
    private var lastClick = 0L
    private val doubleClickThreshold = 500

    private lateinit var enKeyboard: KeyboardLayoutBinding
    private lateinit var ruKeyboard: KeyboardRuLayoutBinding
    private var currentLanguage = Language.EN

    val keyboardNumbers = KeyboardNumbers(this)
    private val keyboardSpecialCharacters = KeyboardSpecialCharacters(this)

    private val enKeyboardList = arrayOf(
        R.id.btnq, R.id.btnw, R.id.btne, R.id.btnr, R.id.btnt, R.id.btny, R.id.btnu, R.id.btni, R.id.btno, R.id.btnp,
        R.id.btna, R.id.btns, R.id.btnd, R.id.btnf, R.id.btng, R.id.btnh, R.id.btnj, R.id.btnk, R.id.btnl, R.id.btnz,
        R.id.btnx, R.id.btnc, R.id.btnv, R.id.btnb, R.id.btnn, R.id.btnm, R.id.btnComma, R.id.btnPoint
    )
    private val ruKeyboardList = arrayOf(
        R.id.btnй, R.id.btnц, R.id.btnу, R.id.btnк, R.id.btnе, R.id.btnн, R.id.btnг, R.id.btnш, R.id.btnщ, R.id.btnз, R.id.btnх,
        R.id.btnф, R.id.btnы, R.id.btnв, R.id.btnа, R.id.btnп, R.id.btnр, R.id.btnо, R.id.btnл, R.id.btnд, R.id.btnж, R.id.btnэ,
        R.id.btnя, R.id.btnч, R.id.btnс, R.id.btnм, R.id.btnи, R.id.btnт, R.id.btnь, R.id.btnб, R.id.btnю, R.id.btnComma, R.id.btnPoint
    )

    override fun onCreateInputView(): View {
        enKeyboard = KeyboardLayoutBinding.inflate(layoutInflater)
        ruKeyboard = KeyboardRuLayoutBinding.inflate(layoutInflater)
        setupKeyboard(enKeyboard)
        setupKeyboard(ruKeyboard)
        return if (currentLanguage == Language.EN) enKeyboard.root else ruKeyboard.root
    }

    private fun setupKeyboard(keyboarding: Any) {
        val buttonList = when (keyboarding) {
            is KeyboardLayoutBinding -> enKeyboardList
            is KeyboardRuLayoutBinding -> ruKeyboardList
            else -> throw IllegalArgumentException("Unknown binding type")
        }

        val rootView = when (keyboarding) {
            is KeyboardLayoutBinding -> keyboarding.root
            is KeyboardRuLayoutBinding -> keyboarding.root
            else -> throw IllegalArgumentException("Unknown binding type")
        }

        for(buttonId in buttonList){
            val button = rootView.findViewById<Button>(buttonId)
            button.setOnClickListener{
                val input = currentInputConnection
                var text = button.text.toString()
                text = if (capsLockFull || capsLockOne) text.uppercase() else text.lowercase()
                input?.commitText(text, 1)

                if (capsLockOne && !capsLockFull) {
                    capsLockOne = false
                    updateButtonLabels()
                }
            }
        }

        val btnSpace = rootView.findViewById<Button>(R.id.btnSpace)
        btnSpace?.setOnTouchListener(object : View.OnTouchListener {
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
                                currentLanguage = if (currentLanguage == Language.EN) Language.RU else Language.EN
                                setInputView(if (currentLanguage == Language.EN) enKeyboard.root else ruKeyboard.root)
                                updateSpaceButtonText()
                                isLongPress = false
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isLongPress) {
                            currentInputConnection?.commitText(" ", 1)
                        }
                        isLongPress = false
                        return true
                    }
                }
                return false
            }
        })

        val btnUp = rootView.findViewById<Button>(R.id.btnUp)
        btnUp?.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastClick = currentTime - lastClick
            if (timeSinceLastClick <= doubleClickThreshold && !capsLockFull) {
                capsLockFull = true
                capsLockOne = false
            }
            else if (capsLockFull) {
                capsLockFull = false
                capsLockOne = false
            }
            else if (capsLockOne) {
                capsLockFull = false
                capsLockOne = false
            }
            else {
                capsLockOne = true
                capsLockFull = false
            }
            updateButtonLabels()
            lastClick = currentTime
        }

        val btnNumbers = rootView.findViewById<Button>(R.id.btnNumbers)
        btnNumbers?.setOnClickListener {
            setInputView(keyboardNumbers.onCreateInputView())
        }

        //// NEED!!!!!!!!!!!!!!!!!!!!!
//        keyboarding.btnVoiceInput.setOnClickListener{
//            val input = currentInputConnection
//            input?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
//            return@setOnClickListener
//        }
        //// NEED!!!!!!!!!!!!!!!!!!!!!

        val btnEnter = rootView.findViewById<Button>(R.id.btnEnter)
        btnEnter?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }

        val btnDelete = rootView.findViewById<Button>(R.id.btnDelete)
        btnDelete?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun updateButtonLabels() {
        val currentBinding = if (currentLanguage == Language.EN) enKeyboard else ruKeyboard
        val currentButtonList = if (currentLanguage == Language.EN) enKeyboardList else ruKeyboardList

        for (buttonId in currentButtonList) {
            val button = currentBinding.root.findViewById<Button>(buttonId)
            val baseText = button.text.toString().lowercase()
            button.text = if (capsLockFull || capsLockOne) baseText.uppercase() else baseText
        }
    }

    private fun updateSpaceButtonText() {
        val spaceText = if (currentLanguage == Language.EN) "< English >" else "< Русский >"
        enKeyboard.btnSpace.text = spaceText
        ruKeyboard.btnSpace.text = spaceText
    }

    fun getKeyboardSpecialCharacters(): KeyboardSpecialCharacters = keyboardSpecialCharacters
    fun getCurrentLanguage(): Language = currentLanguage
    fun setCurrentLanguage(language: Language) {
        currentLanguage = language
        updateSpaceButtonText()
    }
}















