package com.example.chalkak.domain.quiz

/**
 * Core quiz question model shared across domain/UI.
 */
data class QuizQuestion(
    val imagePath: String? = null,
    val imageRes: Int? = null, // Fallback for when imagePath is null
    val boundingBox: String? = null, // Bounding box string: "[left, top, right, bottom]"
    val englishWord: String,
    val koreanWord: String,
    val exampleEnglish: String,
    val exampleKorean: String,
    val correctAnswer: String, // English word
    val options: List<String>, // English words
    val parentPhotoId: Long // Photo ID for getting createdAt
)
