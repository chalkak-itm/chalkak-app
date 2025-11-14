package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {
    private var currentFragmentTag: String = "home"

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

        // Apply WindowInsets to bottom navigation bar
        val bottomNav = findViewById<android.view.View>(R.id.bottom_nav_include)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = systemBars.bottom
            v.layoutParams = layoutParams
            insets
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
            updateNavigationHighlight(currentFragmentTag)
        }
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
    }

    private fun setupBottomNavigation() {
        findViewById<TextView>(R.id.nav_home)?.setOnClickListener {
            navigateToFragment(HomeFragment(), "home")
        }
        findViewById<TextView>(R.id.nav_log)?.setOnClickListener {
            navigateToFragment(LogFragment(), "log")
        }
        findViewById<TextView>(R.id.nav_quiz)?.setOnClickListener {
            navigateToFragment(QuizFragment(), "quiz")
        }
        findViewById<TextView>(R.id.nav_setting)?.setOnClickListener {
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
        updateNavigationHighlight(tag)
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

    private fun updateNavigationHighlight(tag: String) {
        val activeColor = "#9810FA"
        val inactiveColor = "#99A1AF"

        findViewById<TextView>(R.id.nav_home)?.setTextColor(
            android.graphics.Color.parseColor(if (tag == "home") activeColor else inactiveColor)
        )
        findViewById<TextView>(R.id.nav_log)?.setTextColor(
            android.graphics.Color.parseColor(if (tag == "log") activeColor else inactiveColor)
        )
        findViewById<TextView>(R.id.nav_quiz)?.setTextColor(
            android.graphics.Color.parseColor(if (tag == "quiz") activeColor else inactiveColor)
        )
        findViewById<TextView>(R.id.nav_setting)?.setTextColor(
            android.graphics.Color.parseColor(if (tag == "setting") activeColor else inactiveColor)
        )
    }
}
