package com.example.chalkak.domain.speech

import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.chalkak.R

enum class RecordingState {
    IDLE,        // Initial state (mic button)
    LISTENING    // STT recognition in progress (stop button)
}

class SpeechRecognitionHelper(
    private val context: Context,
    private val onStateChanged: (RecordingState) -> Unit,
    private val onSttResult: (String, Boolean) -> Unit // (recognized text, accuracy result)
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
            // If already recognizing, stop
            stopSpeechRecognition()
            return
        }
        if (targetWord.isEmpty()) {
            Toast.makeText(context, "Target word is not set.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        // Change state
        currentState = RecordingState.LISTENING
        onStateChanged(currentState)

        // Show loading dialog
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
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Unknown error"
                    }
                    
                    // Error logging (for debugging)
                    android.util.Log.d("SpeechRecognition", "STT Error: $errorMessage (code: $error)")
                    
                    // ERROR_NO_MATCH or ERROR_SPEECH_TIMEOUT can be normal cases, so don't show toast
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(context, "Speech recognition failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                    // Change state
                    currentState = RecordingState.IDLE
                    onStateChanged(currentState)
                    
                    // Call callback even if no recognition result
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
                    
                    // Debug logging
                    android.util.Log.d("SpeechRecognition", "STT Results: $matches")
                    android.util.Log.d("SpeechRecognition", "Target Word: $targetWord")
                    
                    if (matches == null || matches.isEmpty()) {
                        android.util.Log.w("SpeechRecognition", "No recognition results")
                        onSttResult("", false)
                        releaseRecognizer()
                        return
                    }
                    
                    // Select the most similar result among multiple candidates to the target word
                    val bestMatch = findBestMatch(matches, targetWord)
                    val recognizedText = bestMatch.lowercase().trim()
                    
                    android.util.Log.d("SpeechRecognition", "Best Match: $recognizedText")
                    
                    // Change state
                    currentState = RecordingState.IDLE
                    onStateChanged(currentState)
                    
                    // Close loading dialog
                    dismissLoadingDialog()
                    
                    if (recognizedText.isNotEmpty()) {
                        // Treat as correct if exactly matches or has high similarity
                        val isCorrect = isSimilar(recognizedText, targetWord)
                        android.util.Log.d("SpeechRecognition", "Is Correct: $isCorrect")
                        onSttResult(recognizedText, isCorrect)
                    } else {
                        // Call callback even if no recognized text
                        android.util.Log.w("SpeechRecognition", "Empty recognized text")
                        onSttResult("", false)
                    }
                    releaseRecognizer()
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    // Check partial results and use if better result is available
                    val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (partialMatches != null && partialMatches.isNotEmpty()) {
                        // Only log partial results, final results are processed in onResults
                    }
                }
                
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.US.toString()) // Explicit "en-US" setting
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10) // Get more candidate results
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Also receive partial results
            // Additional settings to improve recognition accuracy
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) // Use online recognition (more accurate)
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
     * Shows the loading dialog.
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
     * Closes the loading dialog.
     */
    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    /**
     * Finds the most similar result among multiple recognition candidates to the target word.
     */
    private fun findBestMatch(matches: java.util.ArrayList<String>?, targetWord: String): String {
        if (matches == null || matches.isEmpty()) return ""
        
        // Filter out empty strings from matches
        val validMatches = matches.filter { it.trim().isNotEmpty() }
        if (validMatches.isEmpty()) return ""
        
        // Return exact match first if exists
        validMatches.forEach { match ->
            if (match.lowercase().trim() == targetWord.lowercase().trim()) {
                return match
            }
        }
        
        // Find the one with highest similarity
        var bestMatch = validMatches.first()
        var bestSimilarity = calculateSimilarity(bestMatch.lowercase().trim(), targetWord.lowercase().trim())
        
        validMatches.forEach { match ->
            val similarity = calculateSimilarity(match.lowercase().trim(), targetWord.lowercase().trim())
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestMatch = match
            }
        }
        
        return bestMatch
    }

    /**
     * Determines if two words are similar (returns true if exactly matches or has high similarity).
     */
    private fun isSimilar(text1: String, text2: String): Boolean {
        // If either string is empty, they cannot be similar (except if both are empty, which is handled below)
        if (text1.isEmpty() || text2.isEmpty()) {
            // Only return true if both are empty (exact match case)
            return text1 == text2
        }
        
        // Exact match
        if (text1 == text2) return true
        
        // Check containment relationship (e.g., "apple" and "an apple")
        // Note: We already checked for empty strings above, so this is safe
        if (text1.contains(text2) || text2.contains(text1)) return true
        
        // Calculate similarity (based on Levenshtein distance)
        val similarity = calculateSimilarity(text1, text2)
        // Treat as correct if similarity is 80% or higher
        return similarity >= 0.8
    }

    /**
     * Calculates the similarity between two strings (0.0 ~ 1.0).
     * Uses Levenshtein distance.
     */
    private fun calculateSimilarity(str1: String, str2: String): Double {
        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0
        
        val maxLength = maxOf(str1.length, str2.length)
        val distance = levenshteinDistance(str1, str2)
        
        return 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Calculates Levenshtein distance.
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
