package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.appwrite.ID
import io.appwrite.enums.OAuthProvider
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), MessageListener {

    private lateinit var webView: WebView
    private lateinit var client: Client
    private lateinit var account: Account
    private lateinit var database: Databases
    private val messageReceiver = MessageReceiver(this)

    interface MessageListener {
        fun onMessageReceived(message: String)
    }

    class MessageReceiver(private val listener: MessageListener) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("me.moontree.treekiosk.v3.MESSAGE_FROM_NEW_WEB_ACTIVITY")
            message?.let { listener.onMessageReceived(it) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView(webView)

        webView.loadUrl("file:///android_asset/index.html")
        AppwriteManager.initialize(this)

        val intentFilter = IntentFilter("me.moontree.treekiosk.v3.MESSAGE_FROM_NEW_WEB_ACTIVITY")
        registerReceiver(messageReceiver, intentFilter)
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
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
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

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(view?.context ?: return false)
                    .setTitle("알림")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, which: Int ->
                        result?.confirm()
                    }
                    .setCancelable(false)
                    .show()
                return true
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "AndroidApp")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(messageReceiver)
    }

    inner class WebAppInterface {

        @JavascriptInterface
        fun closeWindow() {
            runOnUiThread { finish() }
        }

        @JavascriptInterface
        fun googleLogin() {
            lifecycleScope.launch {
                try {
                    AppwriteManager.account.createOAuth2Session(
                        activity = this@MainActivity,
                        provider = OAuthProvider.GOOGLE
                    )

                    val user = AppwriteManager.account.get()
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }

                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Unknown error"
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginFailure('<span class="math-inline">errorMessage'\)", null\)
}
}
}
}
@JavascriptInterface
fun checkAuthState() {
    lifecycleScope.launch {
        try {
            val user = AppwriteManager.account.get()
            val email = user.email.replace("'", "\\'") // JavaScript 문자열 안전 처리

            runOnUiThread {
                webView.evaluateJavascript("onLoginSuccess('$email')", null)
            }
        } catch (e: Exception) {
            runOnUiThread {
                webView.evaluateJavascript("onLoginFailure('noid')", null)
            }
        }
    }
}
        @JavascriptInterface
        fun logout() {
            lifecycleScope.launch {
                try {
                    AppwriteManager.account.deleteSession("current")
                    runOnUiThread {
                        webView.evaluateJavascript("onLogoutSuccess()", null)
                    }
                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Unknown error"
                    runOnUiThread {
                        webView.evaluateJavascript("onLoginFailure('$errorMessage')", null)
                    }
                }
            }
        }

        @JavascriptInterface
        fun submitOrder(phoneNumber: String, email: String, shop: String, orderJson: String) {
            lifecycleScope.launch {
                try {
                    val ownerDocuments = AppwriteManager.database.listDocuments(
                        databaseId = "tree-kiosk",
                        collectionId = "owner",
                        queries = listOf(Query.equal("email", email))
                    )

                    if (ownerDocuments.documents.isEmpty()) {
                        throw Exception("해당 이메일을 가진 가게 정보를 찾을 수 없습니다.")
                    }

                    val ownerDocument = ownerDocuments.documents.first()
                    val currentOrderNumber = (ownerDocument.data["order"] as? String)?.toIntOrNull() ?: 0

                    val newOrder = mapOf(
                        "shop" to shop,
                        "number" to phoneNumber,
                        "ordernumber" to currentOrderNumber.toString(),
                        "order" to orderJson
                    )

                    val validDocumentId = ID.unique()

                    AppwriteManager.database.createDocument(
                        databaseId = "tree-kiosk",
                        collectionId = "data",
                        documentId = validDocumentId,
                        data = newOrder
                    )

                    val newOrderNumber = currentOrderNumber + 1
                    AppwriteManager.database.updateDocument(
                        databaseId = "tree-kiosk",
                        collectionId = "owner",
                        documentId = ownerDocument.id,
                        data = mapOf("order" to newOrderNumber.toString())
                    )

                    runOnUiThread {
                        webView.evaluateJavascript("finishsend()", null)
                    }

                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Unknown error"
                    runOnUiThread {
                        webView.evaluateJavascript("errorsend('주문 제출 실패: $errorMessage')", null)
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

        @JavascriptInterface
        fun openNewTab(url: String) {
            val intent = Intent(this@MainActivity, NewWebActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
        }

        @JavascriptInterface
        fun sendMessageToMainActivity(message: String) {
            val intent = Intent("me.moontree.treekiosk.v3.MESSAGE_FROM_NEW_WEB_ACTIVITY")
            intent.putExtra("me.moontree.treekiosk.v3.MESSAGE_FROM_NEW_WEB_ACTIVITY", message)
            sendBroadcast(intent)
        }

    }

    private suspend fun getUserDocument(email: String): Pair<Boolean, String?> {
        try {
            val response = AppwriteManager.database.listDocuments(
                databaseId = "tree-kiosk",
                collectionId = "owner",
                queries = listOf(Query.equal("email", email))
            )

            if (response.documents.isNotEmpty()) {
                val document = response.documents.first()
                val name = document.data["name"] as? String ?: "Unknown"
                return Pair(true, name)
            } else {
                return Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e("Appwrite", "사용자 데이터를 가져오는 중 오류 발생: ${e.message}")
            return Pair(false, null)
        }
    }

    private fun readJsonFromAssets(): String {
        try {
            val inputStream = assets.open("image/file.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            return reader.use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            return "{}"
        }
    }

    override fun onMessageReceived(message: String) {
    val safeMessage = message.replace("'", "\\'")
    webView.evaluateJavascript("messagenew('$safeMessage')", null)
}

}
