package com.example.huaweiwifi

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        // إعدادات مهمة باش ما تبقاش شاشة بيضا
        val s: WebSettings = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.loadsImagesAutomatically = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.javaScriptCanOpenWindowsAutomatically = true
        // mixed content (مفيد إلا كان http)
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // باش يفتح داخل التطبيق ومايخرجش للمتصفح
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                Toast.makeText(this@MainActivity, "WebView error: ${error.description}", Toast.LENGTH_LONG).show()
            }
        }

        webView.webChromeClient = WebChromeClient()

        // حمّل index.html من الأصول
        webView.loadUrl("file:///android_asset/index.html")

        setContentView(webView)
    }
}
