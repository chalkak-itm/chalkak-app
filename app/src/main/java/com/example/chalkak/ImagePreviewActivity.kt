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
import androidx.core.view.WindowCompat
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
		btnCapture.text = getString(R.string.upload)
		btnRetake.text = getString(R.string.retake)

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

		// Apply WindowInsets using helper
		val root = findViewById<android.view.View>(R.id.camera_root)
		val bottomNavContainer = findViewById<android.view.View>(R.id.bottom_nav_container)
		WindowInsetsHelper.applyToActivity(
			rootView = root,
			bottomNavContainer = bottomNavContainer,
			resources = resources
		)

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

					// 3. DetectionResult -> DetectionResultItem using converter
					val w = outputBitmap.width.toFloat()
					val h = outputBitmap.height.toFloat()
					val uiResults = DetectionResultConverter.convertToDetectionResultItems(results, w, h)

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

							NavigationHelper.navigateToMainActivityWithFragmentType(
								context = this@ImagePreviewActivity,
								fragmentType = "object_input",
								imagePath = imageFile.absolutePath,
								mainNavTag = mainNavTag
							)
						} else {
							// Has detection results - go to result Fragment
							Toast.makeText(
								this@ImagePreviewActivity,
								"Executed successfully!",
								Toast.LENGTH_SHORT
							).show()

							NavigationHelper.navigateToMainActivityWithFragmentType(
								context = this@ImagePreviewActivity,
								fragmentType = "detection_result",
								imagePath = imageFile.absolutePath,
								mainNavTag = mainNavTag,
								detectionResults = uiResults
							)
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
