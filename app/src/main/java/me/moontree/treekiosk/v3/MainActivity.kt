package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.services.Account
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

        webView = findViewById(R.id.webView)
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
            account.get()
                .addOnSuccessListener { user ->
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }
                }
                .addOnFailureListener {
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginFailure()", null)
                    }
                }
        }

        @JavascriptInterface
        fun logout() {
            account.deleteSession("current")
                .addOnSuccessListener {
                    runOnUiThread {
                        webView.evaluateJavascript("onLogoutSuccess()", null)
                    }
                }
        }

        @JavascriptInterface
        fun getUserData(email: String) {
            database.listDocuments(
                "tree-kiosk",
                "owner",
                listOf(Query.equal("email", email))
            ).addOnSuccessListener { response ->
                if (response.documents.isNotEmpty()) {
                    val document = response.documents[0]
                    val name = document.data["name"].toString()
                    val active = document.data["active"] as Boolean

                    if (active) {
                        runOnUiThread {
                            webView.evaluateJavascript("onUserDataReceived('$name')", null)
                        }
                    } else {
                        runOnUiThread {
                            webView.evaluateJavascript("onUserInactive()", null)
                        }
                    }
                } else {
                    runOnUiThread {
                        webView.evaluateJavascript("onUserNotFound()", null)
                    }
                }
            }
        }
    }
}
