package com.example.huaweiwifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // نطلب صلاحيات الموقع (مطلوبة لقراءة SSID/RSSI على أندرويد 10+)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // بعد ما يعطي الصلاحية، نخبر الصفحة تحدث القيم
        runOnUiThread { webView.evaluateJavascript("window.__updateWifi && window.__updateWifi()", null) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // نضيف جسر JS اسمه Android
        webView.addJavascriptInterface(WifiBridge(this), "Android")

        // نحمّل الواجهة من assets
        webView.loadUrl("file:///android_asset/index.html")

        // نطلب الصلاحيات إذا ما كانتش مخوّلة
        ensureLocationPermissions()
    }

    private fun ensureLocationPermissions() {
        val needFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val needCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        if (needFine || needCoarse) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            // (اختياري) أندرويد 13+: هذا مطلوب لمسح الشبكات، مش لقراءة الشبكة المتصلة
            if (Build.VERSION.SDK_INT >= 33) {
                // Manifest.permission.NEARBY_WIFI_DEVICES
                // إذا بغيت تدير Scan مستقبلاً زِدوها فـ AndroidManifest.xml وطلُبوها هنا.
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    class WifiBridge(private val context: Context) {
        @JavascriptInterface
        fun getWifiInfo(): String {
            val result = mutableMapOf<String, Any?>(
                "ssid" to "",
                "rssi" to null,
                "error" to ""
            )
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wm.connectionInfo
                var ssid = info?.ssid ?: ""
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length - 1)
                }
                result["ssid"] = ssid
                // rssi بوحدة dBm (كلما كان أقرب لـ 0 كان أحسن)
                result["rssi"] = info?.rssi
            } catch (e: Exception) {
                result["error"] = e.message ?: "unknown"
            }
            return JSONObject(result as Map<*, *>).toString()
        }

        @JavascriptInterface
        fun requestPermissions() {
            // JS يقدر ينادي هذه إذا احتاج يعاود يطلب الصلاحيات
            (context as? MainActivity)?.ensureLocationPermissions()
        }
    }
}
