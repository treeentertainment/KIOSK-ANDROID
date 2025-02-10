package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.appwrite.Client
import io.appwrite.enums.OAuthProvider
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var client: Client
    private lateinit var account: Account
    private lateinit var database: Databases
    private var newWebView: WebView? = null  // Stores the new WebView instance

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView(webView)

        webView.loadUrl("file:///android_asset/index.html")

        // Initialize Appwrite Client
        client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("treekiosk")

        database = Databases(client)
        account = Account(client)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true  // ✅ Enable file:// access
            setSupportMultipleWindows(true)  // ✅ Enable multiple windows
            javaScriptCanOpenWindowsAutomatically = true  // ✅ Allow window.open()
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                Log.d("WebView", "New window requested")

                // Create a new WebView instance
                newWebView = WebView(this@MainActivity).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportMultipleWindows(true)
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webChromeClient = this@MainActivity.webView.webChromeClient
                    webViewClient = WebViewClient()
                }

                // Add the new WebView to the layout
                addContentView(newWebView, newWebView!!.layoutParams)

                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()

                return true
            }

            override fun onCloseWindow(window: WebView?) {
                Log.d("WebView", "Closing new window")
                newWebView?.let {
                    (it.parent as? ViewGroup)?.removeView(it)
                    newWebView = null
                }
            }
        }
    }

    inner class WebAppInterface {

        @JavascriptInterface
        fun googleLogin() {
            lifecycleScope.launch {
                try {
                    Log.d("Appwrite", "Starting Google OAuth login...")

                    account.createOAuth2Session(
                        activity = this@MainActivity,
                        provider = OAuthProvider.GOOGLE
                    )

                    val user = account.get()
                    Log.d("Appwrite", "User logged in: ${user.email}")

                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }
                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Unknown error"
                    Log.e("Appwrite", "OAuth login failed: $errorMessage")

                    runOnUiThread {
                        webView.evaluateJavascript("onLoginFailure('$errorMessage')", null)
                    }
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

        @JavascriptInterface
        fun submitOrder(phoneNumber: String, email: String, shop: String, orderJson: String) {
            lifecycleScope.launch {
                try {
                    val ownerDocuments = database.listDocuments(
                        databaseId = "tree-kiosk",
                        collectionId = "owner",
                        queries = listOf(Query.equal("email", email))
                    )

                    if (ownerDocuments.documents.isEmpty()) {
                        throw Exception("No shop found for this email.")
                    }

                    val ownerDocument = ownerDocuments.documents.first()
                    val currentOrderNumber = (ownerDocument.data["order"] as? String)?.toIntOrNull() ?: 0

                    val newOrder = mapOf(
                        "shop" to shop,
                        "number" to phoneNumber,
                        "ordernumber" to currentOrderNumber.toString(),
                        "order" to orderJson
                    )

                    val validDocumentId = currentOrderNumber.toString()

                    database.createDocument(
                        databaseId = "tree-kiosk",
                        collectionId = "data",
                        documentId = validDocumentId,
                        data = newOrder
                    )

                    val newOrderNumber = currentOrderNumber + 1
                    database.updateDocument(
                        databaseId = "tree-kiosk",
                        collectionId = "owner",
                        documentId = ownerDocument.id,
                        data = mapOf("order" to newOrderNumber.toString())
                    )

                    runOnUiThread {
                        webView.evaluateJavascript("finishsend()", null)
                    }

                } catch (e: Exception) {
                    Log.e("Appwrite", "Order submission error: ${e.message}")

                    runOnUiThread {
                        webView.evaluateJavascript("errorsend('Order submission failed: ${e.message}')", null)
                    }
                }
            }
        }

        @JavascriptInterface
        fun checkUserDocument(email: String) {
            lifecycleScope.launch {
                val (exists, name) = getUserDocument(email)
                runOnUiThread {
                    webView.evaluateJavascript("onUserExists($exists, '$email', '$name');", null)
                }
            }
        }

        @JavascriptInterface
        fun getJson(): String {
            return readJsonFromAssets()
        }
    }

    private fun readJsonFromAssets(): String {
        return try {
            val inputStream = assets.open("image/file.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.use { it.readText() }
        } catch (e: Exception) {
            "{}"
        }
    }
}
