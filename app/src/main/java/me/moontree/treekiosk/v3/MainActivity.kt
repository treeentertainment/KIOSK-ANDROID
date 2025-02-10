package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.enums.OAuthProvider
import kotlinx.coroutines.launch
import io.appwrite.Query
import io.appwrite.services.Databases

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var client: Client
    private lateinit var account: Account
    private lateinit var database: Databases

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // WebView 설정
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(), "AndroidApp")

        webView.loadUrl("file:///android_asset/index.html")

        // Appwrite 클라이언트 초기화
        client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1") // ✅ Appwrite API 엔드포인트
            .setProject("treekiosk") // ✅ 프로젝트 ID 입력

        database = Databases(client)
        account = Account(client)
    }

    inner class WebAppInterface {

        @JavascriptInterface
        fun checkUserDocument(email: String) {
            lifecycleScope.launch {
                val exists = isDocumentExists(email)
                runOnUiThread {
                    webView.evaluateJavascript("onUserExists($exists);", null)
                }
            }
        }

        @JavascriptInterface
        fun googleLogin() {
            try {
                Log.d("Appwrite", "Starting Google OAuth login...")

                account.createOAuth2Session(
                    this@MainActivity,
                    OAuthProvider.GOOGLE
                )

                Log.d("Appwrite", "Google OAuth login process started.")

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e("Appwrite", "OAuth login failed: $errorMessage")

                runOnUiThread {
                    webView.evaluateJavascript("onLoginFailure('$errorMessage')", null)
                }
            }
        }

        @JavascriptInterface
        fun checkAuthState() {
            lifecycleScope.launch {
                try {
                    Log.d("Appwrite", "Checking auth state...")

                    val user = account.get()
                    Log.d("Appwrite", "User is logged in: ${user.email}")

                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }
                } catch (e: Exception) {
                    Log.d("Appwrite", "User not logged in.")

                    runOnUiThread {
                        webView.evaluateJavascript("onLoginFailure('Not logged in')", null)
                    }
                }
            }
        }

        @JavascriptInterface
        fun logout() {
            lifecycleScope.launch {
                try {
                    Log.d("Appwrite", "Logging out...")

                    account.deleteSession("current")

                    Log.d("Appwrite", "User logged out successfully.")

                    runOnUiThread {
                        webView.evaluateJavascript("onLogoutSuccess()", null)
                    }
                } catch (e: Exception) {
                    Log.e("Appwrite", "Logout error: ${e.message}")

                    runOnUiThread {
                        webView.evaluateJavascript("console.log('LogoutError: ${e.message}')", null)
                    }
                }
            }
        }
    }

    suspend fun isDocumentExists(email: String): Boolean {
        return try {
            val response = database.listDocuments(
                databaseId = "tree-kiosk",
                collectionId = "owner",
                queries = listOf(Query.equal("email", email))
            )
            response.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e("Appwrite", "사용자 데이터를 가져오는 중 오류 발생: ${e.message}")
            false
        }
    }
}
