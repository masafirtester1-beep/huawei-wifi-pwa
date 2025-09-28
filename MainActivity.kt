package com.example.huaweiwifi

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var btnInject: Button
    lateinit var etIp: EditText
    lateinit var etUser: EditText
    lateinit var etPass: EditText
    lateinit var etSsid: EditText
    lateinit var etWifiPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        btnInject = findViewById(R.id.btnInject)
        etIp = findViewById(R.id.etIp)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        etSsid = findViewById(R.id.etSsid)
        etWifiPass = findViewById(R.id.etWifiPass)

        // Setup secure prefs for storing admin credentials (optional)
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "creds_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Load saved credentials if exist
        etUser.setText(sharedPrefs.getString("admin_user", "admin"))
        etPass.setText(sharedPrefs.getString("admin_pass", ""))

        // WebView settings
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(JSBridge(this), "AndroidBridge")

        // Load default ip if present
        etIp.setText(sharedPrefs.getString("last_ip", "192.168.8.1"))

        // Open router page when IP changed or when user clicks IP field done
        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            val ip = etIp.text.toString().trim()
            if (ip.isNotEmpty()) {
                val url = if (ip.startsWith("http")) ip else "http://$ip/"
                webView.loadUrl(url)
                Toast.makeText(this, "فتح صفحة الراوتر: $url", Toast.LENGTH_SHORT).show()
                sharedPrefs.edit().putString("last_ip", ip).apply()
            }
        }

        btnInject.setOnClickListener {
            // confirm and then inject
            val user = etUser.text.toString()
            val pass = etPass.text.toString()
            val ssid = etSsid.text.toString()
            val wpass = etWifiPass.text.toString()

            AlertDialog.Builder(this)
                .setTitle("تأكيد")
                .setMessage("هل أنت متأكد أنك تريد تنفيذ التغييرات على الراوتر؟")
                .setPositiveButton("نعم") { _, _ ->
                    // save credentials securely
                    sharedPrefs.edit().putString("admin_user", user).putString("admin_pass", pass).apply()
                    val js = buildInjectionScript(user, pass, ssid, wpass)
                    webView.evaluateJavascript(js, null)
                    Toast.makeText(this, "تم تنفيذ الحقن — راجع صفحة الراوتر", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("لا", null)
                .show()
        }
    }

    private fun buildInjectionScript(user:String, pass:String, ssid:String, wpass:String): String {
        // Build a JS string that will run in the page context
        val esc = { s: String -> s.replace("\\", "\\\\").replace("'", "\\'").replace(""", "\\"") }
        val js = "(function(){ try{ " +
                "var u='${esc(user)}'; var p='${esc(pass)}'; var s='${esc(ssid)}'; var w='${esc(wpass)}';\n" +
                "function set(sel,val){ try{ var e=document.querySelector(sel); if(e){ e.focus(); e.value=val; e.dispatchEvent(new Event('input',{bubbles:true})); return true; } }catch(e){} return false;}\n" +
                "function clickOne(arr){ for(var i=0;i<arr.length;i++){ try{ var b=document.querySelector(arr[i]); if(b){ b.click(); return true; } }catch(e){} } return false;}\n" +
                // try login
                "set('#userName',u) || set('input[name=Username]',u) || set('input[name=UserName]',u);" +
                "set('#pcPassword',p) || set('input[name=Password]',p) || set('input[type=password]',p);" +
                "clickOne(['#loginBtn','button[type=submit]','#loginSubmit','input[type=submit]']);" +
                "setTimeout(function(){ set('#ssid',s) || set('input[name=ssid]',s) || set('input[name=SSID]',s); set('#wpa_psk',w) || set('input[name=wlanKey]',w) || set('input[name=KeyPassphrase]',w); clickOne(['#apply','button[name=apply]','button[type=submit]','input[type=submit]']); alert('تم الحقن — راجع الصفحة.'); },1200);" +
                "}catch(ex){ alert('خطأ: '+ex.message);} })();" 
        return js
    }

    class JSBridge(val ctx: Context) {
        @JavascriptInterface fun showToast(msg: String) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
