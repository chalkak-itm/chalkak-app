
package com.example.chalkak

import DetectionResultItem
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout.LayoutParams as FLP

class DetectionResultActivity : AppCompatActivity() {

    private lateinit var imgResult: ImageView
    private lateinit var boxOverlay: FrameLayout

    private lateinit var scrollObjectButtons: HorizontalScrollView
    private lateinit var layoutObjectButtons: LinearLayout

    private lateinit var cardWordDetail: LinearLayout
    private lateinit var txtSelectedObject: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView

    private lateinit var btnBack: ImageButton

    private var detectionResults: List<DetectionResultItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_result)

        // view binding
        imgResult = findViewById(R.id.img_result)
        boxOverlay = findViewById(R.id.box_overlay_container)

        scrollObjectButtons = findViewById(R.id.scroll_object_buttons)
        layoutObjectButtons = findViewById(R.id.layout_object_buttons)

        cardWordDetail = findViewById(R.id.card_word_detail)
        txtSelectedObject = findViewById(R.id.txt_selected_object)
        txtKoreanMeaning = findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = findViewById(R.id.txt_example_sentence)

        btnBack = findViewById(R.id.btn_back)

        // Take Data from the Intent
        val imagePath = intent.getStringExtra("image_path")
        detectionResults =
            intent.getParcelableArrayListExtra<DetectionResultItem>("detection_results") ?: emptyList()

        // Image setting
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imgResult.setImageBitmap(bitmap)
        }

        // Addition the Box overlay
        boxOverlay.post {
            addBoxOverlays()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     *Add a clickable transparent View to each box location in detectionResults.
     */
    private fun addBoxOverlays() {
        val width = boxOverlay.width.toFloat()
        val height = boxOverlay.height.toFloat()

        boxOverlay.removeAllViews()

        detectionResults.forEach { item ->
            val boxWidth = (item.right - item.left) * width
            val boxHeight = (item.bottom - item.top) * height
            val leftMargin = item.left * width
            val topMargin = item.top * height

            val boxView = View(this).apply {
                isClickable = true
                isFocusable = true
                setBackgroundColor(Color.TRANSPARENT)

                setOnClickListener {
                    onBoxClicked(item)
                }
            }

            val params = FLP(boxWidth.toInt(), boxHeight.toInt())
            params.leftMargin = leftMargin.toInt()
            params.topMargin = topMargin.toInt()

            boxOverlay.addView(boxView, params)
        }
    }

    /**
     * click box → provide one word button
     */
    private fun onBoxClicked(item: DetectionResultItem) {
        cardWordDetail.visibility = View.GONE
        scrollObjectButtons.visibility = View.VISIBLE

        // represent only the selected word
        layoutObjectButtons.removeAllViews()

        val wordButton = TextView(this).apply {
            text = item.label
            setBackgroundResource(R.drawable.bg_button_purple_ripple)
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(32, 16, 32, 16)
            isClickable = true
            isFocusable = true

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

        layoutObjectButtons.addView(wordButton)
    }

    /**
     * click word button → show detail card
     */
    private fun showWordDetail(item: DetectionResultItem) {
        cardWordDetail.visibility = View.VISIBLE

        txtSelectedObject.text = item.label

        // bring the example sentence and korean meaning by GPT
        txtKoreanMeaning.text = "한국어 뜻."
        txtExampleSentence.text = "It is a space for example sentence."
    }
}
