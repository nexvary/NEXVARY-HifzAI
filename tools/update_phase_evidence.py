"""Generate every backlog row with conservative evidence; never auto-check completion."""
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
L = 'app/src/main/java/com/nexvary/hifzai/learning/'
T = 'app/src/test/java/com/nexvary/hifzai/learning/'
UI = 'app/src/main/java/com/nexvary/hifzai/MainActivity.kt'
# Evidence links specify implementation scope; overall acceptance remains separate.
MAP = {}

def group(numbers, files, tests, limitation):
    for number in numbers:
        MAP[number] = (files, tests, limitation)

group([2], ['docs/AUDIT_2026-10-14.md'], ['Git history and CI logs inspected'], 'التدقيق الأولي مثبت؛ الإصلاحات اللاحقة لها أدلة مستقلة')
group([3,181], ['.github/workflows/android.yml'], ['Android CI unit/build/lint'], 'راجع نتيجة CI لنفس commit؛ لا يغطي فحص القرآن البصري')
group([6,7,8,10,11,12,142,187], [L+'QuranCorpus.kt','docs/OFFICIAL_CORPUS_COMPARISON.json'], ['tests/test_embedded_quran.py',T+'QuranCorpusJvmTest.kt'], 'مطابقة الأصل الرسمي مثبتة؛ تدقيق العرض العلمي الداخلي لم يكتمل')
group([9,157,159], ['NOTICE.md','docs/THIRD_PARTY_CONTENT_REGISTER.md',L+'RecitationRights.kt'], [T+'RecitationSafetyTest.kt'], 'النص والخط موثقان؛ لا تراخيص تسجيلات خارجية معتمدة')
group([13,43,47,48,50,51,54,58,59], [UI], ['app/src/androidTest/java/com/nexvary/hifzai/HifzUiSmokeTest.kt'], 'تنفيذ الواجهة موجود؛ فحص الهاتف والتابلت وTalkBack والخط الكبير ليس مثبتًا بعد')
group([15,16,28,36,39,57,170,171,172,191], [L+'ProgressStore.kt',UI], ['app/src/androidTest/java/com/nexvary/hifzai/LocalDataAndAudioTest.kt'], 'التخزين محلي؛ التصدير ملخص نصي فقط؛ لا استيراد أو مزامنة متعددة الأجهزة')
group([24,25,26,29,33,34,35,38], [L+'RecallLadder.kt',L+'HifzEngine.kt',UI], [T+'RecallLadderTest.kt',T+'SafetyRegressionTest.kt'], 'تقييم ذاتي؛ التكيف والقيمة التعليمية يحتاجان تقييمًا مستقلًا')
group([23], [UI,L+'QuranSearchIndex.kt'], [T+'SafetyRegressionTest.kt'], 'اختيار سورة وآية موجود؛ تقسيم عرض 8 آيات ليس صفحات المدينة')
group([46,56,186], ['app/src/main/res/font/amiri_quran.ttf',UI,'NOTICE.md'], ['Unicode cmap coverage; pending visual inspection'], 'تغطية المحارف لا تثبت سلامة تركيب كل علامة على كل جهاز')
group([61,62,63,64], [L+'ReciterCatalog.kt',L+'RecitationRights.kt'], [T+'ReciterCatalogTest.kt'], 'دليل أسماء وحقول حقوق فقط؛ لا مصادر تسجيلات معتمدة أو معلمين موثقين')
group([66], ['docs/THIRD_PARTY_CONTENT_REGISTER.md',L+'RecitationRights.kt'], [T+'RecitationSafetyTest.kt'], 'حقول المنع موجودة؛ لا وثائق أذون تسجيلات فعلية')
group([69,70,72], [L+'LicensedRecitationPlayer.kt'], [T+'RecitationSafetyTest.kt'], 'محول تشغيل محلي للربط لاحقًا؛ لا تسجيل مرخص لتجربة التكرار والسرعة والاستئناف')
group([86,90,91,92,100], [L+'RecitationAssessment.kt'], [T+'RecitationSafetyTest.kt'], 'واجهة قارئ صوت ومقارنة محدودة مع حالة عدم اليقين؛ لا نموذج تعرف صوتي مستقل التقييم')
group([93,96], [L+'PrivateRecitationPractice.kt',UI], ['app/src/androidTest/java/com/nexvary/hifzai/LocalDataAndAudioTest.kt'], 'تسجيل اختياري مؤقت للاستماع الذاتي؛ لا تصحيح آلي')
group([101,102,103,104,105,106,107,108,110,112,113], [L+'AdaptivePractice.kt',L+'RecallLadder.kt',L+'ProgressStore.kt',UI], [T+'AdaptivePracticeTest.kt',T+'RecallLadderTest.kt',T+'SafetyRegressionTest.kt'], 'بصمة محلية وتوصيات وميزانية؛ فعالية التكيف والمراجعة تحتاج تجربة مستقلة')
group([126,127,130,132,134,135], [L+'TeacherNotebook.kt','app/src/main/java/com/nexvary/hifzai/TeacherNotebookScreen.kt'], ['app/src/androidTest/java/com/nexvary/hifzai/LocalDataAndAudioTest.kt'], 'الجزء المحلي: مواعيد وملاحظات وإقرار ولي الأمر وعدد جلسات وتدوين مشكلة؛ لا تفرغ معلم أو حجز متصل أو صلاحيات حسابات أو بلاغ مرسل')
group([121,122,123,126,127,128,130,131,132,133,134,135,137],
      ['academy/service.py','academy/server.py','academy/console.html','academy/console.js','docs/ACADEMY_SERVICE.md',L+'TeacherNotebook.kt'],
      ['tests/test_academy.py','tests/academy_console_smoke.cjs','.github/workflows/academy-console.yml'],
      'خادم داخلي وقاعدة وصلاحيات ومراجعة بشرية وحجز وخطط وبلاغات ولوحة ويب؛ غير مربوط بـAndroid أو منشور، ولا تحقق فعلي من الهوية أو الوصاية أو المؤهلات؛ القبول النهائي معلق')
group([143,151,152,154,200,213], ['docs/OPEN_SOURCE_RESEARCH.md',L+'RecitationAssessment.kt','docs/RELEASE_GATES.md'], [T+'RecitationSafetyTest.kt'], 'وثائق وحدود معلنة؛ لا تقييم علمي مستقل أو موافقات قانونية موثقة')
group([19,161,162,163,164,165,168,174,175,176,177], ['SECURITY.md','app/src/main/AndroidManifest.xml',L+'PrivateRecitationPractice.kt',L+'TeacherNotebook.kt'], ['app/src/androidTest/java/com/nexvary/hifzai/LocalDataAndAudioTest.kt'], 'تحكم محلي وغياب إذن الإنترنت؛ يحتاج تدقيقًا مستقلًا وقياسًا على أجهزة فعلية')
group([182,183,189], ['.github/workflows/android-ui-proof.yml','tools/verify_screenshot_proof.py'], ['app/src/androidTest/java/com/nexvary/hifzai/HifzUiSmokeTest.kt','tests/test_screenshot_proof.py'], 'صور فعلية فقط بعد نجاح PNG validation؛ ليست مقارنة baseline أو اعتمادًا بصريًا')
group([201,202,205], ['app/build.gradle.kts','.github/workflows/android.yml'], ['Gradle debug build and SHA256'], 'نسخة تطوير غير نهائية؛ APK لا ينشر في المستودع العام')
group([20], ['tools/prepare_review_bundle.py','docs/review/READINESS.json','docs/SCHOLARLY_REVIEW_DOSSIER.md'], ['tests/test_review_bundle_gate.py'], 'بوابة مانعة موجودة؛ الحزمة والطلب النهائي والإصدار المعتمد غير مكتملة')

group([31,32], [L+'QuranPartitions.kt', 'app/src/main/java/com/nexvary/hifzai/PartitionPickerScreen.kt', 'app/src/main/java/com/nexvary/hifzai/RangePracticeScreen.kt'], [T+'QuranPartitionsTest.kt', 'app/src/androidTest/java/com/nexvary/hifzai/HifzUiSmokeTest.kt'], 'نطاق أجزاء وصفحات مثبت؛ تدريب إكمال نهاية الصفحة إلى التالية وأهداف مقاطع مخصصة غير مكتملين')

group([195], ['docs/DEPENDENCY_AUDIT_2026-10-14.json'], ['OSV querybatch on 93 resolved debugRuntimeClasspath coordinates'], 'لا سجلات ثغرات أعيدت وقت الفحص؛ نطاق الفحص لا يشمل plugins أو نظام Android أو تدقيق أمان مستقل')

def main():
    phases = re.findall(r'\*\*Phase (\d{3})\*\* — (.+)', (ROOT/'docs/PHASES_002_220.md').read_text())
    assert [int(n) for n,_ in phases] == list(range(2,221))
    revision = subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    rows=[]
    for number,title in phases:
        n=int(number)
        files,tests,problem = MAP.get(n, ([],[], 'لم يتم إثبات تنفيذ هذا المتطلب؛ يلزم التنفيذ واختبار القبول'))
        commits={}
        for path in files:
            sha=subprocess.check_output(['git','log','-1','--format=%H','--',path],cwd=ROOT,text=True).strip()
            commits[path]=sha or None
        rows.append({'phase':number,'required_function':title,'status':'partial_unaccepted' if files else 'not_implemented_or_not_evidenced',
                     'implementation':files,'implementation_commits':commits,'tests_or_checks':tests,
                     'verification':'Referenced tests must be matched to reports for the exact source revision; no overall completion claim',
                     'problems':problem})
    data={'source_revision_audited':revision,'all_phase_numbers_present':True,'phase_count':len(rows),'final_complete_phases':0,'phases':rows}
    (ROOT/'docs/PHASE_EVIDENCE.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
    lines=['# مصفوفة المراحل 002–220 — أدلة وحالة فعلية','',f'كل المراحل الـ219 موجودة. مراجعة المصدر: `{revision}`.','',
           'الحالة «جزئي» تصف وجود تنفيذ أو جزء من بنيته؛ لا تعني تحقيق كل وظيفة في عنوان المرحلة. لا مربعات إنجاز تُملأ آليًا. البنية القابلة للربط ليست ميزة نهائية. `PHASE_EVIDENCE.json` يحتوي ملفات التنفيذ وCommit لكل ملف والاختبارات والمانع لكل مرحلة.','',
           '| المرحلة | الوظيفة المطلوبة | الحالة | التنفيذ والقيود |','|---|---|---|---|']
    for row in rows:
        implementation=', '.join(Path(p).name for p in row['implementation']) or 'لا دليل تنفيذ'
        lines.append(f"| {row['phase']} | {row['required_function']} | {'جزئي / غير مقبول نهائيًا' if row['implementation'] else 'غير منفذ أو بلا دليل'} | {implementation}؛ {row['problems']} |")
    (ROOT/'docs/PHASE_EVIDENCE.md').write_text('\n'.join(lines)+'\n')

if __name__=='__main__': main()
