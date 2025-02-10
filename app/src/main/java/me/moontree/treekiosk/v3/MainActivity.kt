package me.moontree.treekiosk.v3

import android.annotation.SuppressLint
import android.os.Bundle
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
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            allowFileAccessFromFileURLs = true
            setSupportMultipleWindows(true)
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(), "AndroidApp")
        WebView.setWebContentsDebuggingEnabled(true)

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
        fun googleLogin() {
            lifecycleScope.launch {
                try {
                    Log.d("Appwrite", "Starting Google OAuth login...")

                    account.createOAuth2Session(
                        activity = this@MainActivity,
                        provider = OAuthProvider.GOOGLE
                    )

                    // ✅ 로그인 성공 후, 사용자 정보 가져오기
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
                    // 가게 정보 가져오기
                    val ownerDocuments = database.listDocuments(
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
                    database.createDocument(
                        databaseId = "tree-kiosk",
                        collectionId = "data",
                        documentId = validDocumentId,
                        data = newOrder
                    )

                    // 주문 번호 증가 후 업데이트
                    val newOrderNumber = currentOrderNumber + 1
                    database.updateDocument(
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
    }

    private suspend fun getUserDocument(email: String): Pair<Boolean, String?> {
        return try {
            val response = database.listDocuments(
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
}
