package com.example.chalkak

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.widget.Toast
import java.io.File
import java.io.IOException

enum class RecordingState {
    IDLE,        // 초기 상태 (녹음 버튼)
    RECORDING,   // 녹음 중
    RECORDED,    // 녹음 완료 (재생 버튼)
    PLAYING      // 재생 중 (정지 버튼)
}

class SpeechRecognitionHelper(
    private val context: Context,
    private val onStateChanged: (RecordingState) -> Unit,
    private val onSttResult: (String, Boolean) -> Unit // (인식된 텍스트, 정확도 결과)
) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioFile: File? = null
    private var currentState = RecordingState.IDLE
    private var targetWord: String = ""

    fun setTargetWord(word: String) {
        targetWord = word.lowercase().trim()
    }

    fun startRecording() {
        if (currentState == RecordingState.RECORDING) return

        try {
            // 임시 파일 생성
            audioFile = File.createTempFile("recording_", ".3gp", context.cacheDir)
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                
                try {
                    prepare()
                    start()
                    currentState = RecordingState.RECORDING
                    onStateChanged(currentState)
                    
                    // 녹음 시작과 동시에 STT도 시작
                    startSpeechRecognition()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, "녹음 시작 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    releaseRecorder()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "녹음 초기화 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        if (currentState != RecordingState.RECORDING) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // STT는 이미 녹음 중에 시작되었으므로, 여기서는 상태만 변경
            // STT 결과는 RecognitionListener의 onResults에서 처리됨
            currentState = RecordingState.RECORDED
            onStateChanged(currentState)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "녹음 중지 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            releaseRecorder()
            releaseRecognizer() // STT도 중지
            currentState = RecordingState.IDLE
            onStateChanged(currentState)
        }
    }

    fun startPlaying() {
        if (currentState == RecordingState.PLAYING || audioFile == null) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile?.absolutePath)
                prepare()
                setOnCompletionListener {
                    currentState = RecordingState.RECORDED
                    onStateChanged(currentState)
                    releasePlayer()
                }
                start()
            }
            currentState = RecordingState.PLAYING
            onStateChanged(currentState)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "재생 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            releasePlayer()
            currentState = RecordingState.RECORDED
            onStateChanged(currentState)
        }
    }

    fun stopPlaying() {
        if (currentState != RecordingState.PLAYING) return

        releasePlayer()
        currentState = RecordingState.RECORDED
        onStateChanged(currentState)
    }

    fun startSpeechRecognition() {
        if (targetWord.isEmpty()) {
            Toast.makeText(context, "대상 단어가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "음성 인식 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // Note: Android SpeechRecognizer works with real-time audio, not recorded files
        // After recording, we start real-time recognition for pronunciation check

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "오디오 오류"
                        SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
                        SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                        SpeechRecognizer.ERROR_NO_MATCH -> "인식 결과 없음"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기 사용 중"
                        SpeechRecognizer.ERROR_SERVER -> "서버 오류"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 타임아웃"
                        else -> "알 수 없는 오류"
                    }
                    // ERROR_NO_MATCH나 ERROR_SPEECH_TIMEOUT은 정상적인 경우일 수 있으므로 토스트 표시 안 함
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(context, "음성 인식 실패: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                    // 인식 결과가 없어도 콜백 호출
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        onSttResult("", false)
                    }
                    releaseRecognizer()
                }

                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.firstOrNull()?.lowercase()?.trim() ?: ""
                    
                    if (recognizedText.isNotEmpty()) {
                        val isCorrect = recognizedText == targetWord
                        onSttResult(recognizedText, isCorrect)
                    } else {
                        // 인식된 텍스트가 없을 때도 콜백 호출
                        onSttResult("", false)
                    }
                    releaseRecognizer()
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.US.toString()) // "en-US" 명시적 설정
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5) // 더 많은 결과 받기
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // 부분 결과도 받기
        }

        speechRecognizer?.startListening(intent)
    }

    fun getCurrentState(): RecordingState = currentState

    fun hasRecordedAudio(): Boolean = audioFile != null && audioFile?.exists() == true

    fun reset() {
        releaseRecorder()
        releasePlayer()
        releaseRecognizer()
        audioFile?.delete()
        audioFile = null
        currentState = RecordingState.IDLE
        onStateChanged(currentState)
    }

    fun cleanup() {
        reset()
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    private fun releaseRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        speechRecognizer = null
    }
}

