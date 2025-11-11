package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.example.chalkak.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager
    private val TAG = "LoginActivity"

    private val webClientId by lazy { getString(R.string.default_web_client_id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        if (auth.currentUser != null) {
            Log.d(TAG, "Already Login: ${auth.currentUser?.displayName}")
            navigateToMain()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        binding.btnSignIn?.setOnClickListener {
            lifecycleScope.launch {
                launchGoogleSignIn()
            }
        }
    }

    private suspend fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(this, request)
            val credential = result.credential


            if (credential is GoogleIdTokenCredential) {

                Log.d(TAG, "SUCCESS: Got 'GoogleIdTokenCredential'. Proceeding with Firebase.")
                firebaseAuthWithGoogle(credential.idToken)

            }
            else {
                val credentialType = credential::class.java.simpleName
                Log.e(TAG, "--- PROOF OF ERROR ---")
                Log.e(TAG, "Expected 'GoogleIdTokenCredential' (Google 로그인)...")
                Log.e(TAG, "...but got '$credentialType' instead. (This is the Passkey/CustomCredential)")
                Log.e(TAG, "----------------------")
                updateUI(null)
            }

        } catch (e: GetCredentialException) {
            Log.e(TAG, "Login Failed(GetCredentialException): ${e.message}", e)
            updateUI(null)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "${user.displayName} \n Nice to Meet you!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        } else {
            Log.d(TAG, "Login Required")
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
