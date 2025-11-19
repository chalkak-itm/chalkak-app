package com.example.chalkak

import DetectionResultItem
import android.Manifest
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.FrameLayout.LayoutParams as FLP

class DetectionResultFragment : Fragment() {

    private lateinit var imgResult: ImageView
    private lateinit var boxOverlay: FrameLayout

    private lateinit var scrollObjectButtons: HorizontalScrollView
    private lateinit var layoutObjectButtons: LinearLayout

    private lateinit var cardWordDetail: LinearLayout
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView

    private lateinit var btnBack: ImageButton

    // TTS helper
    private var ttsHelper: TtsHelper? = null

    // Speech recognition manager
    private var speechRecognitionManager: SpeechRecognitionManager? = null

    private var detectionResults: List<DetectionResultItem> = emptyList()
    private var mainNavTag: String = "home"
    private val wordButtons = mutableMapOf<String, TextView>() // Map to store buttons by label
    private var selectedButton: TextView? = null // Currently selected button

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
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

        // Initialize TTS helper
        ttsHelper = TtsHelper(requireContext(), cardWordDetail)

        // Initialize word buttons for all detected objects
        initializeWordButtons()

        // Addition the Box overlay
        boxOverlay.post {
            addBoxOverlays()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Initialize speech recognition manager
        speechRecognitionManager = SpeechRecognitionManager(
            context = requireContext(),
            cardWordDetail = cardWordDetail,
            requestPermissionLauncher = requestPermissionLauncher
            // 기본 다이얼로그가 표시됩니다
        )
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

        // bring the example sentence and korean meaning by GPT
        txtKoreanMeaning.text = "한국어 뜻."
        txtExampleSentence.text = "It is a space for example sentence."

        // Update speech recognition manager with new word
        speechRecognitionManager?.updateTargetWord(item.label)
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

    override fun onDestroyView() {
        super.onDestroyView()
        ttsHelper?.cleanup()
        speechRecognitionManager?.cleanup()
    }
}

