# مصفوفة المراحل 002–220 — أدلة وحالة فعلية

كل المراحل الـ219 موجودة. مراجعة المصدر: `7c5269e7c5b1778506c5852d0810f97c891183d0`.

الحالة «جزئي» تصف وجود تنفيذ أو جزء من بنيته؛ لا تعني تحقيق كل وظيفة في عنوان المرحلة. لا مربعات إنجاز تُملأ آليًا. البنية القابلة للربط ليست ميزة نهائية. `PHASE_EVIDENCE.json` يحتوي ملفات التنفيذ وCommit لكل ملف والاختبارات والمانع لكل مرحلة.

| المرحلة | الوظيفة المطلوبة | الحالة | التنفيذ والقيود |
|---|---|---|---|
| 002 | Audit GitHub HEAD and existing code | جزئي / غير مقبول نهائيًا | AUDIT_2026-10-14.md؛ التدقيق الأولي مثبت؛ الإصلاحات اللاحقة لها أدلة مستقلة |
| 003 | Repair the Android SDK CI bootstrap | جزئي / غير مقبول نهائيًا | android.yml؛ راجع نتيجة CI لنفس commit؛ لا يغطي فحص القرآن البصري |
| 004 | Produce verifiable debug APK and checksum | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 005 | Establish README and architecture decisions | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 006 | Freeze canonical Quran text interfaces | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 007 | Add full-corpus structural validation | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 008 | Verify independently pinned source hashes | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 009 | Document text and recitation licensing | جزئي / غير مقبول نهائيًا | NOTICE.md, THIRD_PARTY_CONTENT_REGISTER.md, RecitationRights.kt؛ النص والخط موثقان؛ لا تراخيص تسجيلات خارجية معتمدة |
| 010 | Add provenance fields and versioning | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 011 | Prevent mutation of canonical text | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 012 | Add representative Quran text tests | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 013 | Provide right-to-left Arabic baseline | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 014 | Add accessibility labels for all controls | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 015 | Persist review progress locally | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 016 | Add reset confirmation for user data | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 017 | Separate UI, domain and persistence | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 018 | Document demo-vs-production status | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 019 | Implement protected secrets policy | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 020 | Define first end-to-end release gate | جزئي / غير مقبول نهائيًا | prepare_review_bundle.py, READINESS.json, SCHOLARLY_REVIEW_DOSSIER.md؛ بوابة مانعة موجودة؛ الحزمة والطلب النهائي والإصدار المعتمد غير مكتملة |
| 021 | Provide first-run learning introduction | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 022 | Let learners choose memorization goal | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 023 | Create surah and ayah selector | جزئي / غير مقبول نهائيًا | MainActivity.kt, QuranSearchIndex.kt؛ اختيار سورة وآية موجود؛ تقسيم عرض 8 آيات ليس صفحات المدينة |
| 024 | Add read-first practice stage | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 025 | Implement progressive word concealment | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 026 | Add full-verse active recall | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 027 | Add optional first-word hints | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 028 | Track hint usage in review events | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 029 | Implement verse transition practice | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 030 | Add reverse transition prompts | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 031 | Add end-of-page continuation practice | جزئي / غير مقبول نهائيًا | QuranPartitions.kt, PartitionPickerScreen.kt, RangePracticeScreen.kt؛ نطاق أجزاء وصفحات مثبت؛ تدريب إكمال نهاية الصفحة إلى التالية وأهداف مقاطع مخصصة غير مكتملين |
| 032 | Allow learner-supplied segment goals | جزئي / غير مقبول نهائيًا | QuranPartitions.kt, PartitionPickerScreen.kt, RangePracticeScreen.kt؛ نطاق أجزاء وصفحات مثبت؛ تدريب إكمال نهاية الصفحة إلى التالية وأهداف مقاطع مخصصة غير مكتملين |
| 033 | Provide repetition without losing place | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 034 | Support slow learners with shorter tasks | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 035 | Allow manual replay of failed items | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 036 | Add review due-date display | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 037 | Separate memorization from revision modes | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 038 | Add recovery drill for forgotten ayat | جزئي / غير مقبول نهائيًا | RecallLadder.kt, HifzEngine.kt, MainActivity.kt؛ تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا |
| 039 | Enable saved stop points | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 040 | Test entire memorization flow on device | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 041 | Implement single-screen focus practice | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 042 | Design clear home dashboard | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 043 | Provide full in-app back navigation | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 044 | Make large readable touch targets | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 045 | Support tablets and foldables | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 046 | Design authentic Quran reading typography | جزئي / غير مقبول نهائيًا | amiri_quran.ttf, MainActivity.kt, NOTICE.md؛ تغطية المحارف لا تثبت سلامة تركيب كل علامة على كل جهاز |
| 047 | Add controlled text-size settings | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 048 | Support contrast-friendly dark mode | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 049 | Provide optional bright reading mode | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 050 | Use real vector icons and labels | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 051 | Keep all Arabic screens fully RTL | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 052 | Preserve LTR for URLs and numbers | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 053 | Improve TalkBack navigation order | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 054 | Add screen-reader friendly verse indices | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 055 | Offer minimal distraction mode | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 056 | Support font scaling without clipping | جزئي / غير مقبول نهائيًا | amiri_quran.ttf, MainActivity.kt, NOTICE.md؛ تغطية المحارف لا تثبت سلامة تركيب كل علامة على كل جهاز |
| 057 | Handle rotation without losing session | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 058 | Show empty and error states | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 059 | Clarify microphone permissions in UI | جزئي / غير مقبول نهائيًا | MainActivity.kt؛ تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد |
| 060 | Run usability sessions with learners | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 061 | Define licensed recitation contracts | جزئي / غير مقبول نهائيًا | ReciterCatalog.kt, RecitationRights.kt؛ دليل أسماء وحقول حقوق فقط؛ لا مصادر تسجيلات معتمدة أو معلمين موثقين |
| 062 | Design reciter profile schema | جزئي / غير مقبول نهائيًا | ReciterCatalog.kt, RecitationRights.kt؛ دليل أسماء وحقول حقوق فقط؛ لا مصادر تسجيلات معتمدة أو معلمين موثقين |
| 063 | Record Egyptian reciters metadata | جزئي / غير مقبول نهائيًا | ReciterCatalog.kt, RecitationRights.kt؛ دليل أسماء وحقول حقوق فقط؛ لا مصادر تسجيلات معتمدة أو معلمين موثقين |
| 064 | Record Saudi reciters metadata | جزئي / غير مقبول نهائيًا | ReciterCatalog.kt, RecitationRights.kt؛ دليل أسماء وحقول حقوق فقط؛ لا مصادر تسجيلات معتمدة أو معلمين موثقين |
| 065 | Link each recording to chapter and ayah | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 066 | Verify recording provenance and rights | جزئي / غير مقبول نهائيًا | THIRD_PARTY_CONTENT_REGISTER.md, RecitationRights.kt؛ حقول المنع موجودة؛ لا وثائق أذون تسجيلات فعلية |
| 067 | Build streaming audio adapter | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 068 | Add background audio service | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 069 | Enable verse repeat and sequence | جزئي / غير مقبول نهائيًا | LicensedRecitationPlayer.kt؛ محول تشغيل محلي للربط لاحقًا؛ لا تسجيل مرخص لتجربة التكرار والسرعة والاستئناف |
| 070 | Provide adjustable playback speed | جزئي / غير مقبول نهائيًا | LicensedRecitationPlayer.kt؛ محول تشغيل محلي للربط لاحقًا؛ لا تسجيل مرخص لتجربة التكرار والسرعة والاستئناف |
| 071 | Design gapless ayah transitions | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 072 | Add pause and resume state | جزئي / غير مقبول نهائيًا | LicensedRecitationPlayer.kt؛ محول تشغيل محلي للربط لاحقًا؛ لا تسجيل مرخص لتجربة التكرار والسرعة والاستئناف |
| 073 | Support headset media controls | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 074 | Enable approved offline downloads | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 075 | Manage offline audio storage quotas | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 076 | Apply recording expiration conditions | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 077 | Design favorite reciter setting | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 078 | Support multiple authorized narrations | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 079 | Provide playable sample validation | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 080 | Test low-connectivity playback | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 081 | Define ASR evaluation corpus | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 082 | Collect consented recitation examples | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 083 | Measure Egyptian Arabic recitation variations | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 084 | Measure Saudi recitation variations | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 085 | Baseline word alignment accuracy | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 086 | Build offline-capable ASR prototype | جزئي / غير مقبول نهائيًا | RecitationAssessment.kt؛ واجهة قارئ صوت ومقارنة محدودة مع حالة عدم اليقين؛ لا نموذج تعرف صوتي مستقل التقييم |
| 087 | Measure false positives by verse | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 088 | Measure false negatives by verse | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 089 | Separate omitted-word vs substitution signals | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 090 | Provide uncertainty thresholds | جزئي / غير مقبول نهائيًا | RecitationAssessment.kt؛ واجهة قارئ صوت ومقارنة محدودة مع حالة عدم اليقين؛ لا نموذج تعرف صوتي مستقل التقييم |
| 091 | Suppress low-confidence automated judgments | جزئي / غير مقبول نهائيًا | RecitationAssessment.kt؛ واجهة قارئ صوت ومقارنة محدودة مع حالة عدم اليقين؛ لا نموذج تعرف صوتي مستقل التقييم |
| 092 | Do not grade makharij by text match alone | جزئي / غير مقبول نهائيًا | RecitationAssessment.kt؛ واجهة قارئ صوت ومقارنة محدودة مع حالة عدم اليقين؛ لا نموذج تعرف صوتي مستقل التقييم |
| 093 | Allow recitation replay for users | جزئي / غير مقبول نهائيًا | PrivateRecitationPractice.kt, MainActivity.kt؛ تسجيل اختياري مؤقت للاستماع الذاتي؛ لا تصحيح آلي |
| 094 | Provide expert escalation workflow | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 095 | Implement correction consent control | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 096 | Avoid recording storage by default | جزئي / غير مقبول نهائيًا | PrivateRecitationPractice.kt, MainActivity.kt؛ تسجيل اختياري مؤقت للاستماع الذاتي؛ لا تصحيح آلي |
| 097 | Provide opt-in encrypted recordings | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 098 | Evaluate tajweed with qualified teachers | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 099 | Measure latency and resource needs | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 100 | Gate voice grading behind validation | جزئي / غير مقبول نهائيًا | RecitationAssessment.kt؛ واجهة قارئ صوت ومقارنة محدودة مع حالة عدم اليقين؛ لا نموذج تعرف صوتي مستقل التقييم |
| 101 | Define per-ayah memory fingerprint | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 102 | Track unaided recall events | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 103 | Track hint-dependent recall events | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 104 | Track transition misses | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 105 | Track repetitions and lapses | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 106 | Implement due-review priority | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 107 | Add adaptive masking difficulty | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 108 | Model similarity confusion indicators | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 109 | Add differentiated recency scores | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 110 | Create daily workload budget | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 111 | Tune review intervals empirically | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 112 | Allow learner to override training plan | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 113 | Explain recommended drills transparently | جزئي / غير مقبول نهائيًا | AdaptivePractice.kt, RecallLadder.kt, ProgressStore.kt, MainActivity.kt؛ بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة |
| 114 | Add confidence estimate to model choices | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 115 | Track long-term retention cohorts | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 116 | Add weekly learning trends | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 117 | Measure improvement by practice mode | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 118 | Detect disengagement with consent | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 119 | Avoid addictive or misleading streaks | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 120 | Calibrate algorithm by human study outcomes | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 121 | Specify teacher registration and consent | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 122 | Design Quran teacher verification | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 123 | Verify certificates without public identity leakage | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 124 | Offer Egypt region filtering | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 125 | Offer Saudi Arabia region filtering | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 126 | Add teacher availability calendar | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 127 | Design class booking workflow | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 128 | Support students and guardians roles | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 129 | Provide private live teaching sessions | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 130 | Create teacher correction notes | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 131 | Enable human quality review queue | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 132 | Add student progress with permissions | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 133 | Provide teacher-assigned revision plan | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 134 | Design parental consent for minors | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 135 | Add safety and abuse-report workflow | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 136 | Build controlled messaging policies | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 137 | Manage teacher schedule changes | جزئي / غير مقبول نهائيًا | service.py, server.py, console.html, console.js, ACADEMY_SERVICE.md, TeacherNotebook.kt؛ خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق |
| 138 | Issue completion attestations not ijazat | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 139 | Define documented ijazah verification | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 140 | Pilot with approved Quran teachers | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 141 | Establish scholarly review board guidelines | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 142 | Specify Quran text provenance checks | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 143 | Register teaching methodology sources | جزئي / غير مقبول نهائيًا | OPEN_SOURCE_RESEARCH.md, RecitationAssessment.kt, RELEASE_GATES.md؛ وثائق وحدود معلنة؛ لا تقييم علمي مستقل أو موافقات قانونية موثقة |
| 144 | Define metadata for classical reciters | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 145 | Define metadata for contemporary reciters | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 146 | Add verified biography editor | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 147 | Track editorial changes and approvals | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 148 | Build tafsir content attribution layer | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 149 | Create carefully scoped verse explanation view | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 150 | Validate narration-specific differences | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 151 | Document qiraat handling boundaries | جزئي / غير مقبول نهائيًا | OPEN_SOURCE_RESEARCH.md, RecitationAssessment.kt, RELEASE_GATES.md؛ وثائق وحدود معلنة؛ لا تقييم علمي مستقل أو موافقات قانونية موثقة |
| 152 | Avoid blending incompatible riwayat | جزئي / غير مقبول نهائيًا | OPEN_SOURCE_RESEARCH.md, RecitationAssessment.kt, RELEASE_GATES.md؛ وثائق وحدود معلنة؛ لا تقييم علمي مستقل أو موافقات قانونية موثقة |
| 153 | Add vetted tajweed learning modules | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 154 | Identify ambiguous recitation cases | جزئي / غير مقبول نهائيًا | OPEN_SOURCE_RESEARCH.md, RecitationAssessment.kt, RELEASE_GATES.md؛ وثائق وحدود معلنة؛ لا تقييم علمي مستقل أو موافقات قانونية موثقة |
| 155 | Build report-content-error workflow | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 156 | Require two-person editorial review | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 157 | Track licensing for rare recordings | جزئي / غير مقبول نهائيًا | NOTICE.md, THIRD_PARTY_CONTENT_REGISTER.md, RecitationRights.kt؛ النص والخط موثقان؛ لا تراخيص تسجيلات خارجية معتمدة |
| 158 | Implement external link validation | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 159 | Preserve historical recording attribution | جزئي / غير مقبول نهائيًا | NOTICE.md, THIRD_PARTY_CONTENT_REGISTER.md, RecitationRights.kt؛ النص والخط موثقان؛ لا تراخيص تسجيلات خارجية معتمدة |
| 160 | Publish content correction policy | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 161 | Specify minimal data collection | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 162 | Document clear privacy notice | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 163 | Complete data inventory | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 164 | Audit Android permissions | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 165 | Keep API secrets off the client | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 166 | Design authorized server proxy | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 167 | Add HTTPS security review | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 168 | Establish encryption-at-rest threat model | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 169 | Implement secure authentication when needed | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 170 | Provide local data deletion | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 171 | Add export of user progress | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 172 | Support anonymous local mode | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 173 | Define cloud opt-in consent | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 174 | Protect children's data by default | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 175 | Prevent raw microphone log collection | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 176 | Review third-party SDK telemetry | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 177 | Add retention schedules and deletion jobs | جزئي / غير مقبول نهائيًا | SECURITY.md, AndroidManifest.xml, PrivateRecitationPractice.kt, TeacherNotebook.kt؛ تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية |
| 178 | Implement account deletion on server | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 179 | Threat-model teacher-student interactions | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 180 | Perform independent privacy review | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 181 | Enforce JVM unit test gate | جزئي / غير مقبول نهائيًا | android.yml؛ راجع نتيجة CI لنفس commit؛ لا يغطي فحص القرآن البصري |
| 182 | Add Android instrumentation tests | جزئي / غير مقبول نهائيًا | android-ui-proof.yml, verify_screenshot_proof.py؛ صور فعلية فقط بعد نجاح PNG validation؛ ليست مقارنة baseline أو اعتمادًا بصريًا |
| 183 | Run screenshot test baselines | جزئي / غير مقبول نهائيًا | android-ui-proof.yml, verify_screenshot_proof.py؛ صور فعلية فقط بعد نجاح PNG validation؛ ليست مقارنة baseline أو اعتمادًا بصريًا |
| 184 | Verify responsive tablet layout | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 185 | Test TalkBack paths | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 186 | Run Arabic typography review | جزئي / غير مقبول نهائيًا | amiri_quran.ttf, MainActivity.kt, NOTICE.md؛ تغطية المحارف لا تثبت سلامة تركيب كل علامة على كل جهاز |
| 187 | Validate Quran checksum in release CI | جزئي / غير مقبول نهائيًا | QuranCorpus.kt, OFFICIAL_CORPUS_COMPARISON.json؛ مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل |
| 188 | Add battery and memory benchmarks | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 189 | Test offline reading with no internet | جزئي / غير مقبول نهائيًا | android-ui-proof.yml, verify_screenshot_proof.py؛ صور فعلية فقط بعد نجاح PNG validation؛ ليست مقارنة baseline أو اعتمادًا بصريًا |
| 190 | Test long background audio sessions | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 191 | Test corrupted local preferences | جزئي / غير مقبول نهائيًا | ProgressStore.kt, MainActivity.kt؛ التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة |
| 192 | Test database schema migrations | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 193 | Add fuzz tests for verse index bounds | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 194 | Test errors at API access revocation | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 195 | Audit dependency vulnerabilities | جزئي / غير مقبول نهائيًا | DEPENDENCY_AUDIT_2026-10-14.json؛ لا سجلات ثغرات أعيدت وقت الفحص؛ نطاق الفحص لا يشمل plugins أو نظام Android أو تدقيق أمان مستقل |
| 196 | Check APK signing pipeline | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 197 | Pilot learner interviews | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 198 | Design randomized retention comparison | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 199 | Measure recall at 1 7 and 14 days | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 200 | Publish accurate measured limitations | جزئي / غير مقبول نهائيًا | OPEN_SOURCE_RESEARCH.md, RecitationAssessment.kt, RELEASE_GATES.md؛ وثائق وحدود معلنة؛ لا تقييم علمي مستقل أو موافقات قانونية موثقة |
| 201 | Establish alpha release naming | جزئي / غير مقبول نهائيًا | build.gradle.kts, android.yml؛ نسخة تطوير غير نهائية؛ APK لا ينشر في المستودع العام |
| 202 | Version features with change logs | جزئي / غير مقبول نهائيًا | build.gradle.kts, android.yml؛ نسخة تطوير غير نهائية؛ APK لا ينشر في المستودع العام |
| 203 | Add reproducible signed release builds | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 204 | Create dependency lock policy | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 205 | Publish APK checksum and artifact link | جزئي / غير مقبول نهائيًا | build.gradle.kts, android.yml؛ نسخة تطوير غير نهائية؛ APK لا ينشر في المستودع العام |
| 206 | Configure Play internal test track | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 207 | Write localized store description | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 208 | Add Arabic and English localization | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 209 | Plan Turkish and Urdu localization | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 210 | Plan French and German localization | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 211 | Plan Persian and Spanish localization | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 212 | Create website landing page | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 213 | Prepare terms and educational disclaimers | جزئي / غير مقبول نهائيًا | OPEN_SOURCE_RESEARCH.md, RecitationAssessment.kt, RELEASE_GATES.md؛ وثائق وحدود معلنة؛ لا تقييم علمي مستقل أو موافقات قانونية موثقة |
| 214 | Set up support ticket process | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 215 | Create monitoring for backend health | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 216 | Build anonymous opt-in crash reports | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 217 | Add in-app update check | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 218 | Document server deployment and rollback | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 219 | Open scholarly feedback channel | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
| 220 | Release first expert-approved stable version | غير منفذ أو بلا دليل | لا دليل تنفيذ؛ لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول |
