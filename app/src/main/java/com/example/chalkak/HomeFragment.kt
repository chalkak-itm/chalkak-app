package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    private lateinit var userPreferencesHelper: UserPreferencesHelper
    private lateinit var txtWelcome: TextView
    private lateinit var progressWords: ProgressBar
    private lateinit var txtProgressLabel: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UserPreferencesHelper
        userPreferencesHelper = UserPreferencesHelper(requireContext())

        // Initialize header views from included layout
        val headerView = view.findViewById<View>(R.id.header_common)
        val imgMascot = headerView.findViewById<ImageView>(R.id.img_header_mascot)
        txtWelcome = headerView.findViewById<TextView>(R.id.txt_header_title)
        imgMascot.setImageResource(R.drawable.main_character)
        
        // Set larger size for home screen mascot (220dp)
        val sizeInPx = (220 * resources.displayMetrics.density).toInt()
        imgMascot.layoutParams.width = sizeInPx
        imgMascot.layoutParams.height = sizeInPx

        // Initialize views
        progressWords = view.findViewById(R.id.progressWords)
        txtProgressLabel = view.findViewById(R.id.txtProgressLabel)

        // Update welcome message with nickname
        updateWelcomeMessage()
        
        // Update progress bar based on label text
        updateProgressBar()

        val magicButton: LinearLayout = view.findViewById(R.id.btnMagicAdventure)
        magicButton.setOnClickListener {
            // Navigate to MagicAdventureFragment
            (activity as? MainActivity)?.navigateToFragment(
                MagicAdventureFragment(),
                "magic_adventure"
            )
        }

        val reviewProgressButton: LinearLayout = view.findViewById(R.id.btnReviewProgress)
        reviewProgressButton.setOnClickListener {
            // Navigate to QuizFragment
            (activity as? MainActivity)?.navigateToFragment(
                QuizFragment(),
                "quiz"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Update welcome message when fragment resumes (in case nickname was changed)
        updateWelcomeMessage()
    }

    private fun updateWelcomeMessage() {
        val nickname = userPreferencesHelper.getNickname()
        txtWelcome.text = "Welcome back,\n$nickname"
    }
    
    private fun updateProgressBar() {
        // Parse progress from label text (e.g., "8 / 12 words learned today!")
        val labelText = txtProgressLabel.text.toString()
        val regex = Regex("(\\d+)\\s*/\\s*(\\d+)")
        val matchResult = regex.find(labelText)
        
        if (matchResult != null) {
            val current = matchResult.groupValues[1].toIntOrNull() ?: 0
            val total = matchResult.groupValues[2].toIntOrNull() ?: 1
            
            if (total > 0) {
                val percentage = (current * 100) / total
                progressWords.progress = percentage
            }
        }
    }
}

