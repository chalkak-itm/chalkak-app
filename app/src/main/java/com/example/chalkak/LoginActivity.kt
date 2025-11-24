package com.example.chalkak

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.example.chalkak.databinding.ActivityLoginBinding
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"

    private val firestoreRepo = FirestoreRepository()

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "Google Sign In Succeeded, getting token...")
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    val statusCode = e.statusCode
                    Log.e(TAG, "Google sign in failed: ApiException (statusCode: $statusCode)", e)
                    updateUI(null)
                }
            } else {
                Log.w(TAG, "Google sign in flow cancelled by user (resultCode: ${result.resultCode})")
                updateUI(null)
            }
        }

        // Apply WindowInsets using helper (LoginActivity includes bottom padding)
        WindowInsetsHelper.applyToRootLayout(
            rootView = binding.loginRoot,
            resources = resources,
            includeBottomPadding = true
        )

        binding.btnSignIn?.setOnClickListener {
            Log.d(TAG, "Launching Google Sign In Intent...")
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
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