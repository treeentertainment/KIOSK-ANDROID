package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.enums.OAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var client: Client
    private lateinit var account: Account

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

        account = Account(client)
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun googleLogin() {
            lifecycleScope.launch {
                try {
                    val user = account.get() // ✅ 기존 세션 확인
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }
                } catch (e: Exception) {
                    // 세션이 없으면 OAuth 로그인 시작
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginFailure('No session found. Redirecting to OAuth login.')", null)
                    }
                    startOAuthLogin()
                }
            }
        }

        private fun startOAuthLogin() {
    lifecycleScope.launch {
        try {
            account.createOAuth2Session(
                activity = this@MainActivity,
                provider = OAuthProvider.GOOGLE
            )

            // ✅ OAuth 로그인 실행 후 UI 업데이트
            runOnUiThread {
                webView.evaluateJavascript("document.body.innerHTML += '<p>Redirecting to login...</p>'", null)
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            runOnUiThread {
                // WebView에서 에러 메시지 출력 (Android Studio 없이 확인 가능)
                webView.evaluateJavascript("document.body.innerHTML += '<p style=\"color:red;\">Login Error: $errorMessage</p>'", null)
                webView.evaluateJavascript("onLoginFailure('$errorMessage')", null)
            }
        }
    }
}

        @JavascriptInterface
        fun checkAuthState() {
            lifecycleScope.launch {
                try {
                    val user = account.get()
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }
                } catch (e: Exception) {
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
                    account.deleteSession("current")
                    runOnUiThread {
                        webView.evaluateJavascript("onLogoutSuccess()", null)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        webView.evaluateJavascript("console.log('LogoutError: ${e.message}')", null)
                    }
                }
            }
        }
    }
}
