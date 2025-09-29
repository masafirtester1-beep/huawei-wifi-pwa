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

    // لائحة الأذونات (Android 13+ فيه إذن إضافي للوايفاي القريب)
    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.INTERNET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val denied = results.filterValues { !it }
            if (denied.isNotEmpty()) {
                Toast.makeText(this, "خاصك تعطي جميع الصلاحيات باش التطبيق يخدم", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        // طلب الصلاحيات
        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        }

        // إعداد WebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // حمّل صفحة ويب ديالك (غيّر الرابط إذا بغيت)
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
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
                    rssi = info.rssi
                    ssid = info.ssid
                }

                // تنظيف SSID
                if (ssid == null || ssid == "<unknown ssid>") {
                    json.put("ssid", JSONObject.NULL)
                } else {
                    val cleaned = ssid.trim().removePrefix("\"").removeSuffix("\"")
                    json.put("ssid", cleaned)
                }

                // RSSI
                if (rssi == null || rssi == android.net.wifi.WifiManager.INVALID_RSSI) {
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

        @JavascriptInterface
        fun openLocationSettings() {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun isLocationEnabled(): Boolean {
        val locationMode: Int
        return try {
            locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
            locationMode != Settings.Secure.LOCATION_MODE_OFF
        } catch (e: Exception) {
            false
        }
    }
}
