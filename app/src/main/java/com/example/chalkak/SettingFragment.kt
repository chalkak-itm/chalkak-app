package com.example.chalkak

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class SettingFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var txtUserName: TextView
    private lateinit var txtNickname: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var btnEditNickname: ImageButton
    private lateinit var btnLogout: View
    private lateinit var btnClearCache: View
    private lateinit var txtAppVersion: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Initialize Google Sign-In Client
        val webClientId = getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Initialize views
        txtUserName = view.findViewById(R.id.txt_user_name)
        txtNickname = view.findViewById(R.id.txt_nickname)
        txtUserEmail = view.findViewById(R.id.txt_user_email)
        btnEditNickname = view.findViewById(R.id.btn_edit_nickname)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnClearCache = view.findViewById(R.id.btn_clear_cache)
        txtAppVersion = view.findViewById(R.id.txt_app_version)

        // Load user information
        loadUserInfo()

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
    }

    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            // Display user name
            val displayName = user.displayName ?: "User"
            txtUserName.text = displayName

            // Display nickname (from SharedPreferences) or displayName
            val nickname = sharedPreferences.getString("user_nickname", null) ?: displayName
            txtNickname.text = nickname

            // Display email
            txtUserEmail.text = user.email ?: "No email"
        } else {
            // User not logged in
            txtUserName.text = "Guest"
            txtNickname.text = "Guest"
            txtUserEmail.text = "Not logged in"
        }
    }

    private fun showEditNicknameDialog() {
        val currentNickname = sharedPreferences.getString("user_nickname", null)
            ?: auth.currentUser?.displayName
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
        sharedPreferences.edit()
            .putString("user_nickname", nickname)
            .apply()
        
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
        auth.signOut()
        
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
            .setMessage("This will clear all cached data. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                try {
                    // Clear image cache
                    val cacheDir = requireContext().cacheDir
                    if (cacheDir.exists() && cacheDir.isDirectory) {
                        cacheDir.listFiles()?.forEach { file ->
                            if (file.name.startsWith("detection_") || file.name.startsWith("capture_")) {
                                file.delete()
                            }
                        }
                    }

                    Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to clear cache", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
