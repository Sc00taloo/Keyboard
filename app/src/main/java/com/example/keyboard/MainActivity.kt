package com.example.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.keyboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply{
            if (isKeyboardEnable()){
                btnEnableKeyboard.isEnabled = false
            }
            btnEnableKeyboard.setOnClickListener{
                if(!isKeyboardEnable()){
                    keyboardSetting()
                }
            }
            btnChooseKeyboard.setOnClickListener{
                if(isKeyboardEnable()){
                    keyboardChoose()
                }
                else{
                    Toast.makeText(this@MainActivity, "Test", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun isKeyboardEnable() : Boolean{
        val input = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enableInput = input.enabledInputMethodList.map {it.id}
        return enableInput.contains("com.example.keyboard/.Keyboard")
    }
    private fun keyboardChoose(){
        val choose = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        choose.showInputMethodPicker()
    }
    private fun keyboardSetting(){
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent)
    }
}













