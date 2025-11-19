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
 * 녹음/재생/STT 기능을 관리하는 매니저 클래스
 * UI 바인딩과 이벤트 처리를 포함하여 코드 중복을 제거합니다.
 * card_word_detail 레이아웃의 루트 뷰를 받아서 내부에서 모든 버튼을 찾습니다.
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
    
    // 버튼들을 내부에서 찾아서 저장
    private lateinit var btnRecordMeaning: ImageView
    private lateinit var btnRecordExample: ImageView
    private lateinit var btnPlayMeaning: ImageView
    private lateinit var btnPlayExample: ImageView
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtExampleSentence: TextView

    init {
        initializeViews()
        initialize()
    }
    
    private fun initializeViews() {
        // card_word_detail 레이아웃 내부의 모든 뷰를 찾습니다
        btnRecordMeaning = cardWordDetail.findViewById(R.id.btn_record_meaning)
        btnRecordExample = cardWordDetail.findViewById(R.id.btn_record_example)
        btnPlayMeaning = cardWordDetail.findViewById(R.id.btn_play_meaning)
        btnPlayExample = cardWordDetail.findViewById(R.id.btn_play_example)
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
                // 단어 비교 결과도 다이얼로그로 표시
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
                // 기본 예문 비교 로직
                val exampleText = txtExampleSentence.text.toString().lowercase().trim()
                val isExampleCorrect = recognizedText.contains(exampleText) || exampleText.contains(recognizedText)
                
                // 커스텀 핸들러가 있으면 사용, 없으면 다이얼로그 표시
                if (onExampleSttResult != null) {
                    onExampleSttResult.invoke(recognizedText, isExampleCorrect)
                } else {
                    showSttResultDialog(recognizedText, isExampleCorrect, exampleText)
                }
            }
        )

        btnRecordMeaning.setOnClickListener {
            handleRecordButtonClick(speechHelperMeaning, txtSelectedWord.text.toString())
        }

        btnRecordExample.setOnClickListener {
            handleRecordButtonClick(speechHelperExample, txtSelectedWord.text.toString())
        }

        btnPlayMeaning.setOnClickListener {
            handlePlayButtonClick(speechHelperMeaning)
        }

        btnPlayExample.setOnClickListener {
            handlePlayButtonClick(speechHelperExample)
        }
    }

    private fun handleRecordButtonClick(helper: SpeechRecognitionHelper?, targetWord: String) {
        if (helper == null) return

        if (!checkPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        helper.setTargetWord(targetWord)
        val currentState = helper.getCurrentState()

        when (currentState) {
            RecordingState.IDLE -> {
                helper.startRecording()
            }
            RecordingState.RECORDING -> {
                helper.stopRecording()
                // 녹음 완료 후 자동으로 STT 수행
                helper.startSpeechRecognition()
            }
            RecordingState.RECORDED -> {
                // 재녹음 시작
                helper.reset()
                helper.setTargetWord(targetWord)
                helper.startRecording()
            }
            RecordingState.PLAYING -> {
                // 재생 중에는 녹음 불가
            }
        }
    }

    private fun handlePlayButtonClick(helper: SpeechRecognitionHelper?) {
        if (helper == null) return

        val currentState = helper.getCurrentState()

        when (currentState) {
            RecordingState.RECORDED -> {
                helper.startPlaying()
            }
            RecordingState.PLAYING -> {
                helper.stopPlaying()
            }
            else -> {
                // 재생 가능한 상태가 아님
            }
        }
    }

    private fun updateButtonsForMeaning(state: RecordingState) {
        when (state) {
            RecordingState.IDLE -> {
                btnRecordMeaning.setImageResource(R.drawable.ic_mic)
                btnRecordMeaning.visibility = View.VISIBLE
                btnPlayMeaning.visibility = View.GONE
            }
            RecordingState.RECORDING -> {
                btnRecordMeaning.setImageResource(R.drawable.ic_stop)
                btnRecordMeaning.visibility = View.VISIBLE
                btnPlayMeaning.visibility = View.GONE
            }
            RecordingState.RECORDED -> {
                btnRecordMeaning.setImageResource(R.drawable.ic_mic)
                btnRecordMeaning.visibility = View.VISIBLE
                btnPlayMeaning.setImageResource(R.drawable.ic_play)
                btnPlayMeaning.visibility = View.VISIBLE
            }
            RecordingState.PLAYING -> {
                btnRecordMeaning.setImageResource(R.drawable.ic_mic)
                btnRecordMeaning.visibility = View.VISIBLE
                btnPlayMeaning.setImageResource(R.drawable.ic_stop)
                btnPlayMeaning.visibility = View.VISIBLE
            }
        }
    }

    private fun updateButtonsForExample(state: RecordingState) {
        when (state) {
            RecordingState.IDLE -> {
                btnRecordExample.setImageResource(R.drawable.ic_mic)
                btnRecordExample.visibility = View.VISIBLE
                btnPlayExample.visibility = View.GONE
            }
            RecordingState.RECORDING -> {
                btnRecordExample.setImageResource(R.drawable.ic_stop)
                btnRecordExample.visibility = View.VISIBLE
                btnPlayExample.visibility = View.GONE
            }
            RecordingState.RECORDED -> {
                btnRecordExample.setImageResource(R.drawable.ic_mic)
                btnRecordExample.visibility = View.VISIBLE
                btnPlayExample.setImageResource(R.drawable.ic_play)
                btnPlayExample.visibility = View.VISIBLE
            }
            RecordingState.PLAYING -> {
                btnRecordExample.setImageResource(R.drawable.ic_mic)
                btnRecordExample.visibility = View.VISIBLE
                btnPlayExample.setImageResource(R.drawable.ic_stop)
                btnPlayExample.visibility = View.VISIBLE
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
        
        // 인식된 텍스트 표시
        txtRecognized.text = if (recognizedText.isNotEmpty()) recognizedText else "인식된 텍스트가 없습니다"
        
        // 결과 상태 표시
        if (isCorrect) {
            txtResultStatus.text = "정확합니다! ✓"
            txtResultStatus.setTextColor(0xFF4CAF50.toInt()) // Green
            try {
                iconResult.setImageResource(R.drawable.ic_checkmark_green)
            } catch (e: Exception) {
                iconResult.setImageResource(android.R.drawable.checkbox_on_background)
            }
            txtTargetWordLabel.visibility = View.GONE
            txtTargetWord.visibility = View.GONE
        } else {
            txtResultStatus.text = "틀렸습니다"
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

