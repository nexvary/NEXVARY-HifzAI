# نتائج الاختبارات — دفعة 2 نوفمبر 2026

سجل هذه الدفعة منفصل عن نتائج Android السابقة. لا يُفترض نجاح CI جديد حتى انتهاء المسار المطابق للرأس.

| الفحص | النتيجة الفعلية |
|---|---|
| baseline المحلي عند 91a80cd | 56 Python، صفر فشل/أخطاء/تخطٍ |
| بعد الإصلاحات ونقل اختبارات c92f2e3 من PR #9 | 69 Python، صفر فشل/أخطاء/تخطٍ؛ أعيدت كاملة على commit نظيف e9f1f61d3f3a9b2a1989b99ee0ae34292787809a |
| اختبارات حزمة المراجعة | رفض المصدر المعدل والجديد والمحذوف، واختبار مستودع Git فعلي؛ fixtures لا تمثل موافقات حقيقية |
| اختبار تقييم الصوت | WER/CER/الامتناع/السحب/الحقوق/الانتهاء/فصل المتحدثين، أمثلة اصطناعية فقط |
| النص والتقسيم | اختبارات البصمات والعدد 114/6236 والتقسيم والترقيم نجحت ضمن Python |
| verify_local_only.py | نجح على manifests المصدر؛ لا APK جديد محليًا لفحص merged permissions |
| prepare_review_bundle.py --audit | رفض مقصود: بوابات pending وAPK غير موجود وmanifest غير مثبت؛ لا حزمة منشأة |
| صفوف المراحل | 90 صفًا فريدًا مرتبة 61–150 |
| git diff --check | نجح قبل الحفظ |
| Gradle/JVM/Lint/Debug/emulator محلي لهذه الدفعة | لم تُشغل: لا Gradle أو Android SDK أو emulator مثبتة هنا؛ Java17 وحدها لا تكفي |
| جهاز فعلي/TalkBack/بطارية/حرارة/دراسة تعلم/تقييم تلاوة | لم تُنفذ |

## دليل CI السابق، للأساس فقط

استُعلمت GitHub Actions مباشرة: الأربعة completed/success وhead_sha مطابق `91a80cdaf095175d7d2f738fcea357075cb94655`:

- [Android CI 41473982106](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41473982106)
- [Android UI Device Proof 41473981879](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41473981879)
- [Small Phone and Tablet 41473982215](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41473982215)
- [Private Academy Console 41473982370](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41473982370)

وصف PR #11 يسجل 92 JVM و204 instrumented executions و192 PNG، لكنها أرقام من الدفعة السابقة؛ لم تُنزّل تقاريرها أو صورها لإعادة تدقيق العد في هذه الدفعة. حالة المسارات مؤكدة بالـAPI، ولا تُنسب اختبارات المحاكي إلى جهاز فعلي. اختبارات التسجيل/الحذف/التنقل/الخط الكبير وRTL تبقى مرتبطة بمصدرها السابق حتى CI الجديد.

## سجل محلي محفوظ

[evidence/2026-11-02/python-tests.json](evidence/2026-11-02/python-tests.json) يحتفظ بمعرّفات الاختبارات الـ69 ومدتها وبصمات ملفات Python ومصدرها `e9f1f61d3f3a9b2a1989b99ee0ae34292787809a` و`working_tree_dirty=false`. أي Commit توثيق تالٍ لا يغير المصدر الذي اختُبر؛ نتائج Android الجديدة تُراجع من طلب السحب قبل الدمج.

## النتيجة النهائية للكود المنشور في هذه الجلسة

PR #12 عند `adeb81d6bfc6f6424360a823717e688bf207c724` اجتاز جميع وظائف CI السبع في المسارات الأربعة. PR #13 عند `a925198f88a6ede8cc5665011d466fe7ed1516f3` اجتازها أيضًا. هذه النتائج للكود المذكور؛ أي commit لاحق لحفظ هذا التوثيق لا يغير Kotlin أو الأدوات أو الاختبارات ولا يُنسب إليه تشغيل جديد لم يكتمل.

| فحص أحدث كود a925198 | النتيجة المثبتة |
|---|---|
| Python محلي، شجرة نظيفة | 69؛ صفر failures/errors/skips، مصدر a925198؛ JSON محفوظ في evidence/2026-11-02/bookmarks-python-tests.json |
| Python في CI | corpus-tests: success |
| JVM | 19 تقرير XML، 92 اختبارًا، صفر failures/errors/skips |
| Lint | صفر أخطاء، 8 تحذيرات؛ لا ادعاء خلو شامل من المشاكل |
| Debug/native/APK | البناء، النموذج/الرخص، ABIs/16KiB، الصلاحيات المدمجة، التوقيع/alignment وبوابة Release نجحت |
| API26 هاتف + هاتف صغير، API35 هاتف + تابلت | كل ملف: 38 طريقة عادية +17 بخط1.5؛ مجموع220 تنفيذ instrumented، بلا فشل |
| الصور | 25 PNG لكل خط/ملف جهاز، مجموع200؛ نُزلت أرشيفات الملفات الأربعة، وطابقت SHA-256 المعلن وبصمات PNG، وفُحصت الحاويات محليًا |
| مراجعة الصور | فُحصت شاشة العلامات للهاتف الصغير والتابلت بالخطين؛ ليست مراجعة بصرية لكل صور المصحف أو اعتمادًا متخصصًا |
| optional academy console | success ببيانات اصطناعية؛ ليس تحقق هوية/وصاية حقيقية |
| التوزيع | Upload APK skipped؛ لم يُرفع APK عام أو إصدار معتمد |

مسارات أحدث كود: [Android CI](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41516029779)، [الهاتفان](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41516030229)، [الهاتف الصغير والتابلت](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41516030274)، [الأكاديمية الاختيارية](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41516029951).

بناء CI يختبر merge commit `bda4d6d27ee9df9b97bee0d41915bbb3547f3805`؛ شجرته `5e9d0f64f26fd6f02179a7e18ff88051a9f25625` مطابقة تمامًا لفرع a925198، وتظهر في manifest صور الأجهزة. تفاصيل الأرشيفات في evidence/2026-11-02/session-ci.json. لا أجهزة فعلية أو تقييم كلام/تعلم حقيقي أو قبول علمي؛ READINESS.json ما زال pending.

## نتيجة إعادة التشغيل اللاحقة — 3 نوفمبر 2026

تشغيل PR #13 التوثيقي897df45 أظهر انهيار SlotWriter متقطعًا على هاتفAPI35 بخط1.5 رغم نجاح الكود السابق وبقية الملفات. التفاصيل ورابط الفشل وخطة المعالجة في COMPOSE_LIFECYCLE_2026-11-03.md. لا تُلغى الأدلة القديمة ولا يُستنتج منها استقرار شامل؛ تشغيل التحديث الجديد معلق.
