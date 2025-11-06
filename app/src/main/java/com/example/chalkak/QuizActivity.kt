package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class QuizActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_quiz)

        // Bottom nav actions
        findViewById<android.widget.TextView>(R.id.nav_home)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<android.widget.TextView>(R.id.nav_log)?.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        // Back button
        findViewById<android.widget.ImageButton>(R.id.btn_back)?.setOnClickListener { finish() }

        // Placeholder: set demo content (will be replaced with DB data)
        val choices = listOf(R.id.choice1, R.id.choice2, R.id.choice3, R.id.choice4)
        choices.forEachIndexed { idx, rid ->
            findViewById<android.widget.TextView>(rid)?.text = "Choice ${idx + 1}"
        }
        findViewById<android.widget.ImageView>(R.id.img_quiz)?.setImageResource(R.drawable.demo)

        // Apply insets to root
        val root = findViewById<android.view.View>(R.id.quiz_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }
    }
}


