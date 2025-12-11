package com.example.chalkak.ui.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.chalkak.R
import com.example.chalkak.base.BaseFragment
import com.example.chalkak.data.local.AppDatabase
import com.example.chalkak.domain.detection.BoundingBoxHelper
import com.example.chalkak.domain.quiz.QuizQuestion
import com.example.chalkak.domain.quiz.QuizQuestionGenerator
import com.example.chalkak.domain.quiz.SpacedRepetitionManager
import com.example.chalkak.ui.activity.MainActivity
import com.example.chalkak.util.ImageLoaderHelper
import com.example.chalkak.util.ToastHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class QuizQuestionFragment : BaseFragment() {
    companion object {
        // Constants for quiz configuration
        private const val NUM_OPTIONS = 4
    }
    
    private lateinit var txtProgress: TextView
    private lateinit var txtProgressPercent: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var cardQuizInfo: LinearLayout
    private lateinit var imgQuiz: ImageView
    private lateinit var layoutWordInfo: LinearLayout
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView
    
    private lateinit var btnOption1: LinearLayout
    private lateinit var btnOption2: LinearLayout
    private lateinit var btnOption3: LinearLayout
    private lateinit var btnOption4: LinearLayout
    private lateinit var txtOption1: TextView
    private lateinit var txtOption2: TextView
    private lateinit var txtOption3: TextView
    private lateinit var txtOption4: TextView
    private lateinit var iconOption1: ImageView
    private lateinit var iconOption2: ImageView
    private lateinit var iconOption3: ImageView
    private lateinit var iconOption4: ImageView
    private lateinit var btnNextQuestion: LinearLayout

    private var currentQuestion: QuizQuestion? = null
    private var isAnswered = false
    private var selectedAnswer: String? = null
    private var currentQuestionIndex = 0
    private var quizQuestions = emptyList<QuizQuestion>()
    private val roomDb by lazy { AppDatabase.getInstance(requireContext()) }
    // Spaced repetition manager for learning algorithm
    private lateinit var spacedRepetitionManager: SpacedRepetitionManager

    override fun getCardWordDetailView(): View {
        // Return a temporary view if layoutWordInfo is not initialized yet
        return if (::layoutWordInfo.isInitialized) {
            layoutWordInfo
        } else {
            // Return a temporary view to avoid crash
            View(requireContext())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quiz_question, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        txtProgress = view.findViewById(R.id.txt_progress)
        txtProgressPercent = view.findViewById(R.id.txt_progress_percent)
        progressBar = view.findViewById(R.id.progress_bar)
        cardQuizInfo = view.findViewById(R.id.card_quiz_info)
        imgQuiz = view.findViewById(R.id.img_quiz)
        layoutWordInfo = view.findViewById(R.id.layout_word_info)
        txtSelectedWord = view.findViewById(R.id.txt_selected_word)
        txtKoreanMeaning = view.findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = view.findViewById(R.id.txt_example_sentence)
        btnOption1 = view.findViewById(R.id.btn_option_1)
        btnOption2 = view.findViewById(R.id.btn_option_2)
        btnOption3 = view.findViewById(R.id.btn_option_3)
        btnOption4 = view.findViewById(R.id.btn_option_4)
        txtOption1 = view.findViewById(R.id.txt_option_1)
        txtOption2 = view.findViewById(R.id.txt_option_2)
        txtOption3 = view.findViewById(R.id.txt_option_3)
        txtOption4 = view.findViewById(R.id.txt_option_4)
        iconOption1 = view.findViewById(R.id.icon_option_1)
        iconOption2 = view.findViewById(R.id.icon_option_2)
        iconOption3 = view.findViewById(R.id.icon_option_3)
        iconOption4 = view.findViewById(R.id.icon_option_4)
        btnNextQuestion = view.findViewById(R.id.btn_next_question)

        // Initialize TTS and Speech Recognition (from BaseFragment)
        initializeTtsAndSpeechRecognition()

        // Setup option button click listeners
        btnOption1.setOnClickListener { onOptionSelected(0) }
        btnOption2.setOnClickListener { onOptionSelected(1) }
        btnOption3.setOnClickListener { onOptionSelected(2) }
        btnOption4.setOnClickListener { onOptionSelected(3) }

        // Setup next question button
        btnNextQuestion.setOnClickListener {
            loadNextQuestion()
        }

        // Initialize spaced repetition manager
        spacedRepetitionManager = SpacedRepetitionManager(roomDb, viewLifecycleOwner.lifecycleScope)

        // Load quiz questions from database
        loadQuizQuestionsFromDatabase()
    }
    
    private fun loadQuizQuestionsFromDatabase() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use QuizQuestionGenerator to generate questions
                val wordToObjectIdMap = mutableMapOf<String, Long>()
                val questions = QuizQuestionGenerator.generateQuizQuestions(roomDb, wordToObjectIdMap)
                quizQuestions = questions
                
                // Initialize spaced repetition manager with questions
                spacedRepetitionManager.initializeQuestions(questions, wordToObjectIdMap)
                spacedRepetitionManager.setTotalCount(questions.size)
                
                // Switch to Main dispatcher for UI operations
                withContext(Dispatchers.Main) {
                    if (spacedRepetitionManager.isEmpty()) {
                        // No data available - show message and navigate back
                        ToastHelper.showCenterToast(
                            requireContext(),
                            "No quiz questions available. Please take photos first."
                        )
                        (activity as? MainActivity)?.navigateToFragment(QuizFragment(), "quiz")
                    } else {
                        // Load first question from queue
                        loadNextQuestionFromQueue()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // On error, navigate back to quiz screen (on Main thread)
                withContext(Dispatchers.Main) {
                    ToastHelper.showCenterToast(
                        requireContext(),
                        "Failed to load quiz questions. Please try again."
                    )
                    (activity as? MainActivity)?.navigateToFragment(QuizFragment(), "quiz")
                }
            }
        }
    }
    

    private fun loadNextQuestionFromQueue() {
        // Get next question from spaced repetition manager
        val question = spacedRepetitionManager.getNextQuestion()
        
        if (question == null) {
            // Quiz completed - navigate back or show completion screen
            ToastHelper.showCenterToast(
                requireContext(),
                "Great job! You've completed all questions! 🎉"
            )
            (activity as? MainActivity)?.navigateToFragment(QuizFragment(), "quiz")
            return
        }

        currentQuestion = question
        val remaining = spacedRepetitionManager.getRemainingCount()
        currentQuestionIndex = quizQuestions.size - remaining - 1 // Track progress
        isAnswered = false
        selectedAnswer = null

        // Reset UI
        resetUI()

        // Verify file exists before loading image
        if (question.imagePath != null) {
            val imageFile = java.io.File(question.imagePath)
            if (!imageFile.exists()) {
                // File doesn't exist, skip to next question
                loadNextQuestionFromQueue()
                return
            }
            
            // Try to crop image using bounding box if available
            if (question.boundingBox != null) {
                loadImageWithBoundingBox(question.imagePath, question.boundingBox)
            } else {
                ImageLoaderHelper.loadImageToView(imgQuiz, question.imagePath)
            }
        } else if (question.imageRes != null) {
            imgQuiz.setImageResource(question.imageRes)
        }
        
        // Ensure we have exactly NUM_OPTIONS options
        if (question.options.size >= NUM_OPTIONS) {
            txtOption1.text = question.options[0]
            txtOption2.text = question.options[1]
            txtOption3.text = question.options[2]
            txtOption4.text = question.options[3]
        } else {
            // Fallback: fill with available options and empty strings
            txtOption1.text = question.options.getOrNull(0) ?: ""
            txtOption2.text = question.options.getOrNull(1) ?: ""
            txtOption3.text = question.options.getOrNull(2) ?: ""
            txtOption4.text = question.options.getOrNull(3) ?: ""
        }

        // Hide word info
        layoutWordInfo.visibility = View.GONE
        btnNextQuestion.visibility = View.GONE

        // Enable all option buttons
        enableAllOptions()

        // Update progress
        updateProgress()
    }

    private fun updateProgress() {
        val total = quizQuestions.size
        if (total == 0) {
            txtProgress.text = "0 / 0"
            txtProgressPercent.text = "0%"
            progressBar.progress = 0
            return
        }
        
        val remaining = spacedRepetitionManager.getRemainingCount()
        val completed = total - remaining
        val percent = if (total > 0) (completed * 100 / total) else 0

        txtProgress.text = "$completed / $total"
        txtProgressPercent.text = "$percent%"
        progressBar.progress = percent
    }

    private fun resetUI() {
        // Reset all option buttons to default state
        btnOption1.background = requireContext().getDrawable(R.drawable.bg_button_quiz_option_default)
        btnOption2.background = requireContext().getDrawable(R.drawable.bg_button_quiz_option_default)
        btnOption3.background = requireContext().getDrawable(R.drawable.bg_button_quiz_option_default)
        btnOption4.background = requireContext().getDrawable(R.drawable.bg_button_quiz_option_default)

        iconOption1.visibility = View.GONE
        iconOption2.visibility = View.GONE
        iconOption3.visibility = View.GONE
        iconOption4.visibility = View.GONE

        txtOption1.setTextColor(android.graphics.Color.parseColor("#333333"))
        txtOption2.setTextColor(android.graphics.Color.parseColor("#333333"))
        txtOption3.setTextColor(android.graphics.Color.parseColor("#333333"))
        txtOption4.setTextColor(android.graphics.Color.parseColor("#333333"))
    }

    private fun onOptionSelected(optionIndex: Int) {
        if (isAnswered) {
            // If already answered correctly, do nothing
            if (selectedAnswer == currentQuestion?.correctAnswer) {
                return
            }
            // If already answered and wrong, allow retry
            // Reset previous wrong answer state
            val previousWrongIndex = currentQuestion?.options?.indexOf(selectedAnswer ?: "")
            previousWrongIndex?.let { idx ->
                if (idx >= 0 && idx < 4) {
                    resetOptionButton(idx)
                }
            }
            // Reset state for new selection
            isAnswered = false
            selectedAnswer = null
        }

        val selectedOption = currentQuestion?.options?.getOrNull(optionIndex) ?: return
        selectedAnswer = selectedOption
        val isCorrect = selectedOption == currentQuestion?.correctAnswer

        // Disable all options
        disableAllOptions()

        if (isCorrect) {
            // Correct answer
            handleCorrectAnswer(optionIndex)
        } else {
            // Wrong answer
            handleWrongAnswer(optionIndex)
        }
    }

    private fun handleCorrectAnswer(optionIndex: Int) {
        isAnswered = true

        // Show popup feedback
        showFeedbackPopup(true, "Correct! ✨")

        // Update selected button to correct state
        updateOptionButton(optionIndex, true)

        // Spaced Repetition Algorithm: Update lastStudied date in DB with photo's createdAt
        currentQuestion?.let { question ->
            spacedRepetitionManager.handleCorrectAnswer(question.englishWord, question.parentPhotoId)
        }

        // Show word info in the same card after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            currentQuestion?.let { question ->
                txtSelectedWord.text = question.englishWord
                txtKoreanMeaning.text = question.koreanWord
                // Show example sentence with translation if available
                val exampleText = if (question.exampleEnglish.isNotEmpty() && question.exampleKorean.isNotEmpty()) {
                    "${question.exampleEnglish}\n(${question.exampleKorean})"
                } else if (question.exampleEnglish.isNotEmpty()) {
                    question.exampleEnglish
                } else {
                    "No example sentence available."
                }
                txtExampleSentence.text = exampleText
                layoutWordInfo.visibility = View.VISIBLE

                // Update speech recognition manager with new word
                updateTargetWord(question.englishWord)
            }

            // Show next question button
            btnNextQuestion.visibility = View.VISIBLE
        }, 500)
    }

    private fun handleWrongAnswer(optionIndex: Int) {
        isAnswered = true

        // Show popup feedback
        showFeedbackPopup(false, "Incorrect. Please select again.")

        // Update selected button to incorrect state
        updateOptionButton(optionIndex, false)

        // Spaced Repetition Algorithm: Add question back to end of queue
        // (Don't update lastStudied - word will appear again soon)
        currentQuestion?.let { question ->
            spacedRepetitionManager.handleWrongAnswer(question)
        }

        // Don't show correct answer - let user try again
        // Re-enable all options for retry
        enableAllOptions()
    }

    private fun showFeedbackPopup(isCorrect: Boolean, message: String) {
        // Get Activity's root view (decorView's content)
        val activity = activity ?: return
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return
        
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_feedback, null)

        val iconFeedback = popupView.findViewById<ImageView>(R.id.icon_feedback_popup)
        val txtFeedback = popupView.findViewById<TextView>(R.id.txt_feedback_popup)
        val popupContainer = popupView.findViewById<LinearLayout>(R.id.popup_feedback)

        // Set icon and background based on correctness
        if (isCorrect) {
            iconFeedback.setImageResource(R.drawable.ic_checkmark_green)
            popupContainer.background = requireContext().getDrawable(R.drawable.bg_feedback_correct)
            txtFeedback.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            iconFeedback.setImageResource(R.drawable.ic_close_red)
            popupContainer.background = requireContext().getDrawable(R.drawable.bg_feedback_incorrect)
            txtFeedback.setTextColor(android.graphics.Color.parseColor("#F44336"))
        }

        txtFeedback.text = message

        // Create FrameLayout to center the popup
        val frameLayout = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Add popup to frame layout
        val popupParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        frameLayout.addView(popupView, popupParams)

        // Add frame layout to root view
        rootView.addView(frameLayout)

        // Set initial alpha to 0 and scale to 0.5
        popupView.alpha = 0f
        popupView.scaleX = 0.5f
        popupView.scaleY = 0.5f

        // Animate in
        val fadeIn = ObjectAnimator.ofFloat(popupView, "alpha", 0f, 1f)
        val scaleXIn = ObjectAnimator.ofFloat(popupView, "scaleX", 0.5f, 1f)
        val scaleYIn = ObjectAnimator.ofFloat(popupView, "scaleY", 0.5f, 1f)

        fadeIn.duration = 300
        scaleXIn.duration = 300
        scaleYIn.duration = 300
        scaleXIn.interpolator = DecelerateInterpolator()
        scaleYIn.interpolator = DecelerateInterpolator()

        fadeIn.start()
        scaleXIn.start()
        scaleYIn.start()

        // Animate out and remove after delay
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeOut = ObjectAnimator.ofFloat(popupView, "alpha", 1f, 0f)
            val scaleXOut = ObjectAnimator.ofFloat(popupView, "scaleX", 1f, 0.8f)
            val scaleYOut = ObjectAnimator.ofFloat(popupView, "scaleY", 1f, 0.8f)

            fadeOut.duration = 200
            scaleXOut.duration = 200
            scaleYOut.duration = 200

            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        rootView.removeView(frameLayout)
                    } catch (e: Exception) {
                        // View already removed
                    }
                }
            })

            fadeOut.start()
            scaleXOut.start()
            scaleYOut.start()
        }, 1500) // Show for 1.5 seconds
    }

    private fun updateOptionButton(index: Int, isCorrect: Boolean) {
        val button = when (index) {
            0 -> btnOption1
            1 -> btnOption2
            2 -> btnOption3
            3 -> btnOption4
            else -> return
        }

        val icon = when (index) {
            0 -> iconOption1
            1 -> iconOption2
            2 -> iconOption3
            3 -> iconOption4
            else -> return
        }

        val textView = when (index) {
            0 -> txtOption1
            1 -> txtOption2
            2 -> txtOption3
            3 -> txtOption4
            else -> return
        }

        if (isCorrect) {
            button.background = requireContext().getDrawable(R.drawable.bg_button_quiz_option_correct)
            icon.setImageResource(R.drawable.ic_checkmark_green)
            icon.visibility = View.VISIBLE
            textView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            button.background = requireContext().getDrawable(R.drawable.bg_button_quiz_option_incorrect)
            icon.setImageResource(R.drawable.ic_close_red)
            icon.visibility = View.VISIBLE
            textView.setTextColor(android.graphics.Color.parseColor("#F44336"))
        }
    }

    private fun resetOptionButton(index: Int) {
        val button = when (index) {
            0 -> btnOption1
            1 -> btnOption2
            2 -> btnOption3
            3 -> btnOption4
            else -> return
        }

        val icon = when (index) {
            0 -> iconOption1
            1 -> iconOption2
            2 -> iconOption3
            3 -> iconOption4
            else -> return
        }

        val textView = when (index) {
            0 -> txtOption1
            1 -> txtOption2
            2 -> txtOption3
            3 -> txtOption4
            else -> return
        }

        button.background = requireContext().getDrawable(R.drawable.bg_button_quiz_option_default)
        icon.visibility = View.GONE
        textView.setTextColor(android.graphics.Color.parseColor("#333333"))
    }

    private fun enableAllOptions() {
        btnOption1.isEnabled = true
        btnOption2.isEnabled = true
        btnOption3.isEnabled = true
        btnOption4.isEnabled = true
        btnOption1.isClickable = true
        btnOption2.isClickable = true
        btnOption3.isClickable = true
        btnOption4.isClickable = true
    }

    private fun disableAllOptions() {
        btnOption1.isEnabled = false
        btnOption2.isEnabled = false
        btnOption3.isEnabled = false
        btnOption4.isEnabled = false
        btnOption1.isClickable = false
        btnOption2.isClickable = false
        btnOption3.isClickable = false
        btnOption4.isClickable = false
    }

    private fun loadNextQuestion() {
        // Load next question from queue (spaced repetition algorithm)
        loadNextQuestionFromQueue()
    }
    
    private fun loadImageWithBoundingBox(imagePath: String, boundingBoxString: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Verify file exists before attempting to load
                val imageFile = java.io.File(imagePath)
                if (!imageFile.exists()) {
                    // File doesn't exist, skip to next question
                    withContext(Dispatchers.Main) {
                        loadNextQuestion()
                    }
                    return@launch
                }
                
                // Load original bitmap
                val originalBitmap = ImageLoaderHelper.loadBitmapFromPath(imagePath)
                if (originalBitmap != null) {
                    // Use BoundingBoxHelper to crop
                    val boundingBox = BoundingBoxHelper.parseBoundingBox(boundingBoxString)
                    if (boundingBox != null) {
                        val croppedBitmap = BoundingBoxHelper.cropBitmap(originalBitmap, boundingBox)
                        if (croppedBitmap != null) {
                            withContext(Dispatchers.Main) {
                                imgQuiz.setImageBitmap(croppedBitmap)
                            }
                            return@launch
                        }
                    }
                }
                // Fallback to full image (file exists but loading failed)
                withContext(Dispatchers.Main) {
                    ImageLoaderHelper.loadImageToView(imgQuiz, imagePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // On error, try to load full image if file exists
                val imageFile = java.io.File(imagePath)
                if (imageFile.exists()) {
                    withContext(Dispatchers.Main) {
                        ImageLoaderHelper.loadImageToView(imgQuiz, imagePath)
                    }
                } else {
                    // File doesn't exist, skip to next question
                    withContext(Dispatchers.Main) {
                        loadNextQuestion()
                    }
                }
            }
        }
    }

}
