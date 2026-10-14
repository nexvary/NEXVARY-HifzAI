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

## Open-source development and single committee submission

The application source is developed on [dev/foundation](../../tree/dev/foundation), under [MIT](LICENSE). Independent Quran text, recitations, tafsir, fonts and images retain their own rights and provenance. The current repository is a **development project**, not a certified Quran edition.

**Policy:** One formal scholarly committee submission **only after the complete feature contract, on-device QA, source integrity, content rights and security checks are ready**. Do not submit partial prototypes for committee approval.

[Implementation plan](../../blob/dev/foundation/docs/WORK_PLAN_SINGLE_REVIEW.md) · [Premium UI design](../../blob/dev/foundation/docs/LUXURY_UI_UX_SPEC.md) · [Rights notice](../../blob/dev/foundation/NOTICE.md).
