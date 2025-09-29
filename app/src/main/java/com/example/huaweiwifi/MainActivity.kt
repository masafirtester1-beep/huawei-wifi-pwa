package com.example.huaweiwifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Activity Result API لطلب الصلاحيات
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // نبعتو تحديث للـ JS بعد ما يسالي الطلب
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        runOnUiThread {
            webView.evaluateJavascript(
                "window.onPermissionsUpdated && window.onPermissionsUpdated(${granted});",
                null
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            // للسماح بتحميل http من الراوتر
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // نضيف جسر JS
        webView.addJavascriptInterface(JsBridge(this), "Android")

        // حمّل صفحة الواجهة من الأصول
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class JsBridge(private val ctx: Context) {

        /** يناديه JS باش يطلب/يحدّث الصلاحيات. */
        @JavascriptInterface
        fun askPermissions(): String {
            // إذا الموقع طافي، نقترحو على المستخدم يفتحو
            if (!isLocationEnabled(ctx)) {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return "requested"
        }

        /** يرجّع JSON فيه ssid و rssi. */
        @JavascriptInterface
        fun getWifiInfo(): String {
            val js = JSONObject()
            val (ssid, rssi) = readWifiInfo(ctx)
            js.put("ssid", ssid ?: "N/A")
            js.put("rssi", rssi ?: "N/A")
            return js.toString()
        }
    }

    // -------- Utilities --------

    private fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            try {
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE) !=
                        Settings.Secure.LOCATION_MODE_OFF
            } catch (_: Exception) { false }
        }
    }

    @SuppressLint("MissingPermission")
    private fun readWifiInfo(context: Context): Pair<String?, Int?> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // تأكد أننا فعلاً على واي-فاي
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)
        val onWifi = nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!onWifi) return Pair(null, null)

        // connectionInfo مازال مفيد وكيخدم لمعظم الأجهزة
        val info = wm.connectionInfo
        var ssid = info?.ssid
        val rssi = info?.rssi

        // بعض الأجهزة كترجع ssid بين علامات اقتباس
        if (!ssid.isNullOrEmpty() && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length - 1)
        }
        // Android 8+ بدون صلاحيات/لوكيشن طافي ➜ غالباً "<unknown ssid>"
        if (ssid.equals("<unknown ssid>", ignoreCase = true)) ssid = null

        return Pair(ssid, rssi)
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
