package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HomeFragment : Fragment() {
    private lateinit var userPreferencesHelper: UserPreferencesHelper
    private lateinit var txtWelcome: TextView
    private var quickSnapSensorHelper: QuickSnapSensorHelper? = null
    
    // Learning activity views
    private lateinit var txtStreakDays: TextView
    private val activityDots = mutableListOf<View>()
    
    // Calendar view
    private lateinit var calendarView: CalendarView
    
    private val roomDb by lazy { AppDatabase.getInstance(requireContext()) }
    
    // Set of dates when quiz was completed (in milliseconds)
    private val completedDates = mutableSetOf<Long>()

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

        // Update welcome message with nickname
        updateWelcomeMessage()
        
        // Initialize learning activity views
        txtStreakDays = view.findViewById(R.id.txt_streak_days)
        
        // Initialize activity dots (7 days)
        val dotIds = arrayOf(
            R.id.dot_0, R.id.dot_1, R.id.dot_2, R.id.dot_3,
            R.id.dot_4, R.id.dot_5, R.id.dot_6
        )
        dotIds.forEach { dotId ->
            activityDots.add(view.findViewById(dotId))
        }
        
        // Initialize calendar view
        calendarView = view.findViewById(R.id.calendar_view)
        
        // Load learning data
        loadLearningData()

        val magicButton: LinearLayout = view.findViewById(R.id.btnMagicAdventure)
        magicButton.setOnClickListener {
            // Navigate to MagicAdventureFragment
            (activity as? MainActivity)?.navigateToFragment(
                MagicAdventureFragment(),
                "magic_adventure"
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
        txtWelcome.text = getString(R.string.welcome_back, nickname)
    }

    private fun loadLearningData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allPhotos = roomDb.photoLogDao().getAllPhotos()
                val allDetectedObjects = roomDb.detectedObjectDao().getAllDetectedObjects()
                
                // Calculate consecutive days
                val consecutiveDays = calculateConsecutiveDays(allPhotos)
                
                // Calculate last 7 days activity
                val last7DaysActivity = calculateLast7DaysActivity(allPhotos)
                
                // Calculate quiz completion dates
                val quizCompletionDates = calculateQuizCompletionDates(allDetectedObjects)
                
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    txtStreakDays.text = getString(R.string.streak_days_format, consecutiveDays)
                    updateActivityDots(last7DaysActivity)
                    updateCalendar(quizCompletionDates)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    txtStreakDays.text = getString(R.string.streak_days_format, 0)
                    updateActivityDots(List(7) { false })
                }
            }
        }
    }
    
    private fun calculateConsecutiveDays(photos: List<PhotoLog>): Int {
        if (photos.isEmpty()) return 0
        
        val learningDates = photos
            .filter { it.localImagePath != "firebase_sync" }
            .map { photo ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = photo.createdAt
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            }
            .distinct()
            .sortedDescending()
        
        if (learningDates.isEmpty()) return 0
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val todayMillis = today.timeInMillis
        val yesterdayMillis = yesterday.timeInMillis
        
        val mostRecent = learningDates.first()
        if (mostRecent != todayMillis && mostRecent != yesterdayMillis) {
            return 0
        }
        
        var streak = 0
        var currentDate = if (mostRecent == todayMillis) todayMillis else yesterdayMillis
        var dateIndex = 0
        
        while (dateIndex < learningDates.size) {
            val expectedDate = Calendar.getInstance().apply {
                timeInMillis = currentDate
            }
            val expectedMillis = expectedDate.timeInMillis
            
            if (learningDates[dateIndex] == expectedMillis) {
                streak++
                dateIndex++
                expectedDate.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = expectedDate.timeInMillis
            } else if (learningDates[dateIndex] > expectedMillis) {
                dateIndex++
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun calculateLast7DaysActivity(photos: List<PhotoLog>): List<Boolean> {
        val activity = mutableListOf<Boolean>()
        
        val learningDates = photos
            .filter { it.localImagePath != "firebase_sync" }
            .map { photo ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = photo.createdAt
                }
                cal.get(Calendar.YEAR) * 10000 + 
                (cal.get(Calendar.MONTH) + 1) * 100 + 
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .toSet()
        
        for (i in 6 downTo 0) {
            val checkDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateValue = checkDate.get(Calendar.YEAR) * 10000 + 
                           (checkDate.get(Calendar.MONTH) + 1) * 100 + 
                           checkDate.get(Calendar.DAY_OF_MONTH)
            
            activity.add(learningDates.contains(dateValue))
        }
        
        return activity
    }
    
    private fun updateActivityDots(last7DaysActivity: List<Boolean>) {
        for (i in 0 until minOf(7, activityDots.size)) {
            val hasLearned = if (i < last7DaysActivity.size) last7DaysActivity[i] else false
            val drawableRes = if (hasLearned) {
                R.drawable.bg_circle_green
            } else {
                R.drawable.bg_circle_red
            }
            activityDots[i].background = ContextCompat.getDrawable(requireContext(), drawableRes)
        }
    }
    
    private fun calculateQuizCompletionDates(objects: List<DetectedObject>): Set<Long> {
        val completionDates = mutableSetOf<Long>()
        val now = System.currentTimeMillis()
        
        objects.forEach { obj ->
            if (obj.lastStudied > 0 && obj.lastStudied <= now) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = obj.lastStudied
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                completionDates.add(calendar.timeInMillis)
            }
        }
        
        return completionDates
    }
    
    private fun updateCalendar(completionDates: Set<Long>) {
        if (!isAdded || !::calendarView.isInitialized) {
            return
        }
        
        completedDates.clear()
        completedDates.addAll(completionDates)
        
        val calendarDays = mutableListOf<CalendarDay>()
        val context = requireContext()
        
        completionDates.forEach { dateMillis ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val drawable = ContextCompat.getDrawable(context, R.drawable.bg_circle_green)
            val calendarDay = if (drawable != null) {
                try {
                    val day = CalendarDay(calendar)
                    try {
                    val iconDrawableField = CalendarDay::class.java.getDeclaredField("iconDrawable")
                    iconDrawableField.isAccessible = true
                    iconDrawableField.set(day, drawable)
                    } catch (e2: NoSuchFieldException) {
                    try {
                        val drawableField = CalendarDay::class.java.getDeclaredField("drawable")
                        drawableField.isAccessible = true
                        drawableField.set(day, drawable)
                        } catch (e3: NoSuchFieldException) {
                            try {
                                val labelDrawableField = CalendarDay::class.java.getDeclaredField("labelDrawable")
                                labelDrawableField.isAccessible = true
                                labelDrawableField.set(day, drawable)
                            } catch (e4: NoSuchFieldException) {
                                try {
                                    val imageDrawableField = CalendarDay::class.java.getDeclaredField("imageDrawable")
                                    imageDrawableField.isAccessible = true
                                    imageDrawableField.set(day, drawable)
                                } catch (e5: Exception) {}
                            } catch (e4: Exception) {}
                        } catch (e3: Exception) {}
                    } catch (e2: Exception) {}
                    day
                } catch (e: Exception) {
                    CalendarDay(calendar)
                }
            } else {
                CalendarDay(calendar)
            }
            calendarDays.add(calendarDay)
        }
        
        calendarView.setCalendarDays(calendarDays)
        
        calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
            override fun onClick(calendarDay: CalendarDay) {
                if (!isAdded) return
                
                val selectedDate = calendarDay.calendar
                selectedDate.set(Calendar.HOUR_OF_DAY, 0)
                selectedDate.set(Calendar.MINUTE, 0)
                selectedDate.set(Calendar.SECOND, 0)
                selectedDate.set(Calendar.MILLISECOND, 0)
                val selectedDateMillis = selectedDate.timeInMillis
                
                val isCompleted = completedDates.contains(selectedDateMillis)
                
                val message = if (isCompleted) {
                    "Learning completed on this day! ✨"
                } else {
                    "No learning completed on this day"
                }
                
                ToastHelper.showCenterToast(requireContext(), message)
            }
        })
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val oneYearAgo = Calendar.getInstance().apply {
            add(Calendar.YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        calendarView.setMinimumDate(oneYearAgo)
        calendarView.setMaximumDate(today)
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

