// ===== Huawei HiLink helpers (inside MainActivity.kt) =====
import android.util.Base64
import android.webkit.JavascriptInterface
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

private data class HiLinkSession(val sessionId: String, val token: String)

private fun sha256Hex(s: String): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(s.toByteArray())
}
private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

// GET /api/webserver/SesTokInfo  -> يعطينا SessionID + __RequestVerificationToken
private fun hilinkGetSesTokInfo(gw: String): HiLinkSession {
    val url = URL("http://$gw/api/webserver/SesTokInfo")
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 6000
        readTimeout = 6000
    }
    val body = conn.inputStream.bufferedReader().readText()
    // مثال الرد:
    // <response><SesInfo>SessionID=...</SesInfo><TokInfo>__RequestVerificationToken</TokInfo></response>
    val sessionId = Regex("<SesInfo>(.*?)</SesInfo>").find(body)?.groupValues?.get(1)
        ?.replace("SessionID=", "") ?: ""
    val token = Regex("<TokInfo>(.*?)</TokInfo>").find(body)?.groupValues?.get(1) ?: ""
    return HiLinkSession(sessionId, token)
}

// POST XML
private fun httpPostXml(urlStr: String, xml: String, cookies: String?, token: String?): Pair<Int, String> {
    val url = URL(urlStr)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 7000
        readTimeout = 7000
        doOutput = true
        setRequestProperty("Content-Type", "text/xml; charset=UTF-8")
        if (!cookies.isNullOrEmpty()) setRequestProperty("Cookie", cookies)
        if (!token.isNullOrEmpty()) setRequestProperty("__RequestVerificationToken", token)
    }
    conn.outputStream.use { it.write(xml.toByteArray(Charsets.UTF_8)) }
    val code = conn.responseCode
    val stream = if (code in 200..399) conn.inputStream else conn.errorStream
    val resp = stream?.bufferedReader()?.readText() ?: ""
    return Pair(code, resp)
}

// تسجيل الدخول HiLink:
// password_type=4 و Password = Base64(SHA256(user + Base64(SHA256(pass)) + token))
private fun hilinkLogin(gw: String, user: String, pass: String): Pair<Boolean, HiLinkSession?> {
    val st = hilinkGetSesTokInfo(gw)
    val inner = b64(sha256Hex(pass))                   // Base64(SHA256(pass))
    val combo = user + inner + st.token
    val pwdB64 = b64(sha256Hex(combo))                 // Base64(SHA256(...))
    val xml = """
        <request>
            <Username>${user}</Username>
            <Password>${pwdB64}</Password>
            <password_type>4</password_type>
        </request>
    """.trimIndent()
    val cookies = "SessionID=${st.sessionId}"
    val (code, resp) = httpPostXml("http://$gw/api/user/login", xml, cookies, st.token)
    val ok = code in 200..399 && resp.contains("<response>OK</response>", ignoreCase = true)
    // بعد login، من الأفضل ناخدو Token جديد
    val st2 = if (ok) hilinkGetSesTokInfo(gw) else null
    return Pair(ok, st2)
}

// جلب الـ RSSI والSSID
private fun hilinkGetWifiInfo(gw: String, cookies: String?): Pair<String?, String?> {
    // SSID
    val ssidUrl = URL("http://$gw/api/wlan/basic-settings")
    val ssidConn = (ssidUrl.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 6000
        readTimeout = 6000
        if (!cookies.isNullOrEmpty()) setRequestProperty("Cookie", cookies)
    }
    val ssidXml = ssidConn.inputStream.bufferedReader().readText()
    val ssid = Regex("<SsID>(.*?)</SsID>", RegexOption.IGNORE_CASE).find(ssidXml)
        ?.groupValues?.get(1)

    // RSSI (كيختلف بين الأجهزة: monitoring/status أو device/signal)
    var rssi: String? = null
    try {
        val sUrl = URL("http://$gw/api/monitoring/status")
        val sConn = (sUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            if (!cookies.isNullOrEmpty()) setRequestProperty("Cookie", cookies)
        }
        val sXml = sConn.inputStream.bufferedReader().readText()
        // بعض الموديلات فيها <SignalIcon> أو <SignalStrength> أو <rssi>
        rssi = Regex("<SignalStrength>(.*?)</SignalStrength>", RegexOption.IGNORE_CASE)
            .find(sXml)?.groupValues?.get(1)
            ?: Regex("<rssi>(.*?)</rssi>", RegexOption.IGNORE_CASE).find(sXml)?.groupValues?.get(1)
    } catch (_: Exception) { }

    return Pair(ssid, rssi)
}

// تغيير كلمة سر الواي-فاي
// بعض الموديلات كتحتاج /api/wlan/security-settings، وبعضها /api/wlan/basic-settings
@JavascriptInterface
fun changeWifiPasswordHuawei(gw: String, user: String, pass: String, newPass: String): String {
    return try {
        val (okLogin, st) = hilinkLogin(gw, user, pass)
        if (!okLogin || st == null) return """{"ok":false,"error":"Login failed"}"""
        val cookies = "SessionID=${st.sessionId}"

        // 1) جرّب security-settings
        var tokenNow = st.token
        var xml = """
           <request>
              <WifiAuthMode>WPA2-PSK</WifiAuthMode>
              <WifiEncrypType>AES</WifiEncrypType>
              <WifiPassword>${newPass}</WifiPassword>
           </request>
        """.trimIndent()
        var (code, resp) = httpPostXml("http://$gw/api/wlan/security-settings", xml, cookies, tokenNow)
        var success = code in 200..399 && resp.contains("<response>OK</response>", true)

        // بعض الموديلات كتحتاج Token جديد قبل كل POST
        if (!success) {
            val st3 = hilinkGetSesTokInfo(gw)
            tokenNow = st3.token
            val r2 = httpPostXml("http://$gw/api/wlan/security-settings", xml, cookies, tokenNow)
            code = r2.first; resp = r2.second
            success = code in 200..399 && resp.contains("<response>OK</response>", true)
        }

        if (!success) {
            // 2) جرّب basic-settings (بعض الأجهزة كتقبل Pass هنا مع باقي الحقول)
            val st4 = hilinkGetSesTokInfo(gw)
            tokenNow = st4.token
            // كيطلب SsID و HideSSID إلخ. غادي نقرا SSID الحالي ونحافظ عليه
            val (ssidNow, _) = hilinkGetWifiInfo(gw, cookies)
            val safeSsid = ssidNow ?: "HiLinkWiFi"
            xml = """
              <request>
                 <WifiEnable>1</WifiEnable>
                 <WifiRestart>1</WifiRestart>
                 <SsID>${safeSsid}</SsID>
                 <HideSSID>0</HideSSID>
                 <WifiAuthMode>WPA2-PSK</WifiAuthMode>
                 <WifiEncrypType>AES</WifiEncrypType>
                 <WifiPassword>${newPass}</WifiPassword>
              </request>
            """.trimIndent()
            val r3 = httpPostXml("http://$gw/api/wlan/basic-settings", xml, cookies, tokenNow)
            success = r3.first in 200..399 && r3.second.contains("<response>OK</response>", true)
        }

        if (success) """{"ok":true}""" else """{"ok":false,"error":"API reject"}"""
    } catch (e: Exception) {
        """{"ok":false,"error":"${e.message}"}"""
    }
}

// ترجع SSID و RSSI عبر الواجهة
@JavascriptInterface
fun getHuaweiWifiInfo(gw: String, user: String, pass: String): String {
    return try {
        val (okLogin, st) = hilinkLogin(gw, user, pass)
        if (!okLogin || st == null) return """{"ok":false,"error":"Login failed"}"""
        val cookies = "SessionID=${st.sessionId}"
        val (ssid, rssi) = hilinkGetWifiInfo(gw, cookies)
        """{"ok":true,"ssid":"${ssid ?: ""}","rssi":"${rssi ?: ""}"}"""
    } catch (e: Exception) {
        """{"ok":false,"error":"${e.message}"}"""
    }
}
