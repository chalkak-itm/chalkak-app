package com.example.chalkak

import DetectionResultItem
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var currentFragmentTag: String = "home"
    private var currentMainNavigationTag: String = "home" // Track main navigation fragment
    private var backPressedTime: Long = 0
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val backPressRunnable = Runnable { backPressedTime = 0 }
    private var bottomNavContainer: View? = null
    private val roomDb by lazy { AppDatabase.getInstance(this) }
    private val firestoreRepo = FirestoreRepository()

    companion object {
        private val MAIN_NAVIGATION_TAGS = setOf("home", "log", "quiz", "setting")
        
        // Constants for word processing
        private const val MAX_DETECTED_ITEMS = 2
        private const val MIN_EXAMPLES_REQUIRED = 3
    }
    
    fun getCurrentMainNavigationTag(): String {
        return currentMainNavigationTag
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Setup bottom navigation
        setupBottomNavigation()

        // Apply WindowInsets using helper
        val root = findViewById<android.view.View>(R.id.main_container)
        bottomNavContainer = findViewById(R.id.bottom_nav_container)
        
        WindowInsetsHelper.applyToActivity(
            rootView = root,
            bottomNavContainer = bottomNavContainer,
            resources = resources,
            onBottomNavInsetsApplied = {
                // Apply bottom padding to fragment container based on bottom nav height
                applyBottomPaddingToFragments()
            }
        )
        
        // Initial padding application after layout
        bottomNavContainer?.post {
            applyBottomPaddingToFragments()
        }

        // Handle Intent to navigate to specific fragment
        val fragmentTag = intent.getStringExtra("fragment_tag")
        val fragmentType = intent.getStringExtra("fragment_type")
        if (savedInstanceState == null) {
            when {
                fragmentType != null -> {
                    handleFragmentType(fragmentType)
                }
                fragmentTag != null -> {
                    navigateToFragmentByTag(fragmentTag)
                }
                else -> {
                    navigateToFragment(HomeFragment(), "home")
                }
            }
        } else {
            // Restore current fragment tag
            currentFragmentTag = savedInstanceState.getString("current_fragment_tag", "home")
            currentMainNavigationTag = savedInstanceState.getString("current_main_navigation_tag", "home")
            updateNavigationHighlight(currentMainNavigationTag)
        }

        // Setup back press handler for double tap to exit
        setupBackPressHandler()

        // Initialize navigation icons with default color
        initializeNavigationIcons()

        // Sync words from Firebase on app start
        syncWordsFromFirebase()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val fragmentTag = intent.getStringExtra("fragment_tag")
        val fragmentType = intent.getStringExtra("fragment_type")
        when {
            fragmentType != null -> {
                handleFragmentType(fragmentType)
            }
            fragmentTag != null -> {
                navigateToFragmentByTag(fragmentTag)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_fragment_tag", currentFragmentTag)
        outState.putString("current_main_navigation_tag", currentMainNavigationTag)
    }

    private fun setupBottomNavigation() {
        findViewById<android.view.View>(R.id.nav_home)?.setOnClickListener {
            navigateToFragment(HomeFragment(), "home")
        }
        findViewById<android.view.View>(R.id.nav_log)?.setOnClickListener {
            navigateToFragment(LogFragment(), "log")
        }
        findViewById<android.view.View>(R.id.nav_quiz)?.setOnClickListener {
            navigateToFragment(QuizFragment(), "quiz")
        }
        findViewById<android.view.View>(R.id.nav_setting)?.setOnClickListener {
            navigateToFragment(SettingFragment(), "setting")
        }
    }

    fun navigateToFragment(fragment: Fragment, tag: String) {
        // If navigating to a main navigation fragment, clear back stack
        // Otherwise, add to back stack for proper navigation
        if (tag in MAIN_NAVIGATION_TAGS) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit()
        } else {
            // Add to back stack for sub-fragments
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commit()
        }
        
        currentFragmentTag = tag
        
        // Only update navigation highlight if navigating to a main navigation fragment
        if (tag in MAIN_NAVIGATION_TAGS) {
            currentMainNavigationTag = tag
            updateNavigationHighlight(tag)
        } else {
            // Keep the previous main navigation highlight
            updateNavigationHighlight(currentMainNavigationTag)
        }
        
        // Apply bottom padding after fragment is added
        supportFragmentManager.executePendingTransactions()
        applyBottomPaddingToFragments()
    }

    private fun navigateToFragmentByTag(tag: String) {
        val fragment = when (tag) {
            "home" -> HomeFragment()
            "log" -> LogFragment()
            "quiz" -> QuizFragment()
            "setting" -> SettingFragment()
            "magic_adventure" -> MagicAdventureFragment()
            else -> HomeFragment()
        }
        navigateToFragment(fragment, tag)
    }

    private fun handleFragmentType(fragmentType: String) {
        val fragment = when (fragmentType) {
            "object_input" -> {
                val imagePath = intent.getStringExtra("image_path")
                val mainNavTag = intent.getStringExtra("main_nav_tag") ?: "home"
                ObjectInputFragment.newInstance(imagePath, mainNavTag)
            }
            "detection_result" -> {
                val imagePath = intent.getStringExtra("image_path")
                val detectionResults = intent.getParcelableArrayListExtra<DetectionResultItem>("detection_results") ?: emptyList()
                val mainNavTag = intent.getStringExtra("main_nav_tag") ?: "home"

                if (detectionResults.isNotEmpty() && imagePath != null) {
                    processDetectedWords(detectionResults, imagePath)
                }

                DetectionResultFragment.newInstance(imagePath, detectionResults, mainNavTag)
            }
            else -> HomeFragment()
        }
        val tag = when (fragmentType) {
            "object_input" -> "object_input"
            "detection_result" -> "detection_result"
            else -> "home"
        }
        navigateToFragment(fragment, tag)
    }

    private fun initializeNavigationIcons() {
        // Clear any color filters to show original icon colors
        val iconIds = listOf(
            R.id.nav_home_icon,
            R.id.nav_log_icon,
            R.id.nav_quiz_icon,
            R.id.nav_setting_icon
        )
        iconIds.forEach { iconId ->
            findViewById<android.widget.ImageView>(iconId)?.clearColorFilter()
        }
    }

    private fun updateNavigationHighlight(tag: String) {
        val navigationItems = listOf(
            Triple("home", R.id.nav_home, R.id.nav_home_icon),
            Triple("log", R.id.nav_log, R.id.nav_log_icon),
            Triple("quiz", R.id.nav_quiz, R.id.nav_quiz_icon),
            Triple("setting", R.id.nav_setting, R.id.nav_setting_icon)
        )
        
        navigationItems.forEach { (itemTag, layoutId, iconId) ->
            val layout = findViewById<android.view.View>(layoutId)
            val icon = findViewById<android.widget.ImageView>(iconId)
            
            if (tag == itemTag) {
                layout?.setBackgroundResource(R.drawable.bg_nav_item_selected)
                layout?.elevation = 4f
                icon?.alpha = 1.0f
            } else {
                layout?.setBackgroundResource(R.drawable.bg_nav_item_unselected)
                layout?.elevation = 0f
                icon?.alpha = 1.0f
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if current fragment is one of the main navigation screens
                val isMainScreen = currentFragmentTag in MAIN_NAVIGATION_TAGS
                
                if (isMainScreen) {
                    // Double tap to exit
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - backPressedTime > 500) {
                        // First back press
                        backPressedTime = currentTime
                        Toast.makeText(
                            this@MainActivity,
                            "Press again to exit",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Reset after 0.5 seconds
                        backPressHandler.removeCallbacks(backPressRunnable)
                        backPressHandler.postDelayed(backPressRunnable, 500)
                    } else {
                        // Second back press within 0.5 seconds - exit app
                        backPressHandler.removeCallbacks(backPressRunnable)
                        finish()
                    }
                } else {
                    // Not on main screen - navigate to home
                    navigateToFragment(HomeFragment(), "home")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressHandler.removeCallbacks(backPressRunnable)
    }
    
    private fun applyBottomPaddingToFragments() {
        bottomNavContainer?.let { navContainer ->
            val bottomNavHeight = navContainer.height
            val systemBars = ViewCompat.getRootWindowInsets(navContainer)?.getInsets(WindowInsetsCompat.Type.systemBars())
            val totalBottomPadding = bottomNavHeight + (systemBars?.bottom ?: 0) + 16 // 16dp extra space
            
            val fragmentContainer = findViewById<View>(R.id.fragment_container)
            fragmentContainer?.post {
                applyPaddingToScrollableViews(fragmentContainer, totalBottomPadding)
            }
        }
    }
    
    private fun applyPaddingToScrollableViews(parent: View, padding: Int) {
        when (parent) {
            is androidx.recyclerview.widget.RecyclerView -> {
                // Log screen RecyclerView needs less padding
                val isLogRecyclerView = parent.id == R.id.recyclerLog
                val actualPadding = if (isLogRecyclerView) {
                    // For log screen, use reduced padding (about 75% of full padding)
                    (padding * 0.75).toInt()
                } else {
                    padding
                }
                parent.setPadding(
                    parent.paddingLeft,
                    parent.paddingTop,
                    parent.paddingRight,
                    actualPadding
                )
                return // Don't recurse into RecyclerView children
            }
            is android.widget.ScrollView -> {
                if (parent.childCount > 0) {
                    val child = parent.getChildAt(0)
                    // Setting screen ScrollView needs less padding
                    val isSettingScrollView = parent.id == R.id.scroll_setting
                    val actualPadding = if (isSettingScrollView) {
                        // For setting screen, use minimal padding
                        (padding * 0.8).toInt() // Reduce padding by 60%
                    } else {
                        padding
                    }
                    child.setPadding(
                        child.paddingLeft,
                        child.paddingTop,
                        child.paddingRight,
                        actualPadding
                    )
                }
                return // Don't recurse into ScrollView children
            }
        }
        
        // For other views, also check if they need padding (like HomeFragment's spacer View)
        if (parent is android.view.ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                // Apply padding to spacer views in HomeFragment
                if (child is View && child.layoutParams is android.widget.LinearLayout.LayoutParams) {
                    val params = child.layoutParams as android.widget.LinearLayout.LayoutParams
                    if (params.height == 0 && params.weight == 0f) {
                        params.height = padding
                        child.layoutParams = params
                    }
                }
                applyPaddingToScrollableViews(child, padding)
            }
        }
    }

    // Update: Added imagePath parameter
    // Update: Logic changed to use addNewWordCount & updateReviewTime
    // Optimized: Batch loading to avoid N+1 queries
    fun processDetectedWords(results: List<DetectionResultItem>, imagePath: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            // 1. Save Photo
            val photoLog = PhotoLog(localImagePath = imagePath)
            val newPhotoId = roomDb.photoLogDao().insert(photoLog)

            // 2. Process Items (Max MAX_DETECTED_ITEMS)
            val itemsToProcess = results.take(MAX_DETECTED_ITEMS)
            
            // Batch load all existing objects for detected words to avoid N+1 queries
            val detectedWords = itemsToProcess.map { it.label }
            val allExistingObjects = roomDb.detectedObjectDao().getAllDetectedObjects()
                .filter { it.englishWord in detectedWords }
            
            // Group by english word for efficient lookup
            val existingObjectsMap = allExistingObjects.groupBy { it.englishWord.lowercase() }
                .mapValues { it.value.firstOrNull() } // Get first object for each word
            
            // Batch load all example sentences for existing objects
            val existingObjectIds = allExistingObjects.map { it.objectId }
            val allExampleSentences = if (existingObjectIds.isNotEmpty()) {
                roomDb.exampleSentenceDao().getAllExampleSentences()
                    .filter { it.wordId in existingObjectIds }
            } else {
                emptyList()
            }
            
            // Group examples by wordId for efficient lookup
            val examplesByWordId = allExampleSentences.groupBy { it.wordId }
            
            itemsToProcess.forEach { item ->
                val detectedWord = item.label
                val wordLowercase = detectedWord.lowercase()
                val existingObject = existingObjectsMap[wordLowercase]
                val isKnown = existingObject != null

                val hasEnoughExamples = if (isKnown && existingObject != null) {
                    val examples = examplesByWordId[existingObject.objectId] ?: emptyList()
                    examples.size >= MIN_EXAMPLES_REQUIRED
                } else {
                    false
                }

                if (isKnown && hasEnoughExamples) {
                    // 단어가 이미 있고 예문이 3개 이상이면 GPT 호출 스킵
                    existingObject?.let { obj ->
                        val boxString = "[${item.left}, ${item.top}, ${item.right}, ${item.bottom}]"
                        roomDb.detectedObjectDao().insert(
                            DetectedObject(
                                parentPhotoId = newPhotoId,
                                englishWord = detectedWord,
                                koreanMeaning = obj.koreanMeaning,
                                boundingBox = boxString
                            )
                        )
                    }
                    firestoreRepo.updateReviewTime(uid, detectedWord)
                    roomDb.detectedObjectDao().updateLastStudied(detectedWord, System.currentTimeMillis())
                    val exampleCount = if (existingObject != null) {
                        examplesByWordId[existingObject.objectId]?.size ?: 0
                    } else {
                        0
                    }
                    Log.d("WordCheck", "$detectedWord: Already exists with $exampleCount examples, skipping GPT")
                } else {
                    // 새 단어이거나 예문이 3개 미만이면 GPT 호출
                    if (!isKnown) {
                        firestoreRepo.addNewWordCount(uid)
                    }
                    val boxString = "[${item.left}, ${item.top}, ${item.right}, ${item.bottom}]"
                    if (!isKnown) {
                        val newObject = DetectedObject(
                            parentPhotoId = newPhotoId,
                            englishWord = detectedWord,
                            koreanMeaning = "Searching...",
                            boundingBox = boxString
                        )
                        roomDb.detectedObjectDao().insert(newObject)
                    } else {
                        existingObject?.let { obj ->
                            roomDb.detectedObjectDao().insert(
                                DetectedObject(
                                    parentPhotoId = newPhotoId,
                                    englishWord = detectedWord,
                                    koreanMeaning = obj.koreanMeaning,
                                    boundingBox = boxString
                                )
                            )
                        }
                    }
                    firestoreRepo.fetchWordFromGPT(
                        word = detectedWord,
                        onSuccess = { wordDto ->
                            lifecycleScope.launch {
                                try {
                                    firestoreRepo.saveWordToFirebase(wordDto)
                                    Log.d("GPT", "Saved to Firebase: $detectedWord")
                                    if (!isKnown) {
                                        roomDb.detectedObjectDao().updateMeaning(detectedWord, wordDto.meaning)
                                    }
                                    // Reload object to get updated data (including newly inserted ones)
                                    val detectedObject = roomDb.detectedObjectDao().getObjectByEnglishWord(detectedWord)
                                    detectedObject?.let { obj ->
                                        // Reload examples for this specific object
                                        val existingExamples = roomDb.exampleSentenceDao().getSentencesByWordId(obj.objectId)
                                        val existingSentences = existingExamples.map { it.sentence }.toSet()
                                        wordDto.examples.forEach { example ->
                                            if (!existingSentences.contains(example.sentence)) {
                                                roomDb.exampleSentenceDao().insert(
                                                    ExampleSentence(
                                                        wordId = obj.objectId,
                                                        sentence = example.sentence,
                                                        translation = example.translation
                                                    )
                                                )
                                            }
                                        }
                                        val totalExamples = roomDb.exampleSentenceDao().getSentencesByWordId(obj.objectId).size
                                        Log.d("GPT", "Saved ${wordDto.examples.size} examples to Room DB for: $detectedWord (Total: $totalExamples)")
                                    }
                                } catch (e: Exception) {
                                    Log.e("GPT", "Error saving word data", e)
                                    runOnUiThread {
                                        Toast.makeText(this@MainActivity, "저장 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onFailure = { e ->
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "GPT 오류: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            Log.e("GPT", "Fail", e)
                        }
                    )
                }
            }
        }
    }

    private fun syncWordsFromFirebase() {
        lifecycleScope.launch {
            try {
                val words = firestoreRepo.getAllWordsFromFirebase()
                val existingPhotos = roomDb.photoLogDao().getAllPhotos()
                val dummyPhoto = existingPhotos.find { it.localImagePath == "firebase_sync" }

                val dummyPhotoId = if (dummyPhoto == null) {
                    roomDb.photoLogDao().insert(
                        PhotoLog(
                            localImagePath = "firebase_sync",
                            createdAt = 0
                        )
                    )
                } else {
                    dummyPhoto.photoId
                }

                words.forEach { wordDto ->
                    val exists = roomDb.detectedObjectDao().isWordExist(wordDto.originalWord)
                    if (!exists) {
                        val objectId = roomDb.detectedObjectDao().insert(
                            DetectedObject(
                                parentPhotoId = dummyPhotoId,
                                englishWord = wordDto.originalWord,
                                koreanMeaning = wordDto.meaning,
                                boundingBox = "",
                                lastStudied = 0
                            )
                        )
                        wordDto.examples.forEach { example ->
                            roomDb.exampleSentenceDao().insert(
                                ExampleSentence(
                                    wordId = objectId,
                                    sentence = example.sentence,
                                    translation = example.translation
                                )
                            )
                        }
                    } else {
                        roomDb.detectedObjectDao().updateMeaning(
                            wordDto.originalWord,
                            wordDto.meaning
                        )
                        val existingObject = roomDb.detectedObjectDao().getObjectByEnglishWord(wordDto.originalWord)
                        existingObject?.let { obj ->
                            val existingExamples = roomDb.exampleSentenceDao().getSentencesByWordId(obj.objectId)
                            val existingSentences = existingExamples.map { it.sentence }.toSet()
                            wordDto.examples.forEach { example ->
                                if (!existingSentences.contains(example.sentence)) {
                                    roomDb.exampleSentenceDao().insert(
                                        ExampleSentence(
                                            wordId = obj.objectId,
                                            sentence = example.sentence,
                                            translation = example.translation
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Log.d("Sync", "Synced ${words.size} words from Firebase")
            } catch (e: Exception) {
                Log.e("Sync", "Error syncing words from Firebase", e)
            }
        }
    }
}
