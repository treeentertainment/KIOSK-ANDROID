package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.appwrite.Client
import io.appwrite.enums.OAuthProvider
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.Query
import kotlinx.coroutines.launch

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
        setupWebView(webView)

        webView.loadUrl("file:///android_asset/index.html")

        client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("treekiosk")

        database = Databases(client)
        account = Account(client)
    }

    private fun setupWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            allowFileAccessFromFileURLs = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = WebView(this@MainActivity).apply {
                    setupWebView(this)
                }
                resultMsg?.sendToTarget()

                val newUrl = view?.url ?: "about:blank"
                val intent = Intent(this@MainActivity, NewWebActivity::class.java)
                intent.putExtra("url", newUrl)
                startActivity(intent)

                return true
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "AndroidApp")
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun closeWindow() {
            runOnUiThread { finish() }
        }
    }
}
