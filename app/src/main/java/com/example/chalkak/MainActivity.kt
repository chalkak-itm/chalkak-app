package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val magicButton: LinearLayout = findViewById(R.id.btnMagicAdventure)
        magicButton.setOnClickListener {
            startActivity(Intent(this, MagicAdventureActivity::class.java))
        }
    }
}