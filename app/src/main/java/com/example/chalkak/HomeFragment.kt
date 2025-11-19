package com.example.chalkak

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.sqrt

class HomeFragment : Fragment(), SensorEventListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var txtWelcome: TextView
    private var shakeEnabled = false
    private lateinit var layoutShakeIcon: LinearLayout
    private lateinit var imgShakeToggle: ImageView

    // Shaking sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Shaking sensibility setting
    private val shakeThresholdGravity = 4.3f   // How strongly shaking
    private val shakeSlopTimeMs = 800         // detect term
    private var lastShakeTime: Long = 0L

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

        // Sensor Manager / Speed Sensor Initializing
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    }

    override fun onResume() {
        super.onResume()
        // Update welcome message when fragment resumes (in case nickname was changed)
        updateWelcomeMessage()

        // register sensor listener
        accelerometer?.also { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Sensor listener drop
        sensorManager.unregisterListener(this)
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        // if OFF --> directly return
        if (!shakeEnabled) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > shakeThresholdGravity) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime < shakeSlopTimeMs) {
                // prevent the too many action
                return
            }
            lastShakeTime = now

            // move to camera action
            goToCameraDirect()
        }
    }

    private fun goToCameraDirect() {
        // only for shake-on mode
        if (!shakeEnabled) return

        // first show toast in home
        showCenterToast("Shaking detected! Starting Magic Adventure...")

        (activity as? MainActivity)?.navigateToFragment(
            MagicAdventureFragment.newInstance(autoLaunchCamera = true),
            "magic_adventure"
        )
    }



}

