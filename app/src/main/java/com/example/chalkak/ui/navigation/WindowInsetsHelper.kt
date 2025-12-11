package com.example.chalkak.ui.navigation

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chalkak.R

/**
 * Helper class for applying WindowInsets to Activity layouts
 * Handles system bars and display cutout (camera cutout) padding
 * Reduces code duplication across multiple Activities
 */
object WindowInsetsHelper {
    
    /**
     * Apply WindowInsets to root layout with camera cutout consideration
     * @param rootView The root view of the activity
     * @param resources Resources instance for accessing dimension values
     * @param includeBottomPadding Whether to include bottom padding (default: false, as most activities handle it separately)
     */
    fun applyToRootLayout(
        rootView: View,
        resources: android.content.res.Resources,
        includeBottomPadding: Boolean = false
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            
            // S23 FE 전면 카메라 영역 고려: systemBars.top과 displayCutout.top 중 큰 값 사용
            val topPadding = maxOf(systemBars.top, displayCutout.top)
            val cameraCutoutPadding = resources.getDimensionPixelSize(R.dimen.camera_cutout_padding_top)
            val totalTopPadding = topPadding + cameraCutoutPadding
            
            val bottomPadding = if (includeBottomPadding) systemBars.bottom else 0
            
            v.setPadding(systemBars.left, totalTopPadding, systemBars.right, bottomPadding)
            insets
        }
    }
    
    /**
     * Apply WindowInsets to bottom navigation container
     * @param bottomNavContainer The bottom navigation container view
     * @param onInsetsApplied Optional callback after insets are applied (e.g., for MainActivity's fragment padding)
     */
    fun applyToBottomNavigation(
        bottomNavContainer: View,
        onInsetsApplied: (() -> Unit)? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            
            // Callback for additional processing (e.g., MainActivity's fragment padding)
            onInsetsApplied?.invoke()
            
            insets
        }
    }
    
    /**
     * Apply WindowInsets to both root layout and bottom navigation
     * Convenience method for Activities with bottom navigation
     * @param rootView The root view of the activity
     * @param bottomNavContainer The bottom navigation container view (can be null)
     * @param resources Resources instance for accessing dimension values
     * @param onBottomNavInsetsApplied Optional callback after bottom nav insets are applied
     */
    fun applyToActivity(
        rootView: View,
        bottomNavContainer: View?,
        resources: android.content.res.Resources,
        onBottomNavInsetsApplied: (() -> Unit)? = null
    ) {
        applyToRootLayout(rootView, resources)
        bottomNavContainer?.let {
            applyToBottomNavigation(it, onBottomNavInsetsApplied)
        }
    }
}
