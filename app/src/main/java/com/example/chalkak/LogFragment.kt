package com.example.chalkak

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LogFragment : BaseFragment() {
    private lateinit var headerDefault: LinearLayout
    private lateinit var cardSelectedItem: LinearLayout
    private lateinit var imgSelectedPhoto: ImageView
    private lateinit var txtSelectedWord: TextView
    private lateinit var txtKoreanMeaning: TextView
    private lateinit var txtExampleSentence: TextView
    
    private var selectedEntry: LogEntry? = null // Track currently selected entry

    override fun getCardWordDetailView(): View {
        return cardSelectedItem
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
        
        cardSelectedItem = view.findViewById(R.id.card_selected_item)
        imgSelectedPhoto = view.findViewById(R.id.img_selected_photo)
        txtSelectedWord = view.findViewById(R.id.txt_selected_word)
        txtKoreanMeaning = view.findViewById(R.id.txt_korean_meaning)
        txtExampleSentence = view.findViewById(R.id.txt_example_sentence)

        // Initialize TTS and Speech Recognition (from BaseFragment)
        initializeTtsAndSpeechRecognition()

        val recycler: RecyclerView = view.findViewById(R.id.recyclerLog)

        // Placeholder data; replace with DB-backed repository later
        val sampleEntries = listOf(
            LogEntry(dateIso = "2025-11-06", word = "apple", imageRes = R.drawable.camera),
            LogEntry(dateIso = "2025-11-06", word = "banana", imageRes = R.drawable.frame),
            LogEntry(dateIso = "2025-11-05", word = "cat", imageRes = R.drawable.camera),
            LogEntry(dateIso = "2025-11-05", word = "dog", imageRes = R.drawable.frame),
            LogEntry(dateIso = "2025-11-02", word = "elephant", imageRes = R.drawable.camera),
            LogEntry(dateIso = "2025-11-02", word = "flower", imageRes = R.drawable.frame),
            LogEntry(dateIso = "2025-11-02", word = "guitar", imageRes = R.drawable.camera)
        )

        val items: List<LogUiItem> = buildSectionedItems(sampleEntries)
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
    }

    private fun showItemDetail(entry: LogEntry) {
        // If the same item is clicked again, return to initial state
        if (selectedEntry == entry && cardSelectedItem.visibility == View.VISIBLE) {
            // Return to initial state
            headerDefault.visibility = View.VISIBLE
            cardSelectedItem.visibility = View.GONE
            selectedEntry = null
            return
        }

        // Hide default header, show selected item detail
        headerDefault.visibility = View.GONE
        cardSelectedItem.visibility = View.VISIBLE
        selectedEntry = entry

        // Update detail card with entry data
        imgSelectedPhoto.setImageResource(entry.imageRes)
        txtSelectedWord.text = entry.word

        // Placeholder data for meaning and example
        // TODO: Replace with actual data from database or API
        txtKoreanMeaning.text = "Meaning" // Replace with actual meaning
        txtExampleSentence.text = "Example sentence for ${entry.word}" // Replace with actual example

        // Update speech recognition manager with new word
        updateTargetWord(entry.word)
        
        // Don't scroll - maintain current scroll position
    }
}

data class LogEntry(
    val dateIso: String, // e.g., 2025-11-06; replace with LocalDate if using java.time with minSdk compat
    val word: String,
    val imageRes: Int
)

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
        imageView.setImageResource(entry.imageRes)

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

