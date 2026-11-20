# FocusFlow — مؤقّت تركيز (Kotlin + Jetpack Compose)

تطبيق أندرويد كامل: مؤقّت تركيز/استراحة، أصوات هادئة، إحصائيات محلية، إنجازات، وضع ليلي، عربي + إنجليزي (RTL).
**لا يطلب إنترنت إطلاقًا ولا يجمع أي بيانات** — وهذا يجعل نموذج Data Safety في جوجل بلاي بسيطًا جدًا.

## 1) التشغيل (Android Studio)
1. ثبّت Android Studio (آخر إصدار) ثم `File > Open` واختر مجلد المشروع. انتظر انتهاء Gradle Sync.
2. وصّل هاتفك (USB debugging) أو شغّل محاكيًا ثم اضغط ▶ Run.
3. إذا ظهرت رسالة لتحديث إصدارات Gradle/المكتبات فوافق عليها.

> لا يوجد جهاز كمبيوتر؟ ارفع المشروع إلى GitHub؛ الملف `.github/workflows/build.yml` يبني لك APK للتجربة تلقائيًا (Actions > Build > Artifacts).

## 2) قبل النشر على Google Play (مهم)
- **غيّر `applicationId`** في `app/build.gradle.kts` إلى معرّف فريد لك (مثل `com.اسمك.focusflow`). لا يمكن تغييره بعد أول رفع. (لا تغيّر `namespace` ولا أسماء المجلدات).
- غيّر رابط سياسة الخصوصية في `app/src/main/res/values/config.xml` بعد نشر الملف `docs/privacy-policy.html` على رابط عام (مثلًا GitHub Pages).
- غيّر اسم التطبيق في `res/values/strings.xml` و`res/values-ar/strings.xml` إن أردت.
- ابنِ الحزمة الموقّعة: `Build > Generate Signed App Bundle` ثم اختر **Android App Bundle (.aab)**. احتفظ بملف المفتاح وكلمات السر في مكان آمن.
- اتبع `store_listing/play-console-guide.md` (خطوة بخطوة: Data safety، الإعلانات، التصنيف العمري، إعلانات الصلاحيات...).
- الأيقونة (512×512) وصورة الميزة (1024×500) جاهزتان في `store_listing/`. **لقطات الشاشة** (2 على الأقل) التقطها من هاتفك — لا تستخدم صورًا مصمّمة لأن جوجل تشترط أن تمثّل التطبيق فعليًا.

## 3) ما الذي يجعله متوافقًا مع قواعد جوجل بلاي
| المتطلب | في المشروع |
|---|---|
| استهداف أحدث API (36 / Android 16) | `targetSdk = 36`, `compileSdk = 36` |
| صيغة AAB | `bundleRelease` |
| لا صلاحيات حسّاسة | لا موقع/جهات اتصال/ملفات/ميكروفون/إنترنت |
| الإشعارات | صلاحية `POST_NOTIFICATIONS` تُطلب عند أول بدء جلسة (في سياقها) |
| دقة التنبيه | `SCHEDULE_EXACT_ALARM` مع شاشة إذن وبديل تلقائي إن رُفض (يحتاج **إعلانًا** في Play Console) |
| الصوت في الخلفية | خدمة أمامية من نوع `mediaPlayback` فقط عند تشغيل الصوت (تحتاج **إعلانًا + فيديو قصير**) |
| الخصوصية | بيانات محلية فقط + زر «حذف كل بياناتي» + سياسة خصوصية جاهزة |
| تنبيهات «لا تشتّت» | تعمل بدون قراءة التطبيقات الأخرى (لا Usage Access ولا Accessibility) لتجنب سياسات التقييد |
| الصوت | يُولَّد بالكود (لا ملفات صوتية = لا مشاكل حقوق) |
| 16 KB page size | الكود Kotlin فقط. إن ظهر تحذير في Play Console حدّث `compose-bom` لآخر إصدار |

## 4) هيكل المشروع
```
app/src/main/java/app/focusflow/
  MainActivity.kt          نقطة الدخول
  data/                    Models, Prefs (إعدادات + حالة المؤقّت), SessionDb (SQLite), Stats (إحصائيات + إنجازات)
  timer/                   TimerController (المنطق), Alarms, Notifier, Receivers
  sound/                   AmbientService (خدمة الصوت), NoiseGen (توليد الأصوات)
  ui/                      الشاشات الأربع + الثيم + ViewModel
app/src/main/res/          النصوص (EN/AR), الأيقونات (Adaptive + Monochrome), قواعد النسخ الاحتياطي
```

## 5) تخصيص سريع
- المدد الافتراضية: `data/Models.kt` (`AppSettings`).
- الألوان: `ui/Theme.kt`. الإنجازات: `data/Stats.kt` + نصوصها في `strings.xml`.
- إضافة صوت جديد: أضف عنصرًا في `enum Sound` ثم حالته في `sound/NoiseGen.kt`.

## ملاحظات صريحة
- بُني هذا المشروع دون تشغيل Android SDK، فلم يُجمَّع ولم يُجرَّب على جهاز. فحصتُ آليًا الموارد والترجمات والاستيرادات، لكن قد تظهر عند أول Sync/Build أخطاء صغيرة؛ الصق نص الخطأ وسيُصلَح.
- تأكد من متطلبات جوجل الحالية في Play Console قبل الإرسال، فهي تتغيّر.

## 6) البناء على Codemagic (APK)
1. ارفع المشروع إلى GitHub/GitLab/Bitbucket ثم أضفه في Codemagic واختر **Configuration: codemagic.yaml**.
2. شغّل الـ workflow باسم `FocusFlow - Debug APK`؛ ستجد ملف `app-debug.apk` في Artifacts.
3. ملف `gradle-wrapper.jar` (ملف ثنائي رسمي من Gradle) غير مضمَّن لأنه يُنزَّل من الإنترنت؛ يولّده `codemagic.yaml` تلقائيًا عند غيابه.
