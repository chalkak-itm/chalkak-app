package com.example.chalkak

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MagicAdventureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magic_adventure)

        val takePhoto: LinearLayout = findViewById(R.id.btn_take_photo)
        val upload: LinearLayout = findViewById(R.id.btn_upload)

        takePhoto.setOnClickListener {
            // TODO: 구현 - 카메라 열기
        }
        upload.setOnClickListener {
            // TODO: 구현 - 갤러리 선택
        }
    }
}


