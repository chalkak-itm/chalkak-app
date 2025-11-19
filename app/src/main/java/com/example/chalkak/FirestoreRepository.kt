package com.example.chalkak

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions // 필수 Import
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreRepo"

    // Save User Info
    fun saveUser(uid: String, email: String, nickname: String, fcmToken: String) {
        val userRef = db.collection("users").document(uid)
        val userData = hashMapOf(
            "email" to email,
            "nickname" to nickname,
            "fcmToken" to fcmToken
        )
        userRef.set(userData, SetOptions.merge())
    }

    // [Situation 1] New Word (First Time)
    // Only increment count.
    fun addNewWordCount(uid: String) {
        val userRef = db.collection("users").document(uid)
        userRef.update("stats.totalWordCount", FieldValue.increment(1))
    }

    // [Situation 2] Reviewing Known Word (Renamed from onlyUpdateTimestamp)
    // Update timestamp in studyLog for Review Algorithm
    fun updateReviewTime(uid: String, word: String) {
        val userRef = db.collection("users").document(uid)
        val logRef = userRef.collection("studyLog").document(word.lowercase())

        logRef.set(
            hashMapOf("lastStudied" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        userRef.update("lastStudiedAt", FieldValue.serverTimestamp())
    }

    // Call Cloud Function (GPT)
    fun fetchWordFromGPT(word: String, onSuccess: (WordDTO) -> Unit, onFailure: (Exception) -> Unit) {
        val functions = FirebaseFunctions.getInstance("asia-northeast3")

        val data = hashMapOf("word" to word)

        functions
            .getHttpsCallable("getWordData")
            .call(data)
            .addOnSuccessListener { task ->
                val result = task.data as? Map<String, Any>
                if (result != null && result["isError"] != true) {
                    val dto = WordDTO(
                        originalWord = result["originalWord"] as? String ?: "",
                        meaning = result["meaning"] as? String ?: ""
                        // Add examples parsing if needed
                    )
                    onSuccess(dto)
                } else {
                    onFailure(Exception("Invalid word"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}