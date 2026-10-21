# StickerBridge (Android MVP)

واجهة أولية باللغة العربية لنظام Android باستخدام Kotlin وJetpack Compose.

## ما الذي يعمل في هذه النسخة؟
- مكتبة محلية لملفات الصور المستوردة.
- استيراد الصور عبر Android OpenDocument picker.
- معاينة الصور وحذفها.
- مشاركة الملفات عبر Android Sharesheet باستخدام FileProvider.
- واجهة عربية مع دعم RTL.

## ما الذي لا تفعله؟
- لا تسحب ملفات الستيكرات مباشرة من قاعدة بيانات TikTok.
- لا تتجاوز قيود تيك توك أو إنستغرام.
- لا ترسل الرسائل تلقائيًا ولا تسجل الدخول إلى حساباتك.
- لا تضمن أن Instagram سيقبل كل صيغة أو يعرضها كستيكر أصلي.
- ملفات GIF يمكن استيرادها ومشاركتها، لكن المعاينة في الواجهة الحالية تعرض الإطار الأول فقط.

## فتح المشروع
1. فك ضغط الملف.
2. افتح مجلد `StickerBridge` في Android Studio (إصدار يدعم Android Gradle Plugin 8.7.3).
3. ثبّت Android SDK 35 وJDK 17 عند الطلب.
4. انتظر Gradle Sync، ثم شغّل التطبيق على جهاز Android أو محاكي.

> هذا هيكل MVP قابل للتوسعة، وليس إصدارًا منشورًا أو APK جاهزًا. لم يتم تشغيل بناء Gradle داخل بيئة إنشاء الملفات هذه.


## Build APK without Android Studio (GitHub Actions)
1. Create a GitHub repository in your browser.
2. Upload the contents of this project to the repository (the `.github/workflows/android.yml` file must be included).
3. Open the repository's **Actions** tab and select **Build StickerBridge APK**.
4. Choose **Run workflow** if it has not started automatically.
5. When the run completes, open it and download the artifact named `StickerBridge-debug-apk`.
6. Extract the downloaded artifact ZIP to get `app-debug.apk`, then install it on your Android phone. Android may ask you to allow installs from that browser/file manager.

This workflow builds a debug APK in GitHub's hosted environment. It has not been run or verified from this workspace.
