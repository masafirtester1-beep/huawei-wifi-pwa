package com.example.huaweiwifi

import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // فعّل Debugging باش تقدر تشوف Console من Chrome (chrome://inspect)
        WebView.setWebContentsDebuggingEnabled(true)

        val webView = WebView(this)

        // WebViewClient باش يبقى التحميل داخل التطبيق
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }
        }

        // ChromeClient باش نلقط رسائل console فـ Logcat
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                Log.d("WEBVIEW", "${message.message()} (line ${message.lineNumber()})")
                return true
            }
        }

        // إعدادات مهمة
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true           // للـ localStorage
        s.databaseEnabled = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        // إذا كان app.js كيدير fetch/http لراوتر 192.168.x
        s.allowUniversalAccessFromFileURLs = true
        s.allowFileAccessFromFileURLs = true

        // حمّل index.html من مجلد assets
        webView.loadUrl("file:///android_asset/index.html")

        setContentView(webView)
    }
}
