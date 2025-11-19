package com.example.chalkak

import android.content.Intent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/**
 * Helper class for managing bottom navigation
 * Reduces code duplication for navigation setup and highlighting
 */
class BottomNavigationHelper(
    private val activity: AppCompatActivity,
    private val navigationItems: List<NavigationItem>
) {
    data class NavigationItem(
        val tag: String,
        val layoutId: Int,
        val iconId: Int,
        val selectedBackground: Int,
        val unselectedBackground: Int
    )
    
    /**
     * Setup bottom navigation with click listeners
     * @param onItemClick Callback when item is clicked. If null, uses default Intent navigation
     */
    fun setupBottomNavigation(onItemClick: ((String) -> Unit)? = null) {
        navigationItems.forEach { item ->
            activity.findViewById<View>(item.layoutId)?.setOnClickListener {
                if (onItemClick != null) {
                    onItemClick(item.tag)
                } else {
                    // Default: Navigate using Intent
                    val intent = Intent(activity, MainActivity::class.java).apply {
                        putExtra("fragment_tag", item.tag)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
        }
    }
    
    /**
     * Update navigation highlight using background resources and elevation (MainActivity style)
     */
    fun updateNavigationHighlight(tag: String) {
        navigationItems.forEach { item ->
            val layout = activity.findViewById<View>(item.layoutId)
            val icon = activity.findViewById<ImageView>(item.iconId)
            
            if (tag == item.tag) {
                layout?.setBackgroundResource(item.selectedBackground)
                layout?.elevation = 4f
                icon?.alpha = 1.0f
            } else {
                layout?.setBackgroundResource(item.unselectedBackground)
                layout?.elevation = 0f
                icon?.alpha = 1.0f
            }
        }
    }
    
    /**
     * Update navigation highlight using alpha only (ImagePreviewActivity style)
     */
    fun updateNavigationHighlightAlpha(tag: String) {
        navigationItems.forEach { item ->
            val icon = activity.findViewById<ImageView>(item.iconId)
            icon?.alpha = if (tag == item.tag) 1.0f else 0.5f
        }
    }
    
    /**
     * Initialize navigation icons (clear color filters)
     */
    fun initializeNavigationIcons() {
        navigationItems.forEach { item ->
            activity.findViewById<ImageView>(item.iconId)?.clearColorFilter()
        }
    }
    
    companion object {
        /**
         * Create default navigation items for the app
         */
        fun createDefaultItems(): List<NavigationItem> {
            return listOf(
                NavigationItem(
                    tag = "home",
                    layoutId = R.id.nav_home,
                    iconId = R.id.nav_home_icon,
                    selectedBackground = R.drawable.bg_nav_item_selected,
                    unselectedBackground = R.drawable.bg_nav_item_unselected
                ),
                NavigationItem(
                    tag = "log",
                    layoutId = R.id.nav_log,
                    iconId = R.id.nav_log_icon,
                    selectedBackground = R.drawable.bg_nav_item_selected,
                    unselectedBackground = R.drawable.bg_nav_item_unselected
                ),
                NavigationItem(
                    tag = "quiz",
                    layoutId = R.id.nav_quiz,
                    iconId = R.id.nav_quiz_icon,
                    selectedBackground = R.drawable.bg_nav_item_selected,
                    unselectedBackground = R.drawable.bg_nav_item_unselected
                ),
                NavigationItem(
                    tag = "setting",
                    layoutId = R.id.nav_setting,
                    iconId = R.id.nav_setting_icon,
                    selectedBackground = R.drawable.bg_nav_item_selected,
                    unselectedBackground = R.drawable.bg_nav_item_unselected
                )
            )
        }
    }
}

