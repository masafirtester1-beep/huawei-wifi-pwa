package com.example.huaweiwifi

import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferences لحفظ البيانات
        prefs = getSharedPreferences("huaweiwifi", MODE_PRIVATE)

        // إنشاء WebView
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Bridge Android ↔ JavaScript
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun getSavedCreds(): String {
                val u = prefs.getString("user", "admin") ?: "admin"
                val p = prefs.getString("pass", "admin1234") ?: "admin1234"
                return """{"user":"$u","pass":"$p"}"""
            }

            @JavascriptInterface
            fun saveCreds(u: String, p: String) {
                prefs.edit().putString("user", u).putString("pass", p).apply()
            }
        }, "Android")

        setContentView(webView)

        // تحميل صفحة HTML من assets
        webView.loadUrl("file:///android_asset/index.html")
    }
}
