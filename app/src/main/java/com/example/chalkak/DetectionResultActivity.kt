package com.example.chalkak

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import android.widget.FrameLayout.LayoutParams as FLP

class DetectionResultActivity : AppCompatActivity() {

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
    private lateinit var bottomNavigationHelper: BottomNavigationHelper

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Recording permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_detection_result)

        // view binding
        imgResult = findViewById(R.id.img_result)
        boxOverlay = findViewById(R.id.box_overlay_container)

        scrollObjectButtons = findViewById(R.id.scroll_object_buttons)
        layoutObjectButtons = findViewById(R.id.layout_object_buttons)

        cardWordDetail = findViewById(R.id.card_word_detail)
        txtSelectedWord = findViewById(R.id.txt_selected_word)
        txtKoreanMeaning = findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = findViewById(R.id.txt_example_sentence)
        btnBack = findViewById(R.id.btn_back)

        // Initialize TTS helper
        ttsHelper = TtsHelper(this, cardWordDetail)

        // Take Data from the Intent
        val imagePath = intent.getStringExtra("image_path")
        detectionResults =
            intent.getParcelableArrayListExtra<DetectionResultItem>("detection_results") ?: emptyList()
        mainNavTag = intent.getStringExtra("main_nav_tag") ?: "home"

        // Image setting
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imgResult.setImageBitmap(bitmap)
        }

        // Initialize word buttons for all detected objects
        initializeWordButtons()

        // Addition the Box overlay
        boxOverlay.post {
            addBoxOverlays()
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Restart Magic Adventure button setup
        findViewById<LinearLayout>(R.id.btn_restart_magic_adventure)?.setOnClickListener {
            NavigationHelper.navigateToMainActivity(this, "magic_adventure")
        }

        // Setup bottom navigation using helper
        bottomNavigationHelper = BottomNavigationHelper(this, BottomNavigationHelper.createDefaultItems())
        bottomNavigationHelper.setupBottomNavigation()
        bottomNavigationHelper.updateNavigationHighlightAlpha(mainNavTag)

        // Apply WindowInsets using helper
        val root = findViewById<View>(R.id.result_root)
        val bottomNavContainer = findViewById<View>(R.id.bottom_nav_container)
        WindowInsetsHelper.applyToActivity(
            rootView = root,
            bottomNavContainer = bottomNavContainer,
            resources = resources
        )


        // Initialize speech recognition manager
        speechRecognitionManager = SpeechRecognitionManager(
            context = this,
            cardWordDetail = cardWordDetail,
            requestPermissionLauncher = requestPermissionLauncher
            // Default dialog will be displayed
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
            val wordButton = TextView(this).apply {
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
        val overlayView = View(this).apply {
            isClickable = true
            isFocusable = true
            setBackgroundColor(Color.TRANSPARENT)

            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    val touchX = event.x
                    val touchY = event.y

                    // Use BoundingBoxHelper to find clicked item
                    val clickedItem = BoundingBoxHelper.findItemAtTouch(
                        touchX = touchX,
                        touchY = touchY,
                        imageView = imgResult,
                        detectionResults = detectionResults,
                        reverseOrder = true
                    )

                    if (clickedItem != null) {
                        showWordDetail(clickedItem)
                        true
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
        txtKoreanMeaning.text = getString(R.string.meaning)
        txtExampleSentence.text = getString(R.string.example_sentence_placeholder)

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



    override fun onDestroy() {
        ttsHelper?.cleanup()
        speechRecognitionManager?.cleanup()
        super.onDestroy()
    }

}
