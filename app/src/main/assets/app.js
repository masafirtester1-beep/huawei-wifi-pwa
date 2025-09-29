
window.App = (function () {
  const ssidEl = document.getElementById('ssid');
  const rssiEl = document.getElementById('rssi');

  let timer = null;

  function setInfo(ssid, rssi) {
    ssidEl.textContent = ssid ?? 'غير متاح';
    rssiEl.textContent = (rssi ?? 'غير متاح');
  }

  function readOnce() {
    try {
      if (window.Android && typeof Android.getWifiInfo === 'function') {
        const info = JSON.parse(Android.getWifiInfo());
        setInfo(info.ssid, info.rssi);

        const missing = !info.ssid || info.ssid === 'N/A' ||
                        !info.rssi || info.rssi === 'N/A';
        // إذا ناقص صلاحيات/الموقع طافي، نطلب الصلاحيات تلقائياً
        if (missing && window.Android && Android.askPermissions) {
          Android.askPermissions();
        }
      } else {
        setInfo('خارج WebView', 'خارج WebView');
      }
    } catch (e) {
      setInfo('خطأ', 'خطأ');
      console.log('readOnce error:', e);
    }
  }

  function startAutoRefresh() {
    // قراءة فورية
    readOnce();
    // ونعيد كل ثانيتين حتى تظهر قيم حقيقية
    if (timer) clearInterval(timer);
    timer = setInterval(() => {
      // إذا ظهر SSID حقيقي و RSSI، ممكن توقف المؤقت
      const done = ssidEl.textContent !== 'غير متاح' &&
                   ssidEl.textContent !== 'N/A' &&
                   rssiEl.textContent !== 'غير متاح' &&
                   rssiEl.textContent !== 'N/A';
      if (!done) readOnce();
      // لو تحب تخلّيه دائمًا يحدث، علّق الشرط أعلاه
    }, 2000);
  }

  return {
    startAutoRefresh,
    readOnce
  };
})();
