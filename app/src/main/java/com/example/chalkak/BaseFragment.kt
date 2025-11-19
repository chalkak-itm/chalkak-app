package com.example.chalkak

import android.Manifest
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * Base Fragment class that provides common functionality for TTS and Speech Recognition
 * Reduces code duplication across fragments that use word detail cards
 */
abstract class BaseFragment : Fragment() {
    // TTS helper
    protected var ttsHelper: TtsHelper? = null
    
    // Speech recognition manager
    protected var speechRecognitionManager: SpeechRecognitionManager? = null
    
    // Permission launcher
    protected val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Recording permission is required.", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Get the card word detail view for TTS and Speech Recognition initialization
     * Each fragment must provide its own implementation
     */
    protected abstract fun getCardWordDetailView(): View
    
    /**
     * Initialize TTS and Speech Recognition helpers
     * Should be called in onViewCreated after views are initialized
     */
    protected fun initializeTtsAndSpeechRecognition() {
        val cardWordDetail = getCardWordDetailView()
        
        // Initialize TTS helper
        ttsHelper = TtsHelper(requireContext(), cardWordDetail)
        
        // Initialize speech recognition manager
        speechRecognitionManager = SpeechRecognitionManager(
            context = requireContext(),
            cardWordDetail = cardWordDetail,
            requestPermissionLauncher = requestPermissionLauncher
        )
    }
    
    /**
     * Update target word for speech recognition
     */
    protected fun updateTargetWord(word: String) {
        speechRecognitionManager?.updateTargetWord(word)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup resources
        ttsHelper?.cleanup()
        speechRecognitionManager?.cleanup()
        ttsHelper = null
        speechRecognitionManager = null
    }
}

