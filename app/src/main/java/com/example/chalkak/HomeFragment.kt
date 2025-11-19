package com.example.chalkak

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var txtWelcome: TextView
    private var shakeEnabled = false
    private lateinit var layoutShakeIcon: LinearLayout
    private lateinit var imgShakeToggle: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Initialize views
        txtWelcome = view.findViewById(R.id.txtWelcome)

        // Initialize shaking icon views
        layoutShakeIcon = view.findViewById(R.id.layoutShakeIcon)
        imgShakeToggle = view.findViewById(R.id.imgShakeToggle)

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
        // Shaking ON/OFF Button action
        layoutShakeIcon.setOnClickListener {
            shakeEnabled = !shakeEnabled  // Toggle

            if (shakeEnabled) {
                // ON
                layoutShakeIcon.setBackgroundResource(R.drawable.bg_shake_icon_on)

                showCenterToast("\uD83D\uDD25Shake-to-start feature is now enabled!")

            } else {
                // OFF
                layoutShakeIcon.setBackgroundResource(R.drawable.bg_shake_icon_off)

                showCenterToast("Shake-to-start feature is now disabled.")

            }

        }

    }

    override fun onResume() {
        super.onResume()
        // Update welcome message when fragment resumes (in case nickname was changed)
        updateWelcomeMessage()
    }

    private fun updateWelcomeMessage() {
        val user = auth.currentUser
        val nickname = if (user != null) {
            // Get nickname from SharedPreferences, or use displayName as fallback
            sharedPreferences.getString("user_nickname", null) 
                ?: user.displayName 
                ?: "User"
        } else {
            "Guest"
        }
        
        txtWelcome.text = "Welcome back,\n$nickname"
    }
    //function for setting the toast view
    private fun showCenterToast(message: String) {
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.custom_toast, null)

        val txt = view.findViewById<TextView>(R.id.txtToastMessage)
        txt.text = message

        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_SHORT
        toast.view = view
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

