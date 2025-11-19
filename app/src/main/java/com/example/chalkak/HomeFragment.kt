package com.example.chalkak

import android.content.Context
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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlin.math.sqrt

class HomeFragment : Fragment(), SensorEventListener {
    private lateinit var userPreferencesHelper: UserPreferencesHelper
    private lateinit var txtWelcome: TextView
    private lateinit var txtProgressLabel: TextView
    private lateinit var progressWords: ProgressBar
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

        // Initialize shaking icon views
        layoutShakeIcon = view.findViewById(R.id.layoutShakeIcon)
        imgShakeToggle = view.findViewById(R.id.imgShakeToggle)

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

