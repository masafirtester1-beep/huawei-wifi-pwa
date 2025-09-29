package com.example.huaweiwifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // طلب صلاحيات الموقع/الواي فاي (مطلوبة لقراءة SSID/RSSI)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* لا حاجة لمعالجة النتيجة هنا؛ سنحاول القراءة عند كل استدعاء */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        // إعدادات الـ WebView
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
        }

        webView.webViewClient = WebViewClient()

        // واجهة جافاسكريبت → Android
        webView.addJavascriptInterface(AndroidBridge(this), "Android")

        // طلب الصلاحيات عند الإقلاع (Android 6+)
        requestWifiPermissionsIfNeeded()

        // تحميل واجهة الويب من assets
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun requestWifiPermissionsIfNeeded() {
        val needsFineLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        val needsCoarseLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        val needsWifiState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED

        val toRequest = mutableListOf<String>()
        if (needsFineLocation)  toRequest += Manifest.permission.ACCESS_FINE_LOCATION
        if (needsCoarseLocation) toRequest += Manifest.permission.ACCESS_COARSE_LOCATION
        if (needsWifiState)      toRequest += Manifest.permission.ACCESS_WIFI_STATE

        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    // الجسر الذي تستدعيه الواجهة عبر window.Android.XXX
    inner class AndroidBridge(private val ctx: Context) {

        @JavascriptInterface
        fun getWifiInfo(): String {
            // ملاحظة: على Android 8+، قراءة SSID تتطلب أن يكون الموقع مفعَّلًا والصلاحيات ممنوحة
            return try {
                val wifiManager = ctx.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                val info = wifiManager.connectionInfo

                // بعض الأجهزة ترجع <unknown ssid> إذا لم تتوفر الشروط
                val ssidRaw = info.ssid ?: ""
                val ssid = ssidRaw.replace("\"", "")
                val rssi = info.rssi

                """{"ssid":"$ssid","rssi":$rssi}"""
            } catch (e: Exception) {
                """{"ssid":"غير متاح","rssi":null,"error":"${e.message}"}"""
            }
        }

        // تخزين/قراءة بيانات المدير محليًا (SharedPreferences)
        private val prefs by lazy { ctx.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE) }

        @JavascriptInterface
        fun saveAdmin(user: String, pass: String): Boolean {
            return try {
                prefs.edit()
                    .putString("admin_user", user)
                    .putString("admin_pass", pass)
                    .apply()
                true
            } catch (_: Exception) { false }
        }

        @JavascriptInterface
        fun getAdmin(): String {
            val user = prefs.getString("admin_user", "admin") ?: "admin"
            val pass = prefs.getString("admin_pass", "") ?: ""
            return """{"user":"$user","pass":"$pass"}"""
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
