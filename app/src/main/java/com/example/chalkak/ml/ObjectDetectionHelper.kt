package com.example.chalkak.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

data class  DetectionResult(
    val name: String,        //Name of label
    val score: Float,        //the trust rate
    val boundingBox: RectF      //position of boxes
)

class ObjectDetectionHelper(context: Context) {
    private val detector: ObjectDetector

    init {
        // setting the options
        val baseOptions = BaseOptions.builder()
            .setNumThreads(4)
            .build()

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(2)      // the maximum detected objects
            .setScoreThreshold(0.3f)   // the minimum trust rate
            .build()

        // using assets/1.tflite
        detector = ObjectDetector.createFromFileAndOptions(
            context,
            "1.tflite",
            options
        )
    }

    /**
     * After receive the bitmap
     * Draw new bitmap by adding the boxes
     * finally return the new one
     * with name of label as DetectionResult.name
     */
    fun detect(inputBitmap: Bitmap): Pair<Bitmap, List<DetectionResult>> {
        // always set the bitmap as ARGB_8888
        val argbBitmap = if (inputBitmap.config != Bitmap.Config.ARGB_8888) {
            inputBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            inputBitmap
        }

        // input this image into tensor image
        val image = TensorImage.fromBitmap(argbBitmap)
        val results = detector.detect(image)

        // the copy of origin
        val outputBitmap = argbBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        // box-paint setting
        val boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.RED
        }

        val detectionResults = mutableListOf<DetectionResult>()

        for (det in results) {
            val category = det.categories.first()
            val label = category.label   //name
            val score = category.score
            val box = det.boundingBox

            // draw boxes on bitmap
            canvas.drawRect(box, boxPaint)

            detectionResults.add(
                DetectionResult(
                    name = label,
                    score = score,
                    boundingBox = box
                )
            )
        }

        // Return bitmap with box + name/score/box position
        return outputBitmap to detectionResults
    }
}