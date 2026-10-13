# NEXVARY HifzAI | حِفْظ

منصة للحفظ الذكي للقرآن الكريم، تبدأ بمحرك تدريب متكيّف يساعد المستخدم على الاسترجاع الفعلي والمراجعة المتباعدة وربط الآيات.

> **الحالة:** المشروع في مرحلة التأسيس. لا توجد بعد نسخة إنتاجية أو تقييم تجويد معتمد.

## الفكرة

- **التعلّم:** قراءة الآيات من مصدر قرآني موثوق دون تغيير نصها.
- **التذكّر:** إخفاء كلمات في *طبقة العرض فقط*، ثم اختبار استرجاع الآية كاملة.
- **الربط:** تدريب انتقالات الآيات، وليس حفظ كل آية منعزلة.
- **المراجعة:** جدولة المراجعات بناءً على نتيجة الاسترجاع والتعثر.
- **الخصوصية:** حفظ نتائج التدريب محليًا في البداية، دون إرسال تسجيلات صوتية إلى خوادم طرف ثالث.

## الأولويات التقنية

1. Android (Kotlin + Jetpack Compose) بواجهة عربية RTL.
2. محرك حفظ ومراجعة **قابل للاختبار**، مفصول عن الواجهة وعن النص القرآني.
3. مصدر نص قرآني موثّق؛ لا يُسمح بتعديل النص الأصلي أو توليد آيات بالذكاء الاصطناعي.
4. لاحقًا: التلاوة الصوتية، التعرف على الكلام، واختبارات التسميع بإشراف مراجعة بشرية عند الشك.
5. لاحقًا: معلمون معتمدون من مصر والسعودية، ولوحة إشراف.

## سلامة النص وحقوقه

- يُعرض النص الأصلي كما ورد من المصدر المعتمد، ويُخزّن مع تعريف الإصدار.
- عند الإخفاء أو تقديم تلميح تُغيّر *طريقة العرض* فقط، وليس النص المحفوظ.
- مصدر مرشح: [Tanzil Text License](https://tanzil.net/docs/Text_License) (اشتراط النقل الحرفي والإسناد).
- مصدر API مرشح: [Quran Foundation](https://api-docs.quran.com/docs/quickstart/)؛ تستخدم مفاتيح Content API داخل الخادم فقط.

## التطوير

التطوير الفعلي للنسخة الأولى يتم عبر الفرع `dev/foundation` وPull Requests. سيتم إضافة طريقة بناء APK آليًا عند اكتمال أساس التطبيق.

© NEXVARY. كود المشروع وحقوق النص القرآني والتلاوات تخضع لكلٍ من تراخيصها الخاصة.

## MVP Android (development branch)

The **dev/foundation** branch includes a native Android prototype for the adaptive memorization loop. It supports reading, deterministic visual masking, unaided recall, self-assessed review, and local progress. It **does not** perform speech recognition, tajweed scoring, or audio playback yet.

The local demo uses the four verses of Surah Al-Ikhlas, cross-checked against [Quran.com, Surah 112](https://quran.com/112). This isolated demo is not an approved complete corpus; import and verification of a licensed authoritative Quran text dataset are tracked for the next phase.

Build with Android Studio, or download the debug APK once the **Android CI** GitHub Action finishes successfully. Never distribute the debug build as a final app.

AI features must be evaluated against expert reciters before any pronunciation claims are made.
