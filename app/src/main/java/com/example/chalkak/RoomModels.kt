package com.example.chalkak

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

// 1. 📸 사진 로그 테이블
@Entity(tableName = "photo_logs")
data class PhotoLog(
    @PrimaryKey(autoGenerate = true) val photoId: Long = 0,
    @ColumnInfo(name = "local_image_path") val localImagePath: String, // 갤러리/파일 경로
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// 2. 🍎 탐지된 객체(단어) 테이블
@Entity(
    tableName = "detected_objects",
    foreignKeys = [
        ForeignKey(
            entity = PhotoLog::class,
            parentColumns = ["photoId"],
            childColumns = ["parent_photo_id"],
            onDelete = ForeignKey.CASCADE // 사진 지우면 단어도 삭제
        )
    ]
)
data class DetectedObject(
    @PrimaryKey(autoGenerate = true) val objectId: Long = 0,
    @ColumnInfo(name = "parent_photo_id") val parentPhotoId: Long,

    @ColumnInfo(name = "english_word") val englishWord: String,     // 예: "Apple"
    @ColumnInfo(name = "korean_meaning") val koreanMeaning: String, // 예: "사과"

    @ColumnInfo(name = "bounding_box") val boundingBox: String,     // 박스 좌표 (JSON 등)
    @ColumnInfo(name = "last_studied") val lastStudied: Long = System.currentTimeMillis() // 학습 동기화용
)

// 3. 📝 예문 테이블 (단어 하나에 예문 여러 개)
@Entity(
    tableName = "example_sentences",
    foreignKeys = [
        ForeignKey(
            entity = DetectedObject::class,
            parentColumns = ["objectId"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE // 단어 지우면 예문도 삭제
        )
    ]
)
data class ExampleSentence(
    @PrimaryKey(autoGenerate = true) val sentenceId: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long, // DetectedObject의 objectId와 연결

    val sentence: String,       // 영어 예문
    val translation: String     // 한국어 해석
)