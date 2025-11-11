package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val magicButton: LinearLayout = findViewById(R.id.btnMagicAdventure)
        magicButton.setOnClickListener {
            startActivity(Intent(this, MagicAdventureActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.nav_log)?.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.nav_quiz)?.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        val root = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}