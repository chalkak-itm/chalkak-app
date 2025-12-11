package com.example.chalkak.ui.dialog

import android.Manifest
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.example.chalkak.R
import com.example.chalkak.domain.speech.SpeechRecognitionManager
import com.example.chalkak.domain.speech.TtsHelper
import com.example.chalkak.ui.fragment.LogEntry
import com.example.chalkak.util.ImageLoaderHelper
import com.example.chalkak.util.WordDataLoaderHelper

class LogItemDetailDialogFragment : DialogFragment() {
    
    private lateinit var imgSelectedPhoto: ImageView
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView
    
    private var entry: LogEntry? = null
    private var onDialogDismissed: (() -> Unit)? = null
    private lateinit var wordDataLoader: WordDataLoaderHelper
    
    // TTS and Speech Recognition helpers
    private var ttsHelper: TtsHelper? = null
    private var speechRecognitionManager: SpeechRecognitionManager? = null
    
    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Recording permission is required.", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        private const val ARG_ENTRY = "entry"
        
        fun newInstance(entry: LogEntry): LogItemDetailDialogFragment {
            val fragment = LogItemDetailDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_ENTRY, entry)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            entry = it.getSerializable(ARG_ENTRY) as? LogEntry
        }
        // Set popup style
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            // Cover the full screen
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            window.setFlags(
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            // Make background translucent
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        // Allow dialog cancellation (also via back button)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_log_item_detail, container, false)
        
        val scrollView = view.findViewById<android.widget.ScrollView>(R.id.scroll_view)
        val contentView = view.findViewById<ViewGroup>(R.id.dialog_content)
        
        // Add touch listener to close if touch is outside the content area
        view.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val touchX = event.x
                val touchY = event.y
                
                // Check ScrollView bounds
                scrollView?.let { sv ->
                    val scrollViewRect = android.graphics.Rect()
                    sv.getHitRect(scrollViewRect)
                    
                    // Close if touch is outside ScrollView
                    if (!scrollViewRect.contains(touchX.toInt(), touchY.toInt())) {
                        // Touch is outside content area, close popup
                        dismiss()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
        
        // Detect touches on empty space in ScrollView; close when outside content area
        scrollView?.setOnTouchListener { sv, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val touchX = event.x
                val touchY = event.y
                
                // Check content bounds (ScrollView coordinates)
                contentView?.let { content ->
                    val contentLeft = content.left.toFloat()
                    val contentTop = content.top.toFloat()
                    val contentRight = content.right.toFloat()
                    val contentBottom = content.bottom.toFloat()
                    
                    // Close if touch is outside content area
                    if (touchX < contentLeft || touchX > contentRight || 
                        touchY < contentTop || touchY > contentBottom) {
                        // Touch is outside content, close popup
                        dismiss()
                        return@setOnTouchListener true
                    }
                }
            }
            // Let ScrollView handle touches inside content (scrolling, etc.)
            false
        }
        
        // Content area should not close the dialog on touch
        contentView?.setOnClickListener {
            // Consume clicks to prevent propagation
        }
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        imgSelectedPhoto = view.findViewById(R.id.img_selected_photo)
        txtSelectedWord = view.findViewById(R.id.txt_selected_word)
        txtKoreanMeaning = view.findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = view.findViewById(R.id.txt_example_sentence)
        
        // Initialize WordDataLoaderHelper
        wordDataLoader = WordDataLoaderHelper(requireContext(), viewLifecycleOwner)
        
        entry?.let { logEntry ->
            // Load image from path or use resource
            if (logEntry.imagePath != null) {
                ImageLoaderHelper.loadImageToView(imgSelectedPhoto, logEntry.imagePath)
            } else if (logEntry.imageRes != null) {
                imgSelectedPhoto.setImageResource(logEntry.imageRes)
            }
            
            txtSelectedWord.text = logEntry.word
            
            // Load word data using helper
            loadWordData(logEntry)
        }
        
        // Initialize TTS and Speech Recognition
        initializeTtsAndSpeechRecognition(view)
    }
    
    private fun initializeTtsAndSpeechRecognition(rootView: View) {
        // Find the card_word_detail_dialog view using the include tag ID
        val cardWordDetail = rootView.findViewById<View>(R.id.card_word_detail_dialog)
            ?: rootView.findViewById<ViewGroup>(R.id.dialog_content)?.getChildAt(1)
        
        cardWordDetail?.let { cardView ->
            // Initialize TTS helper
            ttsHelper = TtsHelper(requireContext(), cardView)
            
            // Initialize speech recognition manager
            speechRecognitionManager = SpeechRecognitionManager(
                context = requireContext(),
                cardWordDetail = cardView,
                requestPermissionLauncher = requestPermissionLauncher
            )
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            // Cover the entire screen (including dim area)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Make background translucent
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        
        // Limit ScrollView height to 90% of screen to keep it centered
        view?.let { rootView ->
            val scrollView = rootView.findViewById<android.widget.ScrollView>(R.id.scroll_view)
            scrollView?.let { sv ->
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val maxHeight = (screenHeight * 0.9).toInt()
                
                // Get LayoutParams and apply height cap
                val layoutParams = sv.layoutParams as? android.widget.FrameLayout.LayoutParams
                    ?: android.widget.FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                
                // Enforce max height after measurement even though it uses wrap_content
                sv.post {
                    val measuredHeight = sv.measuredHeight
                    if (measuredHeight > maxHeight) {
                        layoutParams.height = maxHeight
                        sv.layoutParams = layoutParams
                    }
                }
            }
        }
    }
    
    fun setOnDialogDismissedListener(listener: () -> Unit) {
        onDialogDismissed = listener
    }
    
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup TTS and Speech Recognition resources
        ttsHelper?.cleanup()
        speechRecognitionManager?.cleanup()
        ttsHelper = null
        speechRecognitionManager = null
    }

    private fun loadWordData(logEntry: LogEntry) {
        wordDataLoader.loadWordData(
            word = logEntry.word,
            objectId = logEntry.objectId,
            callback = object : WordDataLoaderHelper.WordDataCallback {
                override fun onLoading() {
                    txtKoreanMeaning.text = logEntry.koreanMeaning ?: getString(R.string.loading_meaning)
                    txtExampleSentence.text = getString(R.string.loading_example_sentences)
                }

                override fun onSuccess(data: WordDataLoaderHelper.WordData) {
                    txtKoreanMeaning.text = data.koreanMeaning
                    if (data.exampleSentence.isNotEmpty() && data.exampleTranslation.isNotEmpty()) {
                        txtExampleSentence.text = "${data.exampleSentence}\n(${data.exampleTranslation})"
                    } else if (data.exampleSentence.isNotEmpty()) {
                        txtExampleSentence.text = data.exampleSentence
                    } else {
                        txtExampleSentence.text = getString(R.string.no_example_sentences)
                    }
                }

                override fun onError(message: String) {
                    txtKoreanMeaning.text = getString(R.string.failed_to_load_meaning)
                    txtExampleSentence.text = message
                }
            }
        )
    }
}
