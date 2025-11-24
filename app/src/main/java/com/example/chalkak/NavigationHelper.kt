package com.example.chalkak

import android.content.Context
import android.content.Intent

/**
 * Helper class for creating navigation Intents
 * Standardizes Intent creation and navigation flags across the app
 * Reduces code duplication for Activity navigation
 */
object NavigationHelper {
    
    /**
     * Create Intent to navigate to MainActivity with a fragment tag
     * @param context Context for creating Intent
     * @param fragmentTag Tag of the fragment to navigate to (e.g., "home", "log", "quiz", "setting", "magic_adventure")
     * @return Intent configured with standard flags
     */
    fun createMainActivityIntent(context: Context, fragmentTag: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra("fragment_tag", fragmentTag)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }
    
    /**
     * Create Intent to navigate to MainActivity with fragment type and data
     * @param context Context for creating Intent
     * @param fragmentType Type of fragment ("object_input" or "detection_result")
     * @param imagePath Path to the image file
     * @param mainNavTag Main navigation tag for returning
     * @param detectionResults Optional detection results (for detection_result type)
     * @return Intent configured with standard flags
     */
    fun createFragmentTypeIntent(
        context: Context,
        fragmentType: String,
        imagePath: String,
        mainNavTag: String = "home",
        detectionResults: List<DetectionResultItem>? = null
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra("fragment_type", fragmentType)
            putExtra("image_path", imagePath)
            putExtra("main_nav_tag", mainNavTag)
            
            if (detectionResults != null) {
                putParcelableArrayListExtra("detection_results", ArrayList(detectionResults))
            }
            
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }
    
    /**
     * Navigate to MainActivity and finish current Activity
     * @param context Context for creating Intent and starting Activity
     * @param fragmentTag Tag of the fragment to navigate to
     */
    fun navigateToMainActivity(context: Context, fragmentTag: String) {
        val intent = createMainActivityIntent(context, fragmentTag)
        if (context is android.app.Activity) {
            context.startActivity(intent)
            context.finish()
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Navigate to MainActivity with fragment type and finish current Activity
     * @param context Context for creating Intent and starting Activity
     * @param fragmentType Type of fragment
     * @param imagePath Path to the image file
     * @param mainNavTag Main navigation tag
     * @param detectionResults Optional detection results
     */
    fun navigateToMainActivityWithFragmentType(
        context: Context,
        fragmentType: String,
        imagePath: String,
        mainNavTag: String = "home",
        detectionResults: List<DetectionResultItem>? = null
    ) {
        val intent = createFragmentTypeIntent(context, fragmentType, imagePath, mainNavTag, detectionResults)
        if (context is android.app.Activity) {
            context.startActivity(intent)
            context.finish()
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

