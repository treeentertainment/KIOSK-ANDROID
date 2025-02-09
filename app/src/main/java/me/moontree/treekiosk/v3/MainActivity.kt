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

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var client: Client
    private lateinit var account: Account

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(), "AndroidApp")

        webView.loadUrl("file:///android_asset/index.html")

        client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("treekiosk")

        account = Account(client)
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun googleLogin() {
            lifecycleScope.launch {
                try {
                    // ✅ 1. 기존 로그인된 사용자 확인
                    val user = account.get()
                    Log.d("Login", "User already logged in: ${user.email}")
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }
                } catch (e: Exception) {
                    Log.d("Login", "No existing session, starting OAuth login")
                    startOAuthLogin() // ✅ 기존 세션 없으면 OAuth 로그인 시작
                }
            }
        }

        private fun startOAuthLogin() {
            runOnUiThread {
                lifecycleScope.launch {
                    try {
                        // ✅ 2. 기존 사용자가 존재하는지 확인 (이메일 중복 방지)
                        val userList = account.listIdentities() // Appwrite에 로그인된 계정 리스트 확인
                        if (userList.identities.isNotEmpty()) {
                            val user = account.get()
                            runOnUiThread {
                                webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                            }
                            return@launch
                        }

                        // ✅ 3. 기존 세션이 없을 경우 OAuth2 로그인 수행
                        account.createOAuth2Session(
                            activity = this@MainActivity,
                            provider = OAuthProvider.GOOGLE
                        )

                        val user = account.get()
                        runOnUiThread {
                            webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                        }
                    } catch (e: Exception) {
                        Log.e("OAuthLoginError", e.message ?: "Unknown error")
                        runOnUiThread {
                            webView.evaluateJavascript("onLoginFailure()", null)
                        }
                    }
                }
            }
        }
    }
}
