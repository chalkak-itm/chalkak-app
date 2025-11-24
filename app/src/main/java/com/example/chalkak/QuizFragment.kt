package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.io.File
import java.util.Calendar

class QuizFragment : Fragment() {
    // Main quiz card views
    private lateinit var txtWordCount: TextView
    private lateinit var txtReviewRate: TextView
    private lateinit var txtTotalLearning: TextView
    private lateinit var txtConsecutiveDays: TextView
    private lateinit var txtQuizSubtitle: TextView
    private lateinit var btnStartQuiz: View
    
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
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize main quiz card views
        txtWordCount = view.findViewById(R.id.txt_word_count)
        txtReviewRate = view.findViewById(R.id.txt_accuracy_rate)
        txtTotalLearning = view.findViewById(R.id.txt_total_learning)
        txtConsecutiveDays = view.findViewById(R.id.txt_consecutive_days)
        txtQuizSubtitle = view.findViewById(R.id.txt_quiz_subtitle)
        btnStartQuiz = view.findViewById(R.id.btn_start_quiz)
        
        // Initialize learning activity views
        txtStreakDays = view.findViewById(R.id.txt_streak_days)
        
        // Initialize activity dots (7 days) - use direct resource IDs instead of reflection
        val dotIds = arrayOf(
            R.id.dot_0, R.id.dot_1, R.id.dot_2, R.id.dot_3,
            R.id.dot_4, R.id.dot_5, R.id.dot_6
        )
        dotIds.forEach { dotId ->
            activityDots.add(view.findViewById(dotId))
        }
        
        // Initialize calendar view
        calendarView = view.findViewById(R.id.calendar_view)

        // Load quiz data from database
        loadQuizData()

        // Setup start quiz button click listener
        btnStartQuiz.setOnClickListener {
            (activity as? MainActivity)?.navigateToFragment(QuizQuestionFragment(), "quiz_question")
        }
    }

    private fun loadQuizData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Load all data from database
                val allPhotos = roomDb.photoLogDao().getAllPhotos()
                val allDetectedObjects = roomDb.detectedObjectDao().getAllDetectedObjects()
                
                // Calculate statistics
                val photosMap = allPhotos.associateBy { it.photoId }
                
                // 1. Word Count: Number of words available for quiz (with valid photo files)
                val quizAvailableWords = allDetectedObjects
                    .filter { 
                        it.koreanMeaning.isNotEmpty() && 
                        it.koreanMeaning != "Searching..." &&
                        photosMap.containsKey(it.parentPhotoId) &&
                        photosMap[it.parentPhotoId]?.localImagePath != null &&
                        File(photosMap[it.parentPhotoId]!!.localImagePath).exists()
                    }
                    .map { it.englishWord.lowercase() }
                    .distinct()
                    .count()
                
                // 2. Total Learning: Total unique words learned
                val totalUniqueWords = allDetectedObjects
                    .filter { it.koreanMeaning.isNotEmpty() && it.koreanMeaning != "Searching..." }
                    .map { it.englishWord.lowercase() }
                    .distinct()
                    .count()
                
                // 3. Consecutive Days: Calculate streak from PhotoLog
                val consecutiveDays = calculateConsecutiveDays(allPhotos)
                
                // 4. Activity Dots: Last 7 days learning activity
                val last7DaysActivity = calculateLast7DaysActivity(allPhotos)
                
                // 5. Accuracy Rate: Currently not tracked, show placeholder or calculate from lastStudied
                // For now, we'll show a placeholder or calculate based on recent study activity
                val reviewRate = calculateAccuracyRate(allDetectedObjects)
                
                // 6. Quiz Completion Dates: Get dates when quiz was completed (lastStudied dates)
                val quizCompletionDates = calculateQuizCompletionDates(allDetectedObjects)
                
                // Update UI on Main thread
                withContext(Dispatchers.Main) {
                    txtWordCount.text = quizAvailableWords.toString()
                    txtAccuracyRate.text = getString(R.string.accuracy_rate_format, accuracyRate)
                    txtTotalLearning.text = totalUniqueWords.toString()
                    txtConsecutiveDays.text = getString(R.string.consecutive_days_format, consecutiveDays)
                    txtQuizSubtitle.text = getString(R.string.quiz_subtitle_format, quizAvailableWords)
                    txtStreakDays.text = getString(R.string.streak_days_format, consecutiveDays)
                    
                    // Update activity dots
                    updateActivityDots(last7DaysActivity)
                    
                    // Update calendar with completion dates
                    updateCalendar(quizCompletionDates)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // On error, show default values
                withContext(Dispatchers.Main) {
                    txtWordCount.text = "0"
                    txtAccuracyRate.text = getString(R.string.accuracy_rate_format, 0)
                    txtTotalLearning.text = "0"
                    txtConsecutiveDays.text = getString(R.string.consecutive_days_format, 0)
                    txtQuizSubtitle.text = getString(R.string.quiz_subtitle_format, 0)
                    txtStreakDays.text = getString(R.string.streak_days_format, 0)
                    updateActivityDots(List(7) { false })
                }
            }
        }
    }
    
    private fun calculateConsecutiveDays(photos: List<PhotoLog>): Int {
        if (photos.isEmpty()) return 0
        
        // Get unique dates from photos (excluding firebase_sync)
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
        
        // Start from today or yesterday (allow 1 day gap)
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
        
        // If most recent learning is not today or yesterday, streak is 0
        val mostRecent = learningDates.first()
        if (mostRecent != todayMillis && mostRecent != yesterdayMillis) {
            return 0
        }
        
        // Calculate consecutive days backwards
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
                // Move to previous day
                expectedDate.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = expectedDate.timeInMillis
            } else if (learningDates[dateIndex] > expectedMillis) {
                // Skip dates that are in the future (shouldn't happen, but handle it)
                dateIndex++
            } else {
                // Gap found, break streak
                break
            }
        }
        
        return streak
    }
    
    private fun calculateLast7DaysActivity(photos: List<PhotoLog>): List<Boolean> {
        val activity = mutableListOf<Boolean>()
        
        // Get unique dates from photos (excluding firebase_sync)
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
        
        // Check last 7 days (from 6 days ago to today)
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
    
    private fun calculateAccuracyRate(objects: List<DetectedObject>): Int {
        // Since we don't track quiz accuracy, we'll calculate based on recent study activity
        // Words studied recently (within last 7 days) are considered "active"
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        
        val recentlyStudied = objects.count { it.lastStudied >= sevenDaysAgo }
        val totalWithMeaning = objects.count { 
            it.koreanMeaning.isNotEmpty() && it.koreanMeaning != "Searching..." 
        }
        
        return if (totalWithMeaning > 0) {
            // Calculate percentage of words studied recently
            // This gives an indication of learning activity
            (recentlyStudied * 100 / totalWithMeaning).coerceIn(0, 100)
        } else {
            0
        }
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
    
    /**
     * Calculate dates when quiz was completed
     * Uses lastStudied field from DetectedObject to determine completion dates
     * Only includes dates where quiz was actually completed (lastStudied was updated after object creation)
     */
    private fun calculateQuizCompletionDates(objects: List<DetectedObject>): Set<Long> {
        val completionDates = mutableSetOf<Long>()
        val now = System.currentTimeMillis()
        
        // Get unique dates from lastStudied timestamps
        // Only include dates where lastStudied was explicitly set (not default value)
        // We consider a date valid if lastStudied > 0 and it's not in the future
        objects.forEach { obj ->
            if (obj.lastStudied > 0 && obj.lastStudied <= now) {
                // Convert timestamp to date (midnight)
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
    
    /**
     * Update calendar view with quiz completion dates
     * Uses Material CalendarView to highlight dates with quiz completion
     */
    private fun updateCalendar(completionDates: Set<Long>) {
        if (!isAdded || !::calendarView.isInitialized) {
            return // Fragment not attached or view not initialized
        }
        
        completedDates.clear()
        completedDates.addAll(completionDates)
        
        // Create calendar days for completed dates
        val calendarDays = mutableListOf<CalendarDay>()
        val context = requireContext()
        
        completionDates.forEach { dateMillis ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                // Normalize to midnight for consistent comparison
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // Create calendar day with green circle drawable for completed dates
            val drawable = ContextCompat.getDrawable(context, R.drawable.bg_circle_green)
            val calendarDay = if (drawable != null) {
                // Material CalendarView 1.9.0 only supports CalendarDay(Calendar) constructor
                // Use reflection to set drawable property
                try {
                    val day = CalendarDay(calendar)
                    // Try to set iconDrawable property (most common field name)
                    try {
                        val iconDrawableField = CalendarDay::class.java.getDeclaredField("iconDrawable")
                        iconDrawableField.isAccessible = true
                        iconDrawableField.set(day, drawable)
                    } catch (e2: NoSuchFieldException) {
                        // Try drawable field
                        try {
                            val drawableField = CalendarDay::class.java.getDeclaredField("drawable")
                            drawableField.isAccessible = true
                            drawableField.set(day, drawable)
                        } catch (e3: NoSuchFieldException) {
                            // Try labelDrawable
                            try {
                                val labelDrawableField = CalendarDay::class.java.getDeclaredField("labelDrawable")
                                labelDrawableField.isAccessible = true
                                labelDrawableField.set(day, drawable)
                            } catch (e4: NoSuchFieldException) {
                                // Try imageDrawable
                                try {
                                    val imageDrawableField = CalendarDay::class.java.getDeclaredField("imageDrawable")
                                    imageDrawableField.isAccessible = true
                                    imageDrawableField.set(day, drawable)
                                } catch (e5: Exception) {
                                    // All reflection attempts failed, use day without drawable
                                }
                            } catch (e4: Exception) {
                                // Reflection failed
                            }
                        } catch (e3: Exception) {
                            // Reflection failed
                        }
                    } catch (e2: Exception) {
                        // Reflection failed
                    }
                    day
                } catch (e: Exception) {
                    // Fallback: create without drawable
                    CalendarDay(calendar)
                }
            } else {
                CalendarDay(calendar)
            }
            calendarDays.add(calendarDay)
        }
        
        // Set calendar days to calendar view
        calendarView.setCalendarDays(calendarDays)
        
        // Set up calendar date click listener
        calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
            override fun onClick(calendarDay: CalendarDay) {
                if (!isAdded) return
                
                val selectedDate = calendarDay.calendar
                // Normalize selected date to midnight for comparison
                selectedDate.set(Calendar.HOUR_OF_DAY, 0)
                selectedDate.set(Calendar.MINUTE, 0)
                selectedDate.set(Calendar.SECOND, 0)
                selectedDate.set(Calendar.MILLISECOND, 0)
                val selectedDateMillis = selectedDate.timeInMillis
                
                val isCompleted = completedDates.contains(selectedDateMillis)
                
                // Show toast with completion status using ToastHelper
                val message = if (isCompleted) {
                    "Quiz completed on this day! ✨"
                } else {
                    "No quiz completed on this day"
                }
                
                ToastHelper.showCenterToast(requireContext(), message)
            }
        })
        
        // Set minimum date to 1 year ago and maximum to today
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
}