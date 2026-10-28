# PièceDZ — منصة قطع الغيار الجزائرية

نسخة MVP أولية قابلة للتجربة على Android، بواجهة عربية، مع بحث محلي وإضافة عروض تجريبية والاتصال بالبائع. يوجد مخطط API وخادم اختياري كبداية للنسخة متعددة المستخدمين.

## ما يعمل في نسخة Android الحالية
- واجهة عربية RTL.
- أقسام: السيارات، الشاحنات، الدراجات النارية، الدراجات الهوائية، والهيدروليك.
- البحث في العروض المحلية.
- إضافة عرض قطعة إلى تخزين الهاتف.
- الاتصال بالبائع عبر الهاتف.
- مشاركة بيانات العرض.
- بيانات تجريبية واضحة يمكن حذفها.

## تنبيه مهم
هذه النسخة الأولى تخزّن العروض على الجهاز نفسه. ليست بعدُ سوقًا وطنية مشتركة بين كل المستخدمين. لكي يرى جميع المستخدمين نفس العروض، يجب نشر API وقاعدة بيانات على خادم متاح عبر الإنترنت، ثم ضبط رابط API داخل التطبيق. لا تضع كلمات مرور قاعدة البيانات أو مفاتيح الإدارة داخل APK.

## البناء
المسار الأسهل: ارفع المشروع إلى مستودع GitHub وشغّل workflow الموجود في `.github/workflows/android.yml`. ينتج ملف APK Debug للتجربة.

في Termux:
```bash
pkg update -y
pkg install -y git gh unzip
termux-setup-storage
mkdir -p ~/PieceDZ
# ضع ملفات المشروع داخل ~/PieceDZ أو فك ملف ZIP هناك
cd ~/PieceDZ
git init
git add .
git commit -m "Initial PièceDZ Android MVP"
gh repo create piecedz-android --public --source=. --remote=origin --push
```
إذا كان المستودع موجودًا مسبقًا، لا تنفذ `gh repo create`؛ استخدم `git remote add origin URL` ثم `git push -u origin main`.

بعد رفع الملفات:
```bash
gh workflow run android.yml
gh run list --limit 5
```
إذا كان أول تشغيل يتطلب دفعًا تلقائيًا، افتح صفحة Actions في GitHub ثم شغّل workflow. بعد نجاح البناء:
```bash
gh run list --limit 5
gh run download RUN_ID --name piecedz-debug-apk --dir ~/storage/shared/Download/PieceDZ
```
استبدل `RUN_ID` برقم التشغيل الناجح.

## الخادم الاختياري
مجلد `server/` يحتوي API تجريبيًا باستخدام Node.js وPostgreSQL. راجع `server/README.md`. لا يعتبر جاهزًا للنشر العام قبل ضبط HTTPS، ومصادقة المستخدمين، ومحددات المعدل، والتحقق من المدخلات، وسياسة النسخ الاحتياطي.
