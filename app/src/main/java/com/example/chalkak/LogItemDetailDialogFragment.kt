package com.example.chalkak

import android.Manifest
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment

class LogItemDetailDialogFragment : DialogFragment() {
    
    private lateinit var imgSelectedPhoto: ImageView
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView
    
    private var entry: LogEntry? = null
    private var onDialogDismissed: (() -> Unit)? = null
    
    // TTS and Speech Recognition helpers
    private var ttsHelper: TtsHelper? = null
    private var speechRecognitionManager: SpeechRecognitionManager? = null
    
    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Recording permission is required.", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        private const val ARG_ENTRY = "entry"
        
        fun newInstance(entry: LogEntry): LogItemDetailDialogFragment {
            val fragment = LogItemDetailDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_ENTRY, entry)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            entry = it.getSerializable(ARG_ENTRY) as? LogEntry
        }
        // 팝업 스타일 설정
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            // 전체 화면을 덮도록 설정
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            window.setFlags(
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            // 배경을 반투명하게 설정
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        // 다이얼로그 취소 가능하도록 설정 (뒤로가기 버튼으로도 닫을 수 있음)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_log_item_detail, container, false)
        
        // 바깥 터치로 닫기 (FrameLayout의 배경 클릭)
        view.setOnClickListener {
            dismiss()
        }
        
        // 다이얼로그 내용 영역 (터치해도 닫히지 않도록)
        val contentView = view.findViewById<ViewGroup>(R.id.dialog_content)
        contentView?.setOnClickListener {
            // 내용 영역 클릭은 이벤트 소비하여 부모로 전파되지 않도록 함
            // (아무 동작도 하지 않지만 이벤트는 소비됨)
        }
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        imgSelectedPhoto = view.findViewById(R.id.img_selected_photo)
        txtSelectedWord = view.findViewById(R.id.txt_selected_word)
        txtKoreanMeaning = view.findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = view.findViewById(R.id.txt_example_sentence)
        
        entry?.let { logEntry ->
            imgSelectedPhoto.setImageResource(logEntry.imageRes)
            txtSelectedWord.text = logEntry.word
            
            // Placeholder data for meaning and example
            // TODO: Replace with actual data from database or API
            txtKoreanMeaning.text = "Meaning" // Replace with actual meaning
            txtExampleSentence.text = "Example sentence for ${logEntry.word}" // Replace with actual example
        }
        
        // Initialize TTS and Speech Recognition
        initializeTtsAndSpeechRecognition(view)
    }
    
    private fun initializeTtsAndSpeechRecognition(rootView: View) {
        // Find the card_word_detail_dialog view using the include tag ID
        val cardWordDetail = rootView.findViewById<View>(R.id.card_word_detail_dialog)
            ?: rootView.findViewById<ViewGroup>(R.id.dialog_content)?.getChildAt(1)
        
        cardWordDetail?.let { cardView ->
            // Initialize TTS helper
            ttsHelper = TtsHelper(requireContext(), cardView)
            
            // Initialize speech recognition manager
            speechRecognitionManager = SpeechRecognitionManager(
                context = requireContext(),
                cardWordDetail = cardView,
                requestPermissionLauncher = requestPermissionLauncher
            )
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            // 전체 화면을 덮도록 설정 (딤 영역 포함)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 배경을 반투명하게 설정
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        
        // ScrollView의 최대 높이를 화면 높이의 90%로 제한하여 중앙 정렬 보장
        view?.let { rootView ->
            val scrollView = rootView.findViewById<android.widget.ScrollView>(R.id.scroll_view)
            scrollView?.let { sv ->
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val maxHeight = (screenHeight * 0.9).toInt()
                
                // ScrollView의 LayoutParams를 가져와서 높이 제한 설정
                val layoutParams = sv.layoutParams as? android.widget.FrameLayout.LayoutParams
                    ?: android.widget.FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                
                // ScrollView가 화면 높이를 초과하지 않도록 최대 높이 설정
                // wrap_content이지만 최대 높이를 초과하지 않도록 post로 설정
                sv.post {
                    val measuredHeight = sv.measuredHeight
                    if (measuredHeight > maxHeight) {
                        layoutParams.height = maxHeight
                        sv.layoutParams = layoutParams
                    }
                }
            }
        }
    }
    
    fun setOnDialogDismissedListener(listener: () -> Unit) {
        onDialogDismissed = listener
    }
    
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup TTS and Speech Recognition resources
        ttsHelper?.cleanup()
        speechRecognitionManager?.cleanup()
        ttsHelper = null
        speechRecognitionManager = null
    }
}

