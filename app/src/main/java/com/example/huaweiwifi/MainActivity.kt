
package com.example.huaweiwifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

// ✅ OkHttp (حلّ الخطأ)
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = object : WebViewClient() {}
        webView.webChromeClient = WebChromeClient()

        // واجهة JS ليتواصل معها app.js
        webView.addJavascriptInterface(AppBridge(this), "Android")

        // حمّل الواجهة من مجلد الأصول
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class AppBridge(private val ctx: Context) {

        // تزويد الجافاسكريبت بمعلومات SSID/RSSI إن توفرت
        @JavascriptInterface
        fun getWifiInfo(): String {
            val result = JSONObject()
            try {
                val wifiManager =
                    ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

                @Suppress("DEPRECATION")
                val info = wifiManager.connectionInfo

                // ملاحظة: على أندرويد 8+ قد تحتاج صلاحيات الموقع كي يظهر الـ SSID
                val ssid: String? =
                    if (info != null && info.ssid != WifiManager.UNKNOWN_SSID) {
                        info.ssid?.replace("\"", "")
                    } else {
                        null
                    }

                val rssi: Int? = info?.rssi

                result.put("ssid", ssid ?: "غير متاح")
                result.put("rssi", rssi ?: JSONObject.NULL)
            } catch (e: Exception) {
                result.put("ssid", "خطأ")
                result.put("rssi", JSONObject.NULL)
            }
            return result.toString()
        }

        // حفظ بيانات الأدمن محلياً (SharedPreferences)
        @JavascriptInterface
        fun saveAdmin(user: String, pass: String) {
            val sp = ctx.getSharedPreferences("admin_store", Context.MODE_PRIVATE)
            sp.edit().putString("user", user).putString("pass", pass).apply()
        }

        @JavascriptInterface
        fun getAdminUser(): String {
            val sp = ctx.getSharedPreferences("admin_store", Context.MODE_PRIVATE)
            return sp.getString("user", "admin") ?: "admin"
        }

        @JavascriptInterface
        fun getAdminPass(): String {
            val sp = ctx.getSharedPreferences("admin_store", Context.MODE_PRIVATE)
            return sp.getString("pass", "") ?: ""
        }

        // فحص إن كان الاتصال الحالي عبر Wi-Fi
        @JavascriptInterface
        fun isWifiConnected(): Boolean {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
            }
        }

        // طلب GET عام (للراوتر مثلاً)
        @JavascriptInterface
        fun httpGet(url: String): String {
            return try {
                val request: Request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { resp: Response ->
                    val body = resp.body?.string() ?: ""
                    JSONObject()
                        .put("code", resp.code)
                        .put("ok", resp.isSuccessful)
                        .put("body", body)
                        .toString()
                }
            } catch (e: Exception) {
                JSONObject().put("error", e.message ?: "error").toString()
            }
        }

        // طلب POST عام (JSON) – تُمرَّر من JS (مثلاً لتغيير كلمة السر)
        @JavascriptInterface
        fun httpPostJson(url: String, jsonBody: String, authHeader: String?): String {
            return try {
                val media = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = RequestBody.create(media, jsonBody)

                val builder = Request.Builder()
                    .url(url)
                    .post(body)

                if (!authHeader.isNullOrBlank()) {
                    builder.addHeader("Authorization", authHeader)
                }

                val request: Request = builder.build()

                client.newCall(request).execute().use { resp: Response ->
                    val respBody = resp.body?.string() ?: ""
                    JSONObject()
                        .put("code", resp.code)
                        .put("ok", resp.isSuccessful)
                        .put("body", respBody)
                        .toString()
                }
            } catch (e: Exception) {
                JSONObject().put("error", e.message ?: "error").toString()
            }
        }
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
