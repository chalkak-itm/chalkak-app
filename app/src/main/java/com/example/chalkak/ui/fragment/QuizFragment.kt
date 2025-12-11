package com.example.chalkak.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.chalkak.R
import com.example.chalkak.data.local.AppDatabase
import com.example.chalkak.data.local.DetectedObject
import com.example.chalkak.data.local.PhotoLog
import com.example.chalkak.ui.activity.MainActivity
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
    
    private val roomDb by lazy { AppDatabase.getInstance(requireContext()) }
    
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
                
                // 2. Total Learning: Recently studied words (within last 7 days)
                val now = System.currentTimeMillis()
                val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
                val recentlyStudiedCount = allDetectedObjects
                    .filter { 
                        it.koreanMeaning.isNotEmpty() && 
                        it.koreanMeaning != "Searching..." &&
                        it.lastStudied >= sevenDaysAgo
                    }
                    .map { it.englishWord.lowercase() }
                    .distinct()
                    .count()
                
                // 3. Consecutive Days: Calculate streak from PhotoLog
                val consecutiveDays = calculateConsecutiveDays(allPhotos)
                
                // 4. Activity Dots: Last 7 days learning activity
                val last7DaysActivity = calculateLast7DaysActivity(allPhotos)
                
                // 5. Review Rate: Calculate based on photos studied vs total photos in last 7 days
                val accuracyRate = calculateAccuracyRate(allPhotos, allDetectedObjects)
                
                // Update UI on Main thread
                withContext(Dispatchers.Main) {
                    txtWordCount.text = quizAvailableWords.toString()
                    txtAccuracyRate.text = getString(R.string.accuracy_rate_format, accuracyRate)
                    txtTotalLearning.text = recentlyStudiedCount.toString()
                    txtConsecutiveDays.text = getString(R.string.consecutive_days_format, consecutiveDays)
                    txtQuizSubtitle.text = getString(R.string.quiz_subtitle_format, quizAvailableWords)
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
    
    /**
     * Calculate Review Rate
     * Denominator: Number of photos taken in the last 7 days (from 7 days ago to now)
     * Numerator: Number of photos that have been studied (lastStudied >= sevenDaysAgo)
     * 
     * When quiz is answered correctly, the current quiz time is saved to lastStudied
     * So a photo is considered "studied" if at least one DetectedObject has lastStudied >= sevenDaysAgo
     */
    private fun calculateAccuracyRate(photos: List<PhotoLog>, objects: List<DetectedObject>): Int {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        
        // Denominator: Photos taken in the last 7 days (from 7 days ago to now)
        val recentPhotos = photos.filter { it.createdAt >= sevenDaysAgo }
        val totalRecentPhotos = recentPhotos.size
        
        if (totalRecentPhotos == 0) {
            return 0 // Avoid division by zero
        }
        
        // Group objects by photoId for efficient lookup
        val objectsByPhotoId = objects.groupBy { it.parentPhotoId }
        
        // Numerator: Photos that have been studied (lastStudied >= sevenDaysAgo)
        // A photo is studied if at least one DetectedObject has lastStudied >= sevenDaysAgo
        // (lastStudied = 0 means not studied yet, so we check lastStudied > 0)
        val studiedPhotos = recentPhotos.count { photo ->
            val objectsInPhoto = objectsByPhotoId[photo.photoId] ?: emptyList()
            // Check if at least one object in this photo has been studied via quiz
            // lastStudied > 0 means it was updated (quiz was answered correctly)
            // lastStudied >= sevenDaysAgo means it was studied within the last 7 days
            objectsInPhoto.any { obj ->
                obj.lastStudied > 0 && obj.lastStudied >= sevenDaysAgo
            }
        }
        
        // Calculate percentage
        return ((studiedPhotos * 100) / totalRecentPhotos).coerceIn(0, 100)
    }
}
