Huawei Wi‑Fi Helper - Android (Enhanced)
======================================

هذا مشروع Android بسيط (Kotlin) يحتوي على:
- Activity رئيسية مع WebView تعرض صفحة الراوتر.
- حقن JavaScript مخصص يملأ حقول تسجيل الدخول وWi‑Fi ويضغط زر الحفظ (حسب selectors شائعة).
- تخزين مشفَّر للاعتماديات باستخدام EncryptedSharedPreferences.
- واجهة لإدخال IP، admin user/pass، SSID وPassword جديدة.

كيفية الإستعمال:
1. افتح المشروع فـ Android Studio (يفضل استخدام Android Studio Flamingo أو أحدث).
2. سيقوم Android Studio بتنزيل التبعيات. إذا طلب Gradle wrapper، استخدم `File > Sync Project` ويضبط نفسه.
3. شغّل على جهاز Android حقيقي أو emulator. بعد التشغيل، أدخل IP الراوتر (مثال: 192.168.8.1) ثم اضغط "فتح".
4. بعد تحميل صفحة الراوتر في WebView، عدّل الحقول في الأعلى ثم اضغط "حقن التغييرات" -> Confirm.
5. WebView سيحقن JS داخل الصفحة ويحاول ملء الحقول والضغط على Apply/Save.

أمان وملاحظات:
- احرص أن تستخدم التطبيق فقط على الراوترات اللي عندك أو عندك إذن للتعديل.
- قد تحتاج لتعديل الـselectors في MainActivity.buildInjectionScript لتتناسب مع موديل الراوتر الخاص بك.
- لا أقوم ببناء APK هنا؛ ستحتاج Android Studio لبناء APK أو تشغيله.
