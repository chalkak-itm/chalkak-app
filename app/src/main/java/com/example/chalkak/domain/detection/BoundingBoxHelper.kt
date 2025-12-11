package com.example.chalkak.domain.detection

import android.graphics.Bitmap
import android.graphics.RectF
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for bounding box operations
 * Handles bounding box parsing, image cropping, and coordinate transformations
 * Reduces code duplication for bounding box-related operations
 */
object BoundingBoxHelper {
    
    /**
     * Parse bounding box string to RectF
     * Expected format: "[left, top, right, bottom]" or "left, top, right, bottom"
     * @param boundingBoxString String representation of bounding box
     * @return RectF with normalized coordinates (0.0-1.0), or null if parsing fails
     */
    fun parseBoundingBox(boundingBoxString: String?): RectF? {
        if (boundingBoxString.isNullOrBlank()) return null
        
        return try {
            val cleaned = boundingBoxString.trim().removeSurrounding("[", "]")
            val parts = cleaned.split(",").map { it.trim().toFloatOrNull() }
            
            if (parts.size == 4 && parts.all { it != null }) {
                // Safe unwrap since we checked all parts are not null
                val left = parts[0] ?: return null
                val top = parts[1] ?: return null
                val right = parts[2] ?: return null
                val bottom = parts[3] ?: return null
                RectF(left, top, right, bottom)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Crop bitmap based on bounding box coordinates
     * @param originalBitmap Original bitmap to crop
     * @param boundingBox Bounding box with normalized coordinates (0.0-1.0)
     * @return Cropped bitmap, or null if cropping fails
     */
    fun cropBitmap(originalBitmap: Bitmap, boundingBox: RectF): Bitmap? {
        return try {
            // Validate bounding box coordinates
            if (boundingBox.left < 0f || boundingBox.top < 0f || 
                boundingBox.right > 1f || boundingBox.bottom > 1f ||
                boundingBox.left >= boundingBox.right || 
                boundingBox.top >= boundingBox.bottom) {
                return null
            }
            
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            if (width <= 0 || height <= 0) return null
            
            val pixelLeft = (boundingBox.left * width).toInt().coerceIn(0, width - 1)
            val pixelTop = (boundingBox.top * height).toInt().coerceIn(0, height - 1)
            val pixelRight = (boundingBox.right * width).toInt().coerceIn(1, width)
            val pixelBottom = (boundingBox.bottom * height).toInt().coerceIn(1, height)
            
            if (pixelRight > pixelLeft && pixelBottom > pixelTop) {
                Bitmap.createBitmap(
                    originalBitmap,
                    pixelLeft,
                    pixelTop,
                    pixelRight - pixelLeft,
                    pixelBottom - pixelTop
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Crop bitmap from string bounding box
     * Convenience method that combines parsing and cropping
     * @param originalBitmap Original bitmap to crop
     * @param boundingBoxString String representation of bounding box
     * @return Cropped bitmap, or null if parsing or cropping fails
     */
    suspend fun cropBitmapFromString(
        originalBitmap: Bitmap?,
        boundingBoxString: String?
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (originalBitmap == null) return@withContext null
        
        val boundingBox = parseBoundingBox(boundingBoxString) ?: return@withContext null
        cropBitmap(originalBitmap, boundingBox)
    }
    
    /**
     * Convert touch coordinates to normalized image coordinates (0.0-1.0)
     * Handles fitCenter scaleType where image is centered and scaled to fit
     * @param touchX Touch X coordinate in view pixels
     * @param touchY Touch Y coordinate in view pixels
     * @param imageView ImageView containing the image
     * @return Pair of normalized coordinates (x, y) in range [0.0, 1.0], or null if conversion fails
     */
    fun convertTouchToImageCoordinates(
        touchX: Float,
        touchY: Float,
        imageView: ImageView
    ): Pair<Float, Float>? {
        val drawable = imageView.drawable ?: return null
        
        // Get image dimensions
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        
        // If intrinsic dimensions are not available, try to get from bitmap
        val actualImageWidth = if (imageWidth > 0 && imageHeight > 0) {
            imageWidth
        } else {
            (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.width?.toFloat() ?: return null
        }
        
        val actualImageHeight = if (imageWidth > 0 && imageHeight > 0) {
            imageHeight
        } else {
            (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.height?.toFloat() ?: return null
        }
        
        // Validate image dimensions
        if (actualImageWidth <= 0 || actualImageHeight <= 0) return null
        
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        
        // Validate view dimensions
        if (viewWidth <= 0 || viewHeight <= 0) return null
        
        // Calculate scale factor to maintain aspect ratio (fitCenter)
        val scale = minOf(viewWidth / actualImageWidth, viewHeight / actualImageHeight)
        val scaledImageWidth = actualImageWidth * scale
        val scaledImageHeight = actualImageHeight * scale
        
        // Validate scaled dimensions to prevent division by zero
        if (scaledImageWidth <= 0 || scaledImageHeight <= 0) return null
        
        // Calculate offset (centered)
        val offsetX = (viewWidth - scaledImageWidth) / 2f
        val offsetY = (viewHeight - scaledImageHeight) / 2f
        
        // Convert touch coordinates to image coordinates (0.0-1.0)
        // Clamp to valid range [0.0, 1.0]
        val imageX = ((touchX - offsetX) / scaledImageWidth).coerceIn(0f, 1f)
        val imageY = ((touchY - offsetY) / scaledImageHeight).coerceIn(0f, 1f)
        
        return Pair(imageX, imageY)
    }
    
    /**
     * Check if a point (in normalized coordinates) is inside a bounding box
     * @param pointX Normalized X coordinate (0.0-1.0)
     * @param pointY Normalized Y coordinate (0.0-1.0)
     * @param boundingBox Bounding box with normalized coordinates
     * @return True if point is inside bounding box
     */
    fun isPointInBoundingBox(
        pointX: Float,
        pointY: Float,
        boundingBox: RectF
    ): Boolean {
        return pointX >= boundingBox.left && pointX <= boundingBox.right &&
               pointY >= boundingBox.top && pointY <= boundingBox.bottom
    }
    
    /**
     * Find which DetectionResultItem contains the touch point
     * @param touchX Touch X coordinate in view pixels
     * @param touchY Touch Y coordinate in view pixels
     * @param imageView ImageView containing the image
     * @param detectionResults List of detection results to check
     * @param reverseOrder If true, check in reverse order (prioritize later items)
     * @return DetectionResultItem that contains the point, or null if none found
     */
    fun findItemAtTouch(
        touchX: Float,
        touchY: Float,
        imageView: ImageView,
        detectionResults: List<DetectionResultItem>,
        reverseOrder: Boolean = true
    ): DetectionResultItem? {
        val imageCoords = convertTouchToImageCoordinates(touchX, touchY, imageView) ?: return null
        val (imageX, imageY) = imageCoords
        
        val itemsToCheck = if (reverseOrder) detectionResults.reversed() else detectionResults
        
        return itemsToCheck.firstOrNull { item ->
            // Create RectF from DetectionResultItem for consistency
            val boundingBox = RectF(item.left, item.top, item.right, item.bottom)
            isPointInBoundingBox(imageX, imageY, boundingBox)
        }
    }
}
