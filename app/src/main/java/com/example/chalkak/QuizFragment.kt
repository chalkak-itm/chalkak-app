package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

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

        // Load quiz data (placeholder - replace with actual data from DB)
        loadQuizData()
        updateActivityDots()

        // Setup start quiz button click listener
        btnStartQuiz.setOnClickListener {
            (activity as? MainActivity)?.navigateToFragment(QuizQuestionFragment(), "quiz_question")
        }
    }

    private fun loadQuizData() {
        // Placeholder: Load quiz data from database or SharedPreferences
        // TODO: Replace with actual data from database
        
        // Example data - replace with actual values
        val wordCount = 12
        val accuracyRate = 85
        val totalLearning = 156
        val consecutiveDays = 12
        val completedQuizzes = 68
        val learningDays = 5
        val totalDaysInWeek = 7
        
        // Update main quiz card
        txtWordCount.text = wordCount.toString()
        txtAccuracyRate.text = "$accuracyRate%"
        txtTotalLearning.text = totalLearning.toString()
        txtConsecutiveDays.text = "${consecutiveDays} days"
        txtQuizSubtitle.text = "Review ${wordCount} magic spells"
        
        // Update learning activity
        txtStreakDays.text = "${consecutiveDays} days streak"
    }
    
    private fun updateActivityDots() {
        // Get last 7 days of learning activity
        // TODO: Replace with actual data from database
        // Example: [true, false, true, true, false, true, true] for last 7 days
        val last7DaysActivity = listOf(true, false, true, true, false, true, true)
        
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
}