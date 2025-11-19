package com.example.chalkak

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

/**
 * Manager class for recording/playback/STT functionality
 * Reduces code duplication by handling UI binding and event processing.
 * Receives the root view of card_word_detail layout and finds all buttons internally.
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val cardWordDetail: android.view.View,
    private val requestPermissionLauncher: ActivityResultLauncher<String>,
    private val onWordSttResult: ((String, Boolean) -> Unit)? = null,
    private val onExampleSttResult: ((String, Boolean) -> Unit)? = null
) {
    private var speechHelperMeaning: SpeechRecognitionHelper? = null
    private var speechHelperExample: SpeechRecognitionHelper? = null
    
    // Find and store buttons internally
    private lateinit var btnRecordMeaning: ImageView
    private lateinit var btnRecordExample: ImageView
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtExampleSentence: TextView

    init {
        initializeViews()
        initialize()
    }
    
    private fun initializeViews() {
        // Find all views inside card_word_detail layout
        btnRecordMeaning = cardWordDetail.findViewById(R.id.btn_record_meaning)
        btnRecordExample = cardWordDetail.findViewById(R.id.btn_record_example)
        txtSelectedWord = cardWordDetail.findViewById(R.id.txt_selected_word)
        txtExampleSentence = cardWordDetail.findViewById(R.id.txt_example_sentence)
    }

    private fun initialize() {
        // Helper for meaning recording
        speechHelperMeaning = SpeechRecognitionHelper(
            context,
            onStateChanged = { state ->
                updateButtonsForMeaning(state)
            },
            onSttResult = { recognizedText, isCorrect ->
                onWordSttResult?.invoke(recognizedText, isCorrect)
                // Also show word comparison result in dialog
                val targetWord = txtSelectedWord.text.toString().lowercase().trim()
                showSttResultDialog(recognizedText, isCorrect, targetWord)
            }
        )

        // Helper for example recording
        speechHelperExample = SpeechRecognitionHelper(
            context,
            onStateChanged = { state ->
                updateButtonsForExample(state)
            },
            onSttResult = { recognizedText, isCorrect ->
                // Basic example sentence comparison logic
                val exampleText = txtExampleSentence.text.toString().lowercase().trim()
                val isExampleCorrect = recognizedText.contains(exampleText) || exampleText.contains(recognizedText)
                
                // Use custom handler if available, otherwise show dialog
                if (onExampleSttResult != null) {
                    onExampleSttResult.invoke(recognizedText, isExampleCorrect)
                } else {
                    showSttResultDialog(recognizedText, isExampleCorrect, exampleText)
                }
            }
        )

        btnRecordMeaning.setOnClickListener {
            handleSttButtonClick(speechHelperMeaning, txtSelectedWord.text.toString())
        }

        btnRecordExample.setOnClickListener {
            handleSttButtonClick(speechHelperExample, txtSelectedWord.text.toString())
        }
    }

    private fun handleSttButtonClick(helper: SpeechRecognitionHelper?, targetWord: String) {
        if (helper == null) return

        if (!checkPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        helper.setTargetWord(targetWord)
        val currentState = helper.getCurrentState()

        when (currentState) {
            RecordingState.IDLE -> {
                // Start STT
                helper.startSpeechRecognition()
            }
            RecordingState.LISTENING -> {
                // Stop STT
                helper.stopSpeechRecognition()
            }
        }
    }

    private fun updateButtonsForMeaning(state: RecordingState) {
        when (state) {
            RecordingState.IDLE -> {
                btnRecordMeaning.setImageResource(R.drawable.ic_mic)
            }
            RecordingState.LISTENING -> {
                btnRecordMeaning.setImageResource(R.drawable.ic_stop)
            }
        }
    }

    private fun updateButtonsForExample(state: RecordingState) {
        when (state) {
            RecordingState.IDLE -> {
                btnRecordExample.setImageResource(R.drawable.ic_mic)
            }
            RecordingState.LISTENING -> {
                btnRecordExample.setImageResource(R.drawable.ic_stop)
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 단어가 변경될 때 호출하여 타겟 단어를 업데이트하고 상태를 리셋합니다.
     */
    fun updateTargetWord(word: String) {
        speechHelperMeaning?.reset()
        speechHelperExample?.reset()
        speechHelperMeaning?.setTargetWord(word)
        speechHelperExample?.setTargetWord(word)
    }

    /**
     * 리소스를 정리합니다.
     */
    fun cleanup() {
        speechHelperMeaning?.cleanup()
        speechHelperExample?.cleanup()
    }

    /**
     * STT 결과를 다이얼로그로 표시합니다.
     */
    private fun showSttResultDialog(recognizedText: String, isCorrect: Boolean, targetText: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_stt_result, null)
        
        val txtRecognized = dialogView.findViewById<TextView>(R.id.txt_recognized_text)
        val txtResultStatus = dialogView.findViewById<TextView>(R.id.txt_result_status)
        val iconResult = dialogView.findViewById<ImageView>(R.id.icon_result)
        val txtTargetWordLabel = dialogView.findViewById<TextView>(R.id.txt_target_word_label)
        val txtTargetWord = dialogView.findViewById<TextView>(R.id.txt_target_word)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)
        
        // Display recognized text
        txtRecognized.text = if (recognizedText.isNotEmpty()) recognizedText else "No recognized text"
        
        // Display result status
        if (isCorrect) {
            txtResultStatus.text = "Correct! ✓"
            txtResultStatus.setTextColor(0xFF4CAF50.toInt()) // Green
            try {
                iconResult.setImageResource(R.drawable.ic_checkmark_green)
            } catch (e: Exception) {
                iconResult.setImageResource(android.R.drawable.checkbox_on_background)
            }
            txtTargetWordLabel.visibility = View.GONE
            txtTargetWord.visibility = View.GONE
        } else {
            txtResultStatus.text = "Incorrect"
            txtResultStatus.setTextColor(0xFFFF6B6B.toInt()) // Red
            iconResult.setImageResource(android.R.drawable.ic_delete)
            txtTargetWordLabel.visibility = View.VISIBLE
            txtTargetWord.visibility = View.VISIBLE
            txtTargetWord.text = targetText
        }
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
}

