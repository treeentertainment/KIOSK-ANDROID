package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
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
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Intent
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

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
        AppwriteManager.initialize(this)

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

      webView.settings.setSupportZoom(false) // 화면 줌 허용여부
      webView.settings.builtInZoomControls = false // 화면 확대 축소 허용여부
      webView.settings.displayZoomControls = false // 줌 컨트롤 없애기.

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

    // JavaScript alert() 커스텀 처리
    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
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

    inner class WebAppInterface {
        
        @JavascriptInterface
        fun closeWindow() {
            runOnUiThread { finish() }
        }
        
        @JavascriptInterface
        fun googleLogin() {
            lifecycleScope.launch {
                try {
                    Log.d("Appwrite", "Starting Google OAuth login...")

                    AppwriteManager.account.createOAuth2Session(
                        activity = this@MainActivity,
                        provider = OAuthProvider.GOOGLE
                    )

                    // ✅ 로그인 성공 후, 사용자 정보 가져오기
                    val user = AppwriteManager.account.get()
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

                    val user = AppwriteManager.account.get()
                    Log.d("Appwrite", "User is logged in: ${user.email}")

                    runOnUiThread {
                        webView.evaluateJavascript("onLoginSuccess('${user.email}')", null)
                    }
                } catch (e: Exception) {
                    Log.d("Appwrite", "User not logged in.")

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
                    Log.d("Appwrite", "Logging out...")

                    AppwriteManager.account.deleteSession("current")

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
                    // 가게 정보 가져오기
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

                    // 주문 데이터 생성
                    val newOrder = mapOf(
                        "shop" to shop,
                        "number" to phoneNumber,
                        "ordernumber" to currentOrderNumber.toString(),
                        "order" to orderJson
                    )

                    val validDocumentId = currentOrderNumber.toString()

                    // Appwrite에 주문 추가
                    AppwriteManager.database.createDocument(
                        databaseId = "tree-kiosk",
                        collectionId = "data",
                        documentId = validDocumentId,
                        data = newOrder
                    )

                    // 주문 번호 증가 후 업데이트
                    val newOrderNumber = currentOrderNumber + 1
                   AppwriteManager.database.updateDocument(
                        databaseId = "tree-kiosk",
                        collectionId = "owner",
                        documentId = ownerDocument.id,
                        data = mapOf("order" to newOrderNumber.toString())
                    )

                    // 성공 시 WebView에서 finish() 호출
                    runOnUiThread {
                        webView.evaluateJavascript("finishsend()", null)
                    }

                } catch (e: Exception) {
                    Log.e("Appwrite", "주문 제출 오류: ${e.message}")

                    runOnUiThread {
                        webView.evaluateJavascript("errorsend('주문 제출 실패: ${e.message}')", null)
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
    }

    private suspend fun getUserDocument(email: String): Pair<Boolean, String?> {
        return try {
            val response = AppwriteManager.database.listDocuments(
                databaseId = "tree-kiosk",
                collectionId = "owner",
                queries = listOf(Query.equal("email", email))
            )

            if (response.documents.isNotEmpty()) {
                val document = response.documents.first()
                val name = document.data["name"] as? String ?: "Unknown"
                Pair(true, name)
            } else {
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e("Appwrite", "사용자 데이터를 가져오는 중 오류 발생: ${e.message}")
            Pair(false, null)
        }
    }

    private fun readJsonFromAssets(): String {
        return try {
            val inputStream = assets.open("image/file.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            jsonString
        } catch (e: Exception) {
            e.printStackTrace()
            "{}" // 에러 발생 시 빈 JSON 반환
        }
    }
    private val messageReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val message = intent?.getStringExtra("message") ?: return
        webView.evaluateJavascript(
            "window.dispatchEvent(new MessageEvent('message', { data: '$message' }));",
            null
        )
    }
}

override fun onResume() {
    super.onResume()
    registerReceiver(messageReceiver, IntentFilter("NEW_WEB_MESSAGE"))
}

override fun onPause() {
    super.onPause()
    unregisterReceiver(messageReceiver)
}
}
