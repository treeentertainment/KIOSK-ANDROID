package me.moontree.treekiosk.v3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.services.Account
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: SignInClient
    private lateinit var account: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Appwrite 초기화
        val client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("treekiosk")

        account = Account(client)

        // Google Sign-In 초기화
        googleSignInClient = Identity.getSignInClient(this)
    }

    // Google 로그인 요청
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { handleSignInResult(it) }
        }
    }

    // Google 로그인 버튼 클릭 시 실행
    fun signInWithGoogle() {
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("YOUR_GOOGLE_CLIENT_ID") // Google OAuth Client ID 설정
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

    // Google 로그인 결과 처리
    private fun handleSignInResult(data: Intent) {
        val credential = googleSignInClient.getSignInCredentialFromIntent(data)
        val googleIdToken = credential.googleIdToken

        if (googleIdToken != null) {
            // Appwrite에 OAuth 로그인 요청
            authenticateWithAppwrite(googleIdToken)
        } else {
            Log.e("GoogleSignIn", "Google ID Token is null")
        }
    }

    // 3. Appwrite 세션 생성
    private fun authenticateWithAppwrite(googleIdToken: String) {
        lifecycleScope.launch {
            try {
                val session = account.createOAuth2Session(
                    provider = "google",
                    success = "app://success",
                    failure = "app://failure"
                )
                Log.d("AppwriteAuth", "로그인 성공: ${session.userId}")
            } catch (e: Exception) {
                Log.e("AppwriteAuth", "로그인 실패: ${e.message}")
            }
        }
    }

    // 4. 인증 상태 확인
    fun checkAuthState() {
        lifecycleScope.launch {
            try {
                val user = account.get()
                Log.d("AuthState", "현재 로그인된 사용자: ${user.email}")
            } catch (e: Exception) {
                Log.e("AuthState", "로그인된 사용자 없음")
            }
        }
    }

    // 5. 로그아웃 기능
    fun logout() {
        lifecycleScope.launch {
            try {
                account.deleteSession("current")
                Log.d("Logout", "로그아웃 성공")
            } catch (e: Exception) {
                Log.e("Logout", "로그아웃 실패: ${e.message}")
            }
        }
    }
}
