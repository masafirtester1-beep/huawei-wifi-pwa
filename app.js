const $ = (sel)=>document.querySelector(sel);

function makeBookmarkletCode({user, pass, ssid, wpass}){
  // JS injected into router page.
  const payload = `(function(){
    try{
      const u='${user.replace(/'/g,"\\'")}';
      const p='${pass.replace(/'/g,"\\'")}';
      const s='${ssid.replace(/'/g,"\\'")}';
      const w='${wpass.replace(/'/g,"\\'")}';

      function fill(sel,val){ try{ const el=document.querySelector(sel); if(el && val){ el.value=val; return true;} }catch(_){} return false; }
      function clickAny(...sels){ for(const s of sels){ const el=document.querySelector(s); if(el){ el.click(); return true; } } return false; }

      // Try common Huawei login selectors
      const logged = (document.querySelector('#logoutbtn') || document.querySelector('[data-logout]'));
      if(!logged){
        const ok = fill('#userName', u) | fill('input[name=Username]', u) | fill('#admin_name', u) | fill('input[name=UserName]', u);
        const ok2 = fill('#pcPassword', p) | fill('input[name=Password]', p) | fill('#admin_pwd', p);
        clickAny('#loginBtn','button[type=submit]','#loginSubmit','input[type=submit]');
      }

      // Try Wi-Fi forms if available on current page
      const ssidOK = fill('#ssid', s) | fill('input[name=ssid]', s) | fill('input[name=SSID]', s);
      const passOK = fill('#wpa_psk', w) | fill('input[name=wlanKey]', w) | fill('input[name=KeyPassphrase]', w);
      if(ssidOK || passOK){
        clickAny('#apply','button[name=apply]','button[type=submit]');
      }

      alert('تم تشغيل Bookmarklet: إذا لم يتغير شيء، انتقل يدويًا إلى صفحة إعدادات Wi‑Fi ثم أعد تشغيله.');
    }catch(e){ alert('خطأ في التشغيل: '+e.message); }
  })();`;

  return "javascript:" + encodeURIComponent(payload);
}

function updateLink(){
  const ip = $("#ip").value.trim() || "192.168.8.1";
  $("#adminLink").textContent = `http://${ip}/`;
  $("#adminLink").href = `http://${ip}/`;
}

$("#btnGen").addEventListener("click", ()=>{
  const data = {
    user: $("#adminUser").value.trim(),
    pass: $("#adminPass").value,
    ssid: $("#ssid").value.trim(),
    wpass: $("#wifiPass").value
  };
  const url = makeBookmarkletCode(data);
  const a = document.createElement("a");
  a.textContent = "اضغط مع الاستمرار ثم اختر: إضافة كإشارة مرجعية (Bookmark)";
  a.href = url;
  a.className = "btn";
  const wrap = $("#bmWrap");
  wrap.innerHTML = "";
  wrap.appendChild(a);
});

$("#ip").addEventListener("input", updateLink);
updateLink();
