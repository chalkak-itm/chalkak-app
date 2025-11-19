package com.example.chalkak

import DetectionResultItem
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chalkak.ml.ObjectDetectionHelper

class ImagePreviewActivity : AppCompatActivity() {
	private var photoUri: Uri? = null
	private var isGalleryMode = false
	private var mainNavTag: String = "home"

	//variables for detection model
	private lateinit var detectionHelper: ObjectDetectionHelper
	private var currentBitmap: Bitmap? = null
	
	// Helpers
	private lateinit var imagePickerHelper: ImagePickerHelper
	private lateinit var bottomNavigationHelper: BottomNavigationHelper

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

	// Helper function to update image display
	private fun updateImageDisplay(uri: Uri) {
		val imgQuiz = findViewById<ImageView>(R.id.img_quiz)
		val txtNoImage = findViewById<TextView>(R.id.txt_no_image)
		
		currentBitmap = ImageLoaderHelper.loadBitmapFromUri(uri, contentResolver)
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
		mainNavTag = intent.getStringExtra("main_nav_tag") ?: "home"
		
		if (imageUri != null) {
			photoUri = imageUri
			isGalleryMode = true
			updateImageDisplay(imageUri)
		} else {
			// Show "No Image" text when there's no photo
			imgQuiz.visibility = android.view.View.GONE
			txtNoImage.visibility = android.view.View.VISIBLE
		}
		
		// Button text is always "Upload" and "Retake"
		btnCapture.text = "Upload"
		btnRetake.text = "Retake"

		findViewById<android.widget.ImageButton>(R.id.btn_back)?.setOnClickListener { finish() }

		// Initialize ImagePickerHelper
		imagePickerHelper = ImagePickerHelper(
			context = this,
			packageName = packageName,
			onImageSelected = { uri, _ ->
				photoUri = uri
				isGalleryMode = true
				updateImageDisplay(uri)
			}
		)
		imagePickerHelper.initializeForActivity(
			activity = this,
			takePictureLauncher = takePicture,
			pickImageLauncher = pickImage,
			getMainNavTag = { mainNavTag }
		)

		// Initialize BottomNavigationHelper
		bottomNavigationHelper = BottomNavigationHelper(this, BottomNavigationHelper.createDefaultItems())
		bottomNavigationHelper.setupBottomNavigation()
		bottomNavigationHelper.updateNavigationHighlightAlpha(mainNavTag)

		// Apply WindowInsets to root layout
		val root = findViewById<android.view.View>(R.id.camera_root)
		ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
			insets
		}

		// Apply WindowInsets to bottom navigation bar container
		val bottomNavContainer = findViewById<android.view.View>(R.id.bottom_nav_container)
		ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(0, 0, 0, systemBars.bottom)
			insets
		}

		findViewById<android.widget.TextView>(R.id.btn_capture)?.setOnClickListener {
			// Upload button: select photo from gallery
			imagePickerHelper.pickImage()
		}
		findViewById<android.widget.TextView>(R.id.btn_retake)?.setOnClickListener {
			// Retake button: launch camera
			imagePickerHelper.launchCamera()
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

					// 4. UI Thread --> MainActivity with Fragment
					runOnUiThread {
						btnConfirm.isEnabled = true
						
						if (uiResults.isEmpty()) {
							// No detection results - go to input Fragment
							Toast.makeText(
								this@ImagePreviewActivity,
								"No objects detected. Please enter manually.",
								Toast.LENGTH_SHORT
							).show()

							val intent = Intent(this@ImagePreviewActivity, MainActivity::class.java).apply {
								putExtra("fragment_type", "object_input")
								putExtra("image_path", imageFile.absolutePath)
								putExtra("main_nav_tag", mainNavTag)
								flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
							}
							startActivity(intent)
							finish()
						} else {
							// Has detection results - go to result Fragment
							Toast.makeText(
								this@ImagePreviewActivity,
								"Executed successfully!",
								Toast.LENGTH_SHORT
							).show()

							val intent = Intent(this@ImagePreviewActivity, MainActivity::class.java).apply {
								putExtra("fragment_type", "detection_result")
								putExtra("image_path", imageFile.absolutePath)
								putParcelableArrayListExtra("detection_results", uiResults)
								putExtra("main_nav_tag", mainNavTag)
								flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
							}
							startActivity(intent)
							finish()
						}
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

		// Don't launch camera automatically (user must press button)
	}
}
