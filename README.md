> **قرار المشروع (14 أكتوبر 2026): إكمال التطبيق واختباره بالكامل قبل التقديم العلمي الوحيد إلى لجنة العلماء.** النسخة الحالية تطويرية وليست جاهزة للتقديم أو للنشر العام. راجع [خطة التنفيذ](docs/WORK_PLAN_SINGLE_REVIEW.md)، [مواصفات التصميم](docs/LUXURY_UI_UX_SPEC.md)، و[الترخيص المفتوح MIT](LICENSE). حقوق نص القرآن والتسجيلات مستقلة: [NOTICE](NOTICE.md).

# حِفْظ | NEXVARY HifzAI

**Adaptive Quran Memorization** — تطبيق أندرويد عربي لتدريب الحفظ بالاسترجاع والمراجعة المتباعدة، مع نموذج أولي لبصمة الذاكرة.

> **الوضع الحالي (0.5.0-dev):** مصحف كامل مصدره Tanzil Uthmani 1.1 محليًا مع تحقق تقني من الملف، وقارئ 114 سورة وتمارين حفظ؛ لا يُعد نشرًا مُعتمدًا ولا شهادة بصحة العرض على كل الأجهزة. التلاوات والتسميع الصوتي وأكاديمية المعلمين لم تكتمل.

## الوظائف التي تم رفع كودها

- تطبيق Android بلغة Kotlin مع Jetpack Compose وتصميم داكن، وأيقونات Material وواجهة RTL.
- 5 أقسام: **الرئيسية، الحفظ، تقدمي، المشايخ، الإعدادات**.
- قراءة وتدريب إخفاء كلمات الآيات ثم التسميع من الذاكرة بتقييم المستخدم.
- تدريب **الربط** بين نهاية الآية السابقة وبداية الآية التالية.
- بصمة ذاكرة لكل آية: محاولات، استرجاع دون مساعدة، استخدام التلميحات، وأخطاء الانتقال.
- اختيار تدريب مناسب لكل آية، ومواعيد مراجعة متباعدة؛ النتائج تقريبية لم تُقيَّم بعد في دراسة تعليمية.
- حفظ التقدم على الهاتف، مشاركة ملخص بسيط، وحذف البيانات من داخل التطبيق.
- دليل أسماء أولي لقراء معروفين في مصر والسعودية **من دون ملفات صوتية أو ادعاء حقوق تشغيل**.
- محرك تشابه نصي لأغراض اقتراح التدريبات فقط، وليس لتعديل القرآن الكريم.
- اختبارات برمجية وتجهيز GitHub Actions لبناء Debug APK.

## حدود النسخة التجريبية

أُضيف **مصحف Tanzil Uthmani 1.1 الكامل (114 سورة، 6236 آية)** من `dotquran/corpus` إلى ملفات التطبيق **بنسخة حرفية مطابقة** (Git blob `9813920ae5afce9e50d2ac44562457e29bb4bfca`) وبيانات السور، وتم إنشاء قارئ للمصحف مع اختيار السورة وتغيير حجم الخط وتدريب أي آية. يحتاج الخط والرسم والعرض إلى مراجعة علماء القرآن على أجهزة حقيقية، كما لم تُستكمل الصوتيات والتصحيح أو الأكاديمية.

## خارطة الطريق من 002 إلى 220

- [المراحل الـ219 مع شروط الإنجاز](docs/PHASES_002_220.md)
- [سلامة النص القرآني وحقوقه](docs/CORPUS_INTEGRITY.md)
- [خارطة التطوير العامة](docs/ROADMAP.md)

**لماذا لا ترفع نصًا قرآنيًا مولّدًا؟** يجب اعتماد نص أصيل مُراجع. الإخفاء يغيّر واجهة العرض فقط ولا يبدل الأصل.

## البناء والتحقق

عند نجاح [Android CI](../../actions/workflows/android.yml)، اذهب إلى تشغيل البناء الناجح وحمّل الملف من قسم **Artifacts** باسم `NEXVARY-HifzAI-Android-0.5.0-dev-debug`. لا يُعد وجود Workflow وحده دليلًا على نجاح البناء.

**المفاتيح:** لا تضع API secrets في التطبيق أو GitHub؛ تستخدم Quran Foundation Content APIs خادمًا وسيطًا مع اعتماد الإنتاج. إصدار Pre-live لا يحتوي على المصحف الكامل. انظر [توثيق Quran Foundation](https://api-docs.quran.com/docs/quickstart/).

### Attribution and integrity

- Quran text source candidates: [Tanzil](https://tanzil.net/docs/Text_License) and [Quran Foundation](https://api-docs.quran.com/docs/quickstart/).
- Tanzil permits verbatim copies with explicit attribution; modifying the canonical text is forbidden.
- Tanzil Uthmani 1.1 source is bundled verbatim with full copyright footer; independent scholarly review of real rendering remains pending.
- Metadata in the reciter directory does not grant rights to stream or redistribute recordings.

© NEXVARY. Local software and third-party Quran content have separate license requirements.

## ملف تحضير داخلي للجنة — لا يُرسل قبل اكتمال التطبيق
- [ملف مراجعة اللجنة: المحتوى وطريقة الحفظ والتسميع](docs/SCHOLARLY_REVIEW_DOSSIER.md)
- [مشاريع مفتوحة المصدر تمت دراستها والأدلة العلمية](docs/OPEN_SOURCE_RESEARCH.md)
- [بوابات عدم نشر نسخة غير معتمدة](docs/RELEASE_GATES.md)

**مهم:** هذه نسخة داخلية للتطوير وعرض محدود على لجنة المراجعة؛ نجاح البناء البرمجي لا يعني صلاحية نشر المصحف أو اعتماد طريقة التعلم. حظر إصدار Release في Gradle مقصود إلى حين استكمال مستندات الاعتماد والتحقق منها.


### Offline full Quran asset — technical evidence

- Verbatim original: `app/src/main/assets/quran/uthmani-v1.1.txt` (Tanzil Uthmani v1.1, CC BY 3.0, copyright notice preserved).
- Mirror/source trace: `dotquran/corpus/src/resources/uthmani.txt`, Git blob `9813920ae5afce9e50d2ac44562457e29bb4bfca`.
- Android runtime refuses to display the asset if it differs from the pinned Git object.
- CI test `tests/test_embedded_quran.py`: checks 6236 sequential ayat and 114 surahs and exact bytes.
- Versioned surah metadata and offline browsing (8-verse readable pages).
- **Not yet a certificate:** every diacritic and actual rendered screen must still be examined by qualified scholars on target devices before the one final committee submission.

### ميزات 0.5.0-dev الجديدة
- مصحف كامل 114 سورة مع اختيار السورة والبحث عنها وتصفح الآيات وتغيير حجم الخط.
- تدريبات المتشابهات اللفظية بين السور: اقتراحات آلية لا تمثل حكمًا من عالم مختص.
- تسجيل المستخدم لصوته والاستماع إليه شخصيًا؛ يتطلب موافقة الميكروفون، يعمل بلا إرسال صوت إلى الخادم، ويمسح التسجيل عند مغادرة التدريب أو فتح جلسة جديدة.
- تسميع اختياري بالكتابة دون حفظ كتابات المستخدم بعد انتهاء الجلسة.
- أيقونة تطبيق متجهية بهوية كحلية وذهبية وزمردية، مع تنقل من خمسة تبويبات.
- لا تزال تشغيل تلاوات المشايخ وتصحيح التجويد وأكاديمية المحفظين وظائف غير مكتملة تحتاج أصولًا مرخصة واعتمادًا بشريًا.

## Verified development additions — not final acceptance
Official Tanzil download byte comparison, immutable corpus views, derived Quran search, direct ayah navigation, opening basmala references, Amiri Quran OFL font, session resume, retained hint accounting, lapse prioritisation, private voice cleanup, local teacher notebook, and safety-gated adapters for future approved audio/ASR. No external recording/model is bundled.

- [Initial code/CI audit](docs/AUDIT_2026-10-14.md)
- [Every numbered phase: implementation and remaining evidence](docs/PHASE_EVIDENCE.md)
- [Arabic user guide for existing functions](docs/USER_GUIDE_AR.md)
- [Official corpus comparison](docs/OFFICIAL_CORPUS_COMPARISON.json)
- [Final-review readiness — currently blocked](docs/review/READINESS.json)

A local notebook is not a live academy; an ASR comparator is not an evaluated recognizer; a playback adapter is not a cleared audio catalogue. Public APK upload is blocked as well as production Gradle packaging. Do not submit this development state to the committee.

### Partition learning
An unmodified official Tanzil metadata asset now supplies 30 juz and 604 page verse boundaries. Range sessions use original references, cross surahs, retain assistance, resume locally and stop at the selected boundary. They do not reproduce the printed page layout. Coverage/order/boundary tests are present. The metadata root specifies cc-by without a license version; this remains a final rights-clarification item. Device proof is configured for API 26 and 35 at normal and 1.5x system font; consult matching Actions outcomes before claiming that matrix passed.
