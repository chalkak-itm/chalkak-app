package com.example.chalkak

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class LogItemDetailDialogFragment : DialogFragment() {
    
    private lateinit var imgSelectedPhoto: ImageView
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView
    
    private var entry: LogEntry? = null
    private var onDialogDismissed: (() -> Unit)? = null
    
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
    }
    
    fun setOnDialogDismissedListener(listener: () -> Unit) {
        onDialogDismissed = listener
    }
    
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }
}

