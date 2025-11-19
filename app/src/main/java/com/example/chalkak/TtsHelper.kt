package com.example.chalkak

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale

/**
 * TTS 기능을 관리하는 헬퍼 클래스
 * 초기화, 버튼 바인딩, 음성 재생, 리소스 정리를 담당합니다.
 * card_word_detail 레이아웃의 루트 뷰를 받아서 내부에서 모든 버튼을 찾습니다.
 */
class TtsHelper(
    private val context: Context,
    private val cardWordDetail: View
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    // 버튼들을 내부에서 찾아서 저장
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
        // card_word_detail 레이아웃 내부의 모든 뷰를 찾습니다
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

