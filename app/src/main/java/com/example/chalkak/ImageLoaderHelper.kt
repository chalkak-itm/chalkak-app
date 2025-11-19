package com.example.chalkak

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView

/**
 * Helper class for loading images from various sources
 * Reduces code duplication for bitmap loading operations
 */
object ImageLoaderHelper {
    /**
     * Load bitmap from file path
     */
    fun loadBitmapFromPath(imagePath: String?): Bitmap? {
        if (imagePath == null) return null
        return try {
            BitmapFactory.decodeFile(imagePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Load bitmap from Uri
     */
    fun loadBitmapFromUri(uri: Uri, contentResolver: ContentResolver): Bitmap? {
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
    
    /**
     * Load bitmap from file path and set to ImageView
     */
    fun loadImageToView(imageView: ImageView, imagePath: String?) {
        val bitmap = loadBitmapFromPath(imagePath)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        }
    }
    
    /**
     * Load bitmap from Uri and set to ImageView
     */
    fun loadImageToView(imageView: ImageView, uri: Uri?, contentResolver: ContentResolver) {
        if (uri == null) return
        val bitmap = loadBitmapFromUri(uri, contentResolver)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            // Fallback to set URI directly if bitmap loading fails
            imageView.setImageURI(uri)
        }
    }
}

