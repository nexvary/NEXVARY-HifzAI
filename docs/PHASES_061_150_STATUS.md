# المراحل 061–150: حالة التنفيذ الفعلية

عدد الصفوف: 90. لا يوجد اعتماد نهائي لأي مرحلة من مجرد هذا الجدول. `partial_new` إضافة جزئية في هذه الدفعة، و`partial_existing` تنفيذ سابق جزئي. الاختبارات يجب مطابقتها مع commit الخاص بطلب السحب. أسماء الملفات تشير إلى المستودع؛ راجع وثيقة LOCAL_DEVELOPMENT_061_150.md لحدود التسليم.

| المرحلة | المتطلب الأصلي | الحالة | التنفيذ/الدليل | المتبقي وحدود الادعاء |
|---|---|---|---|---|
| 061 | Define licensed recitation contracts | partial_new | RecitationRights.kt | سجل الحقوق يدعم الانتهاء والسحب؛ يحتاج وثائق تسجيلات فعلية. |
| 062 | Design reciter profile schema | partial_existing | ReciterCatalog.kt | دليل أسماء وبلدان؛ ملفات تعريف مدققة وصوتيات مرخصة غير متاحة. |
| 063 | Record Egyptian reciters metadata | partial_existing | ReciterCatalog.kt | دليل أسماء وبلدان؛ ملفات تعريف مدققة وصوتيات مرخصة غير متاحة. |
| 064 | Record Saudi reciters metadata | partial_existing | ReciterCatalog.kt | دليل أسماء وبلدان؛ ملفات تعريف مدققة وصوتيات مرخصة غير متاحة. |
| 065 | Link each recording to chapter and ayah | partial_new | RecitationCatalog.kt; RecitationCatalogTest.kt; LocalAudioPlaybackTest.kt; LOCAL_AUDIO_CATALOG.md | ربط بمرجع أصلي موجود وطول وبصمة ومراجعة مرجع، مع اختبار تشغيل ملف اصطناعي؛ لا كتالوج تسجيلات خارجية معتمد أو واجهة استيراد. |
| 066 | Verify recording provenance and rights | external_evidence_required | THIRD_PARTY_CONTENT_REGISTER.md | يلزم إثبات مصدر وحقوق كل تسجيل من صاحب الحق. |
| 067 | Build streaming audio adapter | outside_local_scope | LOCAL_ONLY_SCOPE.md | البث الشبكي خارج النطاق المحلي؛ لا تنفيذ. |
| 068 | Add background audio service | not_implemented | — | خدمة خلفية/انتقالات بلا فجوات/أزرار سماعة للتلاوات غير منفذة. |
| 069 | Enable verse repeat and sequence | partial_new | LocalRecitationQueue.kt; LocalAudioPlaybackTest.kt; LOCAL_AUDIO_CATALOG.md | تسلسل أمامي لمراجع متتالية مع تكرار وسرعة وفحص كل ملف، بنغمات اختبار صناعية. لا واجهة مكتبة صوت أو تلاوات مرخصة أو انتقال بلا فجوات أو خدمة خلفية. |
| 070 | Provide adjustable playback speed | partial_existing | LocalAudioPlayer.kt; LicensedRecitationPlayer.kt | تكرار وسرعة واستئناف محلي، مع تسلسل أمامي داخلي؛ لا واجهة مكتبة صوت أو محتوى مرخص. |
| 071 | Design gapless ayah transitions | not_implemented | — | خدمة خلفية/انتقالات بلا فجوات/أزرار سماعة للتلاوات غير منفذة. |
| 072 | Add pause and resume state | partial_existing | LocalAudioPlayer.kt; LicensedRecitationPlayer.kt | تكرار وسرعة واستئناف محلي، مع تسلسل أمامي داخلي؛ لا واجهة مكتبة صوت أو محتوى مرخص. |
| 073 | Support headset media controls | not_implemented | — | خدمة خلفية/انتقالات بلا فجوات/أزرار سماعة للتلاوات غير منفذة. |
| 074 | Enable approved offline downloads | partial_new | OfflineAudioStore.kt; OfflineAudioStoreTest.kt | استيراد محلي محدود كأداة داخلية؛ لا واجهة استيراد أو تنزيل أو محتوى معتمد. |
| 075 | Manage offline audio storage quotas | partial_new | OfflineAudioStore.kt; OfflineAudioStoreTest.kt | حد التخزين وفشل الاستيراد مختبران بوحدات؛ الأداة غير مربوطة بواجهة المستخدم. |
| 076 | Apply recording expiration conditions | partial_new | RecitationRights.kt; LicensedRecitationPlayer.kt | حظر انتهاء/سحب وتحقق دوري؛ لا نظام تجديد حقوق أو سجل صوتيات مستخدم. |
| 077 | Design favorite reciter setting | partial_new | MainActivity.kt; ProgressStore.kt; LearningDevelopmentUiTest.kt | اختيار مفضل وحفظه؛ يحتاج نتيجة اختبار أندرويد المطابقة. |
| 078 | Support multiple authorized narrations | not_implemented | RecitationRights.kt | الروايات الأخرى مرفوضة؛ لا دعم متعدد للروايات. |
| 079 | Provide playable sample validation | external_evidence_required | LocalAudioPlaybackTest.kt | اختبارات المشغل بنغمات صناعية؛ لا عينة تلاوة مرخصة. |
| 080 | Test low-connectivity playback | outside_local_scope | LOCAL_ONLY_SCOPE.md | لا شبكة في التطبيق؛ قارئ دون اتصال، ولا بث/تنزيل. |
| 081 | Define ASR evaluation corpus | partial_new | RECITATION_EVALUATION_PROTOCOL.md; evaluate_recitation.py | بروتوكول وحقول تقييم؛ لا corpus كلام فعلي. |
| 082 | Collect consented recitation examples | external_evidence_required | RECITATION_EVALUATION_PROTOCOL.md | يلزم مشاركون وموافقات وعينات حقيقية ومراجعون متخصصون؛ لا بيانات مصطنعة تدعي تقييمًا. |
| 083 | Measure Egyptian Arabic recitation variations | external_evidence_required | RECITATION_EVALUATION_PROTOCOL.md | يلزم مشاركون وموافقات وعينات حقيقية ومراجعون متخصصون؛ لا بيانات مصطنعة تدعي تقييمًا. |
| 084 | Measure Saudi recitation variations | external_evidence_required | RECITATION_EVALUATION_PROTOCOL.md | يلزم مشاركون وموافقات وعينات حقيقية ومراجعون متخصصون؛ لا بيانات مصطنعة تدعي تقييمًا. |
| 085 | Baseline word alignment accuracy | partial_new | evaluate_recitation.py; test_recitation_evaluation.py | حساب WER والإنذارات والفوات حسب الآية/البلد/التقسيم؛ لا أرقام أداء لنموذج فعلي. |
| 086 | Build offline-capable ASR prototype | partial_existing | LocalTranscription.kt; NativeAsrSmokeTest.kt | Whisper محلي تجريبي سابق؛ دقة القرآن غير مقيسة. |
| 087 | Measure false positives by verse | partial_new | evaluate_recitation.py; test_recitation_evaluation.py | حساب WER والإنذارات والفوات حسب الآية/البلد/التقسيم؛ لا أرقام أداء لنموذج فعلي. |
| 088 | Measure false negatives by verse | partial_new | evaluate_recitation.py; test_recitation_evaluation.py | حساب WER والإنذارات والفوات حسب الآية/البلد/التقسيم؛ لا أرقام أداء لنموذج فعلي. |
| 089 | Separate omitted-word vs substitution signals | partial_new | LexicalAlignment.kt; LexicalAlignmentTest.kt | فصل الحذف والإضافة والاستبدال بمواضع؛ ليس حكمًا قرآنيًا أو تجويديًا. |
| 090 | Provide uncertainty thresholds | partial_new | RecitationAssessment.kt; evaluate_recitation.py | عتبة تحليلية وامتناع؛ لا معايرة مستقلة أو إتاحة تصحيح آلي. |
| 091 | Suppress low-confidence automated judgments | partial_existing | RecitationAssessment.kt; LocalTranscriptionScreen.kt | التفريغ غير معتمد ولا يغذي التقدم أو يصحح التجويد؛ يلزم تقييم مستقل. |
| 092 | Do not grade makharij by text match alone | partial_existing | RecitationAssessment.kt; LocalTranscriptionScreen.kt | التفريغ غير معتمد ولا يغذي التقدم أو يصحح التجويد؛ يلزم تقييم مستقل. |
| 093 | Allow recitation replay for users | partial_existing | PrivateRecitationPractice.kt; LocalTranscription.kt | استماع ذاتي خاص مؤقت وRAM للتفريغ؛ يحتاج تحقق الأجهزة المطابق. |
| 094 | Provide expert escalation workflow | not_implemented | — | لا إحالة خبيرة متصلة أو تصحيح اختياري مفعل أو حفظ تسجيلات مشفرة طويل الأجل. |
| 095 | Implement correction consent control | not_implemented | — | لا إحالة خبيرة متصلة أو تصحيح اختياري مفعل أو حفظ تسجيلات مشفرة طويل الأجل. |
| 096 | Avoid recording storage by default | partial_existing | PrivateRecitationPractice.kt; LocalTranscription.kt | استماع ذاتي خاص مؤقت وRAM للتفريغ؛ يحتاج تحقق الأجهزة المطابق. |
| 097 | Provide opt-in encrypted recordings | not_implemented | — | لا إحالة خبيرة متصلة أو تصحيح اختياري مفعل أو حفظ تسجيلات مشفرة طويل الأجل. |
| 098 | Evaluate tajweed with qualified teachers | external_evidence_required | RECITATION_EVALUATION_PROTOCOL.md | يلزم مشاركون وموافقات وعينات حقيقية ومراجعون متخصصون؛ لا بيانات مصطنعة تدعي تقييمًا. |
| 099 | Measure latency and resource needs | partial_existing | NativeAsrSmokeTest.kt | زمن اختبار نغمة على محاكي؛ قياس ذاكرة/طاقة/حرارة على ARM حقيقي معلق. |
| 100 | Gate voice grading behind validation | partial_existing | RecitationAssessment.kt; LocalTranscriptionScreen.kt | التفريغ غير معتمد ولا يغذي التقدم أو يصحح التجويد؛ يلزم تقييم مستقل. |
| 101 | Define per-ayah memory fingerprint | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 102 | Track unaided recall events | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 103 | Track hint-dependent recall events | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 104 | Track transition misses | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 105 | Track repetitions and lapses | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 106 | Implement due-review priority | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 107 | Add adaptive masking difficulty | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 108 | Model similarity confusion indicators | partial_existing | AdaptivePractice.kt; ProgressStore.kt; RecallLadder.kt | بصمة وتقييم ذاتي وجدولة موجودة؛ فاعلية التعلم غير مثبتة. |
| 109 | Add differentiated recency scores | partial_new | LearningPlan.kt; LearningPlanTest.kt | ترجيح حداثة آخر المحاولات لتوصية تدريب فقط؛ لا تغيير غير مقاس لفواصل المراجعة. |
| 110 | Create daily workload budget | partial_new | DailyWork.kt; ProgressStore.kt; LearningPlan.kt; DailyWorkTest.kt; GlobalDailyPlanTest.kt; LearningDevelopmentUiTest.kt | مراجعات المصحف كله وحفظ جديد من سورة محفوظة مع الجلسة؛ القائمة وزر البدء يستخدمان الخطة نفسها. حد مقترح يومي عبر الجلسات والسور من الآيات المقيّمة، دون خصم مكرر لإعادة المحاولة أو منع التدريب اليدوي؛ العد الجديد لا يخترع تاريخًا للسجلات القديمة. |
| 111 | Tune review intervals empirically | external_evidence_required | RECITATION_EVALUATION_PROTOCOL.md | يلزم تصميم ومشاركون ودراسة احتفاظ فعلية؛ لا cohorts أو فعالية مقيسة. |
| 112 | Allow learner to override training plan | partial_new | LearningGoalControls.kt; LearningPlan.kt; LearningDevelopmentUiTest.kt | أوضاع حفظ/مراجعة وحد يومي مقترح مع إتاحة التدريب اليدوي وإعادة المواضع المتعثرة؛ لا منع عند تجاوز الهدف ولا ادعاء تحسن مقيس. |
| 113 | Explain recommended drills transparently | partial_new | LearningPlan.kt; MainActivity.kt | سبب واضح واقتراح اختياري من المحاولات الأخيرة؛ تحقق UX معلق. |
| 114 | Add confidence estimate to model choices | not_implemented | — | لا درجة ثقة احتمالية معايرة لتوصيات التعلم؛ لا اختلاق نسبة. |
| 115 | Track long-term retention cohorts | external_evidence_required | RECITATION_EVALUATION_PROTOCOL.md | يلزم تصميم ومشاركون ودراسة احتفاظ فعلية؛ لا cohorts أو فعالية مقيسة. |
| 116 | Add weekly learning trends | partial_new | LearningPlan.kt; LearningGoalControls.kt; LearningPlanTest.kt | ملخصات أسبوعية وحسب التدريب من آخر30 محاولة لكل آية؛ ليست تحسنًا سببيًا أو سجلًا كاملًا. |
| 117 | Measure improvement by practice mode | partial_new | LearningPlan.kt; LearningGoalControls.kt; LearningPlanTest.kt | ملخصات أسبوعية وحسب التدريب من آخر30 محاولة لكل آية؛ ليست تحسنًا سببيًا أو سجلًا كاملًا. |
| 118 | Detect disengagement with consent | partial_new | GentleReturnPolicy.kt; ProgressStore.kt; MainActivity.kt; GentleReturnPolicyTest.kt; LocalDataAndAudioTest.kt | اقتراح عودة محلي بعد أسبوع بموافقة اختيارية، وإيقاف يمحو تاريخ التذكير؛ لا إشعارات أو إرسال بيانات أو قياس فاعلية. اختبار أندرويد المطابق مطلوب. |
| 119 | Avoid addictive or misleading streaks | partial_existing | MainActivity.kt | لا سلسلة أيام ملزمة؛ الأرقام تقييم ذاتي، دون ادعاء أثر تعليمي مثبت. |
| 120 | Calibrate algorithm by human study outcomes | external_evidence_required | RECITATION_EVALUATION_PROTOCOL.md | يلزم تصميم ومشاركون ودراسة احتفاظ فعلية؛ لا cohorts أو فعالية مقيسة. |
| 121 | Specify teacher registration and consent | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 122 | Design Quran teacher verification | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 123 | Verify certificates without public identity leakage | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 124 | Offer Egypt region filtering | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 125 | Offer Saudi Arabia region filtering | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 126 | Add teacher availability calendar | partial_new | TeacherNotebook.kt; TeacherNotebookScreen.kt; LocalDataAndAudioTest.kt; NotebookAgenda.kt; NotebookAgendaScreen.kt; NotebookAgendaTest.kt; TeacherNotebookUiTest.kt | جدول يومي محلي للجلسات والمهام، بتصفية اليوم/القادمة/السابقة/المكتملة والطالب وصفحات محدودة؛ ليس حجزًا خارجيًا أو تقدمًا موثقًا من معلم. |
| 127 | Design class booking workflow | partial_new | TeacherNotebook.kt; TeacherNotebookScreen.kt; LocalDataAndAudioTest.kt; NotebookAgenda.kt; NotebookAgendaScreen.kt; NotebookAgendaTest.kt; TeacherNotebookUiTest.kt | جدول يومي محلي للجلسات والمهام، بتصفية اليوم/القادمة/السابقة/المكتملة والطالب وصفحات محدودة؛ ليس حجزًا خارجيًا أو تقدمًا موثقًا من معلم. |
| 128 | Support students and guardians roles | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 129 | Provide private live teaching sessions | outside_local_scope | LOCAL_ONLY_SCOPE.md | الجلسات المتصلة/المراسلة/الشهادات/الإجازات خارج التطبيق المحلي وغير منفذة فيه. |
| 130 | Create teacher correction notes | partial_new | TeacherNotebook.kt; TeacherNotebookScreen.kt; LocalDataAndAudioTest.kt | دفتر محلي قابل للتعديل؛ ليس حجزًا خارجيًا أو تقدمًا موثقًا من معلم. |
| 131 | Enable human quality review queue | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 132 | Add student progress with permissions | partial_new | TeacherNotebook.kt; TeacherNotebookScreen.kt; LocalDataAndAudioTest.kt | دفتر محلي قابل للتعديل؛ ليس حجزًا خارجيًا أو تقدمًا موثقًا من معلم. |
| 133 | Provide teacher-assigned revision plan | partial_new | TeacherNotebook.kt; TeacherNotebookScreen.kt; LocalDataAndAudioTest.kt; TeacherNotebookUiTest.kt | نطاق مراجعة محلي محقق بالمراجع، قابل لتعديل الآيات/الموعد والحذف المؤكد؛ تغيير الآيات يعيد فتح المهمة. لا إرسال للطالب أو خطة موثقة من معلم مستقل. |
| 134 | Design parental consent for minors | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 135 | Add safety and abuse-report workflow | partial_existing | academy/service.py; academy/console.js; TeacherNotebook.kt | خدمة داخلية اختيارية منفصلة ودفتر محلي؛ لا تحقق أشخاص حقيقيين أو نشر أو ربط Android. |
| 136 | Build controlled messaging policies | outside_local_scope | LOCAL_ONLY_SCOPE.md | الجلسات المتصلة/المراسلة/الشهادات/الإجازات خارج التطبيق المحلي وغير منفذة فيه. |
| 137 | Manage teacher schedule changes | partial_new | TeacherNotebook.kt; TeacherNotebookScreen.kt; LocalDataAndAudioTest.kt; NotebookAgenda.kt; NotebookAgendaScreen.kt; NotebookAgendaTest.kt; TeacherNotebookUiTest.kt | تعديل موعد وملاحظات وحذف جلسة منفردة مع ملاحظات المشكلة المرتبطة بها، بعد تأكيد؛ بقية الملفات محفوظة؛ إجراءات من الجدول مع تصفية وصفحات. لا حجز أو إشعارات أطراف. |
| 138 | Issue completion attestations not ijazat | outside_local_scope | LOCAL_ONLY_SCOPE.md | الجلسات المتصلة/المراسلة/الشهادات/الإجازات خارج التطبيق المحلي وغير منفذة فيه. |
| 139 | Define documented ijazah verification | outside_local_scope | LOCAL_ONLY_SCOPE.md | الجلسات المتصلة/المراسلة/الشهادات/الإجازات خارج التطبيق المحلي وغير منفذة فيه. |
| 140 | Pilot with approved Quran teachers | external_evidence_required | LOCAL_ONLY_SCOPE.md | لا تجربة معلمين فعلية؛ الأكاديمية المتصلة مؤجلة. |
| 141 | Establish scholarly review board guidelines | partial_existing | SCHOLARLY_REVIEW_DOSSIER.md; WORK_PLAN_SINGLE_REVIEW.md | وثائق سياسة مراجعة؛ لا لجنة مشكلة أو اعتماد أو اتصال. |
| 142 | Specify Quran text provenance checks | partial_existing | QuranCorpus.kt; test_embedded_quran.py | بصمات المصدر الأصلية ثابتة؛ تدقيق العرض البشري معلق. |
| 143 | Register teaching methodology sources | partial_existing | OPEN_SOURCE_RESEARCH.md; RECITATION_EVALUATION_PROTOCOL.md | مصادر وحدود وبروتوكول؛ لا دراسة فاعلية منفذة. |
| 144 | Define metadata for classical reciters | partial_existing | ReciterCatalog.kt | تعريف أولي بالاسم والبلد؛ لا سيرة موثقة أو تسجيلات معتمدة. |
| 145 | Define metadata for contemporary reciters | partial_existing | ReciterCatalog.kt | تعريف أولي بالاسم والبلد؛ لا سيرة موثقة أو تسجيلات معتمدة. |
| 146 | Add verified biography editor | not_implemented | — | لا محرر سير/مسار موافقات تحريرية/أصول تفسير مرخصة/واجهة تفسير. |
| 147 | Track editorial changes and approvals | not_implemented | — | لا محرر سير/مسار موافقات تحريرية/أصول تفسير مرخصة/واجهة تفسير. |
| 148 | Build tafsir content attribution layer | not_implemented | — | لا محرر سير/مسار موافقات تحريرية/أصول تفسير مرخصة/واجهة تفسير. |
| 149 | Create carefully scoped verse explanation view | not_implemented | — | لا محرر سير/مسار موافقات تحريرية/أصول تفسير مرخصة/واجهة تفسير. |
| 150 | Validate narration-specific differences | external_evidence_required | RecitationRights.kt; RECITATION_EVALUATION_PROTOCOL.md | حفص فقط؛ لا مقارنة فروق روايات أو مراجعة متخصصة متعددة الروايات. |
