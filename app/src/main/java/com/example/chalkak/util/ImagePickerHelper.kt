package com.example.chalkak.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.io.File

/**
 * Helper class for handling image picking from camera or gallery
 * Reduces code duplication across fragments and activities
 */
class ImagePickerHelper(
    private val context: Context,
    private val packageName: String,
    private val onImageSelected: (Uri, String) -> Unit // Uri and mainNavTag
) {
    private var photoUri: Uri? = null
    private var mainNavTag: String = "home"
    
    // For Fragment usage
    private var fragment: Fragment? = null
    private var takePictureLauncher: ActivityResultLauncher<Uri>? = null
    private var pickImageLauncher: ActivityResultLauncher<String>? = null
    
    // For Activity usage
    private var activity: FragmentActivity? = null
    
    /**
     * Initialize for Fragment usage
     */
    fun initializeForFragment(fragment: Fragment, getMainNavTag: () -> String) {
        this.fragment = fragment
        this.mainNavTag = getMainNavTag()
        
        takePictureLauncher = fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                onImageSelected(photoUri!!, mainNavTag)
            }
        }
        
        pickImageLauncher = fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                mainNavTag = getMainNavTag()
                onImageSelected(it, mainNavTag)
            }
        }
    }
    
    /**
     * Initialize for Activity usage
     */
    fun initializeForActivity(
        activity: FragmentActivity,
        takePictureLauncher: ActivityResultLauncher<Uri>,
        pickImageLauncher: ActivityResultLauncher<String>,
        getMainNavTag: () -> String
    ) {
        this.activity = activity
        this.mainNavTag = getMainNavTag()
        this.takePictureLauncher = takePictureLauncher
        this.pickImageLauncher = pickImageLauncher
    }
    
    /**
     * Launch camera to take a picture
     */
    fun launchCamera() {
        val cacheDir = when {
            fragment != null -> fragment!!.requireContext().cacheDir
            activity != null -> activity!!.cacheDir
            else -> context.cacheDir
        }
        
        val photoFile = File.createTempFile("capture_", ".jpg", cacheDir).apply {
            deleteOnExit()
        }
        
        photoUri = FileProvider.getUriForFile(
            context,
            "$packageName.fileprovider",
            photoFile
        )
        
        photoUri?.let { uri ->
            takePictureLauncher?.launch(uri)
        }
    }

    /**
     * Expose the latest camera uri so callers can read it in onActivityResult callbacks.
     */
    fun getCurrentPhotoUri(): Uri? = photoUri
    
    /**
     * Launch gallery to pick an image
     */
    fun pickImage() {
        pickImageLauncher?.launch("image/*")
    }
    
    /**
     * Update main navigation tag
     */
    fun updateMainNavTag(tag: String) {
        mainNavTag = tag
    }
}
