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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingFragment : Fragment() {
    private lateinit var userPreferencesHelper: UserPreferencesHelper

    private lateinit var txtUserName: TextView
    private lateinit var txtNickname: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var btnEditNickname: ImageButton
    private lateinit var btnLogout: View
    private lateinit var btnReset: View
    private lateinit var txtAppVersion: TextView
    private lateinit var switchQuickSnap: android.widget.Switch

    private val roomDb by lazy { AppDatabase.getInstance(requireContext()) }

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

        // Initialize header views from included layout
        val headerView = view.findViewById<View>(R.id.header_common)
        val imgMascot = headerView.findViewById<ImageView>(R.id.img_header_mascot)
        val txtTitle = headerView.findViewById<TextView>(R.id.txt_header_title)
        imgMascot.setImageResource(R.drawable.setting)
        txtTitle.text = getString(R.string.settings)

        // Initialize views
        txtUserName = view.findViewById(R.id.txt_user_name)
        txtNickname = view.findViewById(R.id.txt_nickname)
        txtUserEmail = view.findViewById(R.id.txt_user_email)
        btnEditNickname = view.findViewById(R.id.btn_edit_nickname)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnReset = view.findViewById(R.id.btn_reset)
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
            txtAppVersion.text = getString(R.string.app_version_default)
        }

        // Setup click listeners
        btnEditNickname.setOnClickListener {
            showEditNicknameDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        btnReset.setOnClickListener {
            resetAllData()
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
            txtUserName.text = getString(R.string.guest)
            txtNickname.text = getString(R.string.guest)
            txtUserEmail.text = getString(R.string.not_logged_in)
        }
    }

    private fun showEditNicknameDialog() {
        val currentNickname = userPreferencesHelper.getStoredNickname()
            ?: userPreferencesHelper.getCurrentUser()?.displayName
            ?: ""

        val input = android.widget.EditText(requireContext()).apply {
            setText(currentNickname)
            inputType = InputType.TYPE_CLASS_TEXT
            hint = getString(R.string.enter_nickname)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_nickname))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newNickname = input.text.toString().trim()
                if (newNickname.isNotEmpty()) {
                    saveNickname(newNickname)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.nickname_cannot_be_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun saveNickname(nickname: String) {
        userPreferencesHelper.saveNickname(nickname)
        txtNickname.text = nickname
        Toast.makeText(requireContext(), getString(R.string.nickname_saved), Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirmation))
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                logout()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun logout() {
        // Sign out from Firebase Auth
        // Note: With Credential Manager, we only need to sign out from Firebase Auth
        // Credential Manager handles Google account sign-out automatically
        userPreferencesHelper.signOut()
        
        // Navigate to LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }
    
    private fun resetAllData() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.reset_section_title))
            .setMessage(getString(R.string.reset_confirmation))
            .setPositiveButton(getString(R.string.clear_all_data)) { _, _ ->
                clearDatabaseData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun clearDatabaseData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delete all data from database
                roomDb.photoLogDao().deleteAllPhotos()
                roomDb.detectedObjectDao().deleteAllDetectedObjects()
                roomDb.exampleSentenceDao().deleteAllExampleSentences()
                
                withContext(Dispatchers.Main) {
                    ToastHelper.showCenterToast(requireContext(), getString(R.string.all_data_cleared_successfully))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMessage = e.message ?: getString(R.string.unknown_error)
                    ToastHelper.showCenterToast(requireContext(), getString(R.string.failed_to_clear_data, errorMessage))
                }
            }
        }
    }
    
    private fun loadQuickSnapSetting() {
        val isEnabled = userPreferencesHelper.isQuickSnapEnabled()
        switchQuickSnap.isChecked = isEnabled
    }
    
    private fun toggleQuickSnap(isEnabled: Boolean) {
        userPreferencesHelper.setQuickSnapEnabled(isEnabled)
        
        val message = if (isEnabled) {
            getString(R.string.quick_snap_enabled)
        } else {
            getString(R.string.quick_snap_disabled)
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
