package me.moontree.treekiosk.v3

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import io.appwrite.Client
import io.appwrite.enums.OAuthProvider
import io.appwrite.models.Session
import io.appwrite.services.Account
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: SignInClient
    private lateinit var account: Account
    private lateinit var client: Client
    private lateinit var context: Context

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { handleSignInResult(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this

        client = Client(context)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("treekiosk")

        account = Account(client)

        googleSignInClient = Identity.getSignInClient(this)

        // Check for existing session on app start
        checkAuthState()

        // Example Google Sign-In button click listener (replace with your actual button)
        // findViewById<Button>(R.id.googleSignInButton).setOnClickListener {
        //     signInWithGoogle()
        // }
    }


    fun signInWithGoogle() {
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("YOUR_GOOGLE_CLIENT_ID") // Replace with your Client ID
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        googleSignInClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                signInLauncher.launch(result.pendingIntent.intent)
            }
            .addOnFailureListener { e ->
                Log.e("GoogleSignIn", "Google Sign-In failed: ${e.message}")
            }
    }

    private fun handleSignInResult(data: Intent) {
        val credential = googleSignInClient.getSignInCredentialFromIntent(data)
        val googleIdToken = credential.googleIdToken

        if (googleIdToken != null) {
            authenticateWithAppwrite(googleIdToken)
        } else {
            Log.e("GoogleSignIn", "Google ID Token is null")
        }
    }

    private fun authenticateWithAppwrite(googleIdToken: String) {
        lifecycleScope.launch {
            try {
                val session: Session = account.createOAuth2Session(
                    provider = OAuthProvider.GOOGLE,
                    success = "app://success", // Custom scheme redirect URL
                    failure = "app://failure"  // Custom scheme redirect URL
                )
                Log.d("AppwriteAuth", "Appwrite Login Success: ${session.userId}")
                checkAuthState() // Update UI or perform actions after successful login

            } catch (e: Exception) {
                Log.e("AppwriteAuth", "Appwrite Login Failed: ${e.message}")
            }
        }
    }

    fun checkAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { // Use repeatOnLifecycle
                try {
                    val user = account.get()
                    Log.d("AuthState", "Current User: ${user.email}")
                    // Update UI to show logged-in state
                } catch (e: Exception) {
                    Log.d("AuthState", "No logged-in user")
                    // Update UI to show logged-out state
                }
            }
        }
    }


    fun logout() {
        lifecycleScope.launch {
            try {
                account.deleteSession("current")
                Log.d("Logout", "Logout Success")
                checkAuthState() // Update UI after logout
            } catch (e: Exception) {
                Log.e("Logout", "Logout Failed: ${e.message}")
            }
        }
    }

    // Handle incoming intents (for custom scheme redirects)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri: Uri? = intent?.data
        if (uri != null) {
            Log.d("IntentData", "onNewIntent: $uri")
            // Extract any parameters from the URI if needed (e.g., session ID)
            // Example: val sessionId = uri.getQueryParameter("session_id")
        }
    }


}
