document.getElementById("open").addEventListener("click", () => {
  const gw = document.getElementById("gw").value.trim();
  const u  = document.getElementById("user").value.trim();
  const p  = document.getElementById("pass").value.trim();

  // حاليا يفتح صفحة الراوتر في المتصفح
  const url = gw || "http://192.168.8.1";
  window.location.href = url;
});
