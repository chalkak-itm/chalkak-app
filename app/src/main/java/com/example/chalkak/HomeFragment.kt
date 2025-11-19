package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    private lateinit var userPreferencesHelper: UserPreferencesHelper
    private lateinit var txtWelcome: TextView

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

        // Initialize views
        txtWelcome = view.findViewById(R.id.txtWelcome)

        // Update welcome message with nickname
        updateWelcomeMessage()

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
}

