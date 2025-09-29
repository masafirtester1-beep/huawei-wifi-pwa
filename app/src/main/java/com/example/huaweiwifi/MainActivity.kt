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

    // لائحة الأذونات (Android 13+ فيها إذن إضافي للوايفاي القريب)
    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.INTERNET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            var granted = true
            for ((_, v) in results) {
                if (!v) granted = false
            }
            if (!granted) {
                Toast.makeText(this, "Permissions required for SSID/RSSI", Toast.LENGTH_LONG).show()
            } else {
                // حدّث المعلومات مباشرة بعد منح الصلاحيات
                webView.evaluateJavascript("updateNativeWifiInfo()", null)
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        // إعدادات WebView
        val wsettings = webView.settings
        wsettings.javaScriptEnabled = true
        wsettings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // واجهة JS → أندرويد
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // طلب الأذونات
        checkAndRequestPermissions()

        // حمّل صفحة الويب المرفقة داخل assets/www/index.html
        webView.loadUrl("file:///android_asset/index.html")
    }

    // طلب الأذونات اللي ناقصة فقط
    private fun checkAndRequestPermissions() {
        val toRequest = mutableListOf<String>()
        for (p in requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p)
            }
        }
        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    /** واجهة متاحة لصفحة الويب */
    inner class WebAppInterface {

        /** إرجاع JSON فيه ssid/rssi وحالة الموقع */
        @JavascriptInterface
        fun getWifiInfo(): String {
            val json = JSONObject()
            try {
                val wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info: WifiInfo? = wifiMgr.connectionInfo

                var ssid: String? = null
                var rssi: Int? = null
                if (info != null) {
                    rssi = info.rssi                 // قد تكون INVALID_RSSI إذا غير متاح
                    ssid = info.ssid                 // "<unknown ssid>" إذا الموقع مطفّي
                }

                // تنظيف SSID
                if (ssid == null || ssid == "<unknown ssid>") {
                    json.put("ssid", JSONObject.NULL)
                } else {
                    val cleaned = ssid.trim().removePrefix("\"").removeSuffix("\"")
                    json.put("ssid", cleaned)
                }

                // RSSI
                if (rssi == null || rssi == WifiInfo.INVALID_RSSI) {
                    json.put("rssi", JSONObject.NULL)
                } else {
                    json.put("rssi", rssi)
                }

                json.put("locationEnabled", isLocationEnabled())
                json.put("ok", true)
            } catch (e: Exception) {
                json.put("ok", false)
                json.put("error", e.message)
            }
            return json.toString()
        }

        /** افتح إعدادات الموقع للمستخدم */
        @JavascriptInterface
        fun requestOpenLocationSettings() {
            val i = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    /** هل الموقع شغّال في الجهاز؟ */
    private fun isLocationEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val lm = applicationContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                lm.isLocationEnabled
            } else {
                val mode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                mode != Settings.Secure.LOCATION_MODE_OFF
            }
        } catch (_: Exception) {
            false
        }
    }
}
