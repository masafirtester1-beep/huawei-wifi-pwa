package com.example.huaweiwifi

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true).followSslRedirects(true).build()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class AndroidBridge {

        // === (A) معلومات الواي فاي المحلية ===
        @JavascriptInterface
        fun getWifiInfo(): String {
            return try {
                val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                val conn = wm.connectionInfo
                val ssid = conn.ssid?.replace("\"", "") ?: ""
                val rssi = conn.rssi
                JSONObject().apply {
                    put("ssid", ssid)
                    put("rssi", rssi)
                }.toString()
            } catch (e: Exception) {
                JSONObject().put("error", e.message ?: "wifi error").toString()
            }
        }

        // === (B) طلب تغيير كلمة السر — عام وقابل للتخصيص ===
        // jsonStr أمثلة أسفل (في JS)
        @JavascriptInterface
        fun changeWifiPassword(jsonStr: String): String {
            val resp = JSONObject().put("ok", false)
            return try {
                val j = JSONObject(jsonStr)

                val scheme = j.optString("scheme", "http")
                val host   = j.getString("host")              // مثال: 192.168.1.1
                val port   = j.optInt("port", -1)             // 80/443 أو -1
                val path   = j.getString("path")              // مثال: "/api/change_password"
                val method = j.optString("method", "POST")    // POST/GET
                val auth   = j.optString("auth", "none")      // none/basic/hilink
                val user   = j.optString("user", "")
                val pass   = j.optString("pass", "")
                val newPw  = j.getString("newPassword")       // كلمة السر الجديدة
                val bodyType = j.optString("bodyType", "json")// json/form
                val extra   = j.optJSONObject("extra") ?: JSONObject()

                val url = buildString {
                    append("$scheme://$host")
                    if (port != -1) append(":$port")
                    append(path)
                }

                val headers = Headers.Builder()
                // Authorization
                when (auth.lowercase()) {
                    "basic" -> {
                        val b = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
                        headers.add("Authorization", "Basic $b")
                    }
                    "hilink" -> {
                        // Huawei HiLink: نحتاج token قبل POST
                        // 1) GET token (غالباً من /api/webserver/SesTokInfo)
                        val sesTok = getHiLinkSesTok(host, port)
                        if (!sesTok.first) {
                            return resp.put("message", "HiLink token failed").toString()
                        }
                        headers.add("__RequestVerificationToken", sesTok.second)
                        // كوكي الجلسة يتحط تلقائياً من client (استعملنا نفس OkHttpClient)
                    }
                }

                // Body
                val reqBody: RequestBody = when (bodyType.lowercase()) {
                    "form" -> {
                        // أسماء الحقول تقدر تغيّرها من extra
                        val form = FormBody.Builder()
                            .add(extra.optString("adminUserField", "adminUser"), user)
                            .add(extra.optString("adminPassField", "adminPass"), pass)
                            .add(extra.optString("newPassField",  "newPass"),  newPw)
                            .build()
                        form
                    }
                    else -> {
                        val payload = JSONObject().apply {
                            // أسماء الحقول مرنة:
                            put(extra.optString("adminUserField", "adminUser"), user)
                            put(extra.optString("adminPassField", "adminPass"), pass)
                            put(extra.optString("newPassField",  "newPass"),  newPw)
                        }.toString()
                        RequestBody.create("application/json; charset=utf-8".toMediaType(), payload)
                    }
                }

                val req = Request.Builder()
                    .url(url)
                    .method(method.uppercase(), if (method.equals("POST", true)) reqBody else null)
                    .headers(headers.build())
                    .build()

                val res = client.newCall(req).execute()
                val ok = res.isSuccessful
                val bodyTxt = res.body?.string().orEmpty()

                resp.put("ok", ok)
                    .put("code", res.code)
                    .put("body", if (bodyTxt.length > 500) bodyTxt.take(500) + "..." else bodyTxt)
                    .toString()
            } catch (e: Exception) {
                resp.put("message", e.message ?: "change error").toString()
            }
        }

        // === (C) جلب توكن HiLink (مبسّط) ===
        private fun getHiLinkSesTok(host: String, port: Int): Pair<Boolean, String> {
            return try {
                val url = buildString {
                    append("http://$host")
                    if (port != -1) append(":$port")
                    append("/api/webserver/SesTokInfo")
                }
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { r ->
                    val t = r.body?.string().orEmpty()
                    // التوكن كيتوجد داخل XML مثل:
                    // <TokInfo>__RequestVerificationToken=xxxx</TokInfo> أو <TokInfo><Tok>xxx</Tok>...
                    val token = Regex("<Tok>(.*?)</Tok>").find(t)?.groupValues?.get(1)
                        ?: Regex("__RequestVerificationToken=([A-Za-z0-9]+)").find(t)?.groupValues?.get(1)
                        ?: ""
                    if (token.isNotEmpty()) Pair(true, token) else Pair(false, "")
                }
            } catch (e: Exception) {
                Pair(false, "")
            }
        }
    }
}
