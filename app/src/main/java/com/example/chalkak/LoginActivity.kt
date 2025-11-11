package com.example.chalkak

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.example.chalkak.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"

    // Use the classic GoogleSignInClient (instead of CredentialManager)
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // Get the Web Client ID from google-services.json (via R.string)
    private val webClientId by lazy { getString(R.string.default_web_client_id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        if (auth.currentUser != null) {
            Log.d(TAG, "Already Login: ${auth.currentUser?.displayName}")
            navigateToMain()
            return
        }

        // Configure GoogleSignInOptions to request an ID token for Firebase
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        // Initialize the GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize the ActivityResultLauncher to handle the sign-in result
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Google Sign-In was successful, get the ID token
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "Google Sign In Succeeded, getting token...")
                    // Pass the ID token to Firebase for authentication
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign-In failed
                    val statusCode = e.statusCode
                    Log.e(TAG, "Google sign in failed: ApiException (statusCode: $statusCode)", e)
                    updateUI(null)
                }
            } else {
                // User cancelled the sign-in flow
                Log.w(TAG, "Google sign in flow cancelled by user (resultCode: ${result.resultCode})")
                updateUI(null)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        // Set click listener to launch the Google Sign-In flow
        binding.btnSignIn?.setOnClickListener {
            Log.d(TAG, "Launching Google Sign In Intent...")
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    // Authenticate with Firebase using the Google ID token
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

    // Update UI after sign-in attempt
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "${user.displayName} \n Nice to Meet you!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        } else {
            Log.d(TAG, "Login Required")
        }
    }

    // Navigate to MainActivity after successful login
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}