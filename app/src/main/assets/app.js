// app.js

document.addEventListener('DOMContentLoaded', () => {
  const gatewayInput = document.getElementById('gateway');
  const checkBtn = document.getElementById('checkBtn');
  const devicesBtn = document.getElementById('devicesBtn');
  const routerInfo = document.getElementById('routerInfo');
  const rssiSpan = document.getElementById('rssi');
  const ssidSpan = document.getElementById('ssid');
  const changeBtn = document.getElementById('changePassBtn');
  const changeResult = document.getElementById('changeResult');

  async function tryFetchGateway() {
    const gw = gatewayInput.value.trim();
    if (!gw) return routerInfo.textContent = 'ضع عنوان الراوتر أولا';

    routerInfo.textContent = 'جارٍ الاتصال...';
    try {
      // محاولة بسيطة لقراءة صفحة الراوتر
      const res = await fetch(`http://${gw}/`, { mode: 'no-cors' });
      // ملاحظة: مع كثير من الراوترات، fetch مع no-cors لن يقدّم تفاصيل (opaque)
      // سنعرض نجاح/فشل بناءً على الوصول فقط:
      routerInfo.textContent = 'تم الاتصال (قد يكون الجواب مخفياً بسبب CORS).';
    } catch (err) {
      routerInfo.textContent = 'خطأ في الاتصال: ' + err.message;
    }
  }

  checkBtn.addEventListener('click', tryFetchGateway);

  devicesBtn.addEventListener('click', async () => {
    // محاولة بسيطة: قد يكون الراوتر عنده صفحة أجهزة (مثال) — هذا مجرد مثال عامّي
    const gw = gatewayInput.value.trim();
    if (!gw) return routerInfo.textContent = 'أدخل عنوان الراوتر أولاً';
    try {
      const res = await fetch(`http://${gw}/device_list`, { mode: 'no-cors' });
      routerInfo.textContent = 'تم طلب قائمة الأجهزة — قد تحتاج تعديل المسار حسب الراوتر.';
    } catch (e) {
      routerInfo.textContent = 'فشل طلب الأجهزة — ' + e.message;
    }
  });

  // --- RSSI & SSID retrieval (via Android WebView interface) ---
  function updateNativeWifiInfo() {
    // If running inside Android WebView and JavaScript interface added as "Android"
    if (window.Android && typeof window.Android.getWifiInfo === 'function') {
      try {
        const json = window.Android.getWifiInfo(); // should return JSON string
        const info = JSON.parse(json);
        rssiSpan.textContent = info.rssi ?? 'N/A';
        ssidSpan.textContent = info.ssid ?? 'N/A';
      } catch (e) {
        rssiSpan.textContent = 'خطأ قراءة المعلومات';
      }
    } else {
      rssiSpan.textContent = 'غير متاح — ليس داخل WebView أو لم تُفعّل الواجهة';
      ssidSpan.textContent = 'غير متاح';
    }
  }

  // استدعاء على التحميل
  updateNativeWifiInfo();

  // تغيير كلمة السر — مثال عامّي: نرسل POST إلى endpoint (خاص بالراوتر)
  changeBtn.addEventListener('click', async () => {
    const gw = gatewayInput.value.trim();
    const newPass = document.getElementById('newPass').value;
    const adminUser = document.getElementById('adminUser').value;
    const adminPass = document.getElementById('adminPass').value;

    if (!gw || !newPass || !adminUser) {
      changeResult.textContent = 'عمر جميع الحقول المطلوبة';
      return;
    }

    changeResult.textContent = 'جارٍ إرسال الطلب...';

    // **ملاحظة مهمة**: المسار /api/change_password مجرد مثال. خاصك تعرف API الراوتر الحقيقي.
    const endpoint = `http://${gw}/api/change_password`;

    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        // قد يحتاج headers مخصصة أو body (form) حسب الراوتر
        headers: {
          'Content-Type': 'application/json',
          // يمكن إضافة Authorization header إن لزم
        },
        body: JSON.stringify({
          adminUser,
          adminPass,
          newPass
        }),
        credentials: 'include',
        mode: 'no-cors' // غالبا سيفشل إن لم تكن واجهة الراوتر تدعم CORS
      });
      changeResult.textContent = 'تم إرسال الطلب. راجع صفحة الراوتر للتأكد (CORS ممكن يمنع الاستجابة من الويب).';
    } catch (err) {
      changeResult.textContent = 'خطأ أثناء الإرسال: ' + err.message;
    }
  });

}); // DOMContentLoaded
