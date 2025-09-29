// دالة لعرض رسالة فـ output
function logMessage(msg) {
  const output = document.getElementById("output");
  output.innerHTML += `<p>${msg}</p>`;
}

// فحص الاتصال
function checkStatus() {
  const gateway = document.getElementById("gateway").value || "192.168.8.1";

  // محاكاة
  logMessage("🔍 جاري الفحص للراوتر: " + gateway);
  setTimeout(() => {
    logMessage("✅ الراوتر متصل بالإنترنت.");
  }, 1000);
}

// عرض الأجهزة المتصلة
function listDevices() {
  // محاكاة أجهزة متصلة
  const devices = [
    { name: "هاتف سامسونج", ip: "192.168.8.10" },
    { name: "حاسوب محمول", ip: "192.168.8.15" },
    { name: "هاتف آيفون", ip: "192.168.8.20" }
  ];

  logMessage("💻 الأجهزة المتصلة:");
  devices.forEach(d => {
    logMessage(`• ${d.name} — ${d.ip}`);
  });
}
