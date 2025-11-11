package com.example.chalkak

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MagicAdventureActivity : AppCompatActivity() {
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, ImagePreviewActivity::class.java).apply {
                putExtra("image_uri", it)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_magic_adventure)

        val takePhoto: LinearLayout = findViewById(R.id.btn_take_photo)
        val upload: LinearLayout = findViewById(R.id.btn_upload)
        val backButton: ImageButton = findViewById(R.id.btn_back)
        val navHome: TextView = findViewById(R.id.nav_home)
        val navLog: TextView = findViewById(R.id.nav_log)
        val navQuiz: TextView = findViewById(R.id.nav_quiz)

        takePhoto.setOnClickListener {
            startActivity(Intent(this, ImagePreviewActivity::class.java))
        }
        upload.setOnClickListener {
            pickImage.launch("image/*")
        }

        backButton.setOnClickListener { finish() }
        navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        navLog.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
        navQuiz.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        val root = findViewById<android.view.View>(R.id.magic_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}


