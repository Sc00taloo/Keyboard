package com.example.keyboard.voice

import android.content.Context
import android.content.Intent

class requestMicrophone(private val context: Context) {
    fun requestMicrophonePermission() {
        val intent = Intent(context, requestActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}