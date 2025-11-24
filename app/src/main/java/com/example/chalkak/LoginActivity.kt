package com.example.chalkak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.example.chalkak.databinding.ActivityLoginBinding
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"

    private val firestoreRepo = FirestoreRepository()

    private lateinit var credentialManager: CredentialManager
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val webClientId by lazy { getString(R.string.default_web_client_id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Check if user is already signed in
        if (auth.currentUser != null) {
            Log.d(TAG, "Already Login: ${auth.currentUser?.displayName}")
            navigateToMain()
            return
        }

        // Apply WindowInsets using helper (LoginActivity includes bottom padding)
        WindowInsetsHelper.applyToRootLayout(
            rootView = binding.loginRoot,
            resources = resources,
            includeBottomPadding = true
        )

        binding.btnSignIn?.setOnClickListener {
            Log.d(TAG, "Launching Google Sign In with Credential Manager...")
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )

                when (val credential = result.credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                val googleIdTokenCredential = GoogleIdTokenCredential
                                    .createFrom(credential.data)
                                val idToken = googleIdTokenCredential.idToken
                                Log.d(TAG, "Google Sign In Succeeded, getting token...")
                                firebaseAuthWithGoogle(idToken)
                            } catch (e: GoogleIdTokenParsingException) {
                                Log.e(TAG, "Failed to parse Google ID token", e)
                                updateUI(null)
                            }
                        } else {
                            Log.e(TAG, "Unexpected credential type: ${credential.type}")
                            updateUI(null)
                        }
                    }
                    else -> {
                        Log.e(TAG, "Unexpected credential type")
                        updateUI(null)
                    }
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google sign in failed: ${e.message}", e)
                updateUI(null)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during sign in", e)
                updateUI(null)
            }
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

    // FCM token -> Firestore -> Main page
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // 1. FCM getting
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "FCM 토큰 가져오기 실패", task.exception)
                    // if not succeed, dummy token
                    saveUserAndNavigate(user, "")
                    return@addOnCompleteListener
                }

                // 2. getting token
                val token = task.result
                Log.d(TAG, "FCM Token: $token")

                // save and direct
                saveUserAndNavigate(user, token)
            }
        } else {
            Log.d(TAG, "Login Required (User is null)")
        }
    }

    // screen move
    private fun saveUserAndNavigate(user: FirebaseUser, token: String) {
        firestoreRepo.saveUser(
            uid = user.uid,
            email = user.email ?: "",
            nickname = user.displayName ?: "Unknown",
            fcmToken = token // real token
        )

        Toast.makeText(this, "${user.displayName} \n Nice to Meet you!", Toast.LENGTH_SHORT).show()
        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}