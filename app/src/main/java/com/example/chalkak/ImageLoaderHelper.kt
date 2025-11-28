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
 * Optimized for memory efficiency using inSampleSize
 */
object ImageLoaderHelper {
    // Maximum dimensions for loaded images to prevent memory issues
    private const val MAX_IMAGE_WIDTH = 2048
    private const val MAX_IMAGE_HEIGHT = 2048
    
    /**
     * Calculate inSampleSize for bitmap decoding to reduce memory usage
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Load bitmap from file path with memory optimization
     * @param imagePath Path to the image file
     * @param maxWidth Maximum width for the loaded bitmap (default: MAX_IMAGE_WIDTH)
     * @param maxHeight Maximum height for the loaded bitmap (default: MAX_IMAGE_HEIGHT)
     */
    fun loadBitmapFromPath(
        imagePath: String?,
        maxWidth: Int = MAX_IMAGE_WIDTH,
        maxHeight: Int = MAX_IMAGE_HEIGHT
    ): Bitmap? {
        if (imagePath == null) return null
        return try {
            // First, decode with inJustDecodeBounds=true to get image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)
            
            // Calculate inSampleSize to reduce memory usage
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Load bitmap from Uri with memory optimization
     * @param uri Uri of the image
     * @param contentResolver ContentResolver to access the URI
     * @param maxWidth Maximum width for the loaded bitmap (default: MAX_IMAGE_WIDTH)
     * @param maxHeight Maximum height for the loaded bitmap (default: MAX_IMAGE_HEIGHT)
     */
    fun loadBitmapFromUri(
        uri: Uri,
        contentResolver: ContentResolver,
        maxWidth: Int = MAX_IMAGE_WIDTH,
        maxHeight: Int = MAX_IMAGE_HEIGHT
    ): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                // ImageDecoder supports scaling via OnHeaderDecodedListener
                ImageDecoder.decodeBitmap(source) { decoder, info, source ->
                    val sampleSize = calculateInSampleSizeForDecoder(
                        info.size.width,
                        info.size.height,
                        maxWidth,
                        maxHeight
                    )
                    decoder.setTargetSampleSize(sampleSize)
                }
            } else {
                @Suppress("DEPRECATION")
                // For older versions, use BitmapFactory with options
                val inputStream = contentResolver.openInputStream(uri) ?: return null
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                
                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                options.inJustDecodeBounds = false
                
                val inputStream2 = contentResolver.openInputStream(uri) ?: return null
                val bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
                inputStream2.close()
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Calculate sample size for ImageDecoder
     */
    private fun calculateInSampleSizeForDecoder(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Load bitmap from file path and set to ImageView
     * @param imageView ImageView to set the bitmap to
     * @param imagePath Path to the image file
     * @param maxWidth Maximum width for the loaded bitmap (default: MAX_IMAGE_WIDTH)
     * @param maxHeight Maximum height for the loaded bitmap (default: MAX_IMAGE_HEIGHT)
     */
    fun loadImageToView(
        imageView: ImageView, 
        imagePath: String?,
        maxWidth: Int = MAX_IMAGE_WIDTH,
        maxHeight: Int = MAX_IMAGE_HEIGHT
    ) {
        val bitmap = loadBitmapFromPath(imagePath, maxWidth, maxHeight)
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

