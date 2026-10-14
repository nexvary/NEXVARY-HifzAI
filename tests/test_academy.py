"""Real SQLite/HTTP tests with synthetic adults/aliases, never real children."""
import concurrent.futures
import http.client
import json
import tempfile
import threading
import unittest
from pathlib import Path
from academy.service import Academy, AcademyError
from academy.server import make_server


class AcademyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.base = tempfile.TemporaryDirectory()
        cls.path = Path(cls.base.name) / 'fixture.sqlite'
        cls.academy = Academy(cls.path, clock=lambda: 1_800_000_000)
        cls.password = 'synthetic-test-password-2026'
        cls.admin_id = cls.academy.bootstrap_admin('administrator', cls.password)
        cls.admin = cls.academy.login('administrator', cls.password)['token']
        cls.teacher_id = cls.academy.call(cls.admin, 'account_create',
            {'login': 'teacher', 'password': cls.password, 'role': 'teacher'})['id']
        cls.teacher = cls.academy.login('teacher', cls.password)['token']
        cls.guardian_id = cls.academy.call(cls.admin, 'account_create',
            {'login': 'guardian', 'password': cls.password, 'role': 'guardian'})['id']
        cls.guardian = cls.academy.login('guardian', cls.password)['token']
        cls.other_id = cls.academy.call(cls.admin, 'account_create',
            {'login': 'other-guardian', 'password': cls.password, 'role': 'guardian'})['id']
        cls.other = cls.academy.login('other-guardian', cls.password)['token']

    @classmethod
    def tearDownClass(cls):
        cls.base.cleanup()

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        path = Path(self.temp.name) / 'case.sqlite'
        path.write_bytes(self.path.read_bytes())
        self.now = 1_800_000_000
        self.db = Academy(path, clock=lambda: self.now)

    def tearDown(self):
        self.temp.cleanup()

    def call(self, token, action, **data):
        return self.db.call(token, action, data)

    def error(self, expected_status, token, action, **data):
        with self.assertRaises(AcademyError) as raised:
            self.call(token, action, **data)
        self.assertEqual(expected_status, raised.exception.status)

    def student(self):
        return self.call(self.guardian, 'student_create', alias='طالب تجريبي',
                         adult_guardian=True, consent=True)['id']

    def approved(self):
        self.call(self.teacher, 'teacher_submit', alias='معلم تجريبي', qualification='Synthetic qualification claim')
        self.call(self.admin, 'teacher_review', teacher=self.teacher_id, status='approved',
                  evidence='Synthetic human-review test evidence', human_reviewed=True)

    def booked(self):
        student = self.student()
        self.approved()
        self.call(self.guardian, 'enroll', student=student, teacher=self.teacher_id)
        for offset in (3600, 7200):
            self.call(self.teacher, 'availability_add', starts=self.now+offset, ends=self.now+offset+1800)
        lesson = self.call(self.guardian, 'book', student=student, teacher=self.teacher_id,
                           starts=self.now+3600, ends=self.now+5400)['id']
        return student, lesson

    def test_passwords_and_tokens_are_not_stored_plaintext(self):
        with self.db.connection() as db:
            rows = db.execute('SELECT salt,password FROM accounts').fetchall()
            self.assertEqual(4, len({bytes(r['salt']) for r in rows}))
            self.assertTrue(all(len(r['password']) == 32 for r in rows))
            self.assertEqual(0, db.execute('SELECT count(*) FROM sessions WHERE token_hash=?', (self.admin,)).fetchone()[0])
        self.assertNotIn(self.password.encode(), Path(self.db.database).read_bytes())
        self.assertEqual(0o600, Path(self.db.database).stat().st_mode & 0o777)

    def test_unknown_and_wrong_password_share_error_and_rate_limit_persists(self):
        for login in ('unknown-account', 'guardian'):
            for _ in range(5):
                with self.assertRaises(AcademyError) as raised:
                    self.db.login(login, 'wrong-synthetic-password')
                self.assertEqual((401, 'Invalid credentials'), (raised.exception.status, raised.exception.message))
            with self.assertRaises(AcademyError) as raised:
                self.db.login(login, self.password)
            self.assertEqual(429, raised.exception.status)
        self.now += 301
        self.assertIn('token', self.db.login('guardian', self.password))

    def test_session_expiry_logout_and_no_admin_signup(self):
        token = self.db.login('guardian', self.password)['token']
        self.call(token, 'logout')
        self.error(401, token, 'students')
        self.now += 3601
        self.error(401, self.guardian, 'students')
        self.now -= 3601
        self.error(403, self.guardian, 'account_create', login='attacker', password=self.password, role='admin')
        self.error(400, self.admin, 'account_create', login='admin-two', password=self.password, role='admin')
        with self.assertRaises(AcademyError):
            self.db.bootstrap_admin('replacement', self.password)

    def test_human_review_required_and_edits_revoke_approval(self):
        self.call(self.teacher, 'teacher_submit', alias='معلم', qualification='Claim only')
        self.assertEqual([], self.call(self.guardian, 'teachers')['teachers'])
        self.error(403, self.teacher, 'teacher_review', teacher=self.teacher_id, status='approved', evidence='claim', human_reviewed=True)
        self.error(400, self.admin, 'teacher_review', teacher=self.teacher_id, status='approved', evidence='claim', human_reviewed=False)
        self.approved()
        self.assertEqual(1, len(self.call(self.guardian, 'teachers')['teachers']))
        self.call(self.teacher, 'teacher_submit', alias='معلم', qualification='Changed claim')
        self.assertEqual([], self.call(self.guardian, 'teachers')['teachers'])

    def test_guardian_consent_and_cross_account_isolation(self):
        self.error(400, self.guardian, 'student_create', alias='طالب', adult_guardian=True, consent=False)
        self.error(403, self.teacher, 'student_create', alias='طالب', adult_guardian=True, consent=True)
        student = self.student()
        self.assertEqual([], self.call(self.other, 'students')['students'])
        self.error(404, self.other, 'student_export', student=student)
        self.error(404, self.other, 'student_delete', student=student, confirm=True)
        self.error(404, self.teacher, 'lessons', student=student)
        self.error(409, self.guardian, 'enroll', student=student, teacher=self.teacher_id)

    def test_booking_overlap_is_atomic_and_only_guardian_can_book(self):
        student, lesson = self.booked()
        self.error(403, self.teacher, 'book', student=student, teacher=self.teacher_id, starts=self.now+7200, ends=self.now+9000)
        self.error(409, self.guardian, 'book', student=student, teacher=self.teacher_id, starts=self.now+4000, ends=self.now+5000)
        def book():
            try:
                self.call(self.guardian, 'book', student=student, teacher=self.teacher_id, starts=self.now+7200, ends=self.now+9000)
                return 200
            except AcademyError as e:
                return e.status
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            self.assertEqual([200, 409], sorted(pool.map(lambda _: book(), range(2))))
        self.assertEqual(2, len(self.call(self.guardian, 'lessons', student=student)['lessons']))

    def test_lesson_notes_completion_and_complaint_resolution(self):
        student, lesson = self.booked()
        self.error(403, self.guardian, 'lesson_update', lesson=lesson, status='completed', note='claim')
        self.error(409, self.teacher, 'lesson_update', lesson=lesson, status='completed', note='not yet')
        self.now += 5400
        # Reauthenticate after one-hour expiry, as actual callers must do.
        guardian = self.db.login('guardian', self.password)['token']
        teacher = self.db.login('teacher', self.password)['token']
        admin = self.db.login('administrator', self.password)['token']
        self.call(teacher, 'lesson_update', lesson=lesson, status='completed', note='ملاحظة بشرية تجريبية')
        self.assertEqual('completed', self.call(guardian, 'lessons', student=student)['lessons'][0]['status'])
        complaint = self.call(guardian, 'complaint_create', lesson=lesson, note='بلاغ تجريبي')['id']
        self.error(403, teacher, 'complaint_resolve', complaint=complaint, resolution='self close')
        self.call(admin, 'complaint_resolve', complaint=complaint, resolution='تقرير معالجة بشري')
        self.assertEqual('resolved', self.call(guardian, 'complaints')['complaints'][0]['status'])
        exported = self.call(guardian, 'student_export', student=student)
        self.assertEqual(1, len(exported['complaints']))

    def test_unenroll_suspension_and_disable_revoke_access(self):
        student, lesson = self.booked()
        self.call(self.guardian, 'unenroll', student=student, teacher=self.teacher_id)
        self.error(404, self.teacher, 'lessons', student=student)
        self.assertEqual('cancelled', self.call(self.guardian, 'lessons', student=student)['lessons'][0]['status'])
        self.call(self.guardian, 'enroll', student=student, teacher=self.teacher_id)
        self.call(self.admin, 'teacher_review', teacher=self.teacher_id, status='suspended', evidence='human review', human_reviewed=True)
        self.error(404, self.teacher, 'lessons', student=student)
        self.error(409, self.guardian, 'book', student=student, teacher=self.teacher_id, starts=self.now+7200, ends=self.now+9000)
        self.call(self.admin, 'account_disable', account=self.teacher_id)
        self.error(401, self.teacher, 'students')

    def test_disabled_guardian_removes_teacher_access(self):
        student, _ = self.booked()
        self.call(self.admin, 'account_disable', account=self.guardian_id)
        self.error(401, self.guardian, 'students')
        self.error(404, self.teacher, 'lessons', student=student)

    def test_delete_cascades_and_audit_contains_no_private_notes(self):
        student, lesson = self.booked()
        self.call(self.guardian, 'complaint_create', lesson=lesson, note='sensitive-test-note')
        self.call(self.guardian, 'student_delete', student=student, confirm=True)
        with self.db.connection() as db:
            for table in ('students', 'enrollments', 'lessons', 'complaints'):
                self.assertEqual(0, db.execute('SELECT count(*) FROM '+table).fetchone()[0])
        events = self.call(self.admin, 'audit')['events']
        self.assertNotIn('sensitive-test-note', json.dumps(events))
        self.assertNotIn('طالب', json.dumps(events, ensure_ascii=False))
        self.error(404, self.guardian, 'student_export', student=student)

    def test_review_queue_is_admin_only_and_directory_has_no_qualification(self):
        self.approved()
        self.error(403, self.guardian, 'teacher_review_queue')
        self.error(403, self.teacher, 'teacher_review_queue')
        row = self.call(self.admin, 'teacher_review_queue')['teachers'][0]
        self.assertIn('qualification', row)
        self.assertIn('evidence', row)
        self.assertNotIn('qualification', self.call(self.guardian, 'teachers')['teachers'][0])
        self.assertEqual('approved', self.call(self.teacher, 'teacher_profile')['profile']['status'])

    def test_availability_and_schedule_change_preserve_booked_lesson(self):
        student, lesson = self.booked()
        slot = self.call(self.guardian, 'availability', teacher=self.teacher_id)['slots'][0]['id']
        self.error(409, self.teacher, 'availability_remove', slot=slot)
        self.error(403, self.guardian, 'availability_add', starts=self.now+9000, ends=self.now+10800)
        self.error(409, self.teacher, 'availability_add', starts=self.now+4000, ends=self.now+5000)
        self.error(409, self.guardian, 'book', student=student, teacher=self.teacher_id,
                   starts=self.now+12000, ends=self.now+13800)
        self.call(self.teacher, 'lesson_update', lesson=lesson, status='cancelled', note='Schedule change')
        self.call(self.teacher, 'availability_remove', slot=slot)
        self.assertEqual(1, len(self.call(self.guardian, 'availability', teacher=self.teacher_id)['slots']))

    def test_qualification_change_cancels_booking_and_revokes_access(self):
        student, _ = self.booked()
        self.call(self.teacher, 'teacher_submit', alias='معلم', qualification='Changed claim')
        self.error(404, self.teacher, 'lessons', student=student)
        self.assertEqual('cancelled', self.call(self.guardian, 'lessons', student=student)['lessons'][0]['status'])

    def test_password_change_requires_current_password_and_revokes_all_sessions(self):
        second = self.db.login('guardian', self.password)['token']
        self.error(401, self.guardian, 'password_change', current_password='wrong-synthetic-password',
                   new_password='new-synthetic-password-2026')
        self.call(self.guardian, 'password_change', current_password=self.password,
                  new_password='new-synthetic-password-2026')
        self.error(401, self.guardian, 'students')
        self.error(401, second, 'students')
        with self.assertRaises(AcademyError):
            self.db.login('guardian', self.password)
        self.assertIn('token', self.db.login('guardian', 'new-synthetic-password-2026'))

    def test_teacher_plans_validate_real_ayah_addresses_and_distinguish_self_report(self):
        student, _ = self.booked()
        self.error(403, self.guardian, 'plan_create', student=student, chapter=1,
                   first_ayah=1, last_ayah=7, due=self.now+86400)
        self.error(400, self.teacher, 'plan_create', student=student, chapter=1,
                   first_ayah=1, last_ayah=8, due=self.now+86400)
        plan = self.call(self.teacher, 'plan_create', student=student, chapter=1,
                         first_ayah=1, last_ayah=7, due=self.now+86400, instruction='مراجعة بشرية')['id']
        result = self.call(self.guardian, 'plan_observe', plan=plan, result='recalled')
        self.assertEqual('guardian_self_report', result['kind'])
        result = self.call(self.teacher, 'plan_observe', plan=plan, result='needs_review', note='ملاحظة المعلم')
        self.assertEqual('teacher_observation', result['kind'])
        self.error(404, self.other, 'plan_observe', plan=plan, result='recalled')
        self.assertEqual(2, len(self.call(self.guardian, 'plans', student=student)['plans'][0]['observations']))
        exported = self.call(self.guardian, 'student_export', student=student)
        self.assertEqual((1, 2), (len(exported['plans']), len(exported['observations'])))
        self.call(self.guardian, 'student_delete', student=student, confirm=True)
        with self.db.connection() as db:
            self.assertEqual(0, db.execute('SELECT count(*) FROM revision_plans').fetchone()[0])
            self.assertEqual(0, db.execute('SELECT count(*) FROM observations').fetchone()[0])

    def test_http_transport_real_login_and_host_origin_body_guards(self):
        server = make_server(self.db)
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        try:
            def request(body, headers=None, path='/v1/login'):
                conn = http.client.HTTPConnection('127.0.0.1', server.server_port, timeout=10)
                conn.request('POST', path, body, headers or {'Content-Type': 'application/json'})
                response = conn.getresponse()
                result = response.status, json.loads(response.read()), dict(response.getheaders())
                conn.close()
                return result
            body = json.dumps({'login': 'guardian', 'password': self.password})
            status, login, headers = request(body)
            self.assertEqual(200, status)
            self.assertEqual('no-store', headers['Cache-Control'])
            status, result, _ = request(json.dumps({'action': 'students'}),
                {'Content-Type': 'application/json', 'Authorization': 'Bearer '+login['token']}, '/v1/operation')
            self.assertEqual((200, []), (status, result['students']))
            self.assertEqual(403, request(body, {'Content-Type': 'application/json', 'Host': 'evil.test'})[0])
            self.assertEqual(403, request(body, {'Content-Type': 'application/json', 'Origin': 'https://evil.test'})[0])
            self.assertEqual(400, request('{"login":"x","login":"y"}')[0])
            self.assertEqual(413, request('a'*16385)[0])
            self.assertEqual(401, request('{"action":"students"}', path='/v1/operation')[0])
        finally:
            server.shutdown()
            server.server_close()
            worker.join()
