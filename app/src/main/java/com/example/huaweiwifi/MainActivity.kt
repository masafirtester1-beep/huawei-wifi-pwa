package com.example.huaweiwifi

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // Register JavaScript interface
        webView.addJavascriptInterface(AndroidBridge(this), "Android")

        // load the local assets/index.html
        webView.loadUrl("file:///android_asset/index.html")

        setContentView(webView)
    }

    class AndroidBridge(private val ctx: Context) {
        // Called from JS: window.Android.getWifiInfo()
        @android.webkit.JavascriptInterface
        fun getWifiInfo(): String {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wm.connectionInfo
            val rssi = wifiInfo.rssi // RSSI in dBm
            val ssid = wifiInfo.ssid ?: ""
            val gateway = getGateway(ctx) // يمكنك تنفيذ طريقة لاستخراج gateway إن أردت

            val jo = JSONObject()
            jo.put("rssi", rssi)
            jo.put("ssid", ssid)
            jo.put("gateway", gateway)
            return jo.toString()
        }

        private fun getGateway(ctx: Context): String? {
            try {
                val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcp = wm.dhcpInfo
                val gw = dhcp.gateway
                // convert int to dotted ip:
                val ip = (gw and 0xFF).toString() + "." + ((gw shr 8) and 0xFF) + "." + ((gw shr 16) and 0xFF) + "." + ((gw shr 24) and 0xFF)
                return ip
            } catch (e: Exception) {
                return null
            }
        }
    }
}
