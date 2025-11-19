package com.example.chalkak

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale

/**
 * Helper class for managing TTS functionality
 * Handles initialization, button binding, speech playback, and resource cleanup.
 * Receives the root view of card_word_detail layout and finds all buttons internally.
 */
class TtsHelper(
    private val context: Context,
    private val cardWordDetail: View
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    // Find and store buttons internally
    private lateinit var btnTtsWord: ImageView
    private lateinit var btnTtsExample: ImageView
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtExampleSentence: TextView

    init {
        initializeViews()
        initializeTts()
        setupButtonListeners()
    }
    
    private fun initializeViews() {
        // Find all views inside card_word_detail layout
        btnTtsWord = cardWordDetail.findViewById(R.id.btn_tts_word)
        btnTtsExample = cardWordDetail.findViewById(R.id.btn_tts_example)
        txtSelectedWord = cardWordDetail.findViewById(R.id.txt_selected_word)
        txtExampleSentence = cardWordDetail.findViewById(R.id.txt_example_sentence)
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                tts?.setSpeechRate(0.7f)
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                               result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    private fun setupButtonListeners() {
        btnTtsWord.setOnClickListener {
            val text = txtSelectedWord.text.toString()
            if (text.isNotBlank()) {
                speak(text)
            }
        }

        btnTtsExample.setOnClickListener {
            val text = txtExampleSentence.text.toString()
            if (text.isNotBlank()) {
                speak(text)
            }
        }
    }

    fun speak(text: String) {
        if (!isInitialized || tts == null) return

        tts?.stop()
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "chalkak_tts"
        )
    }

    fun cleanup() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

