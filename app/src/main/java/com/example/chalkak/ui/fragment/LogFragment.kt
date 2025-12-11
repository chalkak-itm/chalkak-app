package com.example.chalkak.ui.fragment

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
import com.example.chalkak.R
import com.example.chalkak.base.BaseFragment
import com.example.chalkak.data.local.AppDatabase
import com.example.chalkak.ui.dialog.LogItemDetailDialogFragment
import com.example.chalkak.util.ImageLoaderHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LogFragment : BaseFragment() {
    private lateinit var headerDefault: LinearLayout
    private var dialogFragment: LogItemDetailDialogFragment? = null
    private val roomDb by lazy { AppDatabase.getInstance(requireContext()) }
    private lateinit var recycler: RecyclerView
    
    companion object {
        // Thread-safe SimpleDateFormat cache to avoid creating new instances
        private val dateFormatCache = ConcurrentHashMap<String, SimpleDateFormat>()
        
        private fun getDateFormat(pattern: String): SimpleDateFormat {
            return dateFormatCache.getOrPut(pattern) {
                SimpleDateFormat(pattern, Locale.getDefault())
            }
        }
    }

    override fun getCardWordDetailView(): View {
        // Not used anymore because DialogFragment is in place
        // initializeTtsAndSpeechRecognition in BaseFragment still runs,
        // so return a temporary view or handle null if needed
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
        txtTitle.text = getString(R.string.log)

        recycler = view.findViewById(R.id.recyclerLog)

        // Load data from Room database
        loadDataFromDatabase()
    }

    private fun loadDataFromDatabase() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load all data in parallel to avoid N+1 query problem
                val photos = roomDb.photoLogDao().getAllPhotos()
                val allDetectedObjects = roomDb.detectedObjectDao().getAllDetectedObjects()
                
                // Group detected objects by photoId for efficient lookup
                val objectsByPhotoId = allDetectedObjects.groupBy { it.parentPhotoId }
                
                val entries = mutableListOf<LogEntry>()
                // Reuse SimpleDateFormat instance from cache
                val dateFormat = getDateFormat("yyyy-MM-dd")

                photos.forEach { photo ->
                    if (photo.localImagePath != "firebase_sync") {
                        // Get objects from memory map instead of querying database
                        val detectedObjects = objectsByPhotoId[photo.photoId] ?: emptyList()
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
                
                // RecyclerView optimizations
                recycler.setHasFixedSize(true) // Item sizes are fixed, so keep as true
                recycler.setItemViewCacheSize(20) // Increase cache size (default is 2)
                recycler.setRecycledViewPool(RecyclerView.RecycledViewPool().apply {
                    // Pool sizes for multiple ViewHolder types
                    setMaxRecycledViews(SectionedLogAdapter.TYPE_HEADER, 5)
                    setMaxRecycledViews(SectionedLogAdapter.TYPE_ENTRY, 20)
                })
                
                // Keep item spacing consistent to ensure equal widths
                val spacing = resources.getDimensionPixelSize(R.dimen.padding_small)
                recycler.addItemDecoration(GridSpacingItemDecoration(2, spacing, true))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showItemDetail(entry: LogEntry) {
        // Close any existing dialog if open
        dialogFragment?.dismiss()
        
        // Create and show a new dialog
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
    val imagePath: String? = null, // Actual image path
    val imageRes: Int? = null, // For placeholder data (backward compatibility)
    val koreanMeaning: String? = null, // Korean meaning
    val objectId: Long? = null // ID of the DetectedObject
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
        const val TYPE_HEADER = 0
        const val TYPE_ENTRY = 1
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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is LogEntryViewHolder) {
            holder.onRecycled()
        }
    }

    override fun getItemCount(): Int = items.size
}

class LogEntryViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val wordView: android.widget.TextView = itemView.findViewById(R.id.txtWord)
    private val imageView: android.widget.ImageView = itemView.findViewById(R.id.imgPhoto)
    
    // Job for canceling image loading when ViewHolder is reused
    private var imageLoadJob: Job? = null
    
    // CoroutineScope for image loading
    private val viewHolderScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        // Cache for optimal image size calculation
        private var cachedOptimalSize: Pair<Int, Int>? = null
        private var cachedScreenWidth: Int = 0
    }

    /**
     * Calculate optimal image size for GridLayout item
     * Considers 2-column grid layout, padding, spacing, and image card dimensions
     * Results are cached to avoid recalculation
     */
    private fun calculateOptimalImageSize(): Pair<Int, Int> {
        val context = itemView.context
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        // Return cached value if screen width hasn't changed
        if (cachedOptimalSize != null && cachedScreenWidth == screenWidth) {
            return cachedOptimalSize!!
        }
        
        // Get RecyclerView padding (padding_standard = 16dp on each side)
        val recyclerPadding = resources.getDimensionPixelSize(R.dimen.padding_standard) * 2
        
        // Get spacing between items (padding_small = 8dp)
        val itemSpacing = resources.getDimensionPixelSize(R.dimen.padding_small)
        val availableWidth = screenWidth - recyclerPadding - itemSpacing
        
        // 2-column grid: each item gets half of available width
        val itemWidth = availableWidth / 2
        
        // Image card height from dimens (120dp)
        val imageCardHeightDp = 120
        val imageCardHeightPx = (imageCardHeightDp * displayMetrics.density).toInt()
        
        // Load at 1.5x size for high-density displays and smooth scaling
        val optimalWidth = (itemWidth * 1.5f).toInt()
        val optimalHeight = (imageCardHeightPx * 1.5f).toInt()
        
        val result = Pair(optimalWidth, optimalHeight)
        
        // Cache the result
        cachedOptimalSize = result
        cachedScreenWidth = screenWidth
        
        return result
    }

    fun bind(entry: LogEntry, onItemClick: (LogEntry) -> Unit) {
        // Cancel previous image loading job if exists
        imageLoadJob?.cancel()
        imageLoadJob = null
        
        // Clear previous image to avoid flickering when ViewHolder is reused
        imageView.setImageBitmap(null)
        
        wordView.text = entry.word
        wordView.visibility = View.VISIBLE
        
        // Use the actual image path when available; otherwise show a placeholder
        if (entry.imagePath != null) {
            val (maxWidth, maxHeight) = calculateOptimalImageSize()
            val imagePath = entry.imagePath
            
            // Set tag first to identify which image this ViewHolder is loading
            imageView.tag = imagePath
            
            // Load image in background thread
            imageLoadJob = viewHolderScope.launch {
                try {
                    // Load bitmap on IO dispatcher
                    val bitmap = withContext(Dispatchers.IO) {
                        ImageLoaderHelper.loadBitmapFromPath(imagePath, maxWidth, maxHeight)
                    }
                    
                    // Set bitmap on main thread only if job is still active and tag matches
                    if (isActive && imageView.tag == imagePath) {
                        imageView.setImageBitmap(bitmap)
                    }
                } catch (e: CancellationException) {
                    // Job was cancelled, ignore
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (entry.imageRes != null) {
            imageView.setImageResource(entry.imageRes)
            imageView.tag = null
        } else {
            imageView.tag = null
        }

        // Set click listener on the card
        itemView.setOnClickListener {
            onItemClick(entry)
        }
    }
    
    /**
     * Clean up resources when ViewHolder is recycled
     */
    fun onRecycled() {
        imageLoadJob?.cancel()
        imageLoadJob = null
        imageView.setImageBitmap(null)
        imageView.tag = null
    }
}

class HeaderViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val sectionDate: android.widget.TextView = itemView.findViewById(R.id.txtSectionDate)
    fun bind(dateIso: String) {
        sectionDate.text = dateIso
    }
}

/**
 * Item spacing helper for GridLayoutManager
 * Keeps spacing consistent so all items maintain the same width
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {
    
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: android.view.View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemViewType = parent.adapter?.getItemViewType(position) ?: -1
        
        // Header spans the full width because it uses spanCount columns
        if (itemViewType == 0) { // TYPE_HEADER
            if (includeEdge) {
                outRect.left = spacing
                outRect.right = spacing
                outRect.top = if (position < spanCount) spacing else spacing / 2
                outRect.bottom = spacing
            } else {
                outRect.left = 0
                outRect.right = 0
                outRect.top = if (position < spanCount) 0 else spacing / 2
                outRect.bottom = 0
            }
        } else {
            // Standard item (Entry)
            val column = position % spanCount
            
            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount
                outRect.top = spacing
                outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                outRect.top = spacing
                outRect.bottom = spacing
            }
        }
    }
}
