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
