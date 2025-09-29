package com.example.huaweiwifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiInfo
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

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION, // للحصول على SSID على Android 10+
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // نعلنو للمستخدم
            var granted = true
            for ((_, v) in results) {
                if (!v) granted = false
            }
            if (!granted) {
                Toast.makeText(this, "لاPermissions: بعض الصلاحيات مرفوضة، قد لا تعرض SSID/RSSI.", Toast.LENGTH_LONG).show()
            } else {
                // reload to update wifi info
                webView.evaluateJavascript("updateNativeWifiInfo && updateNativeWifiInfo();", null)
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تصميم بسيط: سنعرض WebView وحيد
        webView = WebView(this)
        setContentView(webView)

        // WebView settings
        val wsettings = webView.settings
        wsettings.javaScriptEnabled = true
        wsettings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // طلب الصلاحيات اللازمة
        checkAndRequestPermissions()

        // حمل صفحة index.html من assets
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun checkAndRequestPermissions() {
        val toRequest = mutableListOf<String>()
        for (p in requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p)
            }
        }
        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        } else {
            // permissions already granted: nothing special
        }
    }

    // واجهة JS: توفر getWifiInfo() اللي يعطينا JSON string
    inner class WebAppInterface(private val ctx: Context) {

        @JavascriptInterface
        fun getWifiInfo(): String {
            val json = JSONObject()
            try {
                val wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info: WifiInfo? = wifiMgr.connectionInfo

                // Android 8+ و10+ تحتاج صلاحية location مفعلة لإظهار SSID
                var ssid: String? = null
                var rssi: Int? = null
                if (info != null) {
                    // RSSI متاح مباشرة
                    rssi = info.rssi
                    // SSID قد يظهر بـ "<unknown ssid>" إذا مافيه تصاريح
                    ssid = info.ssid
                }

                // بديل: جرب قراءة DHCP / Connectivity — لكن هنا نكتفي بهذه الطريقة
                if (ssid == null || ssid == "<unknown ssid>") {
                    // إذا الصلاحية مرفوضة أو الموقع مطفيّ
                    json.put("ssid", JSONObject.NULL)
                } else {
                    // إزالة علامات الاقتباس المحاطة بالـ SSID إن وُجدت
                    val cleaned = ssid.trim().removePrefix("\"").removeSuffix("\"")
                    json.put("ssid", cleaned)
                }

                if (rssi == null || rssi == WifiInfo.INVALID_RSSI) {
                    json.put("rssi", JSONObject.NULL)
                } else {
                    json.put("rssi", rssi)
                }

                // معطيات إضافية
                json.put("locationEnabled", isLocationEnabled())
                json.put("ok", true)
            } catch (e: Exception) {
                json.put("ok", false)
                json.put("error", e.message)
            }
            return json.toString()
        }

        @JavascriptInterface
        fun requestOpenLocationSettings() {
            // يمكن استعماله من JS لفتح إعدادات الموقع
            val i = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        private fun isLocationEnabled(): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val lm = applicationContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                    lm.isLocationEnabled
                } else {
                    // قبل API 28
                    val mode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                    mode != Settings.Secure.LOCATION_MODE_OFF
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
