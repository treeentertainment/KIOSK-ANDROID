package me.moonmoon.treekioskv4

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.JsResult
import android.app.AlertDialog
import org.json.JSONObject
import org.json.JSONArray
import com.google.firebase.database.DataSnapshot
import android.os.Message
import android.widget.FrameLayout
import android.view.View
import android.view.ViewGroup
import android.app.Activity
import com.google.firebase.database.ServerValue
import android.view.WindowManager
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import android.os.Build
import android.content.Intent
import android.content.pm.ShortcutManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.NetworkCapabilities
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var subWebView: WebView
    private lateinit var auth: FirebaseAuth
    private lateinit var webContainer: FrameLayout
    private lateinit var commonChromeClient: CommonWebChromeClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isConnected(this)) {
         Toast.makeText(this, "인터넷 연결이 없습니다!", Toast.LENGTH_SHORT).show()
         finish() // or disable functionality
         }
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        // 현재 앱의 첫 설치 시간 가져오기
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val currentInstallTime = packageInfo.firstInstallTime

        // 저장된 첫 설치 시간과 비교
        val savedInstallTime = prefs.getLong("last_install_time", -1)

        if (savedInstallTime == -1L || currentInstallTime > savedInstallTime) {
            addShortcut(this)
            prefs.edit().putLong("last_install_time", currentInstallTime).apply()
        }

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // 1. 뷰 초기화
        webView = findViewById(R.id.webView)
        webContainer = findViewById(R.id.webContainer)

        // 2. subWebView 동적 생성 및 초기화
        subWebView = WebView(this)
        subWebView.visibility = View.GONE

        subWebView.setOnCreateContextMenuListener(null)

        subWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false // safer
            allowContentAccess = false // safer
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false // risky: keep disabled unless you really need it
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            builtInZoomControls = false   // 확대/축소 버튼 비활성화
            displayZoomControls = false  // 줌 컨트롤 UI 숨기기
            setSupportZoom(false)
        }

        subWebView.webViewClient = WebViewClient()

        commonChromeClient = CommonWebChromeClient(this, webView, subWebView, webContainer)

        webView.webChromeClient = commonChromeClient
        subWebView.webChromeClient = commonChromeClient


        webContainer.addView(subWebView)  // FrameLayout에 동적으로 추가

        webView.setOnCreateContextMenuListener(null)

        // 3. mainWebView 설정/
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false // safer
            allowContentAccess = false // safer
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false // risky: keep disabled unless you really need it
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            builtInZoomControls = false   // 확대/축소 버튼 비활성화
            displayZoomControls = false  // 줌 컨트롤 UI 숨기기
            setSupportZoom(false)        // 줌 기능 자체 비활성화
        }

        webView.webViewClient = WebViewClient()

        val webInterface = WebAppInterface(this, auth, webView, commonChromeClient)
        webView.addJavascriptInterface(webInterface, "AndroidBridge")
        subWebView.addJavascriptInterface(webInterface, "AndroidBridge")

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            subWebView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }


        // 4. index.html 로딩
        webView.loadUrl("file:///android_asset/index.html")
    }

    fun isConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        val networkInfo = cm.activeNetworkInfo
        networkInfo != null && networkInfo.isConnected
    }
    }

    
    fun addShortcut(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 정확하게 context를 통해 시스템 서비스 가져오기
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            // null 체크 및 지원 여부 확인
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val shortcut = ShortcutInfo.Builder(context, "TREE_KIOSK_SHORT")
                    .setShortLabel("TREE KIOSK V4")
                    .setIcon(Icon.createWithResource(context, R.mipmap.logo500))
                    .setIntent(Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                    })
                    .build()

                shortcutManager.requestPinShortcut(shortcut, null)
            }
        } else {
            // Android 7 이하에서는 기존  방식 사용
            val shortcutIntent = Intent(context, MainActivity::class.java)
            shortcutIntent.action = Intent.ACTION_MAIN

            val addIntent = Intent()
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "TREE KIOSK V4")
            addIntent.putExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context, R.mipmap.logo500)
            )
            addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            addIntent.putExtra("duplicate", false)

            context.sendBroadcast(addIntent)
        }
    }

    class CommonWebChromeClient(
        private val activity: MainActivity,
        private val mainWebView: WebView,
        private var subWebView: WebView,
        private val webContainer: FrameLayout
    ) : WebChromeClient() {


        private var isSubWebViewLoaded = false
        private var pendingMessage: String? = null

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val newWebView = WebView(activity)
            newWebView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false // safer
                allowContentAccess = false // safer
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false // risky: keep disabled unless you really need it
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                builtInZoomControls = false   // 확대/축소 버튼 비활성화
                displayZoomControls = false  // 줌 컨트롤 UI 숨기기
                setSupportZoom(false)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                newWebView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                newWebView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            }

            newWebView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isSubWebViewLoaded = true
                    pendingMessage?.let { msg ->
                        val jsCode = "window.postMessage(${JSONObject.quote(msg)}, '*');"
                        view?.evaluateJavascript(jsCode, null)
                        pendingMessage = null
                    }
                }
            }

            newWebView.webChromeClient = this
            val webInterface = WebAppInterface(activity, FirebaseAuth.getInstance(), newWebView, this)
            newWebView.addJavascriptInterface(webInterface, "AndroidBridge")


            subWebView = newWebView
            webContainer.addView(newWebView)
            mainWebView.visibility = View.INVISIBLE

            val transport = resultMsg?.obj as WebView.WebViewTransport
            transport.webView = newWebView
            resultMsg.sendToTarget()

            return true
        }


        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            AlertDialog.Builder(activity)
                .setTitle("알림")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    result?.confirm()
                }
                .setCancelable(false)
                .create()
                .show()
            return true
        }

        override fun onCloseWindow(window: WebView?) {
            isSubWebViewLoaded = false
            pendingMessage = null

            window?.let {
                val parent = it.parent
                if (parent is ViewGroup) {
                    parent.removeView(it)
                }
                it.destroy()
            }
            mainWebView.visibility = View.VISIBLE
        }


        // 외부에서 호출: 메시지를 WebView에 전달 요청
        fun sendMessageToWebView(isSub: Boolean, message: String) {
            val webView = if (isSub) subWebView else mainWebView
            val jsCode = "window.postMessage(JSON.parse('$message'), '*');"

            if (isSub) {
                if (isSubWebViewLoaded) {
                    webView.evaluateJavascript(jsCode, null)
                } else {
                    // 아직 로드되지 않았다면 대기
                    pendingMessage = message
                }
            } else {
                // mainWebView는 항상 로드되었다고 간주
                webView.evaluateJavascript(jsCode, null)
            }
        }
    }



    class WebAppInterface(
        private val activity: Activity,
        private val auth: FirebaseAuth,
        private val webView: WebView,
        private val commonWebChromeClient: MainActivity.CommonWebChromeClient
    ) {

        private var stateListener: ValueEventListener? = null
        private var stateRef: DatabaseReference? = null

        fun escapeJsString(input: String?): String {
            return input
                ?.replace("\\", "\\\\")
                ?.replace("'", "\\'")
                ?.replace("\"", "\\\"")
                ?.replace("\n", "\\n")
                ?.replace("\r", "")  // 윈도우 개행 제거
                ?: ""
        }
        
        private fun addStateListener(store: String) {
            Log.d("StateListener", "Adding listener for store: $store")

            stateRef = FirebaseDatabase.getInstance().getReference("/people/data/$store/state")

            stateListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("StateListener", "Data snapshot received")

                    val stateData = snapshot.getValue(StateData::class.java)

                    if (stateData != null) {
                        Log.d("StateListener", "State data: $stateData")

                        val stateValue = stateData.state.toInt()
                        val escapedMessage = escapeJsString(stateData.reason.message)
                        val escapedImg = escapeJsString(stateData.reason.img)
                        val moveable = stateValue != 2

                        // Build JavaScript code dynamically
                        val js = StringBuilder()
                        js.append("""
                    (function() {
                        const currentPage = window.location.pathname.split('/').pop();
                """.trimIndent())

                        if (stateValue >= 2) {
                            js.append("""
                        if (currentPage !== 'index.html') {
                            const basePath = window.location.href.substring(0, window.location.href.lastIndexOf('/') + 1);
                            window.location.href = basePath + 'index.html';
                            return;
                        }
                    """.trimIndent())
                        }

                        if (stateValue >= 1) {
                            js.append("if (currentPage === 'index.html') {")
                            if (moveable) {
                                js.append("""
                            document.getElementById('closeicon').style.display = 'block';
                            document.getElementById('closebutton').style.display = 'block';
                            window.moveable = true;
                        """.trimIndent())
                            } else {
                                js.append("""
                            window.moveable = false;
                            document.getElementById('closeicon').style.display = 'none';
                            document.getElementById('closebutton').style.display = 'none';
                        """.trimIndent())
                            }

                            js.append("""
                        document.getElementById('modal-name').innerHTML = '$escapedMessage';
                        document.getElementById('alertbox').classList.add('active');
                    """.trimIndent())

                            if (stateData.reason.img != null && stateData.reason.img != "null" && stateData.reason.img.isNotEmpty()) {
                                js.append("""
                            document.getElementById('modal-image').src = '$escapedImg';
                            document.getElementById('modal-image').style.display = 'block';
                        """.trimIndent())
                            }

                            js.append("}})();")  // Closing the JS function
                        } else {
                            js.append("""
                        document.getElementById('alertbox').classList.remove('active');
                        show('startface', 'login-container');
                    """.trimIndent())
                        }

                        Log.d("StateListener", "JavaScript to execute: $js")

                        // Run JavaScript in the WebView on the main thread
                        activity.runOnUiThread {
                            webView.evaluateJavascript(js.toString(), null)
                        }
                    } else {
                        Log.d("StateListener", "State data is null")

                        // Handle case where stateData is null
                        val js = """
                    document.getElementById('alertbox').classList.remove('active');
                    show('startface', 'login-container');
                """.trimIndent()

                        activity.runOnUiThread {
                            webView.evaluateJavascript(js, null)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StateListener", "StateListener cancelled", error.toException())
                }
            }

            // Add the ValueEventListener to the Firebase reference
            stateRef?.addValueEventListener(stateListener!!)
            Log.d("StateListener", "Listener added for store: $store")
        }


        @JavascriptInterface
        fun postMessage(data: String) {
            Log.d("WebAppInterface", "Received data: $data")

            activity.runOnUiThread {
                try {
                    val json = JSONObject(data)
                    val type = json.getString("type")

                    val isSubTarget = when (type) {
                        "UpdateOrder", "newOrder", "noselect", "original" -> false
                        else -> true // 나머지는 서브 웹뷰
                    }

                    commonWebChromeClient.sendMessageToWebView(isSubTarget, data)
                } catch (e: Exception) {
                    Log.e("WebAppInterface", "JSON parsing failed: ${e.message}")
                }
            }
        }


        @JavascriptInterface
        fun signInWithEmailAndPassword(email: String, password: String) {
            val fixedEmail = email.replace(".", "@")
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val dbRef = FirebaseDatabase.getInstance().getReference("/people/admin/$fixedEmail")
                    dbRef.get().addOnSuccessListener { adminSnapshot ->
                        val adminData = adminSnapshot.value as? Map<*, *>
                        val enabled = adminData?.get("enabled") as? Boolean ?: false

                        // store 값이 숫자든 문자열이든 받아올 수 있게 처리
                        val storeAny = adminData?.get("store")
                        val store = when (storeAny) {
                            is String -> storeAny
                            is Number -> storeAny.toInt().toString()
                            else -> null
                        }

                        if (enabled && store != null) {
                            val dataRef = FirebaseDatabase.getInstance().getReference("/people/data/$store")
                            dataRef.get().addOnSuccessListener { dataSnapshot ->
                                val data = dataSnapshot.value as? Map<*, *>
                                val storedEmail = data?.get("email") as? String
                                val name = data?.get("name") as? String

                                if (storedEmail == fixedEmail) {
                                    val jsCode = """
                                        localStorage.setItem('email', JSON.stringify('$fixedEmail'));
                                        localStorage.setItem('name', JSON.stringify('$name'));
                                        localStorage.setItem('number', JSON.stringify('$store'));      
                                         loginfinish();
                                    """.trimIndent()
                                    activity.runOnUiThread {
                                        webView.evaluateJavascript(jsCode, null)
                                    }

                                    addStateListener(store)  // 🔁 상태 리스너 연결

                                } else {
                                    auth.signOut()
                                    activity.runOnUiThread {
                                        webView.evaluateJavascript("loginfail('이메일이 일치 하지 않습니다.');", null)
                                    }
                                }
                            }
                        } else {
                            auth.signOut()
                            activity.runOnUiThread {
                                webView.evaluateJavascript("loginfail('권한이 없습니다.');", null)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    val errorMessage = it.message ?: "로그인 실패"
                    Log.e("AUTH_ERROR", "로그인 실패: $errorMessage")
                    activity.runOnUiThread {
                        webView.evaluateJavascript("loginfail('$errorMessage');", null)
                    }
                }

        }


        @JavascriptInterface
        fun clearStateListener() {
            stateListener?.let { stateRef?.removeEventListener(it) }
            stateRef = null
            stateListener = null
        }

        @JavascriptInterface
        fun checkAuthState() {
            val user = auth.currentUser
            if (user != null) {
                val email = user.email?.replace(".", "@") ?: return

                val dbRef = FirebaseDatabase.getInstance().getReference("/people/admin/$email")
                dbRef.get().addOnSuccessListener { adminSnapshot ->
                    val adminData = adminSnapshot.value as? Map<*, *>
                    val enabled = adminData?.get("enabled") as? Boolean ?: false

                    val storeAny = adminData?.get("store")
                    val store = when (storeAny) {
                        is String -> storeAny
                        is Number -> storeAny.toInt().toString()
                        else -> null
                    }

                    if (enabled && store != null) {
                        val dataRef = FirebaseDatabase.getInstance().getReference("/people/data/$store")
                        dataRef.get().addOnSuccessListener { dataSnapshot ->
                            val data = dataSnapshot.value as? Map<*, *>
                            val storedEmail = data?.get("email") as? String
                            val name = data?.get("name") as? String

                            if (storedEmail == email) {
                                val jsCode = """
                            localStorage.setItem('email', JSON.stringify('$email'));
                            localStorage.setItem('name', JSON.stringify('$name'));
                            localStorage.setItem('number', JSON.stringify('$store'));
                            loginfinish();
                        """.trimIndent()
                                activity.runOnUiThread {
                                    webView.evaluateJavascript(jsCode, null)
                                }

                                addStateListener(store)  // 🔁 상태 리스너 연결
                            } else {
                                auth.signOut()
                                activity.runOnUiThread {
                                    webView.evaluateJavascript("loginfail('이메일이 일치하지 않습니다.');", null)
                                }
                            }
                        }
                    } else {
                        auth.signOut()
                        activity.runOnUiThread {
                            webView.evaluateJavascript("loginfail('허용된 사용자가 아닙니다.');", null)
                        }
                    }
                }.addOnFailureListener {
                    activity.runOnUiThread {
                        webView.evaluateJavascript("loginfail('권한이 없습니다.');", null)
                    }
                }
            } else {
                activity.runOnUiThread {
                    webView.evaluateJavascript("loginfail(null);", null)
                }
            }
        }

        @JavascriptInterface
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        activity.runOnUiThread {
            webView.evaluateJavascript("show('login-container', 'startface');", null)
        }
    }

    @JavascriptInterface
    fun requestMenuData(number: String) {

        val safeNumber = number.toString()

        val databaseRef =
            FirebaseDatabase.getInstance().getReference("/people/data/$safeNumber/menu")
            databaseRef.get().addOnSuccessListener { snapshot ->
            val cafeSnapshot = snapshot.child("cafe")
            val rootJson = JSONObject()
            val cafe = JSONObject()
        
            // 함수: DataSnapshot → JSONArray
            fun buildArray(snapshot: DataSnapshot): JSONArray {
                val arr = JSONArray()
                for (item in snapshot.children) {
                    val map = item.value as? Map<*, *> ?: continue
                    val json = JSONObject(map as Map<*, *>)
                    arr.put(json)
                }
                return arr
            }
        
            // "cafe" 안의 "drinks"와 "foods"
            cafe.put("drinks", buildArray(cafeSnapshot.child("drinks")))
            cafe.put("foods", buildArray(cafeSnapshot.child("foods")))
        
            // 전체 rootJson에 "cafe"와 "service" 삽입
            rootJson.put("cafe", cafe)
            rootJson.put("services", buildArray(snapshot.child("services")))
        
            // 웹뷰에 전달할 JS 코드
            val jsCode = "javascript:display($rootJson);"
        
            // WebView에 실행
            webView.post {
                webView.evaluateJavascript(jsCode, null)
            }
        }
    }

        @JavascriptInterface
        fun sendOrder(jsonOrder: String, number: String, numberDisplayValue: String) {
            val database = FirebaseDatabase.getInstance().reference
            val numberRef = database.child("people/data/$number/number")

            numberRef.get().addOnSuccessListener { snapshot ->
                val orderNumber = snapshot.getValue(Int::class.java) ?: 0

                val orderArray = JSONArray(jsonOrder)
                val orderList = mutableListOf<Any>()
                orderList.add(numberDisplayValue)

                for (i in 0 until orderArray.length()) {
                    val item = orderArray.getJSONObject(i)

                    // Convert the JSONObject to a Map
                    val optionsMap = mutableMapOf<String, Any>()
                    val rawOptions = item.optJSONArray("options")
                    if (rawOptions != null) {
                        val optionsList = mutableListOf<Any>()
                        for (j in 0 until rawOptions.length()) {
                            val option = rawOptions.get(j)
                            if (option is JSONObject) {
                                val optionMap = mutableMapOf<String, Any>()
                                option.keys().forEach { key ->
                                    optionMap[key] = option.get(key)
                                }
                                optionsList.add(optionMap)
                            } else {
                                optionsList.add(option) // primitive types like String, Int, etc.
                            }
                        }

                        optionsMap["options"] = optionsList
                    }

                    // Create a map for each item
                    val map = mutableMapOf<String, Any>(
                        "id"       to item.getString("id"),
                        "quantity" to item.optInt("quantity", 0),
                        "name"     to item.getString("name")
                    )
                    map.putAll(optionsMap)  // Merge optionsMap into the main map
                    orderList.add(map)
                }

                // Save the order list to Firebase
                val orderRef = database.child("people/data/$number/order/$orderNumber")
                orderRef.setValue(orderList).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updates = hashMapOf<String, Any>(
                            "/people/data/$number/number" to ServerValue.increment(1)
                        )
                        database.updateChildren(updates).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                webView.evaluateJavascript("sendfinish();", null)
                            } else {
                                Log.e("AndroidBridge", "Error updating number", updateTask.exception)
                            }
                        }
                    } else {
                        Log.e("AndroidBridge", "Error saving order", task.exception)
                    }
                }
            }
        }

        @JavascriptInterface
        fun logOut() {
            auth.signOut() // Firebase 로그아웃
            // 상태 리스너를 제거
            clearStateListener()
        }
    }

    data class StateData(
        val state: Int = 0,
        val reason: Reason = Reason()
    )

    data class Reason(
        val message: String = "",
        val img: String = ""
    )


    override fun onDestroy() {
        webView.destroy()
        subWebView.destroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            super.onDestroy()
    }
}
