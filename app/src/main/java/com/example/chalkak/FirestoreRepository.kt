package com.example.chalkak

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreRepo"

    // Saving user info
    fun saveUser(uid: String, email: String, nickname: String, fcmToken: String) {
        val userRef = db.collection("users").document(uid)
        val userData = hashMapOf(
            "email" to email,
            "nickname" to nickname,
            "fcmToken" to fcmToken
        )
        userRef.set(userData, SetOptions.merge())
    }

    // public word dictionary (getting)
    suspend fun getWord(wordId: String): WordDTO? {
        return try {
            val docSnapshot = db.collection("words")
                .document(wordId.lowercase())
                .get()
                .await()

            if (docSnapshot.exists()) {
                docSnapshot.toObject(WordDTO::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fail to bring word", e)
            null
        }
    }

    // count word -> word + 1
    fun updateLastStudied(uid: String) {
        val userRef = db.collection("users").document(uid)

        // field to update
        val updates = hashMapOf<String, Any>(
            "lastStudiedAt" to FieldValue.serverTimestamp(),
            "stats.totalWordCount" to FieldValue.increment(1)
        )

        userRef.update(updates)
            .addOnSuccessListener { Log.d(TAG, "learning time updated") }
            .addOnFailureListener { Log.e(TAG, "update failed", it) }
    }

    // (참고) 만약 기존에 학습한 단어를 복습하는 거라서 카운트를 안 올리고 싶다면?
    // 아래 함수를 따로 만들어서 쓰면 됩니다.
    fun onlyUpdateTimestamp(uid: String) {
        val userRef = db.collection("users").document(uid)
        userRef.update("lastStudiedAt", FieldValue.serverTimestamp())
    }
}