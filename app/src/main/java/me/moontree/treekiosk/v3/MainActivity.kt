package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var client: Client
    private lateinit var account: Account
    private lateinit var database: Databases

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // ✅ XML 레이아웃 파일 존재 확인

        webView = findViewById(R.id.webView) // ✅ ID가 XML에서 정의되어 있어야 함
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(), "AndroidApp")

        webView.loadUrl("file:///android_asset/index.html")

        client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("treekiosk")

        account = Account(client)
        database = Databases(client)
    }

    inner class WebAppInterface {

        @JavascriptInterface
        fun googleLogin() {
            runOnUiThread {
                val authUrl = "https://cloud.appwrite.io/v1/account/sessions/oauth2/google?success=file:///android_asset/index.html"
                webView.loadUrl(authUrl)
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
                        webView.evaluateJavascript("onLoginFailure()", null)
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
                    Log.e("LogoutError", e.message ?: "Unknown error")
                }
            }
        }

        @JavascriptInterface
        fun getUserData(email: String) {
            lifecycleScope.launch {
                try {
                    val response = database.listDocuments(
                        "tree-kiosk",
                        "owner",
                        listOf(Query.equal("email", email))
                    )
                    if (response.documents.isNotEmpty()) {
                        val document = response.documents[0]
                        val name = document.data["name"].toString()
                        val active = document.data["active"] as Boolean

                        runOnUiThread {
                            if (active) {
                                webView.evaluateJavascript("onUserDataReceived('$name')", null)
                            } else {
                                webView.evaluateJavascript("onUserInactive()", null)
                            }
                        }
                    } else {
                        runOnUiThread {
                            webView.evaluateJavascript("onUserNotFound()", null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GetUserDataError", e.message ?: "Unknown error")
                }
            }
        }
    }
}
