package com.example.chalkak

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Check if already logged in
        if (prefs.getBoolean("is_logged_in", false)) {
            navigateToMain()
            return
        }

        // Apply insets
        val root = findViewById<android.view.View>(R.id.login_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        // Sign in button click
        findViewById<android.widget.ImageView>(R.id.btn_sign_in)?.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        // TODO: Implement actual Google Sign-in or authentication logic
        // For now, just mark as logged in and navigate
        prefs.edit().putBoolean("is_logged_in", true).apply()
        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

