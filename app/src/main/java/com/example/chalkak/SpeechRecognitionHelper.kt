package com.example.chalkak

import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

enum class RecordingState {
    IDLE,        // 초기 상태 (마이크 버튼)
    LISTENING    // STT 인식 중 (중지 버튼)
}

class SpeechRecognitionHelper(
    private val context: Context,
    private val onStateChanged: (RecordingState) -> Unit,
    private val onSttResult: (String, Boolean) -> Unit // (인식된 텍스트, 정확도 결과)
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentState = RecordingState.IDLE
    private var targetWord: String = ""
    private var loadingDialog: AlertDialog? = null

    fun setTargetWord(word: String) {
        targetWord = word.lowercase().trim()
    }

    fun startSpeechRecognition() {
        if (currentState == RecordingState.LISTENING) {
            // 이미 인식 중이면 중지
            stopSpeechRecognition()
            return
        }
        if (targetWord.isEmpty()) {
            Toast.makeText(context, "대상 단어가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "음성 인식 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 상태 변경
        currentState = RecordingState.LISTENING
        onStateChanged(currentState)

        // 로딩 다이얼로그 표시
        showLoadingDialog()

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
                    
                    // 에러 로깅 (디버깅용)
                    android.util.Log.d("SpeechRecognition", "STT Error: $errorMessage (code: $error)")
                    
                    // ERROR_NO_MATCH나 ERROR_SPEECH_TIMEOUT은 정상적인 경우일 수 있으므로 토스트 표시 안 함
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(context, "음성 인식 실패: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                    // 상태 변경
                    currentState = RecordingState.IDLE
                    onStateChanged(currentState)
                    
                    // 인식 결과가 없어도 콜백 호출
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        dismissLoadingDialog()
                        onSttResult("", false)
                    } else {
                        dismissLoadingDialog()
                    }
                    releaseRecognizer()
                }

                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    
                    // 디버깅용 로그
                    android.util.Log.d("SpeechRecognition", "STT Results: $matches")
                    android.util.Log.d("SpeechRecognition", "Target Word: $targetWord")
                    
                    if (matches == null || matches.isEmpty()) {
                        android.util.Log.w("SpeechRecognition", "No recognition results")
                        onSttResult("", false)
                        releaseRecognizer()
                        return
                    }
                    
                    // 여러 후보 결과 중에서 타겟 단어와 가장 유사한 것을 선택
                    val bestMatch = findBestMatch(matches, targetWord)
                    val recognizedText = bestMatch.lowercase().trim()
                    
                    android.util.Log.d("SpeechRecognition", "Best Match: $recognizedText")
                    
                    // 상태 변경
                    currentState = RecordingState.IDLE
                    onStateChanged(currentState)
                    
                    // 로딩 다이얼로그 닫기
                    dismissLoadingDialog()
                    
                    if (recognizedText.isNotEmpty()) {
                        // 정확히 일치하거나 유사도가 높으면 정답으로 처리
                        val isCorrect = isSimilar(recognizedText, targetWord)
                        android.util.Log.d("SpeechRecognition", "Is Correct: $isCorrect")
                        onSttResult(recognizedText, isCorrect)
                    } else {
                        // 인식된 텍스트가 없을 때도 콜백 호출
                        android.util.Log.w("SpeechRecognition", "Empty recognized text")
                        onSttResult("", false)
                    }
                    releaseRecognizer()
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    // 부분 결과도 확인하여 더 나은 결과가 있으면 사용
                    val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (partialMatches != null && partialMatches.isNotEmpty()) {
                        // 부분 결과는 로깅만 하고 최종 결과는 onResults에서 처리
                    }
                }
                
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.US.toString()) // "en-US" 명시적 설정
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10) // 더 많은 후보 결과 받기
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // 부분 결과도 받기
            // 인식 정확도 향상을 위한 추가 설정
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) // 온라인 인식 사용 (더 정확)
        }

        speechRecognizer?.startListening(intent)
    }

    fun getCurrentState(): RecordingState = currentState

    fun stopSpeechRecognition() {
        if (currentState == RecordingState.LISTENING) {
            releaseRecognizer()
            dismissLoadingDialog()
            currentState = RecordingState.IDLE
            onStateChanged(currentState)
        }
    }

    fun reset() {
        releaseRecognizer()
        dismissLoadingDialog()
        currentState = RecordingState.IDLE
        onStateChanged(currentState)
    }

    fun cleanup() {
        reset()
    }

    /**
     * 로딩 다이얼로그를 표시합니다.
     */
    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_stt_loading, null)
        loadingDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    /**
     * 로딩 다이얼로그를 닫습니다.
     */
    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    /**
     * 여러 인식 후보 중에서 타겟 단어와 가장 유사한 것을 찾습니다.
     */
    private fun findBestMatch(matches: java.util.ArrayList<String>?, targetWord: String): String {
        if (matches == null || matches.isEmpty()) return ""
        
        // 정확히 일치하는 것이 있으면 우선 반환
        matches.forEach { match ->
            if (match.lowercase().trim() == targetWord.lowercase().trim()) {
                return match
            }
        }
        
        // 유사도가 가장 높은 것을 찾기
        var bestMatch = matches.first()
        var bestSimilarity = calculateSimilarity(bestMatch.lowercase().trim(), targetWord.lowercase().trim())
        
        matches.forEach { match ->
            val similarity = calculateSimilarity(match.lowercase().trim(), targetWord.lowercase().trim())
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestMatch = match
            }
        }
        
        return bestMatch
    }

    /**
     * 두 단어가 유사한지 판단합니다 (정확히 일치하거나 유사도가 높으면 true).
     */
    private fun isSimilar(text1: String, text2: String): Boolean {
        // 정확히 일치
        if (text1 == text2) return true
        
        // 포함 관계 확인 (예: "apple"과 "an apple")
        if (text1.contains(text2) || text2.contains(text1)) return true
        
        // 유사도 계산 (Levenshtein distance 기반)
        val similarity = calculateSimilarity(text1, text2)
        // 80% 이상 유사하면 정답으로 처리
        return similarity >= 0.8
    }

    /**
     * 두 문자열의 유사도를 계산합니다 (0.0 ~ 1.0).
     * Levenshtein distance를 사용합니다.
     */
    private fun calculateSimilarity(str1: String, str2: String): Double {
        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0
        
        val maxLength = maxOf(str1.length, str2.length)
        val distance = levenshteinDistance(str1, str2)
        
        return 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Levenshtein distance를 계산합니다.
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val m = str1.length
        val n = str2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[m][n]
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

