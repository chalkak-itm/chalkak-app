package com.example.chalkak.domain.quiz

import com.example.chalkak.data.local.AppDatabase
import com.example.chalkak.data.local.DetectedObject
import com.example.chalkak.data.local.ExampleSentence
import com.example.chalkak.data.local.PhotoLog
import com.example.chalkak.domain.quiz.QuizQuestion
import java.io.File

/**
 * Helper class for generating quiz questions from database
 * Handles data loading, filtering, and question creation logic
 */
object QuizQuestionGenerator {
    // Constants for quiz configuration
    private const val MIN_OBJECTS_FOR_QUIZ = 1 // Minimum 1 object needed to create quiz
    private const val MAX_QUIZ_QUESTIONS = 20 // Spaced repetition: take top 20 words
    private const val MIN_OPTIONS_REQUIRED = 2 // Minimum 2 options needed (1 correct + 1 wrong)
    
    /**
     * Generate quiz questions using spaced repetition algorithm
     * Selects words that haven't been studied for the longest time
     * 
     * @param roomDb AppDatabase instance
     * @param wordToObjectIdMap Output map to store word to objectId mapping for updating lastStudied
     * @return List of QuizQuestion objects, empty if not enough data
     */
    suspend fun generateQuizQuestions(
        roomDb: AppDatabase,
        wordToObjectIdMap: MutableMap<String, Long>
    ): List<QuizQuestion> {
        // Load all data in parallel to avoid N+1 query problem
        val allPhotos = roomDb.photoLogDao().getAllPhotos()
        val photosMap = allPhotos.associateBy { it.photoId }
        
        // Get all detected objects with valid korean meaning AND existing photo file
        val allDetectedObjects = roomDb.detectedObjectDao().getAllDetectedObjects()
            .filter { 
                it.koreanMeaning.isNotEmpty() && 
                it.koreanMeaning != "Searching..." &&
                photosMap.containsKey(it.parentPhotoId) && // Photo must exist
                photosMap[it.parentPhotoId]?.localImagePath != null && // Image path must exist
                File(photosMap[it.parentPhotoId]!!.localImagePath).exists() // File must actually exist
            }
        
        if (allDetectedObjects.isEmpty()) {
            // Need at least 1 object with photos to create questions
            return emptyList()
        }
        
        // Load all example sentences at once to avoid repeated queries
        val allExampleSentences = roomDb.exampleSentenceDao().getAllExampleSentences()
        // Group examples by wordId for efficient lookup
        val examplesByWordId = allExampleSentences.groupBy { it.wordId }
        
        // Load ALL detected objects (not filtered) for example sentence lookup
        // This ensures we can find examples even if the current object doesn't have one
        val allDetectedObjectsUnfiltered = roomDb.detectedObjectDao().getAllDetectedObjects()
        // Group all objects by english word (lowercase) for example lookup
        val allObjectsByWord = allDetectedObjectsUnfiltered.groupBy { it.englishWord.lowercase() }
        
        // Group objects by photoId for efficient lookup
        val objectsByPhotoId = allDetectedObjects.groupBy { it.parentPhotoId }
        
        // Group objects by english word (lowercase) for efficient lookup
        // This grouping will be reused for both word selection and lookup
        val objectsByWord = allDetectedObjects.groupBy { it.englishWord.lowercase() }
        
        // Get all unique English words from DB for options in a single pass
        // Filter empty words and get distinct words efficiently
        val allUniqueWords = mutableSetOf<String>()
        allDetectedObjects.forEach { obj ->
            if (obj.englishWord.isNotEmpty()) {
                allUniqueWords.add(obj.englishWord)
            }
        }
        
        // Spaced Repetition Algorithm: Select words that haven't been studied for the longest time
        // Sort by lastStudied ASCENDING (oldest first) to prioritize words that need review
        val selectedObjects = objectsByWord.values.mapNotNull { objects ->
            // For each word group, select the object with the oldest lastStudied date
            objects.minByOrNull { it.lastStudied }
        }
            .sortedBy { it.lastStudied } // Sort by lastStudied ascending (oldest first)
            .take(MAX_QUIZ_QUESTIONS) // Take top MAX_QUIZ_QUESTIONS words that need review most
        
        if (selectedObjects.isEmpty()) {
            return emptyList()
        }
        
        val questions = mutableListOf<QuizQuestion>()
        
        for (wordObj in selectedObjects) {
            // Get image path from photo - use the EXACT photo for this specific object
            val photo = photosMap[wordObj.parentPhotoId]
            val imagePath = photo?.localImagePath
            
            // Skip if photo or image path is null, or if file doesn't exist
            if (imagePath == null || photo == null) {
                continue // Skip this question
            }
            
            // Double check: verify file actually exists (in case file was deleted after filtering)
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                continue // Skip this question if file doesn't exist
            }
            
            // Get bounding box for THIS SPECIFIC object - ensure it matches exactly
            val boundingBox = wordObj.boundingBox
            
            // Get example sentence from memory cache
            // Search in ALL objects with the same word (not just filtered ones)
            // This ensures we find examples even if the current object doesn't have one
            val allObjectsWithSameWord = allObjectsByWord[wordObj.englishWord.lowercase()] ?: emptyList()
            var example: ExampleSentence? = null
            for (obj in allObjectsWithSameWord) {
                val examples = examplesByWordId[obj.objectId] ?: emptyList()
                if (examples.isNotEmpty()) {
                    example = examples.random()
                    break
                }
            }
            
            // Generate wrong options from ALL words in DB (not just selectedObjects)
            // Exclude the current word and words from the same photo to avoid confusion
            val objectsInSamePhoto = objectsByPhotoId[wordObj.parentPhotoId] ?: emptyList()
            // Cache lowercase conversion for words in same photo
            val wordsInSamePhoto = objectsInSamePhoto.mapTo(mutableSetOf()) { it.englishWord.lowercase() }
            
            val wordObjLowercase = wordObj.englishWord.lowercase()
            // Filter and shuffle in a single pass
            // First try to get words excluding same photo
            val otherWords = allUniqueWords
                .asSequence()
                .filter { 
                    val wordLower = it.lowercase()
                    wordLower != wordObjLowercase && 
                    !wordsInSamePhoto.contains(wordLower)
                }
                .shuffled()
                .toList()
            
            // If not enough words excluding same photo, use any other words (excluding current word)
            val availableWords = if (otherWords.size < MIN_OPTIONS_REQUIRED - 1) {
                allUniqueWords
                    .asSequence()
                    .filter { it.lowercase() != wordObjLowercase }
                    .shuffled()
                    .toList()
            } else {
                otherWords
            }
            
            // Skip if we don't have at least 1 wrong option (minimum 2 total options needed)
            if (availableWords.isEmpty()) {
                continue // Skip this question if no other words available
            }
            
            // Create options list with available words (correct answer + available wrong answers, shuffled)
            // Use up to 3 wrong options if available, but accept any number >= 1
            val wrongOptions = availableWords.take(3) // Take up to 3 wrong options
            val options = (listOf(wordObj.englishWord) + wrongOptions).shuffled()
            
            val question = QuizQuestion(
                imagePath = imagePath,
                imageRes = null,
                boundingBox = boundingBox,
                englishWord = wordObj.englishWord,
                koreanWord = wordObj.koreanMeaning,
                exampleEnglish = example?.sentence ?: "",
                exampleKorean = example?.translation ?: "",
                correctAnswer = wordObj.englishWord,
                options = options,
                parentPhotoId = wordObj.parentPhotoId
            )
            questions.add(question)
            // Store mapping for updating lastStudied
            wordToObjectIdMap[wordObj.englishWord.lowercase()] = wordObj.objectId
        }
        
        return questions
    }
}
