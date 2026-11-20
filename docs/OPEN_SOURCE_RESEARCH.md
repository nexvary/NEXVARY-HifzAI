# دراسة المشاريع المشابهة — NEXVARY HifzAI
تاريخ المراجعة: 14 أكتوبر 2026. هذه دراسة أفكار وتراخيص؛ لم تُنسخ شيفرات أو أصول من المشاريع أدناه.

## المشاريع المقارنة
| المشروع | الرابط | الوظائف المتحققة من المصادر | رخصة الشيفرة | القرار |
|---|---|---|---|---|
| Quran Revision Companion | https://github.com/Waqar144/quran_memorization_helper | مراجعة الآيات الصعبة، أنواع مصحف، اختبار ومتشابهات | MIT — LICENSE | الاستفادة من مبادئ التدريبات دون نسخ |
| Recite | https://github.com/noureddin/recite | التسميع بالكتابة أو بلا كتابة، صوت بعد الآية، وضع المعلّم | CC0 — LICENSE؛ حقوق بيانات/صوت منفصلة | دراسة تجربة المستخدم |
| Irsaa | https://github.com/noureddin/irsaa | استدعاء كلمات المصحف بالكتابة وتثبيت شكل الصفحة | CC0 في الكود الأساسي؛ صور وبيانات وروايات تخضع لمصادر أخرى | دراسة الفكرة لا استيراد الأصول |
| Hifz Companion | https://github.com/zakariaf/Hifz-Companion | مراجعة دون الإنترنت ودون حساب/تتبع | GPL-3.0-or-later — LICENSE | مرجع معماري، لا دمج مباشر |
| Quran Android | https://github.com/quran/quran_android | مصحف أندرويد قارئ وصوتيات | GPL-3.0 — LICENSE | مرجع استقرار وUI، لا استيراد شيفرة |
| Tarteel (تجاري) | https://support.tarteel.ai/en/articles/12414416-hide-ayahs | إخفاء الآيات وخاصية تصحيح أخطاء الحفظ | تطبيق مملوك؛ ليست رخصة دمج | مرجع مقارنة فقط |

لا يجوز افتراض أن ترخيص الشيفرة يبيح إعادة توزيع النصوص، التسجيلات، الصور أو الخطوط. لا يُستخدم أي كود من هذه المصادر في هذه المرحلة، ويجب إنشاء سجل اعتماد مستقل عند أي دمج لاحق.

## الفارق المقصود في NEXVARY
1. **سُلّم الاسترجاع:** قراءة، إخفاء خفيف، إخفاء أثقل، استرجاع حر، تدريب ربط. لا تُغير التمارين النص المحفوظ.
2. **سجل الدليل:** تسجيل منفصل لنجاح الاسترجاع دون مساعدة مقابل الاسترجاع بعد كشف أو تلميح، دون الادعاء بقياس صحيح للتجويد.
3. **الضعف الشخصي:** يسجّل المستخدم سبب التعثر (كلمات، انتقال، متشابهات)؛ توصيات البرنامج ليست حكمًا شرعيًا أو لغويًا.
4. **خطة اليوم:** المستحقات أولًا ثم عدد محدود من الآيات الجديدة؛ تقييم مناسب للأداء في دراسة تجريبية.
5. **حظر النشر:** إصدار تجريبي فقط حتى مراجعة العلماء والحقوق والتوافق القانوني.

لا نزعم الأصالة المطلقة؛ التفرد المحتمل في **طريقة الجمع والتكيّف** ويحتاج دراسة تطبيقية، وليس في ميزة الإخفاء بذاتها.

## البحث التعليمي وراء التجربة
- Carpenter, Pan, Butler (2022), *The science of effective learning with spacing and retrieval practice*, Nature Reviews Psychology: https://doi.org/10.1038/s44159-022-00089-1
- McDermott (2021), *Practicing Retrieval Facilitates Learning*, Annual Review of Psychology: https://doi.org/10.1146/annurev-psych-010419-051019
- Gonçalves et al. (2025), *Retrieval Practice Versus Elaborative Encoding: A Systematic and Meta-analytic Review*: https://doi.org/10.1007/s10648-025-10076-6 — أهمية التغذية الراجعة وحدود تعميم التفوق.
- Latimier et al. (2021), meta-analysis of spaced retrieval: https://eric.ed.gov/?id=EJ1310148

**حدود الاستدلال:** هذه النتائج من بحوث التعلّم العام؛ لا تثبت بذاتها فعالية عدد أيام المراجعة الحالي أو فاعلية التطبيق في حفظ القرآن. يجب إجراء تجربة حفظ ومراجعة واقعية بإشراف معلمين.


## Rechecked primary sources — 2026-10-14
| Reference | Actual license checked | Useful design | Limitation / decision |
|---|---|---|---|
| Quran Revision Companion (Waqar144/quran_memorization_helper, master/LICENSE) | MIT, Copyright 2023 Waqar Ahmed | Simple revision records; focus on already memorized material | Different Flutter codebase; no code imported; does not license third-party audio |
| Recite (noureddin/recite, gh-pages/LICENSE) | CC0-1.0 | Distinct typed/untyped recall, contiguous ranges, opt-in teacher audio mode | Web interaction rather than native Android; Quran/audio/font rights remain separate |
| Irsaa (noureddin/irsaa, master/LICENSE) | CC0-1.0 | Writing and progressive reveal with immutable references | Web focus; not evidence that matching text diagnoses tajweed |
| Hifz Companion (zakariaf/Hifz-Companion, main/LICENSE) | GPL-3.0 text; additional conditions in LICENSING.md must be checked before reuse | Offline revision and separate data/asset provenance | No implementation imported into the MIT application |
| Quran Android (quran/quran_android, main/LICENSE) | GPL-3.0 | Native reader, audio and source/content separation | Architecture reference only; no GPL source copied into MIT |
| Tarteel | Proprietary service; compared through official help pages | Recognition UX and uncertainty/evaluation needs | No copying its recordings, code or models; public availability does not license redistribution |

The license files above were retrieved directly from raw.githubusercontent.com. No third-party project code was imported in this change. URLs identify the checked branch, which may change; a future source import requires pinning a commit and reviewing that exact version. Recite README: https://github.com/noureddin/recite ; Irsaa README: https://github.com/noureddin/irsaa ; Tarteel privacy explanation: https://support.tarteel.ai/en/articles/12414462-understanding-our-privacy-policy .

### Learning research versus product claims
- Roediger & Karpicke (2006), Test-enhanced learning, PMID 16507066, DOI 10.1111/j.1467-9280.2006.01693.x: https://pubmed.ncbi.nlm.nih.gov/16507066/ . Supports testing retrieval in general learning tasks; it does not validate this Quran application or its 35/70% masking policy.
- Cepeda et al. (2006), Distributed practice in verbal recall, PMID 16719566, DOI 10.1037/0033-2909.132.3.354: https://pubmed.ncbi.nlm.nih.gov/16719566/ . Provides broad evidence for spacing; this deterministic scheduler still needs an application-specific pilot and delayed-recall measures.
- Quran pronunciation research (arXiv:2509.00094): https://arxiv.org/abs/2509.00094 . Research/model/data claims must be independently reproduced, representative learner speech evaluated, and each data/model license reviewed. No model or claimed published accuracy is applied to HifzAI.

### Concrete engineering changes informed by this review
Retain immutable source references, separate normalized indices, keep hint history for the whole attempt, prioritize forgotten verses as reviews, restore sessions without audio/typed responses, and keep ASR comparison unknown until a separately evaluated recognizer is configured. These are design decisions, not proofs of educational effectiveness.
