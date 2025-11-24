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
import com.applandeo.materialcalendarview.EventDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class QuizFragment : Fragment() {
    // Main quiz card views
    private lateinit var txtWordCount: TextView
    private lateinit var txtAccuracyRate: TextView
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
        txtAccuracyRate = view.findViewById(R.id.txt_accuracy_rate)
        txtTotalLearning = view.findViewById(R.id.txt_total_learning)
        txtConsecutiveDays = view.findViewById(R.id.txt_consecutive_days)
        txtQuizSubtitle = view.findViewById(R.id.txt_quiz_subtitle)
        btnStartQuiz = view.findViewById(R.id.btn_start_quiz)
        
        // Initialize learning activity views
        txtStreakDays = view.findViewById(R.id.txt_streak_days)
        
        // Initialize activity dots (7 days)
        for (i in 0..6) {
            val dotId = resources.getIdentifier("dot_$i", "id", requireContext().packageName)
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
                val accuracyRate = calculateAccuracyRate(allDetectedObjects)
                
                // 6. Quiz Completion Dates: Get dates when quiz was completed (lastStudied dates)
                val quizCompletionDates = calculateQuizCompletionDates(allDetectedObjects)
                
                // Update UI on Main thread
                withContext(Dispatchers.Main) {
                    txtWordCount.text = quizAvailableWords.toString()
                    txtAccuracyRate.text = "$accuracyRate%"
                    txtTotalLearning.text = totalUniqueWords.toString()
                    txtConsecutiveDays.text = "$consecutiveDays days"
                    txtQuizSubtitle.text = "Review $quizAvailableWords magic spells"
                    txtStreakDays.text = "$consecutiveDays days streak"
                    
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
                    txtAccuracyRate.text = "0%"
                    txtTotalLearning.text = "0"
                    txtConsecutiveDays.text = "0 days"
                    txtQuizSubtitle.text = "Review 0 magic spells"
                    txtStreakDays.text = "0 days streak"
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
        val calendar = Calendar.getInstance()
        
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
     */
    private fun calculateQuizCompletionDates(objects: List<DetectedObject>): Set<Long> {
        val completionDates = mutableSetOf<Long>()
        
        // Get unique dates from lastStudied timestamps
        objects.forEach { obj ->
            if (obj.lastStudied > 0) {
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
        
        // Create events for completed dates
        val events = mutableListOf<EventDay>()
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
            
            // Create event with green circle drawable for completed dates
            val drawable = ContextCompat.getDrawable(context, R.drawable.bg_circle_green)
            if (drawable != null) {
                val event = EventDay(calendar, drawable)
                events.add(event)
            }
        }
        
        // Set events to calendar view
        calendarView.setEvents(events)
        
        // Set up calendar date click listener
        calendarView.setOnDayClickListener(object : com.applandeo.materialcalendarview.listeners.OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                if (!isAdded) return
                
                val selectedDate = eventDay.calendar
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