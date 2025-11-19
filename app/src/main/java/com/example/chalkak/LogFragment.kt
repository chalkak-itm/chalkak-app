package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class LogFragment : BaseFragment() {
    private lateinit var headerDefault: LinearLayout
    private var dialogFragment: LogItemDetailDialogFragment? = null
    private val roomDb by lazy { AppDatabase.getInstance(requireContext()) }
    private lateinit var recycler: RecyclerView

    override fun getCardWordDetailView(): View {
        // DialogFragment를 사용하므로 더 이상 필요 없음
        // 하지만 BaseFragment의 initializeTtsAndSpeechRecognition이 호출되므로
        // 임시 뷰를 반환하거나 null 처리가 필요할 수 있음
        return view ?: View(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        headerDefault = view.findViewById(R.id.header_default)
        
        // Initialize header views from included layout
        val headerView = view.findViewById<View>(R.id.header_default)
        val imgMascot = headerView.findViewById<ImageView>(R.id.img_header_mascot)
        val txtTitle = headerView.findViewById<TextView>(R.id.txt_header_title)
        imgMascot.setImageResource(R.drawable.egg)
        txtTitle.text = "Log"

        recycler = view.findViewById(R.id.recyclerLog)

        // Load data from Room database
        loadDataFromDatabase()
    }

    private fun loadDataFromDatabase() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val photos = roomDb.photoLogDao().getAllPhotos()
                val entries = mutableListOf<LogEntry>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                photos.forEach { photo ->
                    if (photo.localImagePath != "firebase_sync") {
                        val detectedObjects = roomDb.detectedObjectDao().getObjectsByPhotoId(photo.photoId)
                        detectedObjects.forEach { obj ->
                            val dateStr = dateFormat.format(Date(photo.createdAt))
                            entries.add(
                                LogEntry(
                                    dateIso = dateStr,
                                    word = obj.englishWord,
                                    imagePath = photo.localImagePath,
                                    koreanMeaning = obj.koreanMeaning,
                                    objectId = obj.objectId
                                )
                            )
                        }
                    }
                }

                val items: List<LogUiItem> = buildSectionedItems(entries)
                val adapter = SectionedLogAdapter(items) { entry ->
                    showItemDetail(entry)
                }

                val grid = GridLayoutManager(requireContext(), 2)
                grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (items[position] is LogUiItem.Header) 2 else 1
                    }
                }
                recycler.layoutManager = grid
                recycler.adapter = adapter
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showItemDetail(entry: LogEntry) {
        // 기존 다이얼로그가 열려있으면 닫기
        dialogFragment?.dismiss()
        
        // 새 다이얼로그 생성 및 표시
        dialogFragment = LogItemDetailDialogFragment.newInstance(entry)
        dialogFragment?.setOnDialogDismissedListener {
            dialogFragment = null
        }
        dialogFragment?.show(parentFragmentManager, "LogItemDetailDialog")
    }
}

data class LogEntry(
    val dateIso: String, // e.g., 2025-11-06; replace with LocalDate if using java.time with minSdk compat
    val word: String,
    val imagePath: String? = null, // 실제 이미지 경로
    val imageRes: Int? = null, // 더미 데이터용 (하위 호환성)
    val koreanMeaning: String? = null, // 한국어 의미
    val objectId: Long? = null // DetectedObject의 ID
) : Serializable

// UI items for sectioned list
sealed class LogUiItem {
    data class Header(val dateIso: String) : LogUiItem()
    data class EntryItem(val entry: LogEntry) : LogUiItem()
}

private fun buildSectionedItems(source: List<LogEntry>): List<LogUiItem> {
    val grouped = source.groupBy { it.dateIso }
    val result = mutableListOf<LogUiItem>()
    // Sort by date descending for example
    grouped.keys.sortedDescending().forEach { date ->
        result.add(LogUiItem.Header(date))
        grouped[date]?.forEach { e -> result.add(LogUiItem.EntryItem(e)) }
    }
    return result
}

class SectionedLogAdapter(
    private val items: List<LogUiItem>,
    private val onItemClick: (LogEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is LogUiItem.Header -> TYPE_HEADER
        is LogUiItem.EntryItem -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = android.view.LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_log_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_log_entry, parent, false)
            LogEntryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LogUiItem.Header -> (holder as HeaderViewHolder).bind(item.dateIso)
            is LogUiItem.EntryItem -> (holder as LogEntryViewHolder).bind(item.entry, onItemClick)
        }
    }

    override fun getItemCount(): Int = items.size
}

class LogEntryViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val wordView: android.widget.TextView = itemView.findViewById(R.id.txtWord)
    private val imageView: android.widget.ImageView = itemView.findViewById(R.id.imgPhoto)

    fun bind(entry: LogEntry, onItemClick: (LogEntry) -> Unit) {
        wordView.text = entry.word
        wordView.visibility = View.VISIBLE
        
        // 실제 이미지 경로가 있으면 사용, 없으면 더미 이미지
        if (entry.imagePath != null) {
            ImageLoaderHelper.loadImageToView(imageView, entry.imagePath)
        } else if (entry.imageRes != null) {
            imageView.setImageResource(entry.imageRes)
        }

        // Set click listener on the card
        itemView.setOnClickListener {
            onItemClick(entry)
        }
    }
}

class HeaderViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val sectionDate: android.widget.TextView = itemView.findViewById(R.id.txtSectionDate)
    fun bind(dateIso: String) {
        sectionDate.text = dateIso
    }
}

