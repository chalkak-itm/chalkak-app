package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {
    private var currentFragmentTag: String = "home"
    private var currentMainNavigationTag: String = "home" // Track main navigation fragment
    private var backPressedTime: Long = 0
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val backPressRunnable = Runnable { backPressedTime = 0 }
    private var bottomNavContainer: View? = null
    
    companion object {
        private val MAIN_NAVIGATION_TAGS = setOf("home", "log", "quiz", "setting")
    }
    
    fun getCurrentMainNavigationTag(): String {
        return currentMainNavigationTag
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Setup bottom navigation
        setupBottomNavigation()

        // Apply WindowInsets to root layout
        val root = findViewById<android.view.View>(R.id.main_container)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Apply WindowInsets to bottom navigation bar container
        bottomNavContainer = findViewById(R.id.bottom_nav_container)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            
            // Apply bottom padding to fragment container based on bottom nav height
            applyBottomPaddingToFragments()
            
            insets
        }
        
        // Initial padding application after layout
        bottomNavContainer?.post {
            applyBottomPaddingToFragments()
        }

        // Handle Intent to navigate to specific fragment
        val fragmentTag = intent.getStringExtra("fragment_tag")
        if (savedInstanceState == null) {
            if (fragmentTag != null) {
                navigateToFragmentByTag(fragmentTag)
            } else {
                navigateToFragment(HomeFragment(), "home")
            }
        } else {
            // Restore current fragment tag
            currentFragmentTag = savedInstanceState.getString("current_fragment_tag", "home")
            currentMainNavigationTag = savedInstanceState.getString("current_main_navigation_tag", "home")
            updateNavigationHighlight(currentMainNavigationTag)
        }

        // Setup back press handler for double tap to exit
        setupBackPressHandler()

        // Initialize navigation icons with default color
        initializeNavigationIcons()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val fragmentTag = intent.getStringExtra("fragment_tag")
        if (fragmentTag != null) {
            navigateToFragmentByTag(fragmentTag)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_fragment_tag", currentFragmentTag)
        outState.putString("current_main_navigation_tag", currentMainNavigationTag)
    }

    private fun setupBottomNavigation() {
        findViewById<android.view.View>(R.id.nav_home)?.setOnClickListener {
            navigateToFragment(HomeFragment(), "home")
        }
        findViewById<android.view.View>(R.id.nav_log)?.setOnClickListener {
            navigateToFragment(LogFragment(), "log")
        }
        findViewById<android.view.View>(R.id.nav_quiz)?.setOnClickListener {
            navigateToFragment(QuizFragment(), "quiz")
        }
        findViewById<android.view.View>(R.id.nav_setting)?.setOnClickListener {
            navigateToFragment(SettingFragment(), "setting")
        }
    }

    fun navigateToFragment(fragment: Fragment, tag: String) {
        // Clear back stack and replace with new fragment
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
        
        currentFragmentTag = tag
        
        // Only update navigation highlight if navigating to a main navigation fragment
        if (tag in MAIN_NAVIGATION_TAGS) {
            currentMainNavigationTag = tag
            updateNavigationHighlight(tag)
        } else {
            // Keep the previous main navigation highlight
            updateNavigationHighlight(currentMainNavigationTag)
        }
        
        // Apply bottom padding after fragment is added
        supportFragmentManager.executePendingTransactions()
        applyBottomPaddingToFragments()
    }

    private fun navigateToFragmentByTag(tag: String) {
        val fragment = when (tag) {
            "home" -> HomeFragment()
            "log" -> LogFragment()
            "quiz" -> QuizFragment()
            "setting" -> SettingFragment()
            else -> HomeFragment()
        }
        navigateToFragment(fragment, tag)
    }

    private fun initializeNavigationIcons() {
        // Clear any color filters to show original icon colors
        findViewById<android.widget.ImageView>(R.id.nav_home_icon)?.clearColorFilter()
        findViewById<android.widget.ImageView>(R.id.nav_log_icon)?.clearColorFilter()
        findViewById<android.widget.ImageView>(R.id.nav_quiz_icon)?.clearColorFilter()
        findViewById<android.widget.ImageView>(R.id.nav_setting_icon)?.clearColorFilter()
    }

    private fun updateNavigationHighlight(tag: String) {
        // Update home
        val homeLayout = findViewById<android.view.View>(R.id.nav_home)
        val homeIcon = findViewById<android.widget.ImageView>(R.id.nav_home_icon)
        if (tag == "home") {
            homeLayout?.setBackgroundResource(R.drawable.bg_nav_item_selected)
            homeLayout?.elevation = 4f
            homeIcon?.alpha = 1.0f
        } else {
            homeLayout?.setBackgroundResource(R.drawable.bg_nav_item_unselected)
            homeLayout?.elevation = 0f
            homeIcon?.alpha = 1.0f
        }

        // Update log
        val logLayout = findViewById<android.view.View>(R.id.nav_log)
        val logIcon = findViewById<android.widget.ImageView>(R.id.nav_log_icon)
        if (tag == "log") {
            logLayout?.setBackgroundResource(R.drawable.bg_nav_item_selected)
            logLayout?.elevation = 4f
            logIcon?.alpha = 1.0f
        } else {
            logLayout?.setBackgroundResource(R.drawable.bg_nav_item_unselected)
            logLayout?.elevation = 0f
            logIcon?.alpha = 1.0f
        }

        // Update quiz
        val quizLayout = findViewById<android.view.View>(R.id.nav_quiz)
        val quizIcon = findViewById<android.widget.ImageView>(R.id.nav_quiz_icon)
        if (tag == "quiz") {
            quizLayout?.setBackgroundResource(R.drawable.bg_nav_item_selected)
            quizLayout?.elevation = 4f
            quizIcon?.alpha = 1.0f
        } else {
            quizLayout?.setBackgroundResource(R.drawable.bg_nav_item_unselected)
            quizLayout?.elevation = 0f
            quizIcon?.alpha = 1.0f
        }

        // Update setting
        val settingLayout = findViewById<android.view.View>(R.id.nav_setting)
        val settingIcon = findViewById<android.widget.ImageView>(R.id.nav_setting_icon)
        if (tag == "setting") {
            settingLayout?.setBackgroundResource(R.drawable.bg_nav_item_selected)
            settingLayout?.elevation = 4f
            settingIcon?.alpha = 1.0f
        } else {
            settingLayout?.setBackgroundResource(R.drawable.bg_nav_item_unselected)
            settingLayout?.elevation = 0f
            settingIcon?.alpha = 1.0f
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if current fragment is one of the main navigation screens
                val isMainScreen = currentFragmentTag in MAIN_NAVIGATION_TAGS
                
                if (isMainScreen) {
                    // Double tap to exit
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - backPressedTime > 500) {
                        // First back press
                        backPressedTime = currentTime
                        Toast.makeText(
                            this@MainActivity,
                            "한 번 더 누르면 종료됩니다",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Reset after 0.5 seconds
                        backPressHandler.removeCallbacks(backPressRunnable)
                        backPressHandler.postDelayed(backPressRunnable, 500)
                    } else {
                        // Second back press within 0.5 seconds - exit app
                        backPressHandler.removeCallbacks(backPressRunnable)
                        finish()
                    }
                } else {
                    // Not on main screen - navigate to home
                    navigateToFragment(HomeFragment(), "home")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressHandler.removeCallbacks(backPressRunnable)
    }
    
    private fun applyBottomPaddingToFragments() {
        bottomNavContainer?.let { navContainer ->
            val bottomNavHeight = navContainer.height
            val systemBars = ViewCompat.getRootWindowInsets(navContainer)?.getInsets(WindowInsetsCompat.Type.systemBars())
            val totalBottomPadding = bottomNavHeight + (systemBars?.bottom ?: 0) + 16 // 16dp extra space
            
            val fragmentContainer = findViewById<View>(R.id.fragment_container)
            fragmentContainer?.post {
                applyPaddingToScrollableViews(fragmentContainer, totalBottomPadding)
            }
        }
    }
    
    private fun applyPaddingToScrollableViews(parent: View, padding: Int) {
        when (parent) {
            is androidx.recyclerview.widget.RecyclerView -> {
                // Log screen RecyclerView needs less padding
                val isLogRecyclerView = parent.id == R.id.recyclerLog
                val actualPadding = if (isLogRecyclerView) {
                    // For log screen, use reduced padding (about 75% of full padding)
                    (padding * 0.75).toInt()
                } else {
                    padding
                }
                parent.setPadding(
                    parent.paddingLeft,
                    parent.paddingTop,
                    parent.paddingRight,
                    actualPadding
                )
                return // Don't recurse into RecyclerView children
            }
            is android.widget.ScrollView -> {
                if (parent.childCount > 0) {
                    val child = parent.getChildAt(0)
                    // Setting screen ScrollView needs less padding
                    val isSettingScrollView = parent.id == R.id.scroll_setting
                    val actualPadding = if (isSettingScrollView) {
                        // For setting screen, use minimal padding
                        (padding * 0.8).toInt() // Reduce padding by 60%
                    } else {
                        padding
                    }
                    child.setPadding(
                        child.paddingLeft,
                        child.paddingTop,
                        child.paddingRight,
                        actualPadding
                    )
                }
                return // Don't recurse into ScrollView children
            }
        }
        
        // For other views, also check if they need padding (like HomeFragment's spacer View)
        if (parent is android.view.ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                // Apply padding to spacer views in HomeFragment
                if (child is View && child.layoutParams is android.widget.LinearLayout.LayoutParams) {
                    val params = child.layoutParams as android.widget.LinearLayout.LayoutParams
                    if (params.height == 0 && params.weight == 0f) {
                        params.height = padding
                        child.layoutParams = params
                    }
                }
                applyPaddingToScrollableViews(child, padding)
            }
        }
    }
}
