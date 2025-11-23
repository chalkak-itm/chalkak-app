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
    private lateinit var txtProgressLabel: TextView
    private lateinit var progressWords: ProgressBar
    private var quickSnapSensorHelper: QuickSnapSensorHelper? = null

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

        // Initialize progress views
        txtProgressLabel = view.findViewById(R.id.txtProgressLabel)
        progressWords = view.findViewById(R.id.progressWords)

        // Update welcome message with nickname
        updateWelcomeMessage()
        
        // Update progress bar
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

        // Initialize Quick Snap Sensor Helper
        quickSnapSensorHelper = QuickSnapSensorHelper(
            context = requireContext(),
            onShakeDetected = {
                goToCameraDirect()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // Update welcome message when fragment resumes (in case nickname was changed)
        updateWelcomeMessage()
        
        // Enable quick snap sensor if setting is enabled
        if (userPreferencesHelper.isQuickSnapEnabled()) {
            quickSnapSensorHelper?.enable()
        } else {
            quickSnapSensorHelper?.disable()
        }
    }

    override fun onPause() {
        super.onPause()
        // Disable sensor listener
        quickSnapSensorHelper?.disable()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup sensor helper
        quickSnapSensorHelper?.cleanup()
        quickSnapSensorHelper = null
    }

    private fun updateWelcomeMessage() {
        val nickname = userPreferencesHelper.getNickname()
        txtWelcome.text = "Welcome back,\n$nickname"
    }

    private fun updateProgressBar() {
        // TODO: Replace with actual data from database
        // For now, using placeholder values
        val wordsLearned = 8
        val wordsTotal = 12
        val progressPercent = (wordsLearned * 100) / wordsTotal
        
        txtProgressLabel.text = "$wordsLearned / $wordsTotal words learned today!"
        progressWords.progress = progressPercent
    }

    private fun goToCameraDirect() {
        // Check if quick snap is enabled
        if (!userPreferencesHelper.isQuickSnapEnabled()) return

        // Show toast notification
        ToastHelper.showCenterToast(requireContext(), "Quick snap detected! Starting Magic Adventure...")

        // Navigate to Magic Adventure with auto-launch camera
        (activity as? MainActivity)?.navigateToFragment(
            MagicAdventureFragment.newInstance(autoLaunchCamera = true),
            "magic_adventure"
        )
    }



}

