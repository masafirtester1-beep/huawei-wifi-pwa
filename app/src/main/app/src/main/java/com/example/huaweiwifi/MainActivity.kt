package com.example.huaweiwifi

import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // فعّل ديباغينگ ديال WebView (غيبان فـLogcat)
        WebView.setWebContentsDebuggingEnabled(true)

        val webView = WebView(this)

        // WebViewClient باش نعالجو الأخطاء ديال اللودينگ والنَّافيگيشن
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("HWIFI", "Page finished: $url")
            }

            @Deprecated("Deprecated in API 23, لكن مفيد لعرض الأخطاء على نسخ قديمة")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                Log.e("HWIFI", "WV error ($errorCode) on $failingUrl: $description")
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                Log.e(
                    "HWIFI",
                    "WV error ${error.errorCode} on ${request.url}: ${error.description}"
                )
            }
        }

        // WebChromeClient باش نْلوݣيو console.log من JS
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "HWIFI",
                    "JS: ${consoleMessage.message()} -- ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                )
                return true
            }
        }

        // إعدادات WebView
        with(webView.settings) {
            javaScriptEnabled = true               // ضروري حيث كنستعمل JS
            domStorageEnabled = true               // localStorage/IndexedDB
            allowFileAccess = true
            allowContentAccess = true
            // إلا كان index.html كيسحب ملفات من HTTP، خليه يمر (غير للديباغ/اللان)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // من asset → كيسمح لروابط file:// فالحالة هادي
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        setContentView(webView)

        // لود من assets/index.html (راه عندك فـ app/src/main/assets/index.html)
        val url = "file:///android_asset/index.html"
        Log.d("HWIFI", "Loading $url")
        webView.loadUrl(url)
    }
}
