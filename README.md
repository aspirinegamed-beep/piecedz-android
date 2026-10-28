# PièceDZ Android + API starter (v0.2)

مشروع MVP أولي لمنصة قطع الغيار الجزائرية. يحتوي تطبيق Android بسيطًا وخادم API أوليًا وقاعدة بيانات PostgreSQL.

## Android
- واجهة عربية RTL.
- تصنيفات السيارات، الشاحنات، الدراجات النارية، الدراجات الهوائية، والهيدروليك.
- بحث وإضافة عروض محلية والاتصال بالبائع ومشاركة العرض.
- **تنبيه:** تطبيق Android الحالي لا يتصل بالخادم بعد؛ العروض في التطبيق محفوظة محليًا على الهاتف. لا تعتبره سوقًا وطنية مشتركة حتى يتم ربط التطبيق بـ API واختبار الاتصال.

## API
الخادم في `server/` يوفر:
- تسجيل/دخول بكلمة مرور مشفّرة بـ bcrypt.
- JWT للمصادقة.
- بحث في العروض المعتمدة فقط.
- نشر عروض جديدة بحالة `pending`.
- طلبات شراء، بلاغات، ولوحة إدارة API للموافقة على العروض.
- PostgreSQL schema وقيود مدخلات ومحددات معدل.

## بناء APK
Workflow موجود في `.github/workflows/android.yml`:
1. ارفع المستودع إلى GitHub.
2. افتح Actions وشغّل `Build PièceDZ Android APK`.
3. نزّل artifact باسم `piecedz-debug-apk`.
4. ملف Debug APK للتجربة، وليس إصدارًا موقّعًا للنشر على Google Play.

## Termux: رفع المستودع
```bash
pkg update -y
pkg install -y git gh unzip
termux-setup-storage
mkdir -p ~/PieceDZ
unzip -o ~/storage/shared/Download/PieceDZ-Android-MVP.zip -d ~/
cd ~/PieceDZ
git init -b main
git add .
git commit -m "PièceDZ v0.2"
gh auth status || gh auth login
gh repo create piecedz-android --public --source=. --remote=origin --push
```
إذا المستودع موجود، استخدم:
```bash
git remote remove origin 2>/dev/null || true
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/piecedz-android.git
git branch -M main
git push -u origin main
```

## الخطوة التي تلي ذلك
ربط تطبيق Android بـ API عبر HTTPS، ثم إضافة تسجيل الدخول، قائمة العروض القادمة من الخادم، ونموذج نشر عرض مصادق عليه. لا تضع بيانات اتصال قاعدة البيانات أو `JWT_SECRET` داخل APK.
