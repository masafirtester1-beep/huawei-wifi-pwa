// ✅ تحميل بيانات المدير من التخزين
window.onload = function () {
  if (window.Android && Android.getCreds) {
    let creds = Android.getCreds();
    try {
      let parsed = JSON.parse(creds);
      document.getElementById("adminUser").value = parsed.user || "admin";
      document.getElementById("adminPass").value = parsed.pass || "";
    } catch (e) {
      document.getElementById("adminUser").value = "admin";
    }
  }
  updateWifiInfo();
};

// ✅ تحديث معلومات الشبكة (SSID, RSSI)
function updateWifiInfo() {
  if (window.Android && Android.getWifiInfo) {
    let info = Android.getWifiInfo();
    try {
      let parsed = JSON.parse(info);
      document.getElementById("ssid").innerText = parsed.ssid || "غير متاح";
      document.getElementById("rssi").innerText = parsed.rssi || "غير متاح";
    } catch (e) {
      document.getElementById("ssid").innerText = "خطأ";
      document.getElementById("rssi").innerText = "خطأ";
    }
  } else {
    document.getElementById("ssid").innerText = "غير متاح (خارج التطبيق)";
    document.getElementById("rssi").innerText = "غير متاح";
  }
}

// ✅ فحص الاتصال بالراوتر
function checkConnection() {
  document.getElementById("statusMsg").innerText = "🔍 جاري الفحص...";
  // Placeholder (يمكنك تعديل المسار حسب الراوتر)
  fetch("http://192.168.1.1")
    .then(r => {
      document.getElementById("statusMsg").innerText = "📶 الراوتر متاح";
    })
    .catch(e => {
      document.getElementById("statusMsg").innerText = "❌ تعذر الاتصال بالراوتر";
    });
}

// ✅ طلب عرض الأجهزة (عام)
function getDevices() {
  document.getElementById("statusMsg").innerText = "💻 طلب قائمة الأجهزة...";
  // Placeholder فقط – المسار يختلف حسب الراوتر
}

// ✅ طلب صلاحيات
function requestPermissions() {
  if (window.Android && Android.requestPermissions) {
    Android.requestPermissions();
  } else {
    alert("الميزة Native غير متاحة في هذا البناء.");
  }
}

// ✅ حفظ بيانات المدير
function saveCreds() {
  let user = document.getElementById("adminUser").value;
  let pass = document.getElementById("adminPass").value;
  if (window.Android && Android.saveCreds) {
    Android.saveCreds(user, pass);
    document.getElementById("statusMsg").innerText = "💾 تم حفظ البيانات";
  } else {
    document.getElementById("statusMsg").innerText = "❌ الميزة Native غير متاحة";
  }
}

// ✅ تغيير كلمة السر
function changePassword() {
  let newPass = document.getElementById("newWifiPass").value;
  let user = document.getElementById("adminUser").value;
  let pass = document.getElementById("adminPass").value;

  if (window.Android && Android.changePassword) {
    let result = Android.changePassword(user, pass, newPass);
    document.getElementById("statusMsg").innerText = result;
  } else {
    document.getElementById("statusMsg").innerText = "⚠️ الميزة Native غير متاحة في هذا البناء.";
  }
}
