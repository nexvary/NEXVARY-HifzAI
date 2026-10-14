# حِفْظ | NEXVARY HifzAI

**Adaptive Quran Memorization** — تطبيق أندرويد عربي لتدريب الحفظ بالاسترجاع والمراجعة المتباعدة، مع نموذج أولي لبصمة الذاكرة.

> **الوضع الحالي (0.3.0-dev-dev):** نموذج تجريبي على فرع `dev/foundation`. لا يوجد تقييم تجويد آلي موثّق، ولا مصحف كامل، ولا تسجيلات صوتية مدمجة بعد. لا يُعدّ إصدارًا نهائيًا.

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

التدريب الحالي يستعمل **أربع آيات من سورة الإخلاص فقط** كنموذج. لا يوجد تعرف صوتي، تصحيح مخارج، فحص تجويد، أو أكاديمية مشايخ مباشرة. نحتاج إلى مصحف كامل موثق المصدر والترخيص وحقوق التلاوات قبل أي توزيع إنتاجي.

## خارطة الطريق من 002 إلى 220

- [المراحل الـ219 مع شروط الإنجاز](docs/PHASES_002_220.md)
- [سلامة النص القرآني وحقوقه](docs/CORPUS_INTEGRITY.md)
- [خارطة التطوير العامة](docs/ROADMAP.md)

**لماذا لا ترفع نصًا قرآنيًا مولّدًا؟** يجب اعتماد نص أصيل مُراجع. الإخفاء يغيّر واجهة العرض فقط ولا يبدل الأصل.

## البناء والتحقق

عند نجاح [Android CI](../../actions/workflows/android.yml)، اذهب إلى تشغيل البناء الناجح وحمّل الملف من قسم **Artifacts** باسم `NEXVARY-HifzAI-Android-0.3.0-dev-debug`. لا يُعد وجود Workflow وحده دليلًا على نجاح البناء.

**المفاتيح:** لا تضع API secrets في التطبيق أو GitHub؛ تستخدم Quran Foundation Content APIs خادمًا وسيطًا مع اعتماد الإنتاج. إصدار Pre-live لا يحتوي على المصحف الكامل. انظر [توثيق Quran Foundation](https://api-docs.quran.com/docs/quickstart/).

### Attribution and integrity

- Quran text source candidates: [Tanzil](https://tanzil.net/docs/Text_License) and [Quran Foundation](https://api-docs.quran.com/docs/quickstart/).
- Tanzil permits verbatim copies with explicit attribution; modifying the canonical text is forbidden.
- No upstream canonical full text has yet been bundled in this repository.
- Metadata in the reciter directory does not grant rights to stream or redistribute recordings.

© NEXVARY. Local software and third-party Quran content have separate license requirements.

## لجنة علماء القرآن — مراجعة قبل الإصدار
- [ملف مراجعة اللجنة: المحتوى وطريقة الحفظ والتسميع](docs/SCHOLARLY_REVIEW_DOSSIER.md)
- [مشاريع مفتوحة المصدر تمت دراستها والأدلة العلمية](docs/OPEN_SOURCE_RESEARCH.md)
- [بوابات عدم نشر نسخة غير معتمدة](docs/RELEASE_GATES.md)

**مهم:** هذه نسخة داخلية للتطوير وعرض محدود على لجنة المراجعة؛ نجاح البناء البرمجي لا يعني صلاحية نشر المصحف أو اعتماد طريقة التعلم. حظر إصدار Release في Gradle مقصود إلى حين استكمال مستندات الاعتماد والتحقق منها.

