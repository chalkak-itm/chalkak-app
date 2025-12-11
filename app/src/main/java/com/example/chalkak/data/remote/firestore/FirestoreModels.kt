package com.example.chalkak.data.remote.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class WordDTO(
    val originalWord: String = "",
    val meaning: String = "",
    val examples: List<ExampleItem> = emptyList(),
    val createdAt: Timestamp? = null
)

data class ExampleItem(
    val sentence: String = "",
    val translation: String = ""
)

data class UserDTO(
    val nickname: String = "",
    val email: String = "",
    val fcmToken: String = "",
    val lastStudiedAt: Timestamp? = null,
    val settings: UserSettings = UserSettings(),
    val stats: UserStats = UserStats()
)

data class UserSettings(
    val pushTime: String = "20:00",
    val targetWordsPerDay: Int = 10
)

data class UserStats(
    val totalWordCount: Int = 0,
    val reviewStreak: Int = 0
)
