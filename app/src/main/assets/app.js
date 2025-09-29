document.addEventListener('DOMContentLoaded', () => {
  const gatewayInput = document.getElementById('gateway');
  const checkBtn = document.getElementById('checkBtn');
  const devicesBtn = document.getElementById('devicesBtn');
  const routerInfo = document.getElementById('routerInfo');
  const rssiSpan = document.getElementById('rssi');
  const ssidSpan = document.getElementById('ssid');
  const changeBtn = document.getElementById('changePassBtn');
  const changeResult = document.getElementById('changeResult');
  const saveAdminBtn = document.getElementById('saveAdminBtn');

  // حفظ بيانات المدير محلياً (localStorage)
  saveAdminBtn.addEventListener('click', () => {
    const adminUser = document.getElementById('adminUser').value;
    const adminPass = document.getElementById('adminPass').value;
    localStorage.setItem('adminUser', adminUser);
    localStorage.setItem('adminPass', adminPass);
    changeResult.textContent = 'تم حفظ بيانات المدير محلياً';
  });

  // استرجاع المحفوظ إذا متوفر
  const savedUser = localStorage.getItem('adminUser');
  const savedPass = localStorage.getItem('adminPass');
  if (savedUser) document.getElementById('adminUser').value = savedUser;
  if (savedPass) document.getElementById('adminPass').value = savedPass;

  async function tryFetchGateway() {
    const gw = gatewayInput.value.trim();
    if (!gw) return routerInfo.textContent = 'ضع عنوان الراوتر أولا';

    routerInfo.textContent = 'جارٍ الاتصال...';
    try {
      // تجربة طلب إلى الصفحة الرئيسية — mode no-cors قد يمنع قراءة المحتوى، لكن سنعلم بنجاح الوصول
      const res = await fetch(`http://${gw}/`, { mode: 'no-cors', credentials: 'include' });
      routerInfo.textContent = 'تم الاتصال (قد يكون الجواب مخفياً بسبب CORS).';
    } catch (err) {
      routerInfo.textContent = 'خطأ في الاتصال: ' + err.message;
    }
  }

  checkBtn.addEventListener('click', tryFetchGateway);

  devicesBtn.addEventListener('click', async () => {
    const gw = gatewayInput.value.trim();
    if (!gw) return routerInfo.textContent = 'أدخل عنوان الراوتر أولاً';
    // مثال لمسار قد يختلف حسب الراوتر
    const endpoint = `http://${gw}/api/device_list`;
    try {
      const res = await fetch(endpoint, { mode:'no-cors', credentials: 'include' });
      routerInfo.textContent = 'تم طلب قائمة الأجهزة — قد تحتاج تعديل المسار حسب الراوتر.';
    } catch (e) {
      routerInfo.textContent = 'فشل طلب الأجهزة — ' + e.message;
    }
  });

  // --- RSSI & SSID retrieval (via Android WebView interface) ---
  function updateNativeWifiInfo() {
    if (window.Android && typeof window.Android.getWifiInfo === 'function') {
      try {
        const json = window.Android.getWifiInfo(); // JSON string
        const info = JSON.parse(json);
        if (info.ok) {
          const ssid = info.ssid && info.ssid !== "null" ? info.ssid : 'غير متاح';
          const rssi = (info.rssi && info.rssi !== null) ? (info.rssi + ' dBm') : 'غير متاح';
          ssidSpan.textContent = ssid;
          rssiSpan.textContent = rssi;
          routerInfo.textContent = '';
          // إذا location غير مفعّل نعرض تعليمات
          if (info.locationEnabled === false) {
            routerInfo.textContent = 'خاصك تفعّل الموقع باش يظهر SSID على بعض الأجهزة. اضغط هنا لفتح الإعدادات.';
            routerInfo.style.textDecoration = 'underline';
            routerInfo.onclick = () => {
              if (window.Android && window.Android.requestOpenLocationSettings) {
                window.Android.requestOpenLocationSettings();
              }
            };
          }
        } else {
          ssidSpan.textContent = 'خطأ';
          rssiSpan.textContent = 'خطأ';
          routerInfo.textContent = 'خطأ في جلب معلومات الـ WiFi: ' + (info.error || '');
        }
      } catch (e) {
        ssidSpan.textContent = 'خطأ قراءة المعلومات';
        rssiSpan.textContent = 'خطأ';
        routerInfo.textContent = 'خطأ: ' + e.message;
      }
    } else {
      ssidSpan.textContent = 'غير متاح';
      rssiSpan.textContent = 'غير متاح';
      routerInfo.textContent = 'الميزة Native غير متاحة (ليس داخل WebView أو لم تُفعّل الواجهة)';
    }
  }

  // استدعاء على التحميل
  updateNativeWifiInfo();

  // تغيير كلمة السر — مثال عامّي: مسار إفتراضي
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

    const endpoint = `http://${gw}/api/change_password`; // **غير هذا للمسار الصحيح**
    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ adminUser, adminPass, newPass }),
        credentials: 'include',
        mode: 'no-cors'
      });
      changeResult.textContent = 'تم إرسال الطلب. راجع صفحة الراوتر للتأكد (CORS قد يمنع قراءة الاستجابة).';
    } catch (err) {
      changeResult.textContent = 'خطأ أثناء الإرسال: ' + err.message;
    }
  });

});
