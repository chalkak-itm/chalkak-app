package com.example.chalkak.data.remote.firestore

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

    // Update nickname only
    fun updateNickname(uid: String, nickname: String) {
        val userRef = db.collection("users").document(uid)
        userRef.update("nickname", nickname)
            .addOnSuccessListener {
                Log.d(TAG, "Nickname updated successfully: $nickname")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating nickname", e)
            }
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
                    val examplesData = result["examples"] as? List<Map<String, Any>> ?: emptyList()
                    val examples = examplesData.mapNotNull { exampleMap ->
                        val sentence = exampleMap["sentence"] as? String ?: ""
                        val translation = exampleMap["translation"] as? String ?: ""
                        if (sentence.isNotEmpty() && translation.isNotEmpty()) {
                            ExampleItem(sentence = sentence, translation = translation)
                        } else null
                    }
                    val dto = WordDTO(
                        originalWord = result["originalWord"] as? String ?: "",
                        meaning = result["meaning"] as? String ?: "",
                        examples = examples
                    )
                    onSuccess(dto)
                } else {
                    onFailure(Exception("Invalid word"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Save word to Firebase Firestore
    suspend fun saveWordToFirebase(wordDto: WordDTO) {
        try {
            val wordRef = db.collection("words").document(wordDto.originalWord.lowercase())
            val examplesList = wordDto.examples.map { example ->
                hashMapOf(
                    "sentence" to example.sentence,
                    "translation" to example.translation
                )
            }
            val wordData = hashMapOf(
                "originalWord" to wordDto.originalWord,
                "meaning" to wordDto.meaning,
                "examples" to examplesList
            )
            wordRef.set(wordData, SetOptions.merge()).await()
            Log.d(TAG, "Saved word to Firebase: ${wordDto.originalWord}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving word to Firebase: ${wordDto.originalWord}", e)
            throw e
        }
    }

    // Get all words from Firebase Firestore
    suspend fun getAllWordsFromFirebase(): List<WordDTO> {
        return try {
            val wordsSnapshot = db.collection("words").get().await()
            wordsSnapshot.documents.mapNotNull { document ->
                val data = document.data
                val originalWord = data?.get("originalWord") as? String ?: ""
                val meaning = data?.get("meaning") as? String ?: ""
                val examplesData = data?.get("examples") as? List<Map<String, Any>> ?: emptyList()

                if (originalWord.isNotEmpty() && meaning.isNotEmpty()) {
                    val examples = examplesData.mapNotNull { exampleMap ->
                        val sentence = exampleMap["sentence"] as? String ?: ""
                        val translation = exampleMap["translation"] as? String ?: ""
                        if (sentence.isNotEmpty() && translation.isNotEmpty()) {
                            ExampleItem(sentence = sentence, translation = translation)
                        } else null
                    }
                    WordDTO(
                        originalWord = originalWord,
                        meaning = meaning,
                        examples = examples
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting words from Firebase", e)
            emptyList()
        }
    }
}
