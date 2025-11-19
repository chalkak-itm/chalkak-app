package com.example.chalkak

import DetectionResultItem
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import android.widget.FrameLayout.LayoutParams as FLP

class DetectionResultFragment : Fragment() {

    private lateinit var imgResult: ImageView
    private lateinit var boxOverlay: FrameLayout

    private lateinit var scrollObjectButtons: HorizontalScrollView
    private lateinit var layoutObjectButtons: LinearLayout

    private lateinit var cardWordDetail: LinearLayout
    private lateinit var txtSelectedObject: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView

    private lateinit var btnBack: ImageButton

    // TTS + Speaker buttons
    private lateinit var btnTtsWord: ImageView
    private lateinit var btnTtsExample: ImageView
    private lateinit var tts: android.speech.tts.TextToSpeech

    private var detectionResults: List<DetectionResultItem> = emptyList()
    private var mainNavTag: String = "home"
    private val wordButtons = mutableMapOf<String, TextView>() // Map to store buttons by label
    private var selectedButton: TextView? = null // Currently selected button

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_DETECTION_RESULTS = "detection_results"
        private const val ARG_MAIN_NAV_TAG = "main_nav_tag"

        fun newInstance(
            imagePath: String?,
            detectionResults: List<DetectionResultItem>,
            mainNavTag: String = "home"
        ): DetectionResultFragment {
            return DetectionResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_PATH, imagePath)
                    putParcelableArrayList(ARG_DETECTION_RESULTS, ArrayList(detectionResults))
                    putString(ARG_MAIN_NAV_TAG, mainNavTag)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detection_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View binding
        imgResult = view.findViewById(R.id.img_result)
        boxOverlay = view.findViewById(R.id.box_overlay_container)

        scrollObjectButtons = view.findViewById(R.id.scroll_object_buttons)
        layoutObjectButtons = view.findViewById(R.id.layout_object_buttons)

        cardWordDetail = view.findViewById(R.id.card_word_detail)
        txtSelectedObject = view.findViewById(R.id.txt_selected_object)
        txtKoreanMeaning = view.findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = view.findViewById(R.id.txt_example_sentence)
        btnTtsWord = view.findViewById(R.id.btn_tts_word)
        btnTtsExample = view.findViewById(R.id.btn_tts_example)
        btnBack = view.findViewById(R.id.btn_back)

        // Get arguments
        arguments?.let {
            val imagePath = it.getString(ARG_IMAGE_PATH)
            detectionResults = it.getParcelableArrayList<DetectionResultItem>(ARG_DETECTION_RESULTS) ?: emptyList()
            mainNavTag = it.getString(ARG_MAIN_NAV_TAG, "home")

            // Image setting
            if (imagePath != null) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                imgResult.setImageBitmap(bitmap)
            }
        }

        // Initialization of TTS
        tts = android.speech.tts.TextToSpeech(requireContext()) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(java.util.Locale.US)
                tts.setSpeechRate(0.7f)
            }
        }

        // Initialize word buttons for all detected objects
        initializeWordButtons()

        // Addition the Box overlay
        boxOverlay.post {
            addBoxOverlays()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Reading word
        btnTtsWord.setOnClickListener {
            val text = txtSelectedObject.text.toString()
            if (text.isNotBlank()) {
                speak(text)
            }
        }

        // Reading Example text
        btnTtsExample.setOnClickListener {
            val text = txtExampleSentence.text.toString()
            if (text.isNotBlank()) {
                speak(text)
            }
        }
    }

    /**
     * Initialize word buttons for all detected objects
     */
    private fun initializeWordButtons() {
        layoutObjectButtons.removeAllViews()
        wordButtons.clear()
        selectedButton = null

        detectionResults.forEach { item ->
            val wordButton = TextView(requireContext()).apply {
                text = item.label
                setBackgroundResource(R.drawable.bg_button_purple_ripple)
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(28, 11, 28, 11)
                isClickable = true
                isFocusable = true
                alpha = 0.7f // Initial unselected state

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.rightMargin = 16
                layoutParams = lp

                setOnClickListener {
                    showWordDetail(item)
                }
            }

            wordButtons[item.label] = wordButton
            layoutObjectButtons.addView(wordButton)
        }

        // Show word buttons section
        scrollObjectButtons.visibility = View.VISIBLE
    }

    /**
     * Add a clickable transparent View to each box location in detectionResults.
     */
    private fun addBoxOverlays() {
        val width = boxOverlay.width.toFloat()
        val height = boxOverlay.height.toFloat()

        boxOverlay.removeAllViews()

        detectionResults.forEach { item ->
            val boxWidth = (item.right - item.left) * width
            val boxHeight = (item.bottom - item.top) * height
            val leftMargin = item.left * width
            val topMargin = item.top * height

            val boxView = View(requireContext()).apply {
                isClickable = true
                isFocusable = true
                setBackgroundColor(Color.TRANSPARENT)

                setOnClickListener {
                    showWordDetail(item)
                }
            }

            val params = FLP(boxWidth.toInt(), boxHeight.toInt())
            params.leftMargin = leftMargin.toInt()
            params.topMargin = topMargin.toInt()

            boxOverlay.addView(boxView, params)
        }
    }

    /**
     * Show word detail card
     */
    private fun showWordDetail(item: DetectionResultItem) {
        // Update button selection
        updateButtonSelection(item.label)
        
        cardWordDetail.visibility = View.VISIBLE

        txtSelectedObject.text = item.label

        // bring the example sentence and korean meaning by GPT
        txtKoreanMeaning.text = "한국어 뜻."
        txtExampleSentence.text = "It is a space for example sentence."
    }
    
    /**
     * Update button selection state
     */
    private fun updateButtonSelection(selectedLabel: String) {
        // Reset all buttons to unselected state
        wordButtons.values.forEach { btn ->
            btn.setBackgroundResource(R.drawable.bg_button_purple_ripple)
            btn.alpha = 0.7f // Slightly transparent for unselected
        }
        
        // Reset previous selected button
        selectedButton?.apply {
            setBackgroundResource(R.drawable.bg_button_purple_ripple)
            alpha = 0.7f
        }
        
        // Set new selected button
        val button = wordButtons[selectedLabel]
        if (button != null) {
            // Apply selected effect: darker background and full opacity
            button.setBackgroundResource(R.drawable.bg_button_purple_selected)
            button.alpha = 1.0f
            selectedButton = button
            
            // Scroll to selected button to make it visible
            scrollObjectButtons.post {
                val scrollX = button.left - scrollObjectButtons.width / 2 + button.width / 2
                scrollObjectButtons.smoothScrollTo(scrollX, 0)
            }
        }
    }

    private fun speak(text: String) {
        if (!::tts.isInitialized) return

        tts.stop()
        tts.speak(
            text,
            android.speech.tts.TextToSpeech.QUEUE_FLUSH,
            null,
            "chalkak_tts"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}

