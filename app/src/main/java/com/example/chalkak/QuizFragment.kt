package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
    
    // Weekly summary views
    private lateinit var txtCompletedQuizzes: TextView
    private lateinit var txtLearningDays: TextView

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
        
        // Initialize weekly summary views
        txtCompletedQuizzes = view.findViewById(R.id.txt_completed_quizzes)
        txtLearningDays = view.findViewById(R.id.txt_learning_days)

        // Load quiz data (placeholder - replace with actual data from DB)
        loadQuizData()

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
        txtConsecutiveDays.text = "${consecutiveDays}일"
        txtQuizSubtitle.text = "${wordCount}개의 마법 주문을 복습해요"
        
        // Update learning activity
        txtStreakDays.text = "${consecutiveDays}일 연속"
        
        // Update weekly summary
        txtCompletedQuizzes.text = "${completedQuizzes}개"
        txtLearningDays.text = "$learningDays/${totalDaysInWeek}일"
    }
}