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
    private lateinit var wordDataLoader: WordDataLoaderHelper
    
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
        
        val scrollView = view.findViewById<android.widget.ScrollView>(R.id.scroll_view)
        val contentView = view.findViewById<ViewGroup>(R.id.dialog_content)
        
        // FrameLayout에 터치 리스너 추가 - 터치 위치가 콘텐츠 영역 밖인지 확인
        view.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val touchX = event.x
                val touchY = event.y
                
                // ScrollView의 위치와 크기 확인
                scrollView?.let { sv ->
                    val scrollViewRect = android.graphics.Rect()
                    sv.getHitRect(scrollViewRect)
                    
                    // 터치 위치가 ScrollView 영역 밖인지 확인
                    if (!scrollViewRect.contains(touchX.toInt(), touchY.toInt())) {
                        // 콘텐츠 영역 밖을 터치했으므로 팝업 닫기
                        dismiss()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
        
        // ScrollView의 빈 공간 터치 감지 - 콘텐츠 영역 밖을 터치하면 닫기
        scrollView?.setOnTouchListener { sv, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val touchX = event.x
                val touchY = event.y
                
                // 콘텐츠 영역의 위치와 크기 확인 (ScrollView 내부 좌표 기준)
                contentView?.let { content ->
                    val contentLeft = content.left.toFloat()
                    val contentTop = content.top.toFloat()
                    val contentRight = content.right.toFloat()
                    val contentBottom = content.bottom.toFloat()
                    
                    // 터치 위치가 콘텐츠 영역 밖인지 확인
                    if (touchX < contentLeft || touchX > contentRight || 
                        touchY < contentTop || touchY > contentBottom) {
                        // 콘텐츠 영역 밖을 터치했으므로 팝업 닫기
                        dismiss()
                        return@setOnTouchListener true
                    }
                }
            }
            // 콘텐츠 영역 내부 터치는 ScrollView가 처리 (스크롤 등)
            false
        }
        
        // 다이얼로그 내용 영역 (터치해도 닫히지 않도록)
        contentView?.setOnClickListener {
            // 내용 영역 클릭은 이벤트 소비하여 부모로 전파되지 않도록 함
        }
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        imgSelectedPhoto = view.findViewById(R.id.img_selected_photo)
        txtSelectedWord = view.findViewById(R.id.txt_selected_word)
        txtKoreanMeaning = view.findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = view.findViewById(R.id.txt_example_sentence)
        
        // Initialize WordDataLoaderHelper
        wordDataLoader = WordDataLoaderHelper(requireContext(), viewLifecycleOwner)
        
        entry?.let { logEntry ->
            // Load image from path or use resource
            if (logEntry.imagePath != null) {
                ImageLoaderHelper.loadImageToView(imgSelectedPhoto, logEntry.imagePath)
            } else if (logEntry.imageRes != null) {
                imgSelectedPhoto.setImageResource(logEntry.imageRes)
            }
            
            txtSelectedWord.text = logEntry.word
            
            // Load word data using helper
            loadWordData(logEntry)
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

    private fun loadWordData(logEntry: LogEntry) {
        wordDataLoader.loadWordData(
            word = logEntry.word,
            objectId = logEntry.objectId,
            callback = object : WordDataLoaderHelper.WordDataCallback {
                override fun onLoading() {
                    txtKoreanMeaning.text = logEntry.koreanMeaning ?: "Loading meaning..."
                    txtExampleSentence.text = "Loading example sentences..."
                }

                override fun onSuccess(data: WordDataLoaderHelper.WordData) {
                    txtKoreanMeaning.text = data.koreanMeaning
                    if (data.exampleSentence.isNotEmpty() && data.exampleTranslation.isNotEmpty()) {
                        txtExampleSentence.text = "${data.exampleSentence}\n(${data.exampleTranslation})"
                    } else if (data.exampleSentence.isNotEmpty()) {
                        txtExampleSentence.text = data.exampleSentence
                    } else {
                        txtExampleSentence.text = "No example sentences."
                    }
                }

                override fun onError(message: String) {
                    txtKoreanMeaning.text = "Failed to load meaning."
                    txtExampleSentence.text = message
                }
            }
        )
    }
}

