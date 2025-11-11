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
			findViewById<android.widget.ImageView>(R.id.img_quiz)?.setImageURI(photoUri)
			// After camera capture, hide capture button and show "Choose Another"
			findViewById<android.widget.TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
			findViewById<android.widget.TextView>(R.id.btn_retake)?.text = "Choose Another"
		}
	}

	private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		uri?.let {
			photoUri = it
			isGalleryMode = true
			findViewById<android.widget.ImageView>(R.id.img_quiz)?.setImageURI(it)
			// After gallery selection, hide capture button and show "Choose Another"
			findViewById<android.widget.TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
			findViewById<android.widget.TextView>(R.id.btn_retake)?.text = "Choose Another"
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
				findViewById<android.widget.ImageView>(R.id.img_quiz)?.setImageURI(uri)
				findViewById<android.widget.TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
				findViewById<android.widget.TextView>(R.id.btn_retake)?.text = "Choose Another"
			} else if (pendingCameraUri != null) {
				// Camera capture - use the saved URI
				photoUri = pendingCameraUri
				findViewById<android.widget.ImageView>(R.id.img_quiz)?.setImageURI(pendingCameraUri)
				findViewById<android.widget.TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
				findViewById<android.widget.TextView>(R.id.btn_retake)?.text = "Choose Another"
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
			findViewById<android.widget.ImageView>(R.id.img_quiz)?.setImageURI(imageUri)
			findViewById<android.widget.TextView>(R.id.btn_capture)?.visibility = android.view.View.GONE
			findViewById<android.widget.TextView>(R.id.btn_retake)?.text = "Choose Another"
		}

		val root = findViewById<android.view.View>(R.id.camera_root)
		ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
			val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
			insets
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
		findViewById<android.widget.TextView>(R.id.btn_confirm)?.setOnClickListener {
			photoUri?.let { uri ->
				setResult(RESULT_OK, Intent().setData(uri))
			}
			finish()
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
