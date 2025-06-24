package com.example.keyboard

import android.Manifest
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.example.keyboard.animation.voiceInputAnimation
import com.example.keyboard.databinding.KeyboardLayoutBinding
import com.example.keyboard.databinding.KeyboardRuLayoutBinding
import com.example.keyboard.sentenceClassifier.SentenceClassifier
import com.example.keyboard.spellChecker.SpellChecker
import com.example.keyboard.voice.Microphone
import com.example.keyboard.voice.requestMicrophone
import kotlin.math.abs

class Keyboard : InputMethodService() {
    private var capsLockFull = false
    private var capsLockOne = false
    private var lastClick = 0L
    private val doubleClickThreshold = 500

    private var enKeyboard: KeyboardLayoutBinding? = null
    private var ruKeyboard: KeyboardRuLayoutBinding? = null
    private val currentInput = StringBuilder()

    private lateinit var spellChecker: SpellChecker
    private var currentLanguage = Language.EN
    val keyboardNumbers = KeyboardNumbers(this)

    lateinit var microphone: Microphone
    lateinit var requestMicrophone: requestMicrophone

    lateinit var voiceInputAnimation: voiceInputAnimation

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

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d("Keyboard", "Starting onCreate")
            val classifier = SentenceClassifier(this)
            microphone = Microphone(
                context = this,
                inputConnection = currentInputConnection,
                onTextRecognized = { text ->
                    Log.d("Keyboard", "Text recognized: $text")
                    val rootView = if (currentLanguage == Language.EN) enKeyboard?.root else ruKeyboard?.root
                    rootView?.let {
                        Log.d("Keyboard", "Showing voice input field")
                        voiceInputAnimation.showVoiceInputField(it, currentInputConnection)
                        voiceInputAnimation.setRecognizedText(text)
                        val editText = voiceInputAnimation.voiceInputBinding?.voiceInputEditText
                        editText?.setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                val layout = editText.layout ?: return@setOnTouchListener false
                                val x = event.x
                                val y = event.y
                                val line = layout.getLineForVertical(y.toInt())
                                val offset = layout.getOffsetForHorizontal(line, x)
                                val textContent = editText.text?.toString() ?: return@setOnTouchListener false
                                if (offset in textContent.indices) {
                                    val char = textContent[offset]
                                    Log.d("Touch", "Touched char: '$char' (${char.code}) at offset $offset")
                                    if (char == ' ' || char in listOf(',', '.', '!', '?')) {
                                        showPunctuationPopup(view) { punctuation ->
                                            editText.text.replace(offset, offset + 1, punctuation)
                                        }
                                        return@setOnTouchListener true
                                    }
                                }
                            }
                            false
                        }
                        val lastWord = text.trim().split(" ").lastOrNull() ?: text
                        spellChecker.checkSpelling(it, lastWord, currentLanguage, true)
                    } ?: Log.e("Keyboard", "Root view is null for language: $currentLanguage")
                },
                classifier = classifier
            )
            requestMicrophone = requestMicrophone(this)
            voiceInputAnimation = voiceInputAnimation(this, currentInputConnection)
            Log.d("Keyboard", "Microphone and animations initialized")

            // Инициализация клавиатур
            enKeyboard?.let { setupKeyboard(it) }
            ruKeyboard?.let { setupKeyboard(it) }
        } catch (e: Exception) {
            Log.e("Keyboard", "Failed to initialize keyboard components: ${e.message}", e)
        }
    }


    override fun onCreateInputView(): View {
        val container = ViewGroup.inflate(this, R.layout.container_layout, null) as ViewGroup
        container.removeAllViews()
        // Кэширую макеты, если они еще не созданы
        if (enKeyboard == null) {
            enKeyboard = KeyboardLayoutBinding.inflate(layoutInflater)
            setupKeyboard(enKeyboard!!)
        }
        if (ruKeyboard == null) {
            ruKeyboard = KeyboardRuLayoutBinding.inflate(layoutInflater)
            setupKeyboard(ruKeyboard!!)
        }
        if (!::spellChecker.isInitialized) {
            spellChecker = SpellChecker(this, currentInputConnection) { currentInput.clear() }
        }
        enKeyboard?.root?.let { if (it.parent != null) (it.parent as ViewGroup).removeView(it) }
        ruKeyboard?.root?.let { if (it.parent != null) (it.parent as ViewGroup).removeView(it) }

        val activeKeyboard = if (currentLanguage == Language.EN) enKeyboard else ruKeyboard
        container.addView(activeKeyboard!!.root)
        Log.d("Keyboard", "Added ${if (currentLanguage == Language.EN) "English" else "Russian"} keyboard to container")

        return container
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

        for (buttonId in buttonList) {
            val button = rootView.findViewById<Button>(buttonId)
            button?.setOnClickListener {
                val input = currentInputConnection
                val text = button.text.toString()
                if (text.matches("[a-zA-Zа-яА-Я]".toRegex())) {
                    val charToAppend =
                        if (capsLockFull || capsLockOne) text.uppercase() else text.lowercase()
                    currentInput.append(charToAppend)
                    val currentText =
                        input?.getTextBeforeCursor(100, 0)?.toString() ?: currentInput.toString()
                    val hasSpaceBefore =
                        currentText.isEmpty() || currentText.takeLastWhile { it != ' ' && it != '\n' } == currentInput.toString()
                    spellChecker.checkSpelling(
                        rootView,
                        currentInput.toString(),
                        currentLanguage,
                        hasSpaceBefore
                    )
                }
                input?.commitText(text, 1)
                if (capsLockOne && !capsLockFull) {
                    capsLockOne = false
                    updateButtonLabels()
                    updateUpButtonIcon(rootView.findViewById(R.id.btnUp))
                }
            }
        }

        val btnSpace = rootView.findViewById<Button>(R.id.btnSpace)
        btnSpace?.setOnTouchListener(object : View.OnTouchListener {
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
                            currentLanguage =
                                if (currentLanguage == Language.EN) Language.RU else Language.EN
                            updateInputView()
                            updateSpaceButtonText()
                            currentInput.clear()
                            spellChecker.clearSuggestions(rootView)
                            return true
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!isSwiping) {
                            currentInputConnection?.commitText(" ", 1)
                            currentInput.clear()
                            spellChecker?.clearSuggestions(rootView)
                        }
                        isSwiping = false
                        return true
                    }
                }
                return false
            }
        })

        val btnUp = rootView.findViewById<ImageButton>(R.id.btnUp)
        btnUp?.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastClick = currentTime - lastClick
            if (timeSinceLastClick <= doubleClickThreshold && !capsLockFull) {
                capsLockFull = true
                capsLockOne = false
                btnUp.setImageResource(R.drawable.up_arrow_full)
            } else if (capsLockFull) {
                capsLockFull = false
                capsLockOne = false
                btnUp.setImageResource(R.drawable.up_arrow)
            } else if (capsLockOne) {
                capsLockFull = false
                capsLockOne = false
                btnUp.setImageResource(R.drawable.up_arrow)
            } else {
                capsLockOne = true
                capsLockFull = false
                btnUp.setImageResource(R.drawable.up_arrow_one)
            }
            updateButtonLabels()
            lastClick = currentTime
        }

        val btnNumbers = rootView.findViewById<Button>(R.id.btnNumbers)
        btnNumbers?.setOnClickListener {
            setInputView(keyboardNumbers.onCreateInputView())
        }

        // NEED!!!!!!!!!!!!!!!!!!!!!
        val btnVoiceInput = rootView.findViewById<ImageButton>(R.id.btnVoiceInput)
        btnVoiceInput?.apply {
            isClickable = true
            isFocusable = true
            isEnabled = true
            Log.d("Keyboard", "Кнопка голосового ввода найдена для языка: ${if (keyboarding is KeyboardLayoutBinding) "Английский" else "Русский"}")

            setOnClickListener {
                Log.d("Keyboard", "Кнопка голосового ввода нажата: ${if (currentLanguage == Language.EN) "en-US" else "ru-RU"}")
                Log.d("Keyboard", "Текущее соединение ввода: ${if (currentInputConnection == null) "отсутствует" else "есть"}")
                Log.d("Keyboard", "root элемент: ${if (rootView.parent != null) "присутствует" else "отсутствует"}")
                try {
                    if (ContextCompat.checkSelfPermission(this@Keyboard, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if (rootView.parent != null) {
                            Log.d("Keyboard", "Разрешение на запись получено")
                            voiceInputAnimation.showVoiceInputField(rootView.parent as ViewGroup, currentInputConnection)
                            microphone.startVoiceInput(if (currentLanguage == Language.EN) "en-US" else "ru-RU")
                        } else {
                            Log.e("Keyboard", "Ошибка, отсутствует родительский элемент")
                        }
                    } else {
                        Log.d("Keyboard", "Разрешение на запись не получено")
                        requestMicrophone.requestMicrophonePermission()
                    }
                } catch (e: Exception) {
                    Log.e("Keyboard", "Ошибка при нажатии кнопки голосового ввода: ${e.message}", e)
                }
            }

            setOnTouchListener { _, event ->
                Log.d("Keyboard", "Кнопка голосового ввода нажата: действие ${event.action}")
                false
            }
        }
        // NEED!!!!!!!!!!!!!!!!!!!!!
        val btnEnter = rootView.findViewById<Button>(R.id.btnEnter)
        btnEnter?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER
                )
            )
            currentInput.clear()
            spellChecker.clearSuggestions(rootView)
        }

        val btnDelete = rootView.findViewById<ImageButton>(R.id.btnDelete)
        btnDelete?.setOnTouchListener(object : View.OnTouchListener {
            private var isDeleting = false
            private var deleteHandler: Handler? = null
            private lateinit var deleteRunnable: Runnable

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDeleting = true
                        deleteHandler = Handler(Looper.getMainLooper())
                        deleteRunnable = object : Runnable {
                            override fun run() {
                                if (isDeleting) {
                                    deleteCharacter()
                                    deleteHandler?.postDelayed(this, 200)
                                }
                            }
                        }
                        deleteHandler?.post(deleteRunnable)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDeleting = false
                        deleteHandler?.removeCallbacks(deleteRunnable)
                        return true
                    }
                }
                return false
            }
            private fun deleteCharacter() {
                currentInputConnection?.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                )
                val currentText = currentInputConnection?.getTextBeforeCursor(100, 0)?.toString() ?: ""
                val currentWord = currentText.takeLastWhile { it != ' ' && it != '\n' }
                currentInput.clear()
                currentInput.append(currentWord)
                val hasSpaceBefore = currentText.isEmpty() || currentText.takeLastWhile { it != ' ' && it != '\n' } == currentWord
                if (currentWord.isNotEmpty()) {
                    spellChecker.checkSpelling(
                        rootView,
                        currentWord,
                        currentLanguage,
                        hasSpaceBefore
                    )
                } else {
                    spellChecker.clearSuggestions(rootView)
                }
            }
        })
    }

    override fun onDestroy() {
        microphone.destroy()
        if (::spellChecker.isInitialized) {
            spellChecker.destroy()
        }
        super.onDestroy()
    }

    private fun updateButtonLabels() {
        val currentBinding = if (currentLanguage == Language.EN) enKeyboard else ruKeyboard
        val currentButtonList = if (currentLanguage == Language.EN) enKeyboardList else ruKeyboardList

        for (buttonId in currentButtonList) {
            val button = currentBinding?.root?.findViewById<Button>(buttonId)
            val baseText = button?.text.toString().lowercase()
            if (button != null) {
                button.text = if (capsLockFull || capsLockOne) baseText.uppercase() else baseText
            }
        }
    }

    private fun updateSpaceButtonText() {
        val spaceText = if (currentLanguage == Language.EN) "< English >" else "< Русский >"
        enKeyboard?.btnSpace?.text ?: spaceText
        ruKeyboard?.btnSpace?.text ?: spaceText
    }

    private fun updateUpButtonIcon(button: Button?) {
        button?.setCompoundDrawablesWithIntrinsicBounds(
            when {
                capsLockFull -> R.drawable.up_arrow_full
                capsLockOne -> R.drawable.up_arrow_one
                else -> R.drawable.up_arrow
            }, 0, 0, 0
        )
    }

    fun getKeyboardSpecialCharacters(): KeyboardSpecialCharacters = keyboardSpecialCharacters
    fun getCurrentLanguage(): Language = currentLanguage
    fun setCurrentLanguage(language: Language) {
        currentLanguage = language
        updateSpaceButtonText()
    }

    private fun updateInputView() {
        val container = ViewGroup.inflate(this, R.layout.container_layout, null) as ViewGroup
        container.removeAllViews()

        enKeyboard?.root?.let { if (it.parent != null) (it.parent as ViewGroup).removeView(it) }
        ruKeyboard?.root?.let { if (it.parent != null) (it.parent as ViewGroup).removeView(it) }

        val activeKeyboard = if (currentLanguage == Language.EN) enKeyboard else ruKeyboard
        container.addView(activeKeyboard!!.root)
        Log.d("Keyboard", "Updated input view with ${if (currentLanguage == Language.EN) "English" else "Russian"} keyboard")

        // Обновляем InputConnection для VoiceInputManager
        voiceInputAnimation.updateInputConnection(currentInputConnection)
        setInputView(container)
    }

    private fun showPunctuationPopup(anchor: View, onSelect: (String) -> Unit) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.punctuation_popup_layout, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        val punctuationList = listOf(".", ",", "!", "?", ";", ":")
        val container = popupView.findViewById<LinearLayout>(R.id.popupContainer)
        container.removeAllViews()

        for (symbol in punctuationList) {
            val button = Button(this).apply {
                text = symbol
                textSize = 18f
                setOnClickListener {
                    onSelect(symbol)
                    popupWindow.dismiss()
                }
            }
            container.addView(button)
        }
        popupWindow.showAsDropDown(anchor)
    }

}
