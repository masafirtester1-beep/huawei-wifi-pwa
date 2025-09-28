const CACHE = 'huawei-pwa-v1';
const ASSETS = ['/', '/index.html', '/styles.css', '/app.js', '/icons/icon.svg', '/manifest.webmanifest'];

self.addEventListener('install', (e)=>{
  e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)));
  self.skipWaiting();
});

self.addEventListener('activate', (e)=>{
  e.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (e)=>{
  const url = new URL(e.request.url);
  if (ASSETS.includes(url.pathname)) {
    e.respondWith(caches.match(e.request).then(res=>res || fetch(e.request)));
  }
});
