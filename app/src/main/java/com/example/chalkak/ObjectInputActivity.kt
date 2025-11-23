package com.example.chalkak

import DetectionResultItem
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class ObjectInputActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var edtObjectName: EditText
    private lateinit var btnConfirm: TextView
    private lateinit var btnBack: ImageButton

    private var imagePath: String? = null
    private var mainNavTag: String = "home"
    private lateinit var bottomNavigationHelper: BottomNavigationHelper

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

        // Setup bottom navigation using helper
        bottomNavigationHelper = BottomNavigationHelper(this, BottomNavigationHelper.createDefaultItems())
        bottomNavigationHelper.setupBottomNavigation()
        bottomNavigationHelper.updateNavigationHighlightAlpha(mainNavTag)

        // Apply WindowInsets using helper
        val root = findViewById<View>(R.id.input_root)
        val bottomNavContainer = findViewById<View>(R.id.bottom_nav_container)
        WindowInsetsHelper.applyToActivity(
            rootView = root,
            bottomNavContainer = bottomNavContainer,
            resources = resources
        )
    }

    private fun handleConfirm() {
        val objectName = edtObjectName.text.toString().trim()
        if (objectName.isBlank()) {
            Toast.makeText(this, "Please enter the object name.", Toast.LENGTH_SHORT).show()
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

}

