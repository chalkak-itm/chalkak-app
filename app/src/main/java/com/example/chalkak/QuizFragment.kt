package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
                
                // 4. Accuracy Rate: Calculate based on recent study activity
                val accuracyRate = calculateAccuracyRate(allDetectedObjects)
                
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
}