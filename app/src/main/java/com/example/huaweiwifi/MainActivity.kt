package com.example.huaweiwifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // لائحة الأذونات (Android 13+ فيها إذن إضافي للواي فاي القريب)
    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.INTERNET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)

        // تفعيل الجافاسكريبت
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // إضافة واجهة جافاسكريبت
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        setContentView(webView)

        // تحميل صفحة من assets
        webView.loadUrl("file:///android_asset/index.html")

        // طلب الأذونات
        ActivityCompat.requestPermissions(this, requiredPermissions, 1)
    }

    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun getWifiInfo(): String {
            val json = JSONObject()
            try {
                val wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info: WifiInfo? = wifiMgr.connectionInfo

                var ssid: String? = null
                var rssi: Int? = null

                if (info != null) {
                    ssid = info.ssid
                    rssi = info.rssi
                }

                // تنظيف SSID
                if (ssid == null || ssid == "<unknown ssid>") {
                    json.put("ssid", JSONObject.NULL)
                } else {
                    val cleaned = ssid.trim().removePrefix("\"").removeSuffix("\"")
                    json.put("ssid", cleaned)
                }

                // التحقق من RSSI
                if (rssi == null || rssi == android.net.wifi.WifiManager.INVALID_RSSI) {
                    json.put("rssi", JSONObject.NULL)
                } else {
                    json.put("rssi", rssi)
                }

                // حالة تفعيل الموقع
                json.put("locationEnabled", isLocationEnabled())
                json.put("ok", true)
            } catch (e: Exception) {
                json.put("ok", false)
                json.put("error", e.message)
            }
            return json.toString()
        }

        private fun isLocationEnabled(): Boolean {
            return try {
                val locationMode = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE
                )
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun openLocationSettings() {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        }
    }
}
