package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import java.io.File

class SettingFragment : Fragment() {
    private lateinit var userPreferencesHelper: UserPreferencesHelper
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var txtUserName: TextView
    private lateinit var txtNickname: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var btnEditNickname: ImageButton
    private lateinit var btnLogout: View
    private lateinit var btnClearCache: View
    private lateinit var txtAppVersion: TextView
    private lateinit var switchQuickSnap: android.widget.Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UserPreferencesHelper
        userPreferencesHelper = UserPreferencesHelper(requireContext())

        // Initialize Google Sign-In Client
        val webClientId = getString(R.string.default_web_client_id)
        googleSignInClient = userPreferencesHelper.createGoogleSignInClient(webClientId)

        // Initialize header views from included layout
        val headerView = view.findViewById<View>(R.id.header_common)
        val imgMascot = headerView.findViewById<ImageView>(R.id.img_header_mascot)
        val txtTitle = headerView.findViewById<TextView>(R.id.txt_header_title)
        imgMascot.setImageResource(R.drawable.setting)
        txtTitle.text = "Settings"

        // Initialize views
        txtUserName = view.findViewById(R.id.txt_user_name)
        txtNickname = view.findViewById(R.id.txt_nickname)
        txtUserEmail = view.findViewById(R.id.txt_user_email)
        btnEditNickname = view.findViewById(R.id.btn_edit_nickname)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnClearCache = view.findViewById(R.id.btn_clear_cache)
        txtAppVersion = view.findViewById(R.id.txt_app_version)
        switchQuickSnap = view.findViewById(R.id.switch_quick_snap)

        // Load user information
        loadUserInfo()
        
        // Load quick snap setting
        loadQuickSnapSetting()

        // Set app version
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            txtAppVersion.text = packageInfo.versionName
        } catch (e: Exception) {
            txtAppVersion.text = "1.0"
        }

        // Setup click listeners
        btnEditNickname.setOnClickListener {
            showEditNicknameDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        btnClearCache.setOnClickListener {
            clearCache()
        }

        switchQuickSnap.setOnCheckedChangeListener { _, isChecked ->
            toggleQuickSnap(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload quick snap setting when fragment resumes
        loadQuickSnapSetting()
    }

    private fun loadUserInfo() {
        if (userPreferencesHelper.isLoggedIn()) {
            // Display user name
            val displayName = userPreferencesHelper.getDisplayName()
            txtUserName.text = displayName

            // Display nickname (from SharedPreferences) or displayName
            val nickname = userPreferencesHelper.getNickname()
            txtNickname.text = nickname

            // Display email
            txtUserEmail.text = userPreferencesHelper.getEmail()
        } else {
            // User not logged in
            txtUserName.text = "Guest"
            txtNickname.text = "Guest"
            txtUserEmail.text = "Not logged in"
        }
    }

    private fun showEditNicknameDialog() {
        val currentNickname = userPreferencesHelper.getStoredNickname()
            ?: userPreferencesHelper.getCurrentUser()?.displayName
            ?: ""

        val input = android.widget.EditText(requireContext()).apply {
            setText(currentNickname)
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter nickname"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Nickname")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newNickname = input.text.toString().trim()
                if (newNickname.isNotEmpty()) {
                    saveNickname(newNickname)
                } else {
                    Toast.makeText(requireContext(), "Nickname cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNickname(nickname: String) {
        userPreferencesHelper.saveNickname(nickname)
        txtNickname.text = nickname
        Toast.makeText(requireContext(), "Nickname saved", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        // Sign out from Firebase Auth
        userPreferencesHelper.signOut()
        
        // Sign out from Google
        googleSignInClient.signOut().addOnCompleteListener {
            // Navigate to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        }.addOnFailureListener {
            // Even if Google sign out fails, navigate to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun clearCache() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("This will clear all cached data (images, temporary files). Your learning data will be preserved. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                clearCacheFiles()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearCacheFiles() {
        try {
            var deletedCount = 0
            var totalSize = 0L
            
            // Clear internal cache directory
            val internalCacheDir = requireContext().cacheDir
            if (internalCacheDir.exists() && internalCacheDir.isDirectory) {
                internalCacheDir.listFiles()?.forEach { file ->
                    try {
                        totalSize += file.length()
                        if (file.delete()) {
                            deletedCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // Clear external cache directory (if available)
            val externalCacheDir = requireContext().externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists() && externalCacheDir.isDirectory) {
                externalCacheDir.listFiles()?.forEach { file ->
                    try {
                        totalSize += file.length()
                        if (file.delete()) {
                            deletedCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            // Clear code cache (if available)
            try {
                val codeCacheDir = requireContext().codeCacheDir
                if (codeCacheDir.exists() && codeCacheDir.isDirectory) {
                    codeCacheDir.listFiles()?.forEach { file ->
                        try {
                            totalSize += file.length()
                            if (file.delete()) {
                                deletedCount++
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                // Code cache directory might not be available on all devices
                e.printStackTrace()
            }
            
            // Format size for display
            val sizeInMB = totalSize / (1024.0 * 1024.0)
            val sizeText = if (sizeInMB < 1.0) {
                String.format("%.2f KB", totalSize / 1024.0)
            } else {
                String.format("%.2f MB", sizeInMB)
            }
            
            // Show success message
            val message = if (deletedCount > 0) {
                "Cache cleared successfully!\n$deletedCount files ($sizeText) removed"
            } else {
                "Cache is already empty"
            }
            
            ToastHelper.showCenterToast(requireContext(), message)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastHelper.showCenterToast(requireContext(), "Failed to clear cache: ${e.message ?: "Unknown error"}")
        }
    }
    
    private fun loadQuickSnapSetting() {
        val isEnabled = userPreferencesHelper.isQuickSnapEnabled()
        switchQuickSnap.isChecked = isEnabled
    }
    
    private fun toggleQuickSnap(isEnabled: Boolean) {
        userPreferencesHelper.setQuickSnapEnabled(isEnabled)
        
        val message = if (isEnabled) {
            "🔥Quick Snap feature is now enabled!"
        } else {
            "Quick Snap feature is now disabled."
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
