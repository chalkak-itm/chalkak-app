package com.example.chalkak

import DetectionResultItem
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class ObjectInputActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var edtObjectName: EditText
    private lateinit var btnConfirm: TextView
    private lateinit var btnBack: ImageButton

    private var imagePath: String? = null
    private var mainNavTag: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_object_input)

        // View binding
        imgPreview = findViewById(R.id.img_preview)
        edtObjectName = findViewById(R.id.edt_object_name)
        btnConfirm = findViewById(R.id.btn_confirm)
        btnBack = findViewById(R.id.btn_back)

        // Get data from intent
        imagePath = intent.getStringExtra("image_path")
        mainNavTag = intent.getStringExtra("main_nav_tag") ?: "home"

        // Set image
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imgPreview.setImageBitmap(bitmap)
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Confirm button
        btnConfirm.setOnClickListener {
            handleConfirm()
        }

        // Handle keyboard "Done" button
        edtObjectName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleConfirm()
                true
            } else {
                false
            }
        }

        // Show keyboard when activity appears
        edtObjectName.requestFocus()
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // Setup bottom navigation
        setupBottomNavigation()
        updateBottomNavigationHighlight(mainNavTag)

        // Apply WindowInsets to root layout
        val root = findViewById<View>(R.id.input_root)
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
    }

    private fun handleConfirm() {
        val objectName = edtObjectName.text.toString().trim()
        if (objectName.isBlank()) {
            Toast.makeText(this, "객체 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a DetectionResultItem with the user input
        // Since there's no bounding box, we'll use default values (full image)
        val userInputItem = DetectionResultItem(
            label = objectName,
            score = 1.0f, // User input is considered 100% confident
            left = 0.0f,
            top = 0.0f,
            right = 1.0f,
            bottom = 1.0f
        )

        // Navigate to DetectionResultActivity
        val intent = Intent(this, DetectionResultActivity::class.java).apply {
            putExtra("image_path", imagePath)
            putParcelableArrayListExtra("detection_results", arrayListOf(userInputItem))
            putExtra("main_nav_tag", mainNavTag)
        }
        startActivity(intent)
        finish()
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
}

