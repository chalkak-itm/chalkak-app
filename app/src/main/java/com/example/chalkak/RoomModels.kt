package com.example.chalkak

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

// picture log table
@Entity(tableName = "photo_logs")
data class PhotoLog(
    @PrimaryKey(autoGenerate = true) val photoId: Long = 0,
    @ColumnInfo(name = "local_image_path") val localImagePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// detected table
@Entity(
    tableName = "detected_objects",
    foreignKeys = [
        ForeignKey(
            entity = PhotoLog::class,
            parentColumns = ["photoId"],
            childColumns = ["parent_photo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["parent_photo_id"]),
        androidx.room.Index(value = ["english_word"])
    ]
)
data class DetectedObject(
    @PrimaryKey(autoGenerate = true) val objectId: Long = 0,
    @ColumnInfo(name = "parent_photo_id") val parentPhotoId: Long,

    @ColumnInfo(name = "english_word") val englishWord: String,     // example: "Apple"
    @ColumnInfo(name = "korean_meaning") val koreanMeaning: String, // example: "사과"

    @ColumnInfo(name = "bounding_box") val boundingBox: String,     // location
    @ColumnInfo(name = "last_studied") val lastStudied: Long = System.currentTimeMillis() // For synchronization
)

// example table
@Entity(
    tableName = "example_sentences",
    foreignKeys = [
        ForeignKey(
            entity = DetectedObject::class,
            parentColumns = ["objectId"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["word_id"])]
)
data class ExampleSentence(
    @PrimaryKey(autoGenerate = true) val sentenceId: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,

    val sentence: String,
    val translation: String
)