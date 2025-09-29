// Helpers
const $ = (id) => document.getElementById(id);

function logRouter(msg) {
  const el = $('routerInfo');
  if (!el) return;
  el.textContent = msg;
}

// تعبئة adminUser/adminPass تلقائياً من الجسر
function fillSavedCreds() {
  if (window.Android && Android.getSavedCreds) {
    try {
      const creds = JSON.parse(Android.getSavedCreds());
      if ($('adminUser')) $('adminUser').value = creds.user || 'admin';
      if ($('adminPass')) $('adminPass').value = creds.pass || 'admin1234';
    } catch (e) {
      console.log('تعذر قراءة بيانات الدخول:', e);
    }
  } else {
    // قيم افتراضية إذا كنت تختبر خارج التطبيق
    if ($('adminUser') && !$('adminUser').value) $('adminUser').value = 'admin';
    if ($('adminPass') && !$('adminPass').value) $('adminPass').value = 'admin1234';
  }
}

// حفظ بيانات المدير
function saveCreds() {
  const u = ($('adminUser')?.value || '').trim();
  const p = $('adminPass')?.value || '';
  if (window.Android && Android.saveCreds) {
    Android.saveCreds(u, p);
    $('changeResult').textContent = 'تم حفظ بيانات المدير ✅';
  } else {
    $('changeResult').textContent = 'حُفظت محلياً (خارج التطبيق).';
  }
}

// الاتصال بالراوتر (اختباري)
async function tryFetchGateway() {
  const gw = ($('gateway')?.value || '').trim();
  if (!gw) return logRouter('ضع عنوان الراوتر أولا');
  logRouter('جارٍ الاتصال...');
  try {
    await fetch(`http://${gw}/`, { mode: 'no-cors' });
    logRouter('تم الوصول (قد لا تظهر الاستجابة بسبب CORS).');
  } catch (err) {
    logRouter('خطأ في الاتصال: ' + err.message);
  }
}

async function tryFetchDevices() {
  const gw = ($('gateway')?.value || '').trim();
  if (!gw) return logRouter('أدخل عنوان الراوتر أولاً');
  try {
    await fetch(`http://${gw}/device_list`, { mode: 'no-cors' });
    logRouter('طُلِبت قائمة الأجهزة — قد تحتاج تعديل المسار حسب الراوتر.');
  } catch (e) {
    logRouter('فشل طلب الأجهزة — ' + e.message);
  }
}

// مظهر الـ SSID من بعض الأجهزة يرجع بين ""
function formatSsid(raw) {
  if (!raw) return 'غير متصل';
  return raw.startsWith('"') && raw.endsWith('"') ? raw.slice(1, -1) : raw;
}
function formatRssi(val) {
  if (typeof val === 'number') return `${val} dBm`;
  if (typeof val === 'string' && val.trim() !== '') return `${val} dBm`;
  return 'غير معروف';
}

// تحديث معلومات الواي-فاي من الواجهة Native (Huawei أو قراءة مباشرة)
function updateNativeWifiInfo() {
  const gw = ($('gateway')?.value || '').trim();
  const u = ($('adminUser')?.value || 'admin').trim();
  const p = $('adminPass')?.value || '';

  if (window.Android && Android.getHuaweiWifiInfo && gw && u) {
    try {
      const raw = Android.getHuaweiWifiInfo(gw, u, p);
      const data = JSON.parse(raw);
      if (data.ok) {
        $('ssid').textContent = formatSsid(data.ssid || '');
        $('rssi').textContent = formatRssi(data.rssi || '');
      } else {
        $('ssid').textContent = 'خطأ';
        $('rssi').textContent = 'خطأ';
      }
    } catch {
      $('ssid').textContent = 'خطأ';
      $('rssi').textContent = 'خطأ';
    }
    return;
  }

  // بديل: قراءة من Android.getWifiInfo (بدون تسجيل دخول)
  if (window.Android && Android.getWifiInfo) {
    try {
      const info = JSON.parse(Android.getWifiInfo());
      $('ssid').textContent = formatSsid(info.ssid);
      $('rssi').textContent = formatRssi(info.rssi);
    } catch (e) {
      $('ssid').textContent = 'غير متاح';
      $('rssi').textContent = 'غير متاح';
    }
  } else {
    $('ssid').textContent = 'غير متاح (خارج التطبيق)';
    $('rssi').textContent = 'غير متاح';
  }
}

// تغيير كلمة السر — Native (Huawei HiLink)
function changeWifiPassword() {
  const gw = ($('gateway')?.value || '').trim();
  const newPass = ($('newPass')?.value || '').trim();
  const u = ($('adminUser')?.value || '').trim();
  const p = $('adminPass')?.value || '';

  if (!gw || !newPass || !u) {
    $('changeResult').textContent = 'عَمِّر جميع الحقول المطلوبة';
    return;
  }

  // حفظ البيانات قبل الإرسال
  saveCreds();

  if (window.Android && Android.changeWifiPasswordHuawei) {
    try {
      const raw = Android.changeWifiPasswordHuawei(gw, u, p, newPass);
      const data = JSON.parse(raw);
      $('changeResult').textContent = data.ok
        ? 'تم تغيير كلمة السر ✅ (قد تحتاج إعادة الاتصال بالشبكة).'
        : 'فشل: ' + (data.error || 'غير معروف');
    } catch {
      $('changeResult').textContent = 'رد غير متوقع من الواجهة.';
    }
  } else {
    $('changeResult').textContent = 'الميزة Native غير متاحة في هذا البناء.';
  }
}

// ربط الأزرار
document.addEventListener('DOMContentLoaded', () => {
  fillSavedCreds();

  $('checkBtn')?.addEventListener('click', tryFetchGateway);
  $('devicesBtn')?.addEventListener('click', tryFetchDevices);

  $('permBtn')?.addEventListener('click', () => {
    if (window.Android && Android.requestPermissions) Android.requestPermissions();
    updateNativeWifiInfo();
  });

  $('saveCredsBtn')?.addEventListener('click', saveCreds);
  $('changePassBtn')?.addEventListener('click', changeWifiPassword);

  updateNativeWifiInfo();
  // تحديث دوري (اختياري)
  setInterval(updateNativeWifiInfo, 6000);
});
