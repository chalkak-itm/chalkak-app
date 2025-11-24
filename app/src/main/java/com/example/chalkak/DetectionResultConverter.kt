package com.example.chalkak

import com.example.chalkak.ml.DetectionResult

/**
 * Helper class for converting ML detection results to UI detection result items
 * Handles coordinate normalization and data transformation
 * Reduces code duplication for detection result conversion
 */
object DetectionResultConverter {
    
    /**
     * Convert ML DetectionResult to UI DetectionResultItem
     * Normalizes bounding box coordinates to 0.0-1.0 range
     * @param detectionResult ML detection result from ObjectDetectionHelper
     * @param imageWidth Width of the image in pixels
     * @param imageHeight Height of the image in pixels
     * @return UI DetectionResultItem with normalized coordinates
     */
    fun convertToDetectionResultItem(
        detectionResult: DetectionResult,
        imageWidth: Float,
        imageHeight: Float
    ): DetectionResultItem {
        val box = detectionResult.boundingBox
        return DetectionResultItem(
            label = detectionResult.name,
            score = detectionResult.score,
            left = box.left / imageWidth,
            top = box.top / imageHeight,
            right = box.right / imageWidth,
            bottom = box.bottom / imageHeight
        )
    }
    
    /**
     * Convert list of ML DetectionResults to UI DetectionResultItems
     * @param detectionResults List of ML detection results
     * @param imageWidth Width of the image in pixels
     * @param imageHeight Height of the image in pixels
     * @return List of UI DetectionResultItems with normalized coordinates
     */
    fun convertToDetectionResultItems(
        detectionResults: List<DetectionResult>,
        imageWidth: Float,
        imageHeight: Float
    ): List<DetectionResultItem> {
        return detectionResults.map { result ->
            convertToDetectionResultItem(result, imageWidth, imageHeight)
        }
    }
}

