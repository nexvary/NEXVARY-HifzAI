# متابعة استقرار Compose — 3 نوفمبر 2026

إعادة تشغيل مصدر PR #13 التوثيقي `897df45` نجحت في البناء وPython وبقية ملفات الأجهزة، لكنها فشلت على هاتف API35 بالخط1.5. النجاح السابق للكود a925198 صحيح لوقته؛ لا يثبت غياب انهيار متقطع.

الدليل المحفوظ: [run 41518648605/job125528637537](https://github.com/nexvary/NEXVARY-HifzAI/actions/runs/41518648605/job/125528637537)، artifact13176553743. العادي أكمل38 اختبارًا، والكبير انهار أثناء `dailyBudgetCountsUniqueGradedVersesAcrossSessionsAndStillAllowsManualPractice` عند تدمير Activity: ArrayIndexOutOfBoundsException في SlotTableKt.dataAnchor/SlotWriter.moveSlotGapTo/removeSlots ثم CompositionImpl.dispose. لا يُخفى الفشل ولا يُعالج بتجاوز الاختبار.

تغييرات هذه الدفعة:

- تحديث Kotlin وCompose compiler معًا من2.0.21 إلى2.1.21.
- تحديث Compose BOM للتطبيق والاختبار معًا من2024.12.01 إلى2025.06.01 (خط Compose1.8.3).
- إبقاء AGP8.7.3 وGradle8.9 وSDK35/Java17 والنص والصوت والصلاحيات وبوابات الإصدار.
- توسيع اختبار المسار نفسه إلى أربع دورات حذف/تدريب/تقييم/إعادة إنشاء، في العادي والكبير، دون حذف شروط أو إعادة محاولات تخفي فشلًا.

هذه معالجة مرشحة تعتمد تحديث المترجم والمكتبة؛ ليست إثباتًا لتشخيص upstream أو ضمانًا لعدم التكرار. يجب ربط نجاح CI الجديد بمصدره ومراجعة أي فشل تالٍ. نتائج الاختبارات الجديدة معلقة حتى تشغيل CI؛ لا أدوات Android محلية مثبتة.

مراجع أولية راجعت في هذه الدفعة:

- https://kotlinlang.org/docs/whatsnew2120.html — تحسينات compiler/inline composable lambdas؛ لا تدعي الصفحة أنها تشخّص هذه الحالة تحديدًا.
- https://kotlinlang.org/docs/releases.html — إصدار التصحيح2.1.21.
- https://developer.android.com/jetpack/androidx/releases/compose-runtime — خط runtime1.8.3 المستقر.
- https://developer.android.com/develop/ui/compose/bom/bom-mapping — استخدام عائلة BOM متوافقة، وليست ترقية إلى أحدث إصدار متاح.

## نتيجة المرشح الأول والمتابعة

التحديث وحده لم يحل الحالة: run41572621375/job125708807276 (هاتفAPI26 بخط كبير) وrun41572621090/job125708806115 (تابلتAPI35 عادي) فشلا في SlotWriter.moveSlotGapTo/removeSlots/dispose. فحص البناء نجح؛ لا يعوض فشل التشغيل. أضيف تعطيل StrongSkipping بواسطة ComposeFeatureFlag في توليد التطبيق والاختبارات، كإجراء تجريبي محافظ لمعرفة أثر التحسين؛ لا تعطيل لاختبار أو استثناء. قد يزيد إعادة التركيب؛ قياس الأداء الحقيقي باقٍ. نتائج المرشح التالي يجب فحصها قبل وصفه بالمعالجة المختبرة.

إعداد المترجم الموثق: https://kotlinlang.org/docs/whatsnew2020.html . هذا ليس إثباتًا أن التحسين سبب العطل.
