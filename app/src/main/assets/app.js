// جلب SSID/RSSI عبر HiLink (أضمن من read-only Android)
async function updateNativeWifiInfo() {
  const gw = document.getElementById('gateway').value.trim();
  const adminUser = document.getElementById('adminUser').value.trim() || 'admin';
  const adminPass = document.getElementById('adminPass').value || '';

  if (window.Android && Android.getHuaweiWifiInfo && gw && adminUser) {
    try {
      const raw = Android.getHuaweiWifiInfo(gw, adminUser, adminPass);
      const data = JSON.parse(raw);
      if (data.ok) {
        document.getElementById('ssid').textContent = data.ssid || '—';
        document.getElementById('rssi').textContent = data.rssi || '—';
      } else {
        document.getElementById('ssid').textContent = 'خطأ';
        document.getElementById('rssi').textContent = 'خطأ';
      }
    } catch {
      document.getElementById('ssid').textContent = 'خطأ';
      document.getElementById('rssi').textContent = 'خطأ';
    }
  }
}

// عند الضغط على تغيير كلمة السر
changeBtn.addEventListener('click', () => {
  const gw = gatewayInput.value.trim();
  const newPass = document.getElementById('newPass').value.trim();
  const adminUser = document.getElementById('adminUser').value.trim();
  const adminPass = document.getElementById('adminPass').value;

  if (!gw || !newPass || !adminUser) {
    changeResult.textContent = 'عَمِّر جميع الحقول';
    return;
  }

  if (window.Android && Android.changeWifiPasswordHuawei) {
    const raw = Android.changeWifiPasswordHuawei(gw, adminUser, adminPass, newPass);
    try {
      const data = JSON.parse(raw);
      changeResult.textContent = data.ok
        ? 'تبدّلات بنجاح ✅ (قد يتطلب إعادة اتصال الواي-فاي).'
        : 'فشل: ' + (data.error || 'غير معروف');
    } catch {
      changeResult.textContent = 'رد غير متوقع.';
    }
  } else {
    changeResult.textContent = 'الميزة Native غير متاحة في هاد البناء.';
  }
});
