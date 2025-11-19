package com.example.chalkak

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

// For log
@Dao
interface PhotoLogDao {
    @Insert
    suspend fun insert(photoLog: PhotoLog): Long // get picture's ID

    @Query("SELECT * FROM photo_logs ORDER BY created_at DESC")
    suspend fun getAllPhotos(): List<PhotoLog>
}

// For Word object
@Dao
interface DetectedObjectDao {
    @Insert
    suspend fun insert(obj: DetectedObject): Long

    // If word exist? 1 : 0
    @Query("SELECT EXISTS(SELECT 1 FROM detected_objects WHERE english_word = :word LIMIT 1)")
    suspend fun isWordExist(word: String): Boolean

    // update last learning time
    @Query("UPDATE detected_objects SET last_studied = :timestamp WHERE english_word = :word")
    suspend fun updateLastStudied(word: String, timestamp: Long)

    // getting words
    @Query("SELECT * FROM detected_objects WHERE parent_photo_id = :photoId")
    suspend fun getObjectsByPhotoId(photoId: Long): List<DetectedObject>

    // update words
    @Query("UPDATE detected_objects SET korean_meaning = :meaning WHERE english_word = :word")
    suspend fun updateMeaning(word: String, meaning: String)
}

// For examples
@Dao
interface ExampleSentenceDao {
    @Insert
    suspend fun insert(sentence: ExampleSentence)

    @Query("SELECT * FROM example_sentences WHERE word_id = :wordId")
    suspend fun getSentencesByWordId(wordId: Long): List<ExampleSentence>
}