package com.example.chalkak.domain.quiz

import androidx.lifecycle.LifecycleCoroutineScope
import com.example.chalkak.data.local.AppDatabase
import com.example.chalkak.domain.quiz.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * Manager class for spaced repetition learning algorithm
 * Handles study queue management and answer processing
 * 
 * Algorithm:
 * - Correct answer: Update lastStudied date, remove from queue (word moves to back of review cycle)
 * - Wrong answer: Add back to end of queue (word appears again soon in same session)
 */
class SpacedRepetitionManager(
    private val roomDb: AppDatabase,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    private val studyQueue = ArrayDeque<QuizQuestion>()
    private val wordToObjectIdMap = mutableMapOf<String, Long>()
    
    /**
     * Initialize study queue with questions
     */
    fun initializeQuestions(questions: List<QuizQuestion>, wordToObjectIdMap: Map<String, Long>) {
        studyQueue.clear()
        studyQueue.addAll(questions)
        this.wordToObjectIdMap.clear()
        this.wordToObjectIdMap.putAll(wordToObjectIdMap)
    }
    
    /**
     * Get next question from queue
     * @return QuizQuestion if available, null if queue is empty
     */
    fun getNextQuestion(): QuizQuestion? {
        return if (studyQueue.isNotEmpty()) {
            studyQueue.removeFirst()
        } else {
            null
        }
    }
    
    /**
     * Handle correct answer
     * Updates lastStudied date in database with the current quiz completion time (word moves to back of review cycle)
     * 
     * @param word English word that was answered correctly
     * @param parentPhotoId Photo ID to get createdAt from PhotoLog
     */
    fun handleCorrectAnswer(word: String, parentPhotoId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use "now" as the actual study time instead of the photo's captured time
                val studiedAt = System.currentTimeMillis()
                
                // Get all objects with the same word and update their lastStudied with the photo's createdAt
                val allObjectsWithSameWord = roomDb.detectedObjectDao().getAllObjectsByEnglishWord(word)
                allObjectsWithSameWord.forEach { obj ->
                    roomDb.detectedObjectDao().updateLastStudied(obj.englishWord, studiedAt)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Handle wrong answer
     * Adds question back to end of queue (word appears again soon in same session)
     * Does NOT update lastStudied date
     * 
     * @param question QuizQuestion that was answered incorrectly
     */
    fun handleWrongAnswer(question: QuizQuestion) {
        // Add to end of queue so it appears again later in the session
        studyQueue.addLast(question)
    }
    
    /**
     * Check if study queue is empty
     */
    fun isEmpty(): Boolean = studyQueue.isEmpty()
    
    /**
     * Get remaining questions count
     */
    fun getRemainingCount(): Int = studyQueue.size
    
    /**
     * Get total questions count (including completed)
     * Note: This should be set by Fragment with initial count
     */
    private var totalCount = 0
    
    fun setTotalCount(count: Int) {
        totalCount = count
    }
    
    fun getTotalCount(): Int = totalCount
}
