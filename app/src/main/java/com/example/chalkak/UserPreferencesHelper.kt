package com.example.chalkak

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Helper class for managing user preferences and authentication
 * Centralizes Firebase Auth and SharedPreferences operations
 */
class UserPreferencesHelper(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    /**
     * Get current Firebase user
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * Get Firebase Auth instance
     */
    fun getAuth(): FirebaseAuth = auth
    
    /**
     * Get SharedPreferences instance
     */
    fun getSharedPreferences(): SharedPreferences = sharedPreferences
    
    /**
     * Get user nickname from SharedPreferences, or fallback to displayName, or default to "User"
     */
    fun getNickname(): String {
        val user = auth.currentUser
        return if (user != null) {
            sharedPreferences.getString("user_nickname", null)
                ?: user.displayName
                ?: "User"
        } else {
            "Guest"
        }
    }
    
    /**
     * Save nickname to SharedPreferences
     */
    fun saveNickname(nickname: String) {
        sharedPreferences.edit()
            .putString("user_nickname", nickname)
            .apply()
    }
    
    /**
     * Get current nickname from SharedPreferences (without fallback)
     */
    fun getStoredNickname(): String? {
        return sharedPreferences.getString("user_nickname", null)
    }
    
    /**
     * Get user display name
     */
    fun getDisplayName(): String {
        return auth.currentUser?.displayName ?: "Guest"
    }
    
    /**
     * Get user email
     */
    fun getEmail(): String {
        return auth.currentUser?.email ?: "Not logged in"
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Sign out from Firebase Auth
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Get quick snap enabled state (default: false)
     */
    fun isQuickSnapEnabled(): Boolean {
        return sharedPreferences.getBoolean("quick_snap_enabled", false)
    }
    
    /**
     * Save quick snap enabled state
     */
    fun setQuickSnapEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("quick_snap_enabled", enabled)
            .apply()
    }
    
    /**
     * Get notification enabled state (default: false)
     */
    fun isNotificationEnabled(): Boolean {
        return sharedPreferences.getBoolean("notification_enabled", false)
    }
    
    /**
     * Save notification enabled state
     */
    fun setNotificationEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("notification_enabled", enabled)
            .apply()
    }
}

