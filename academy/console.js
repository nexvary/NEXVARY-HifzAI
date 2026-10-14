'use strict';
// User strings only enter textContent/value. Tokens are memory-only, never storage.
let session = null, activePage = null, controlId = 0;
const $ = id => document.getElementById(id);
const notice = message => { $('notice').textContent = message; };
const messages = {401:'انتهت الجلسة أو بيانات الدخول غير صحيحة.',403:'لا يملك حسابك صلاحية تنفيذ هذا الإجراء.',404:'السجل غير متاح لهذا الحساب.',409:'تعذر الإجراء بسبب حالة الملف أو تعارض الموعد.',429:'توقّف مؤقتًا قبل محاولة الدخول مجددًا.'};
function el(tag, content, parent, attrs = {}) {
  const node = document.createElement(tag);
  if (content !== null && content !== undefined) node.textContent = content;
  for (const [key,value] of Object.entries(attrs)) node.setAttribute(key,value);
  if (parent) parent.append(node);
  return node;
}
function field(form, title, type='text', required=true, max=500) {
  const label = el('label', title, form);
  const input = el(type==='textarea'?'textarea':'input', null, label);
  if(type!=='textarea') input.type=type;
  input.required=required; input.maxLength=max;
  return input;
}
function choose(form, title, rows, display, value) {
  const id='choice-'+(++controlId);
  el('label',title,form,{'for':id});
  const input=el('select',null,form,{id});
  for(const row of rows) el('option',display(row),input,{value:value(row)});
  input.required=true; return input;
}
function button(parent,title,action) {
  const b=el('button',title,parent,{type:'button'});
  b.addEventListener('click',()=>run(b,action)); return b;
}
async function run(control, action) {
  control.disabled=true; notice('جارٍ تنفيذ الإجراء…');
  try { const result=await action(); notice(result===false?'تم إلغاء الإجراء.':'تم تنفيذ الإجراء.'); }
  catch(error) { notice(error.message || 'تعذر الاتصال؛ حاول مجددًا.'); }
  finally { control.disabled=false; }
}
async function request(path,body,authenticated=true) {
  const headers={'Content-Type':'application/json'};
  if(authenticated) headers.Authorization='Bearer '+session.token;
  const response=await fetch(path,{method:'POST',headers,body:JSON.stringify(body),credentials:'omit',cache:'no-store'});
  const data=await response.json();
  if(!response.ok) {
    if(response.status===401 && authenticated) signedOut();
    throw Error(messages[response.status] || 'بيانات الطلب غير صالحة أو الخدمة غير متاحة.');
  }
  return data;
}
const api=(action,data={})=>request('/v1/operation',{action,data});
function signedOut() {
  session=null; activePage=null; $('workspace').hidden=true; $('login').hidden=false;
  $('page').replaceChildren(); $('navigation').replaceChildren(); $('login-password').value='';
}
function form(parent,title,submit) {
  const f=el('form',null,parent), b=el('button',title,f,{type:'submit'});
  f.addEventListener('submit',event=>{event.preventDefault(); run(b,submit);});
  return f;
}
function card(parent,title) {const c=el('div',null,parent,{class:'card'});el('h3',title,c);return c;}
const date=seconds=>new Date(seconds*1000).toLocaleString('ar',{dateStyle:'medium',timeStyle:'short'});
const epoch=value=>Math.floor(new Date(value).getTime()/1000);
function page(title) {const p=$('page');p.replaceChildren();el('h1',title,p);return p;}
function tabs() {
  $('navigation').replaceChildren();
  const routes=session.role==='admin' ? [['المعلمون',reviews],['الحسابات',accounts],['البلاغات',complaints],['سجل التدقيق',audit]]
    : session.role==='teacher' ? [['ملفي',teacherProfile],['مواعيدي',teacherSlots],['طلابي',students],['البلاغات',complaints]]
    : [['طلابي',students],['المعلمون',directory],['البلاغات',complaints]];
  for(const [title,route] of routes) button($('navigation'),title,async()=>{activePage=route;await route();});
  button($('navigation'),'كلمة المرور',passwordPage);
  button($('navigation'),'خروج',async()=>{await api('logout');signedOut();});
  activePage=routes[0][1]; return activePage();
}
$('login-form').addEventListener('submit',event=>{
  event.preventDefault();const b=event.submitter;
  run(b,async()=>{
    const password=$('login-password').value; $('login-password').value='';
    session=await request('/v1/login',{login:$('login-name').value,password},false);
    $('login').hidden=true; $('workspace').hidden=false; await tabs();
  });
});
async function passwordPage() {
  const p=page('تغيير كلمة المرور');el('p','ستحتاج إلى الدخول مجددًا من جميع أجهزتك.',p);
  let current,next;
  const f=form(p,'تغيير كلمة المرور',async()=>{
    const data={current_password:current.value,new_password:next.value};current.value='';next.value='';
    await api('password_change',data);signedOut();
  });
  current=field(f,'كلمة المرور الحالية','password');current.autocomplete='current-password';
  next=field(f,'كلمة مرور جديدة من 15 إلى 128 حرفًا','password',true,128);next.minLength=15;next.autocomplete='new-password';
}
async function reviews() {
  const p=page('مراجعة مؤهلات المعلمين');el('p','هذه ادعاءات مقدمة للفحص البشري. زر الموافقة لا يتحقق من الشهادة أو يمنح إجازة.',p,{class:'warning'});
  const rows=(await api('teacher_review_queue')).teachers;
  if(!rows.length)el('p','لم تُقدّم ملفات بعد.',p);
  for(const row of rows) {
    const c=card(p,row.alias);el('p','الحالة: '+({pending:'معلق',approved:'مقبول داخليًا',suspended:'موقوف'}[row.status]),c);
    el('p','المؤهل المُدّعى: '+row.qualification,c);
    if(row.evidence)el('p','مرجع المراجعة السابقة: '+row.evidence,c);
    let evidence,checked,state;
    const f=form(c,'توثيق قرار المراجعة',async()=>{await api('teacher_review',{teacher:row.id,status:state.value,evidence:evidence.value,human_reviewed:checked.checked});await reviews();});
    evidence=field(f,'مرجع تحقق بشري خاص — دون أسرار أو أرقام هوية','textarea');
    checked=field(f,'أقر بأنني راجعت الأدلة البشرية المخوّل بفحصها','checkbox');
    state=choose(f,'القرار',[{id:'approved',label:'قبول داخلي'},{id:'suspended',label:'إيقاف'}],r=>r.label,r=>r.id);
  }
}
async function accounts() {
  const p=page('إدارة حسابات البالغين');let login,password,role;
  const f=form(p,'إنشاء حساب',async()=>{const data={login:login.value,password:password.value,role:role.value};password.value='';await api('account_create',data);await accounts();});
  login=field(f,'اسم الحساب','text',true,80);
  password=field(f,'كلمة مرور أولية خاصة من 15 إلى 128 حرفًا','password',true,128);password.minLength=15;password.autocomplete='new-password';
  role=choose(f,'الدور',[{id:'guardian',label:'ولي أمر'},{id:'teacher',label:'معلم'}],r=>r.label,r=>r.id);
  for(const row of (await api('accounts')).accounts) {
    const c=card(p,row.login);el('p',(row.role==='teacher'?'معلم':row.role==='guardian'?'ولي أمر':'مدير')+' • '+(row.active?'نشط':'موقوف'),c);
    if(row.active && row.role!=='admin')button(c,'إيقاف الحساب وقطع الوصول',async()=>{if(!confirm('إيقاف هذا الحساب وإلغاء الحجوزات التابعة؟'))return false;await api('account_disable',{account:row.id});await accounts();});
  }
}
async function teacherProfile() {
  const p=page('ملف المعلم');el('p','تعديل الملف يعيد المراجعة ويُلغي الحجوزات الحالية. لا تعرض مؤهلات غير صحيحة.',p,{class:'warning'});
  const profile=(await api('teacher_profile')).profile;
  if(profile)el('p','الحالة: '+({pending:'بانتظار المراجعة',approved:'مقبول داخليًا',suspended:'موقوف'}[profile.status]),p);
  let alias,qualification;
  const f=form(p,'تقديم الملف للمراجعة',async()=>{await api('teacher_submit',{alias:alias.value,qualification:qualification.value});await teacherProfile();});
  alias=field(f,'اسم العرض','text',true,60);qualification=field(f,'وصف المؤهل وطريقة التحقق منه','textarea');
  if(profile){alias.value=profile.alias;qualification.value=profile.qualification;}
}
async function teacherSlots() {
  const p=page('مواعيد تفرغ المعلم');el('p','الفترة من 5 إلى 120 دقيقة. لا يمكن حذف موعد محجوز قبل إلغاء جلسته.',p);
  let start,end;
  const f=form(p,'إضافة وقت متاح',async()=>{await api('availability_add',{starts:epoch(start.value),ends:epoch(end.value)});await teacherSlots();});
  start=field(f,'البداية بتوقيت جهازك','datetime-local');end=field(f,'النهاية بتوقيت جهازك','datetime-local');
  // Session identity is returned at login; no public account lookup is needed.
  for(const slot of (await api('availability',{teacher:session.account})).slots) {
    const c=card(p,date(slot.starts)+' — '+date(slot.ends));
    button(c,'حذف الموعد',async()=>{await api('availability_remove',{slot:slot.id});await teacherSlots();});
  }
}
async function directory() {
  const p=page('دليل المعلمين');el('p','مقبولون وفق مراجعة الإدارة الداخلية. لا يعني ذلك إجازة شرعية أو اعتماد لجنة العلماء.',p,{class:'warning'});
  for(const row of (await api('teachers')).teachers) {
    const c=card(p,row.alias);button(c,'عرض المواعيد',async()=>{
      const slots=(await api('availability',{teacher:row.id})).slots;
      const list=el('div',null,c);if(!slots.length)el('p','لا مواعيد متاحة.',list);
      for(const slot of slots)el('p',date(slot.starts)+' — '+date(slot.ends),list);
    });
  }
}
async function students() {
  const p=page(session.role==='guardian'?'ملفات طلابي':'الطلاب المرتبطون بي');
  if(session.role==='guardian') {
    let alias,adult,consent;
    const f=form(p,'إضافة طالب باسم مستعار',async()=>{await api('student_create',{alias:alias.value,adult_guardian:adult.checked,consent:consent.checked});await students();});
    alias=field(f,'اسم مستعار — دون بيانات هوية','text',true,40);
    adult=field(f,'أنا ولي أمر بالغ مخوّل بإدارة هذا الملف','checkbox');
    consent=field(f,'أوافق على حفظ هذا الملف والملاحظات داخل بيئة الفحص الخاصة','checkbox');
  }
  const rows=(await api('students')).students;
  if(!rows.length)el('p','لا ملفات مرتبطة بحسابك.',p);
  for(const row of rows)button(card(p,row.alias),'خطة الطالب وجلساته',()=>studentPage(row));
}
async function studentPage(student) {
  const p=page(student.alias);button(p,'رجوع إلى الطلاب',students);
  if(session.role==='guardian') {
    const rows=(await api('teachers')).teachers;
    if(rows.length) {
      let teacher;const f=form(p,'ربط بمعلم وعرض مواعيده',async()=>{
        await api('enroll',{student:student.id,teacher:teacher.value});await booking(p,student,teacher.value);
      });teacher=choose(f,'المعلم',rows,r=>r.alias,r=>r.id);
      button(p,'إلغاء الربط بالمعلم المختار',async()=>{if(!confirm('سيُقطع وصول هذا المعلم وتُلغى الحجوزات التابعة. متابعة؟'))return false;await api('unenroll',{student:student.id,teacher:teacher.value});await studentPage(student);});
    }
    button(p,'تنزيل ملف هذا الطالب',async()=>{
      const data=await api('student_export',{student:student.id});
      const url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json'}));
      const link=el('a',null,null,{href:url,download:'student-private-export.json'});link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
    });
    button(p,'حذف الطالب وكل بياناته التابعة',async()=>{if(!confirm('حذف الملف والجلسات والملاحظات والبلاغات نهائيًا من القاعدة؟'))return false;await api('student_delete',{student:student.id,confirm:true});await students();});
  }
  el('h2','الجلسات',p);
  for(const lesson of (await api('lessons',{student:student.id})).lessons) {
    const c=card(p,date(lesson.starts));el('p','الحالة: '+({booked:'محجوزة',completed:'مكتملة',cancelled:'ملغاة'}[lesson.status]),c);
    if(lesson.note)el('p',lesson.note,c);
    if(lesson.status==='booked') {
      button(c,'إلغاء الجلسة',async()=>{await api('lesson_update',{lesson:lesson.id,status:'cancelled',note:''});await studentPage(student);});
      if(session.role==='teacher') {
        let note;const f=form(c,'توثيق إتمام الجلسة',async()=>{await api('lesson_update',{lesson:lesson.id,status:'completed',note:note.value});await studentPage(student);});
        note=field(f,'ملاحظة المعلم بعد نهاية الجلسة','textarea',false);
      }
    }
    let complaint;const f=form(c,'تقديم بلاغ خاص',async()=>{await api('complaint_create',{lesson:lesson.id,note:complaint.value});complaint.value='';});
    complaint=field(f,'وصف المشكلة','textarea');
  }
  await plans(p,student);
}
async function booking(parent,student,teacher) {
  const slots=(await api('availability',{teacher})).slots;
  const c=card(parent,'اختيار الموعد');if(!slots.length){el('p','لا تفرغ معلن حاليًا.',c);return;}
  let chosen;const f=form(c,'حجز الفترة المختارة',async()=>{const slot=slots.find(s=>s.id===chosen.value);await api('book',{student:student.id,teacher,starts:slot.starts,ends:slot.ends});await studentPage(student);});
  chosen=choose(f,'المواعيد بتوقيت جهازك',slots,s=>date(s.starts)+' — '+date(s.ends),s=>s.id);
  el('small','يؤكد الخادم التوافر عند الحجز؛ قد تكون الفترة حُجزت للتو.',c);
}
async function plans(parent,student) {
  el('h2','خطط المراجعة والمتابعة',parent);
  if(session.role==='teacher') {
    let chapter,first,last,due,instruction;
    const f=form(parent,'تعيين خطة مراجعة',async()=>{await api('plan_create',{student:student.id,chapter:Number(chapter.value),first_ayah:Number(first.value),last_ayah:Number(last.value),due:epoch(due.value),instruction:instruction.value});await studentPage(student);});
    chapter=field(f,'رقم السورة','number');chapter.min=1;chapter.max=114;
    first=field(f,'من الآية','number');first.min=1;
    last=field(f,'إلى الآية','number');last.min=1;
    due=field(f,'موعد المراجعة','datetime-local');instruction=field(f,'تعليمات المعلم','textarea',false);
  }
  for(const plan of (await api('plans',{student:student.id})).plans) {
    const c=card(parent,'سورة '+plan.chapter+' • الآيات '+plan.first_ayah+'–'+plan.last_ayah);
    el('p','الموعد: '+date(plan.due),c);el('p',plan.instruction,c);
    for(const o of plan.observations)el('p',(o.kind==='teacher_observation'?'ملاحظة معلم':'تقييم ولي أمر ذاتي')+' • '+(o.result==='recalled'?'استرجع':'يحتاج مراجعة')+' • '+o.note,c);
    let result,note;const f=form(c,'حفظ المتابعة البشرية',async()=>{await api('plan_observe',{plan:plan.id,result:result.value,note:note.value});await studentPage(student);});
    result=choose(f,'النتيجة',[{id:'needs_review',label:'يحتاج مراجعة'},{id:'recalled',label:'استرجع'}],r=>r.label,r=>r.id);
    note=field(f,'ملاحظة اختيارية','textarea',false);
  }
}
async function complaints() {
  const p=page('البلاغات الخاصة');
  const rows=(await api('complaints')).complaints;if(!rows.length)el('p','لا بلاغات متاحة لهذا الحساب.',p);
  for(const row of rows){const c=card(p,row.status==='open'?'بلاغ مفتوح':'بلاغ تمت معالجته');el('p',row.note,c);
    if(row.resolution)el('p','المعالجة: '+row.resolution,c);
    if(session.role==='admin' && row.status==='open'){
      let resolution;const f=form(c,'توثيق المعالجة البشرية',async()=>{await api('complaint_resolve',{complaint:row.id,resolution:resolution.value});await complaints();});
      resolution=field(f,'نتيجة معالجة البلاغ','textarea');
    }
  }
}
async function audit() {const p=page('سجل التدقيق');el('p','معرفات وأفعال فقط. آخر 200 حدث دون كلمات مرور أو محتوى ملاحظات.',p);
  for(const row of (await api('audit')).events){const c=card(p,date(row.at));el('p',row.action,c);el('pre','actor: '+row.actor+'\ntarget: '+row.target,c);}
}
