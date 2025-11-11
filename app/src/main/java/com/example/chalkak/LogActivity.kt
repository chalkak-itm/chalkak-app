package com.example.chalkak

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_log)

        val recycler: RecyclerView = findViewById(R.id.recyclerLog)
        recycler.layoutManager = GridLayoutManager(this, 2)

        // Placeholder data; replace with DB-backed repository later
        val sampleEntries = listOf(
            LogEntry(dateIso = "2025-11-06", word = "apple", imageRes = R.drawable.camera),
            LogEntry(dateIso = "2025-11-06", word = "banana", imageRes = R.drawable.frame),
            LogEntry(dateIso = "2025-11-05", word = "cat", imageRes = R.drawable.camera),
            LogEntry(dateIso = "2025-11-05", word = "dog", imageRes = R.drawable.frame)
        )

        val items: List<LogUiItem> = buildSectionedItems(sampleEntries)
        val adapter = SectionedLogAdapter(items)

        val grid = GridLayoutManager(this, 2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (items[position] is LogUiItem.Header) 2 else 1
            }
        }
        recycler.layoutManager = grid
        recycler.adapter = adapter

        // Ensure bottom bar stays above Android navigation bar by applying system bar insets as padding
        val root = findViewById<android.view.View>(R.id.log_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // bottom nav
        findViewById<android.widget.TextView>(R.id.nav_home)?.setOnClickListener {
            startActivity(android.content.Intent(this, MainActivity::class.java))
        }
        findViewById<android.widget.TextView>(R.id.nav_quiz)?.setOnClickListener {
            startActivity(android.content.Intent(this, QuizActivity::class.java))
        }

        // TODO: Replace with repository observing DB changes
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

class SectionedLogAdapter(private val items: List<LogUiItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
            is LogUiItem.EntryItem -> (holder as LogEntryViewHolder).bind(item.entry)
        }
    }

    override fun getItemCount(): Int = items.size
}

class LogEntryViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val dateView: android.widget.TextView = itemView.findViewById(R.id.txtDate)
    private val wordView: android.widget.TextView = itemView.findViewById(R.id.txtWord)
    private val imageView: android.widget.ImageView = itemView.findViewById(R.id.imgPhoto)

    fun bind(entry: LogEntry) {
        // date shown in section header; keep here for accessibility/testing if needed
        dateView.text = entry.dateIso
        wordView.text = entry.word
        imageView.setImageResource(entry.imageRes)
    }
}

class HeaderViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val sectionDate: android.widget.TextView = itemView.findViewById(R.id.txtSectionDate)
    fun bind(dateIso: String) {
        sectionDate.text = dateIso
    }
}


