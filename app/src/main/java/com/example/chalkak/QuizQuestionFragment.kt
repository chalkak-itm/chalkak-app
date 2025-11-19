package com.example.chalkak

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

data class QuizQuestion(
    val imageRes: Int,
    val englishWord: String,
    val koreanWord: String,
    val exampleEnglish: String,
    val exampleKorean: String,
    val correctAnswer: String,
    val options: List<String>
)

class QuizQuestionFragment : Fragment() {
    private lateinit var txtProgress: TextView
    private lateinit var txtProgressPercent: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var cardQuizInfo: LinearLayout
    private lateinit var imgQuiz: ImageView
    private lateinit var layoutWordInfo: LinearLayout
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView
    
    // TTS helper
    private var ttsHelper: TtsHelper? = null
    
    // Speech recognition manager
    private var speechRecognitionManager: SpeechRecognitionManager? = null
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

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Recording permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // Placeholder quiz data - replace with actual data from database
    private val quizQuestions = listOf(
        QuizQuestion(
            imageRes = R.drawable.egg,
            englishWord = "Apple",
            koreanWord = "사과",
            exampleEnglish = "I eat an apple every day.",
            exampleKorean = "나는 매일 사과를 먹어요.",
            correctAnswer = "사과",
            options = listOf("사과", "바나나", "오렌지", "포도")
        ),
        QuizQuestion(
            imageRes = R.drawable.egg,
            englishWord = "Banana",
            koreanWord = "바나나",
            exampleEnglish = "I like to eat bananas.",
            exampleKorean = "나는 바나나를 좋아해요.",
            correctAnswer = "바나나",
            options = listOf("사과", "바나나", "오렌지", "포도")
        ),
        QuizQuestion(
            imageRes = R.drawable.egg,
            englishWord = "Orange",
            koreanWord = "오렌지",
            exampleEnglish = "The orange is sweet.",
            exampleKorean = "오렌지는 달아요.",
            correctAnswer = "오렌지",
            options = listOf("사과", "바나나", "오렌지", "포도")
        )
    )

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

        // Initialize TTS helper
        // layoutWordInfo contains card_word_detail, so pass it
        ttsHelper = TtsHelper(requireContext(), layoutWordInfo)

        // Setup option button click listeners
        btnOption1.setOnClickListener { onOptionSelected(0) }
        btnOption2.setOnClickListener { onOptionSelected(1) }
        btnOption3.setOnClickListener { onOptionSelected(2) }
        btnOption4.setOnClickListener { onOptionSelected(3) }

        // Initialize speech recognition manager
        // layoutWordInfo contains card_word_detail, so pass it
        speechRecognitionManager = SpeechRecognitionManager(
            context = requireContext(),
            cardWordDetail = layoutWordInfo,
            requestPermissionLauncher = requestPermissionLauncher
            // Default dialog will be displayed
        )

        // Setup next question button
        btnNextQuestion.setOnClickListener {
            loadNextQuestion()
        }

        // Load first question
        loadQuestion(0)
    }

    private fun loadQuestion(index: Int) {
        if (index >= quizQuestions.size) {
            // Quiz completed - navigate back or show completion screen
            (activity as? MainActivity)?.navigateToFragment(QuizFragment(), "quiz")
            return
        }

        currentQuestionIndex = index
        currentQuestion = quizQuestions[index]
        isAnswered = false
        selectedAnswer = null

        // Reset UI
        resetUI()

        // Load question data
        currentQuestion?.let { question ->
            imgQuiz.setImageResource(question.imageRes)
            txtOption1.text = question.options[0]
            txtOption2.text = question.options[1]
            txtOption3.text = question.options[2]
            txtOption4.text = question.options[3]

            // Hide word info
            layoutWordInfo.visibility = View.GONE
            btnNextQuestion.visibility = View.GONE

            // Enable all option buttons
            enableAllOptions()
        }

        // Update progress
        updateProgress()
    }

    private fun updateProgress() {
        val current = currentQuestionIndex + 1
        val total = quizQuestions.size
        val percent = (current * 100) / total

        txtProgress.text = "$current / $total"
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

        // Show word info in the same card after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            currentQuestion?.let { question ->
                txtSelectedWord.text = question.englishWord
                txtKoreanMeaning.text = question.koreanWord
                txtExampleSentence.text = question.exampleEnglish
                layoutWordInfo.visibility = View.VISIBLE

                // Update speech recognition manager with new word
                speechRecognitionManager?.updateTargetWord(question.englishWord)
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
        loadQuestion(currentQuestionIndex + 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsHelper?.cleanup()
        speechRecognitionManager?.cleanup()
    }
}

