package com.example.chalkak

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
			val imgQuiz = findViewById<ImageView>(R.id.img_quiz)

			currentBitmap = loadBitmapFromUri(photoUri!!)
			if (currentBitmap != null) {
				imgQuiz.setImageBitmap(currentBitmap)
			} else {
				imgQuiz.setImageURI(photoUri)
			}

			findViewById<TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
			findViewById<TextView>(R.id.btn_retake)?.text = "Choose Another"
		}
	}

	private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		uri?.let {
			photoUri = it
			isGalleryMode = true

			val imgQuiz = findViewById<ImageView>(R.id.img_quiz)
			currentBitmap = loadBitmapFromUri(it)
			if (currentBitmap != null) {
				imgQuiz.setImageBitmap(currentBitmap)
			} else {
				imgQuiz.setImageURI(it)
			}

			findViewById<TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
			findViewById<TextView>(R.id.btn_retake)?.text = "Choose Another"
		}
	}

	private var pendingCameraUri: Uri? = null
	
	private val chooseImageSource = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == RESULT_OK) {
			val uri = result.data?.data
			if (uri != null) {
				// Gallery selection
				photoUri = uri
				isGalleryMode = true

				val imgQuiz = findViewById<ImageView>(R.id.img_quiz)
				currentBitmap = loadBitmapFromUri(uri)
				if (currentBitmap != null) {
					imgQuiz.setImageBitmap(currentBitmap)
				} else {
					imgQuiz.setImageURI(uri)
				}

				findViewById<TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
				findViewById<TextView>(R.id.btn_retake)?.text = "Choose Another"
			} else if (pendingCameraUri != null) {
				// Camera capture - use the saved URI
				photoUri = pendingCameraUri

				val imgQuiz = findViewById<ImageView>(R.id.img_quiz)
				currentBitmap = loadBitmapFromUri(pendingCameraUri!!)
				if (currentBitmap != null) {
					imgQuiz.setImageBitmap(currentBitmap)
				} else {
					imgQuiz.setImageURI(pendingCameraUri)
				}

				findViewById<TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
				findViewById<TextView>(R.id.btn_retake)?.text = "Choose Another"
				pendingCameraUri = null
			}
		} else {
			pendingCameraUri = null
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
		val btnCapture = findViewById<TextView>(R.id.btn_capture)
		val btnRetake = findViewById<TextView>(R.id.btn_retake)
		val btnConfirm = findViewById<TextView>(R.id.btn_confirm)

		val imageUri = intent.getParcelableExtra<Uri>("image_uri")
		if (imageUri != null) {
			photoUri = imageUri
			isGalleryMode = true

			// Uri → Bitmap and store in currentBitmap
			currentBitmap = loadBitmapFromUri(imageUri)
			if (currentBitmap != null) {
				imgQuiz.setImageBitmap(currentBitmap)
			} else {
				// if change is failure stay in URI
				imgQuiz.setImageURI(imageUri)
			}

			btnCapture.visibility = android.view.View.GONE
			btnRetake.text = "Choose Another"
		}

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

		findViewById<android.widget.TextView>(R.id.btn_capture)?.setOnClickListener {
			launchCamera()
		}
		findViewById<android.widget.TextView>(R.id.btn_retake)?.setOnClickListener {
			// Always show source selection dialog when image is already selected
			if (photoUri != null) {
				showSourceSelectionDialog()
			} else {
				launchCamera()
			}
		}
		btnConfirm.setOnClickListener {
			val bitmap = currentBitmap
			if (bitmap == null) {
				Toast.makeText(this, "Take photo or choose picture in gallery first.", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			// Execute in thread
			Thread {
				// ObjectDetectionHelper
				val (outputBitmap, results) = detectionHelper.detect(bitmap)

				// outputBitmap is image with boxes
				// results is list containing name, score, boundingBox(position)

				// The activity to handle the results
				runOnUiThread {
					Toast.makeText(this@ImagePreviewActivity, "Executed successfully!", Toast.LENGTH_SHORT).show()

					// 나중에 결과를 표시할 Activity가 생기면 여기서 Intent로 넘길 예정:
					// 지금은 아직 UI가 없으니까 finish() 없이 그대로 둠.
				}
			}.start()
		}

		if (savedInstanceState == null && !isGalleryMode) {
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

	private fun showSourceSelectionDialog() {
		// Create camera intent
		val photoFile = File.createTempFile("capture_", ".jpg", cacheDir).apply {
			deleteOnExit()
		}
		val cameraUri = FileProvider.getUriForFile(
			this,
			"${packageName}.fileprovider",
			photoFile
		)
		pendingCameraUri = cameraUri
		
		val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
			putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraUri)
			addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
		}
		
		// Create gallery intent
		val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
			type = "image/*"
		}
		
		// Create chooser with both options
		val chooserIntent = Intent.createChooser(galleryIntent, "Choose Image Source").apply {
			putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
		}
		
		chooseImageSource.launch(chooserIntent)
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
}
