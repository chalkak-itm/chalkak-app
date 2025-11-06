package com.example.chalkak

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class CameraCaptureActivity : AppCompatActivity() {
    private var photoUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            // Preview captured image
            findViewById<android.widget.ImageView>(R.id.img_quiz)?.setImageURI(photoUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_camera_capture)

        // Insets
        val root = findViewById<android.view.View>(R.id.camera_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        // Nav
        findViewById<android.widget.ImageButton>(R.id.btn_back)?.setOnClickListener { finish() }
        findViewById<android.widget.TextView>(R.id.nav_home)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<android.widget.TextView>(R.id.nav_log)?.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
        findViewById<android.widget.TextView>(R.id.nav_quiz)?.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        // Buttons
        findViewById<android.widget.TextView>(R.id.btn_capture)?.setOnClickListener {
            launchCamera()
        }
        findViewById<android.widget.TextView>(R.id.btn_retake)?.setOnClickListener {
            launchCamera()
        }
        findViewById<android.widget.TextView>(R.id.btn_confirm)?.setOnClickListener {
            // Return result to caller (if needed later)
            photoUri?.let { uri ->
                setResult(RESULT_OK, Intent().setData(uri))
            }
            finish()
        }

        // Auto-start camera on first open
        if (savedInstanceState == null) {
            launchCamera()
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile("capture_", ".jpg", cacheDir).apply {
            deleteOnExit()
        }
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        photoUri?.let { uri ->
            takePicture.launch(uri)
        }
    }
}


