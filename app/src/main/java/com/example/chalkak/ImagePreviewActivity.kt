package com.example.chalkak

import DetectionResultItem
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chalkak.ml.ObjectDetectionHelper
import java.io.File

class ImagePreviewActivity : AppCompatActivity() {
	private var photoUri: Uri? = null
	private var isGalleryMode = false

	//variables for detection model
	private lateinit var detectionHelper: ObjectDetectionHelper
	private var currentBitmap: Bitmap? = null

	private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
		if (success && photoUri != null) {
			updateImageDisplay(photoUri!!)
		}
	}

	private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		uri?.let {
			photoUri = it
			isGalleryMode = true
			updateImageDisplay(it)
		}
	}

	// 이미지 표시를 업데이트하는 헬퍼 함수
	private fun updateImageDisplay(uri: Uri) {
		val imgQuiz = findViewById<ImageView>(R.id.img_quiz)
		val txtNoImage = findViewById<TextView>(R.id.txt_no_image)
		
		currentBitmap = loadBitmapFromUri(uri)
		if (currentBitmap != null) {
			imgQuiz.setImageBitmap(currentBitmap)
			imgQuiz.visibility = android.view.View.VISIBLE
			txtNoImage.visibility = android.view.View.GONE
		} else {
			imgQuiz.setImageURI(uri)
			imgQuiz.visibility = android.view.View.VISIBLE
			txtNoImage.visibility = android.view.View.GONE
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		setContentView(R.layout.activity_camera_capture)

		//Initial the ObjectDetectionHelper
		detectionHelper = ObjectDetectionHelper(this)

		// pre-bring the frequent views
		val imgQuiz = findViewById<ImageView>(R.id.img_quiz)
		val txtNoImage = findViewById<TextView>(R.id.txt_no_image)
		val btnCapture = findViewById<TextView>(R.id.btn_capture)
		val btnRetake = findViewById<TextView>(R.id.btn_retake)
		val btnConfirm = findViewById<TextView>(R.id.btn_confirm)

		val imageUri = intent.getParcelableExtra<Uri>("image_uri")
		if (imageUri != null) {
			photoUri = imageUri
			isGalleryMode = true
			updateImageDisplay(imageUri)
		} else {
			// 사진이 없을 때 "No Image" 텍스트 표시
			imgQuiz.visibility = android.view.View.GONE
			txtNoImage.visibility = android.view.View.VISIBLE
		}
		
		// 버튼 텍스트는 항상 "Upload"와 "Retake"로 유지
		btnCapture.text = "Upload"
		btnRetake.text = "Retake"

		findViewById<android.widget.ImageButton>(R.id.btn_back)?.setOnClickListener { finish() }

		// Setup bottom navigation
		setupBottomNavigation()

		// Apply WindowInsets to root layout
		val root = findViewById<android.view.View>(R.id.camera_root)
		ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
			insets
		}

		// Apply WindowInsets to bottom navigation bar
		val bottomNav = findViewById<android.view.View>(R.id.bottom_nav_include)
		ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val layoutParams = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
			layoutParams.bottomMargin = systemBars.bottom
			v.layoutParams = layoutParams
			insets
		}

		findViewById<android.widget.TextView>(R.id.btn_capture)?.setOnClickListener {
			// Upload 버튼: 갤러리에서 사진 선택
			pickImage.launch("image/*")
		}
		findViewById<android.widget.TextView>(R.id.btn_retake)?.setOnClickListener {
			// Retake 버튼: 카메라 실행
			launchCamera()
		}
		btnConfirm.setOnClickListener {
			val bitmap = currentBitmap
			if (bitmap == null) {
				Toast.makeText(this, "Take photo or choose picture in gallery first.", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			// Validate bitmap before processing
			if (bitmap.isRecycled) {
				Toast.makeText(this, "Image is no longer available. Please select another image.", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			// Disable button during processing
			btnConfirm.isEnabled = false

			// Execute in thread
			Thread {
				try {
					// Execute Detection
					val (outputBitmap, results) = detectionHelper.detect(bitmap)
					// results: List<com.example.chalkak.ml.DetectionResult>
					// DetectionResult(name: String, score: Float, boundingBox: RectF)

					// 2. Store the result image in cache
					val imageFile = java.io.File(cacheDir, "detection_${System.currentTimeMillis()}.png")
					java.io.FileOutputStream(imageFile).use { out ->
						outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
					}

					// 3. DetectionResult -> DetectionResultItem
					val uiResults = ArrayList<DetectionResultItem>()
					val w = outputBitmap.width.toFloat()
					val h = outputBitmap.height.toFloat()

					for (r in results) {
						val box = r.boundingBox   // RectF
						uiResults.add(
							DetectionResultItem(
								label = r.name,
								score = r.score,
								left = box.left / w,
								top = box.top / h,
								right = box.right / w,
								bottom = box.bottom / h
							)
						)
					}

					// 4. UI Thread --> result Activity
					runOnUiThread {
						btnConfirm.isEnabled = true
						Toast.makeText(
							this@ImagePreviewActivity,
							"Executed successfully!",
							Toast.LENGTH_SHORT
						).show()

						val intent = Intent(this@ImagePreviewActivity, DetectionResultActivity::class.java).apply {
							putExtra("image_path", imageFile.absolutePath)
							putParcelableArrayListExtra("detection_results", uiResults)
						}
						startActivity(intent)
					}
				} catch (e: Exception) {
					e.printStackTrace()
					runOnUiThread {
						btnConfirm.isEnabled = true
						Toast.makeText(
							this@ImagePreviewActivity,
							"Detection failed: ${e.message ?: "Unknown error"}",
							Toast.LENGTH_LONG
						).show()
					}
				}
			}.start()
		}

		// 자동으로 카메라를 실행하지 않음 (사용자가 버튼을 눌러야 함)
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

	//function for transfer the uri into bitmap
	private fun loadBitmapFromUri(uri: Uri): Bitmap? {
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				val source = ImageDecoder.createSource(contentResolver, uri)
				ImageDecoder.decodeBitmap(source)
			} else {
				@Suppress("DEPRECATION")
				MediaStore.Images.Media.getBitmap(contentResolver, uri)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}

	private fun setupBottomNavigation() {
		findViewById<android.widget.TextView>(R.id.nav_home)?.setOnClickListener {
			val intent = Intent(this, MainActivity::class.java).apply {
				putExtra("fragment_tag", "home")
				flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
			}
			startActivity(intent)
			finish()
		}
		findViewById<android.widget.TextView>(R.id.nav_log)?.setOnClickListener {
			val intent = Intent(this, MainActivity::class.java).apply {
				putExtra("fragment_tag", "log")
				flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
			}
			startActivity(intent)
			finish()
		}
		findViewById<android.widget.TextView>(R.id.nav_quiz)?.setOnClickListener {
			val intent = Intent(this, MainActivity::class.java).apply {
				putExtra("fragment_tag", "quiz")
				flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
			}
			startActivity(intent)
			finish()
		}
		findViewById<android.widget.TextView>(R.id.nav_setting)?.setOnClickListener {
			val intent = Intent(this, MainActivity::class.java).apply {
				putExtra("fragment_tag", "setting")
				flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
			}
			startActivity(intent)
			finish()
		}
	}
}
