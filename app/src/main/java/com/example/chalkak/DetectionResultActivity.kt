
package com.example.chalkak

import DetectionResultItem
import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
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

        // Setup bottom navigation
        setupBottomNavigation()
        updateBottomNavigationHighlight(mainNavTag)

        // Apply WindowInsets to root layout
        val root = findViewById<View>(R.id.result_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Apply WindowInsets to bottom navigation bar container
        val bottomNavContainer = findViewById<View>(R.id.bottom_nav_container)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }


        // Initialize speech recognition manager
        speechRecognitionManager = SpeechRecognitionManager(
            context = this,
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


    private fun setupBottomNavigation() {
        findViewById<View>(R.id.nav_home)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fragment_tag", "home")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.nav_log)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fragment_tag", "log")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.nav_quiz)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fragment_tag", "quiz")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.nav_setting)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fragment_tag", "setting")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }
    
    private fun updateBottomNavigationHighlight(tag: String) {
        // Icons remain in original color, use alpha to show selection state
        findViewById<ImageView>(R.id.nav_home_icon)?.alpha = if (tag == "home") 1.0f else 0.5f
        findViewById<ImageView>(R.id.nav_log_icon)?.alpha = if (tag == "log") 1.0f else 0.5f
        findViewById<ImageView>(R.id.nav_quiz_icon)?.alpha = if (tag == "quiz") 1.0f else 0.5f
        findViewById<ImageView>(R.id.nav_setting_icon)?.alpha = if (tag == "setting") 1.0f else 0.5f
    }

    override fun onDestroy() {
        ttsHelper?.cleanup()
        speechRecognitionManager?.cleanup()
        super.onDestroy()
    }

}
