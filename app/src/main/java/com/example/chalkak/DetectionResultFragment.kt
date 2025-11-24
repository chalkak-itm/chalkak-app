package com.example.chalkak

import DetectionResultItem
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.FrameLayout.LayoutParams as FLP

class DetectionResultFragment : BaseFragment() {

    private lateinit var imgResult: ImageView
    private lateinit var boxOverlay: FrameLayout

    private lateinit var scrollObjectButtons: HorizontalScrollView
    private lateinit var layoutObjectButtons: LinearLayout

    private lateinit var cardWordDetail: LinearLayout
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView

    private lateinit var btnBack: ImageButton

    private var detectionResults: List<DetectionResultItem> = emptyList()
    private var mainNavTag: String = "home"
    private val wordButtons = mutableMapOf<String, TextView>() // Map to store buttons by label
    private var selectedButton: TextView? = null // Currently selected button
    private val roomDb by lazy { AppDatabase.getInstance(requireContext()) }
    private lateinit var wordDataLoader: WordDataLoaderHelper

    override fun getCardWordDetailView(): View {
        return cardWordDetail
    }

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
        txtSelectedWord = view.findViewById(R.id.txt_selected_word)
        txtKoreanMeaning = view.findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = view.findViewById(R.id.txt_example_sentence)
        btnBack = view.findViewById(R.id.btn_back)

        // Initialize WordDataLoaderHelper
        wordDataLoader = WordDataLoaderHelper(requireContext(), viewLifecycleOwner)

        // Get arguments
        arguments?.let {
            val imagePath = it.getString(ARG_IMAGE_PATH)
            detectionResults = it.getParcelableArrayList<DetectionResultItem>(ARG_DETECTION_RESULTS) ?: emptyList()
            mainNavTag = it.getString(ARG_MAIN_NAV_TAG, "home")

            // Image setting using ImageLoaderHelper
            ImageLoaderHelper.loadImageToView(imgResult, imagePath)
        }

        // Initialize TTS and Speech Recognition (from BaseFragment)
        initializeTtsAndSpeechRecognition()

        // Initialize word buttons for all detected objects
        initializeWordButtons()

        // Addition the Box overlay
        boxOverlay.post {
            addBoxOverlays()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Restart Magic Adventure button setup
        view.findViewById<LinearLayout>(R.id.btn_restart_magic_adventure)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToFragment(
                MagicAdventureFragment(),
                "magic_adventure"
            )
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
        boxOverlay.removeAllViews()

        // Create a single clickable overlay that handles all touches
        val overlayView = View(requireContext()).apply {
            isClickable = true
            isFocusable = true
            setBackgroundColor(Color.TRANSPARENT)

            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    val touchX = event.x
                    val touchY = event.y

                    // Calculate the actual image display area considering scaleType="fitCenter"
                    val imageView = imgResult
                    val drawable = imageView.drawable
                    
                    if (drawable != null) {
                        val imageWidth = drawable.intrinsicWidth.toFloat()
                        val imageHeight = drawable.intrinsicHeight.toFloat()
                        
                        // If intrinsic dimensions are not available, try to get from bitmap
                        val actualImageWidth = if (imageWidth > 0 && imageHeight > 0) {
                            imageWidth
                        } else {
                            // Fallback: try to get from bitmap if it's a BitmapDrawable
                            (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.width?.toFloat() ?: return@setOnTouchListener false
                        }
                        val actualImageHeight = if (imageWidth > 0 && imageHeight > 0) {
                            imageHeight
                        } else {
                            (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.height?.toFloat() ?: return@setOnTouchListener false
                        }
                        
                        val viewWidth = imageView.width.toFloat()
                        val viewHeight = imageView.height.toFloat()
                        
                        // Calculate scale factor to maintain aspect ratio (fitCenter)
                        val scale = minOf(viewWidth / actualImageWidth, viewHeight / actualImageHeight)
                        val scaledImageWidth = actualImageWidth * scale
                        val scaledImageHeight = actualImageHeight * scale
                        
                        // Calculate offset (centered)
                        val offsetX = (viewWidth - scaledImageWidth) / 2f
                        val offsetY = (viewHeight - scaledImageHeight) / 2f
                        
                        // Convert touch coordinates to image coordinates (0.0-1.0)
                        // Clamp to valid range [0.0, 1.0]
                        val imageX = ((touchX - offsetX) / scaledImageWidth).coerceIn(0f, 1f)
                        val imageY = ((touchY - offsetY) / scaledImageHeight).coerceIn(0f, 1f)
                        
                        // Find which box contains the touch point
                        // Check in reverse order to prioritize boxes added later (usually smaller or more specific)
                        val clickedItem = detectionResults.reversed().firstOrNull { item ->
                            // item.left, top, right, bottom are normalized coordinates (0.0-1.0)
                            imageX >= item.left && imageX <= item.right &&
                            imageY >= item.top && imageY <= item.bottom
                        }

                        clickedItem?.let { item ->
                            showWordDetail(item)
                            true
                        } ?: false
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }

        val params = FLP(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        boxOverlay.addView(overlayView, params)
    }

    /**
     * Show word detail card
     */
    private fun showWordDetail(item: DetectionResultItem) {
        // Update button selection
        updateButtonSelection(item.label)
        
        cardWordDetail.visibility = View.VISIBLE

        txtSelectedWord.text = item.label

        // Load word data from local database
        loadWordDataFromLocal(item.label)

        // Update speech recognition manager with new word
        updateTargetWord(item.label)
    }

    /**
     * Load word data (meaning and example) from local Room database
     */
    private fun loadWordDataFromLocal(word: String) {
        wordDataLoader.loadWordData(
            word = word,
            objectId = null,
            callback = object : WordDataLoaderHelper.WordDataCallback {
                override fun onLoading() {
                    txtKoreanMeaning.text = getString(R.string.loading_meaning)
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
}

