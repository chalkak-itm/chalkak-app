package com.example.chalkak.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chalkak.data.local.AppDatabase
import com.example.chalkak.data.local.ExampleSentence
import com.example.chalkak.data.remote.firestore.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for loading word data from Room DB and Firebase
 * Centralizes word data loading logic to reduce code duplication
 */
class WordDataLoaderHelper(
    private val context: android.content.Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val roomDb by lazy { AppDatabase.getInstance(context) }
    private val firestoreRepo = FirestoreRepository()

    /**
     * Data class to hold word data for UI display
     */
    data class WordData(
        val koreanMeaning: String,
        val exampleSentence: String,
        val exampleTranslation: String
    )

    /**
     * Callback interface for UI updates
     */
    interface WordDataCallback {
        fun onLoading()
        fun onSuccess(data: WordData)
        fun onError(message: String)
    }

    /**
     * Load word data from local database, with automatic fallback to Firebase if needed
     * @param word English word to load
     * @param objectId Optional objectId to prioritize (for LogEntry)
     * @param callback Callback for UI updates
     */
    fun loadWordData(
        word: String,
        objectId: Long? = null,
        callback: WordDataCallback
    ) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                callback.onLoading()

                // Try to get object by objectId first, then by word
                // Room suspend functions automatically use IO dispatcher
                val detectedObject = if (objectId != null) {
                    roomDb.detectedObjectDao().getObjectById(objectId)
                        ?: roomDb.detectedObjectDao().getObjectByEnglishWord(word)
                } else {
                    roomDb.detectedObjectDao().getObjectByEnglishWord(word)
                }

                if (detectedObject != null) {
                    val koreanMeaning = detectedObject.koreanMeaning.ifEmpty { "Loading meaning..." }

                    // Search for examples across all objectIds with the same word
                    val allObjectsWithSameWord = roomDb.detectedObjectDao().getAllObjectsByEnglishWord(word)

                    // Load all example sentences at once to avoid N+1 queries
                    val allExampleSentences = roomDb.exampleSentenceDao().getAllExampleSentences()
                    val examplesByWordId = allExampleSentences.groupBy { it.wordId }

                    var examples = emptyList<ExampleSentence>()
                    for (obj in allObjectsWithSameWord) {
                        val objExamples = examplesByWordId[obj.objectId] ?: emptyList()
                        if (objExamples.isNotEmpty()) {
                            examples = objExamples
                            break
                        }
                    }

                    if (examples.isNotEmpty()) {
                        val randomExample = examples.random()
                        // Switch to Main dispatcher for UI callback
                        withContext(Dispatchers.Main) {
                            callback.onSuccess(
                                WordData(
                                    koreanMeaning = koreanMeaning,
                                    exampleSentence = randomExample.sentence,
                                    exampleTranslation = randomExample.translation
                                )
                            )
                        }
                    } else {
                        // No examples found, load from Firebase
                        loadWordDataFromFirebase(word, detectedObject.objectId, callback)
                    }
                } else {
                    // Word not in DB, load from Firebase
                    loadWordDataFromFirebase(word, null, callback)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // On error, try Firebase
                loadWordDataFromFirebase(word, objectId, callback)
            }
        }
    }

    /**
     * Load word data from Firebase/GPT
     * @param word English word to load
     * @param primaryObjectId Optional objectId to prioritize
     * @param callback Callback for UI updates
     */
    private fun loadWordDataFromFirebase(
        word: String,
        primaryObjectId: Long? = null,
        callback: WordDataCallback
    ) {
        firestoreRepo.fetchWordFromGPT(
            word = word,
            onSuccess = { wordDto ->
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        // Save to Firebase
                        firestoreRepo.saveWordToFirebase(wordDto)

                        // Get all objects with the same word
                        val allObjectsWithSameWord = withContext(Dispatchers.IO) {
                            roomDb.detectedObjectDao().getAllObjectsByEnglishWord(word)
                        }

                        if (allObjectsWithSameWord.isNotEmpty()) {
                            // Update meaning and save examples for all objects with the same word
                            withContext(Dispatchers.IO) {
                                allObjectsWithSameWord.forEach { obj ->
                                    // Update meaning
                                    roomDb.detectedObjectDao().updateMeaning(word, wordDto.meaning)

                                    // Save examples (check for duplicates)
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

                            // Get examples for UI update
                            val targetObjectId = primaryObjectId ?: allObjectsWithSameWord.first().objectId
                            val allExamples = withContext(Dispatchers.IO) {
                                roomDb.exampleSentenceDao().getSentencesByWordId(targetObjectId)
                            }

                            // Switch to Main dispatcher for UI callback
                            withContext(Dispatchers.Main) {
                                if (allExamples.isNotEmpty()) {
                                    val randomExample = allExamples.random()
                                    callback.onSuccess(
                                        WordData(
                                            koreanMeaning = wordDto.meaning,
                                            exampleSentence = randomExample.sentence,
                                            exampleTranslation = randomExample.translation
                                        )
                                    )
                                } else {
                                    callback.onSuccess(
                                        WordData(
                                            koreanMeaning = wordDto.meaning,
                                            exampleSentence = "",
                                            exampleTranslation = ""
                                        )
                                    )
                                }
                            }
                        } else {
                            // New word (shouldn't normally happen)
                            withContext(Dispatchers.Main) {
                                if (wordDto.examples.isNotEmpty()) {
                                    val randomExample = wordDto.examples.random()
                                    callback.onSuccess(
                                        WordData(
                                            koreanMeaning = wordDto.meaning,
                                            exampleSentence = randomExample.sentence,
                                            exampleTranslation = randomExample.translation
                                        )
                                    )
                                } else {
                                    callback.onSuccess(
                                        WordData(
                                            koreanMeaning = wordDto.meaning,
                                            exampleSentence = "",
                                            exampleTranslation = ""
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val errorMessage = when {
                            e.message?.contains("network", ignoreCase = true) == true ||
                            e.message?.contains("timeout", ignoreCase = true) == true ||
                            e.message?.contains("connection", ignoreCase = true) == true ->
                                "Network error. Please check your connection and try again."
                            e.message?.contains("permission", ignoreCase = true) == true ->
                                "Permission denied. Please check your settings."
                            e.message?.contains("invalid", ignoreCase = true) == true ->
                                "Invalid word data. Please try again."
                            else -> "Failed to load data. Please try again later."
                        }
                        withContext(Dispatchers.Main) {
                            callback.onError(errorMessage)
                        }
                    }
                }
            },
            onFailure = { e ->
                // Error handling for Firebase/GPT call failure
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("connection", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        "Permission denied. Please check your settings."
                    e.message?.contains("invalid", ignoreCase = true) == true ->
                        "Invalid word data. Please try again."
                    else -> "Failed to load data. Please try again later."
                }
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    callback.onError(errorMessage)
                }
            }
        )
    }
}
