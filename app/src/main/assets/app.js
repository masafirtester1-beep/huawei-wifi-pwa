document.addEventListener('DOMContentLoaded', () => {
  const $ = (id) => document.getElementById(id);

  const gatewayInput = $('gateway');
  const checkBtn = $('checkBtn');
  const devicesBtn = $('devicesBtn');
  const routerInfo = $('routerInfo');
  const rssiSpan = $('rssi');
  const ssidSpan = $('ssid');
  const changeBtn = $('changePassBtn');
  const changeResult = $('changeResult');
  const permBtn = $('permBtn'); // (اختياري) زر طلب الصلاحيات إن كان موجود

  // ========= اتصال الراوتر (اختباري فقط من المتصفح) =========
  async function tryFetchGateway() {
    const gw = (gatewayInput.value || '').trim();
    if (!gw) {
      routerInfo.textContent = 'ضع عنوان الراوتر أولا';
      return;
    }
    routerInfo.textContent = 'جارٍ الاتصال...';
    try {
      await fetch(`http://${gw}/`, { mode: 'no-cors' });
      routerInfo.textContent = 'تم الوصول (قد لا تظهر الاستجابة بسبب CORS).';
    } catch (err) {
      routerInfo.textContent = 'خطأ في الاتصال: ' + err.message;
    }
  }
  checkBtn?.addEventListener('click', tryFetchGateway);

  devicesBtn?.addEventListener('click', async () => {
    const gw = (gatewayInput.value || '').trim();
    if (!gw) return (routerInfo.textContent = 'أدخل عنوان الراوتر أولاً');
    try {
      await fetch(`http://${gw}/device_list`, { mode: 'no-cors' });
      routerInfo.textContent = 'طُلِبت قائمة الأجهزة — قد تحتاج تعديل المسار حسب الراوتر.';
    } catch (e) {
      routerInfo.textContent = 'فشل طلب الأجهزة — ' + e.message;
    }
  });

  // ========= SSID / RSSI من واجهة أندرويد =========
  function formatSsid(raw) {
    if (!raw) return 'غير متصل';
    // بعض الأجهزة ترجّع "SSID" بعلامات اقتباس
    if (raw.startsWith('"') && raw.endsWith('"')) {
      return raw.slice(1, -1);
    }
    return raw;
  }

  function formatRssi(val) {
    if (typeof val !== 'number') return 'غير معروف';
    return `${val} dBm`;
  }

  function updateNativeWifiInfo(showFallback = true) {
    if (window.Android && typeof window.Android.getWifiInfo === 'function') {
      try {
        const info = JSON.parse(window.Android.getWifiInfo());

        ssidSpan.textContent = formatSsid(info.ssid);
        rssiSpan.textContent = formatRssi(info.rssi);

        if (info.error) {
          console.log('Wifi error:', info.error);
        }
        return;
      } catch (e) {
        console.log('Parse error:', e);
        ssidSpan.textContent = 'خطأ قراءة المعلومات';
        rssiSpan.textContent = 'خطأ قراءة المعلومات';
        return;
      }
    }

    // خارج WebView (اختبار على المتصفح)
    if (showFallback) {
      ssidSpan.textContent = 'غير متاح (خارج WebView)';
      rssiSpan.textContent = 'غير متاح';
    }
  }

  // طلب صلاحيات من التطبيق (اختياري)
  function requestPermissions() {
    if (window.Android && typeof window.Android.requestPermissions === 'function') {
      window.Android.requestPermissions();
    } else {
      alert('متاح فقط داخل التطبيق.');
    }
  }
  permBtn?.addEventListener('click', requestPermissions);

  // تحديث فوري ثم دوري
  updateNativeWifiInfo();
  let wifiTimer = setInterval(updateNativeWifiInfo, 5000);

  // إن بغيت توقف التحديث الدوري من JS آخر: clearInterval(wifiTimer);

  // ========= تغيير كلمة سر الواي فاي (مسار افتراضي – عدّل حسب الراوتر) =========
  changeBtn?.addEventListener('click', async () => {
    const gw = (gatewayInput.value || '').trim();
    const newPass = ($('newPass')?.value || '').trim();
    const adminUser = ($('adminUser')?.value || '').trim();
    const adminPass = ($('adminPass')?.value || '').trim();

    if (!gw || !newPass || !adminUser) {
      changeResult.textContent = 'عَمِّر جميع الحقول المطلوبة';
      return;
    }

    changeResult.textContent = 'جارٍ إرسال الطلب...';

    const endpoint = `http://${gw}/api/change_password`; // عدّل حسب API الراوتر

    try {
      await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ adminUser, adminPass, newPass }),
        credentials: 'include',
        mode: 'no-cors'
      });
      changeResult.textContent =
        'تم إرسال الطلب. إذا ما بان جواب، راه CORS ممكن حاجبو — تأكد مباشرة من صفحة الراوتر.';
    } catch (err) {
      changeResult.textContent = 'خطأ أثناء الإرسال: ' + err.message;
    }
  });

});
