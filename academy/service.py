"""SQLite-backed academy with authorization on every operation.

The administrator is provisioned offline, never via public signup. Child profiles
are aliases managed by an adult guardian; no child credentials/audio are accepted.
This is an internal integration candidate, not an audited production service.
"""
import contextlib
import hashlib
import hmac
import secrets
import sqlite3
import time
from pathlib import Path


class AcademyError(Exception):
    def __init__(self, status, message):
        self.status, self.message = status, message
        super().__init__(message)


def fail(condition, status=400, message="Invalid request"):
    if not condition:
        raise AcademyError(status, message)


def text(value, maximum=500, minimum=1):
    fail(isinstance(value, str) and minimum <= len(value.strip()) <= maximum)
    return value.strip()


def password_digest(password, salt):
    # OWASP minimum scrypt N=2^17, r=8, p=1; no plaintext passwords retained.
    fail(isinstance(password, str) and 15 <= len(password) <= 128)
    return hashlib.scrypt(password.encode("utf-8"), salt=salt, n=2**17,
                          r=8, p=1, dklen=32, maxmem=256*1024*1024)


SCHEMA = """
CREATE TABLE IF NOT EXISTS accounts(
 id TEXT PRIMARY KEY, login TEXT UNIQUE NOT NULL, role TEXT NOT NULL
 CHECK(role IN ('admin','teacher','guardian')), salt BLOB NOT NULL,
 password BLOB NOT NULL, active INTEGER NOT NULL DEFAULT 1);
CREATE TABLE IF NOT EXISTS sessions(
 token_hash TEXT PRIMARY KEY, account TEXT REFERENCES accounts(id) ON DELETE CASCADE,
 expires INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS login_limits(
 key TEXT PRIMARY KEY, failures INTEGER NOT NULL, until_time INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS teachers(
 account TEXT PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
 alias TEXT NOT NULL, qualification TEXT NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('pending','approved','suspended')),
 reviewed_by TEXT REFERENCES accounts(id), evidence TEXT NOT NULL DEFAULT '');
CREATE TABLE IF NOT EXISTS students(
 id TEXT PRIMARY KEY, guardian TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 alias TEXT NOT NULL, consent_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS enrollments(
 student TEXT REFERENCES students(id) ON DELETE CASCADE,
 teacher TEXT REFERENCES teachers(account) ON DELETE CASCADE,
 PRIMARY KEY(student,teacher));
CREATE TABLE IF NOT EXISTS availability(
 id TEXT PRIMARY KEY, teacher TEXT NOT NULL REFERENCES teachers(account) ON DELETE CASCADE,
 starts INTEGER NOT NULL, ends INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS lessons(
 id TEXT PRIMARY KEY, student TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
 teacher TEXT NOT NULL REFERENCES teachers(account) ON DELETE CASCADE,
 starts INTEGER NOT NULL, ends INTEGER NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('booked','completed','cancelled')),
 note TEXT NOT NULL DEFAULT '');
CREATE TABLE IF NOT EXISTS revision_plans(
 id TEXT PRIMARY KEY, student TEXT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
 teacher TEXT NOT NULL REFERENCES teachers(account) ON DELETE CASCADE,
 chapter INTEGER NOT NULL, first_ayah INTEGER NOT NULL, last_ayah INTEGER NOT NULL,
 due INTEGER NOT NULL, instruction TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS observations(
 id TEXT PRIMARY KEY, plan TEXT NOT NULL REFERENCES revision_plans(id) ON DELETE CASCADE,
 author TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 kind TEXT NOT NULL CHECK(kind IN ('guardian_self_report','teacher_observation')),
 result TEXT NOT NULL CHECK(result IN ('recalled','needs_review')), note TEXT NOT NULL, at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS complaints(
 id TEXT PRIMARY KEY, lesson TEXT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
 reporter TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 note TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'open', resolution TEXT NOT NULL DEFAULT '');
CREATE TABLE IF NOT EXISTS audit(
 id INTEGER PRIMARY KEY, actor TEXT NOT NULL, action TEXT NOT NULL,
 target TEXT NOT NULL, at INTEGER NOT NULL);
"""


class Academy:
    def __init__(self, database, clock=time.time):
        self.database, self.clock = str(database), clock
        metadata = (Path(__file__).resolve().parents[1] /
                    'app/src/main/assets/quran/surah-metadata.txt').read_bytes()
        fail(hashlib.sha256(metadata).hexdigest() ==
             'ec86874d26dd3bcd5474b9dd6afb92541484e13b8c8d0da8138e070885bc5b51',
             500, 'Quran address metadata integrity failed')
        self.ayah_counts = [int(line.split('|')[2]) for line in metadata.decode('utf-8').splitlines()]
        fail(len(self.ayah_counts) == 114 and sum(self.ayah_counts) == 6236, 500)
        Path(self.database).parent.mkdir(parents=True, exist_ok=True)
        with self.connection() as db:
            db.executescript(SCHEMA)
        Path(self.database).chmod(0o600)

    @contextlib.contextmanager
    def connection(self):
        db = sqlite3.connect(self.database, timeout=10, isolation_level=None)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA foreign_keys=ON")
        try:
            db.execute("BEGIN IMMEDIATE")
            yield db
            if db.in_transaction:
                db.commit()
        except BaseException:
            if db.in_transaction:
                db.rollback()
            raise
        finally:
            db.close()

    def now(self):
        return int(self.clock())

    def audit(self, db, actor, action, target):
        # No passwords, tokens, aliases, lesson notes or complaint text in audit.
        db.execute("INSERT INTO audit(actor,action,target,at) VALUES(?,?,?,?)",
                   (actor, action, target, self.now()))

    def _account(self, db, login, password, role):
        login = text(login, 80).lower()
        fail(role in ('admin', 'teacher', 'guardian'))
        salt = secrets.token_bytes(16)
        digest = password_digest(password, salt)
        account = secrets.token_hex(16)
        try:
            db.execute("INSERT INTO accounts(id,login,role,salt,password) VALUES(?,?,?,?,?)",
                       (account, login, role, salt, digest))
        except sqlite3.IntegrityError:
            raise AcademyError(409, "Account cannot be created") from None
        return account

    def bootstrap_admin(self, login, password):
        with self.connection() as db:
            fail(db.execute("SELECT 1 FROM accounts LIMIT 1").fetchone() is None, 409,
                 "Administrator already provisioned")
            return self._account(db, login, password, 'admin')

    def login(self, login, password):
        login = text(login, 80).lower()
        # Persistent per-login throttling. Loopback HTTP adapter also serializes
        # requests and bounds input; production needs independent IP/global limits.
        key = hashlib.sha256(login.encode()).hexdigest()
        invalid = False
        with self.connection() as db:
            limit = db.execute("SELECT * FROM login_limits WHERE key=?", (key,)).fetchone()
            fail(not limit or limit['until_time'] <= self.now(), 429, "Try again later")
            row = db.execute("SELECT * FROM accounts WHERE login=?", (login,)).fetchone()
            salt = row['salt'] if row else bytes(16)
            # Equal KDF work for absent accounts; malformed passwords also fail generically.
            candidate = password if isinstance(password, str) and 15 <= len(password) <= 128 else 'invalid-password-value'
            digest = password_digest(candidate, salt)
            valid = (isinstance(password, str) and 15 <= len(password) <= 128 and row
                     and row['active'] and hmac.compare_digest(digest, row['password']))
            if not valid:
                failures = (limit['failures'] if limit else 0) + 1
                db.execute("INSERT OR REPLACE INTO login_limits VALUES(?,?,?)",
                           (key, failures, self.now() + 300 if failures >= 5 else 0))
                invalid = True
            else:
                db.execute("DELETE FROM login_limits WHERE key=?", (key,))
                db.execute("DELETE FROM sessions WHERE expires<=?", (self.now(),))
                token = secrets.token_urlsafe(32)
                db.execute("INSERT INTO sessions VALUES(?,?,?)",
                           (hashlib.sha256(token.encode()).hexdigest(), row['id'], self.now()+3600))
                self.audit(db, row['id'], 'login', row['id'])
                result = {'token': token, 'expires': self.now()+3600, 'role': row['role'], 'account': row['id']}
        # Raise after transaction so failed attempts are not rolled back.
        if invalid:
            raise AcademyError(401, "Invalid credentials")
        return result

    def actor(self, db, token):
        fail(isinstance(token, str) and 20 <= len(token) <= 100, 401, "Authentication required")
        row = db.execute("SELECT a.* FROM sessions s JOIN accounts a ON a.id=s.account "
                         "WHERE s.token_hash=? AND s.expires>? AND a.active=1",
                         (hashlib.sha256(token.encode()).hexdigest(), self.now())).fetchone()
        fail(row is not None, 401, "Authentication required")
        return row

    def student(self, db, actor, student_id):
        row = db.execute("SELECT * FROM students WHERE id=?", (text(student_id, 32),)).fetchone()
        fail(row is not None, 404, "Record unavailable")
        permitted = actor['role'] == 'admin' or row['guardian'] == actor['id']
        if actor['role'] == 'teacher':
            permitted = db.execute("SELECT 1 FROM enrollments e JOIN teachers t ON t.account=e.teacher "
                                   "WHERE e.student=? AND e.teacher=? AND t.status='approved'",
                                   (student_id, actor['id'])).fetchone() is not None
        fail(permitted, 404, "Record unavailable")
        return row

    def lesson(self, db, actor, lesson_id):
        row = db.execute("SELECT * FROM lessons WHERE id=?", (text(lesson_id, 32),)).fetchone()
        fail(row is not None, 404, "Record unavailable")
        self.student(db, actor, row['student'])
        fail(actor['role'] != 'teacher' or row['teacher'] == actor['id'], 404, "Record unavailable")
        return row

    def call(self, token, action, data):
        fail(isinstance(data, dict) and isinstance(action, str))
        with self.connection() as db:
            actor = self.actor(db, token)
            aid, role = actor['id'], actor['role']
            if action == 'logout':
                db.execute("DELETE FROM sessions WHERE token_hash=?", (hashlib.sha256(token.encode()).hexdigest(),))
                return {'ok': True}
            if action == 'password_change':
                current = data.get('current_password')
                fail(isinstance(current, str) and 15 <= len(current) <= 128, 401, "Invalid credentials")
                fail(hmac.compare_digest(password_digest(current, actor['salt']), actor['password']),
                     401, "Invalid credentials")
                salt = secrets.token_bytes(16)
                digest = password_digest(data.get('new_password'), salt)
                db.execute("UPDATE accounts SET salt=?,password=? WHERE id=?", (salt, digest, aid))
                db.execute("DELETE FROM sessions WHERE account=?", (aid,))
                self.audit(db, aid, action, aid)
                return {'ok': True, 'reauthenticate': True}
            if action == 'accounts':
                fail(role == 'admin', 403, "Administrator required")
                return {'accounts': [dict(r) for r in db.execute(
                    "SELECT id,login,role,active FROM accounts ORDER BY login")]}
            if action == 'account_create':
                fail(role == 'admin', 403, "Administrator required")
                fail(data.get('role') in ('teacher', 'guardian'))
                target = self._account(db, data.get('login'), data.get('password'), data['role'])
                self.audit(db, aid, action, target)
                return {'id': target}
            if action == 'teacher_submit':
                fail(role == 'teacher', 403, "Teacher required")
                alias, qualification = text(data.get('alias'), 60), text(data.get('qualification'), 500)
                # Edits revoke approval and require another human review.
                db.execute("INSERT INTO teachers(account,alias,qualification,status) VALUES(?,?,?,'pending') "
                           "ON CONFLICT(account) DO UPDATE SET alias=excluded.alias,qualification=excluded.qualification,"
                           "status='pending',reviewed_by=NULL,evidence=''", (aid, alias, qualification))
                db.execute("UPDATE lessons SET status='cancelled' WHERE teacher=? AND status='booked'", (aid,))
                self.audit(db, aid, action, aid)
                return {'status': 'pending'}
            if action == 'teacher_review':
                fail(role == 'admin', 403, "Administrator required")
                teacher = text(data.get('teacher'), 32)
                fail(db.execute("SELECT 1 FROM teachers WHERE account=?", (teacher,)).fetchone(), 404)
                fail(data.get('status') in ('approved', 'suspended'))
                evidence = text(data.get('evidence'), 500)
                fail(data.get('human_reviewed') is True, 400, "Human verification required")
                db.execute("UPDATE teachers SET status=?,reviewed_by=?,evidence=? WHERE account=?",
                           (data['status'], aid, evidence, teacher))
                if data['status'] == 'suspended':
                    db.execute("UPDATE lessons SET status='cancelled' WHERE teacher=? AND status='booked'", (teacher,))
                self.audit(db, aid, action, teacher)
                return {'status': data['status']}
            if action == 'teacher_review_queue':
                fail(role == 'admin', 403, "Administrator required")
                return {'teachers': [dict(r) for r in db.execute(
                    "SELECT account AS id,alias,qualification,status,reviewed_by,evidence FROM teachers ORDER BY alias")]}
            if action == 'teacher_profile':
                fail(role == 'teacher', 403, "Teacher required")
                row = db.execute("SELECT alias,qualification,status FROM teachers WHERE account=?", (aid,)).fetchone()
                return {'profile': dict(row) if row else None}
            if action == 'availability_add':
                fail(role == 'teacher', 403, "Teacher required")
                fail(db.execute("SELECT 1 FROM teachers WHERE account=? AND status='approved'", (aid,)).fetchone(), 409)
                start, end = data.get('starts'), data.get('ends')
                fail(type(start) is int and type(end) is int and self.now() < start <= self.now()+366*86400
                     and 5*60 <= end-start <= 120*60)
                fail(db.execute("SELECT 1 FROM availability WHERE teacher=? AND starts<? AND ends>?",
                                (aid, end, start)).fetchone() is None, 409, "Availability conflict")
                target = secrets.token_hex(16)
                db.execute("INSERT INTO availability VALUES(?,?,?,?)", (target, aid, start, end))
                self.audit(db, aid, action, target)
                return {'id': target}
            if action == 'availability':
                teacher = text(data.get('teacher'), 32)
                fail(db.execute("SELECT 1 FROM teachers WHERE account=? AND status='approved'", (teacher,)).fetchone(), 404)
                return {'slots': [dict(r) for r in db.execute(
                    "SELECT id,starts,ends FROM availability WHERE teacher=? AND starts>? ORDER BY starts",
                    (teacher, self.now()))]}
            if action == 'availability_remove':
                fail(role == 'teacher', 403, "Teacher required")
                slot = db.execute("SELECT * FROM availability WHERE id=? AND teacher=?",
                                  (text(data.get('slot'), 32), aid)).fetchone()
                fail(slot is not None, 404)
                # A booked lesson cannot disappear silently from a schedule.
                fail(db.execute("SELECT 1 FROM lessons WHERE teacher=? AND status='booked' AND starts<? AND ends>?",
                                (aid, slot['ends'], slot['starts'])).fetchone() is None, 409, "Cancel booked lessons first")
                db.execute("DELETE FROM availability WHERE id=?", (slot['id'],))
                self.audit(db, aid, action, slot['id'])
                return {'ok': True}
            if action == 'teachers':
                # Guardian directory exposes reviewed alias only; no private qualifications/evidence.
                rows = db.execute("SELECT account AS id,alias,status FROM teachers " +
                                  ("ORDER BY alias" if role == 'admin' else "WHERE status='approved' ORDER BY alias")).fetchall()
                return {'teachers': [dict(r) for r in rows]}
            if action == 'student_create':
                fail(role == 'guardian', 403, "Guardian required")
                fail(data.get('adult_guardian') is True and data.get('consent') is True,
                     400, "Adult guardian consent required")
                fail(db.execute("SELECT count(*) FROM students WHERE guardian=?", (aid,)).fetchone()[0] < 50)
                target = secrets.token_hex(16)
                db.execute("INSERT INTO students VALUES(?,?,?,?)", (target, aid, text(data.get('alias'), 40), self.now()))
                self.audit(db, aid, action, target)
                return {'id': target}
            if action == 'students':
                if role == 'admin':
                    rows = db.execute("SELECT id,alias FROM students ORDER BY alias")
                elif role == 'guardian':
                    rows = db.execute("SELECT id,alias FROM students WHERE guardian=? ORDER BY alias", (aid,))
                else:
                    rows = db.execute("SELECT s.id,s.alias FROM students s JOIN enrollments e ON e.student=s.id "
                                      "JOIN teachers t ON t.account=e.teacher WHERE e.teacher=? AND t.status='approved'", (aid,))
                return {'students': [dict(r) for r in rows]}
            if action == 'enroll':
                student = self.student(db, actor, data.get('student'))
                fail(student['guardian'] == aid, 403, "Guardian required")
                teacher = text(data.get('teacher'), 32)
                fail(db.execute("SELECT 1 FROM teachers WHERE account=? AND status='approved'", (teacher,)).fetchone(), 409,
                     "Teacher is not approved")
                db.execute("INSERT OR IGNORE INTO enrollments VALUES(?,?)", (student['id'], teacher))
                self.audit(db, aid, action, student['id'])
                return {'ok': True}
            if action == 'unenroll':
                student = self.student(db, actor, data.get('student'))
                fail(student['guardian'] == aid, 403, "Guardian required")
                teacher = text(data.get('teacher'), 32)
                db.execute("DELETE FROM enrollments WHERE student=? AND teacher=?", (student['id'], teacher))
                db.execute("UPDATE lessons SET status='cancelled' WHERE student=? AND teacher=? AND status='booked'",
                           (student['id'], teacher))
                self.audit(db, aid, action, student['id'])
                return {'ok': True}
            if action == 'book':
                student = self.student(db, actor, data.get('student'))
                fail(student['guardian'] == aid, 403, "Guardian required")
                teacher = text(data.get('teacher'), 32)
                fail(db.execute("SELECT 1 FROM enrollments e JOIN teachers t ON t.account=e.teacher "
                                "WHERE e.student=? AND e.teacher=? AND t.status='approved'", (student['id'], teacher)).fetchone(), 409)
                start, end = data.get('starts'), data.get('ends')
                fail(type(start) is int and type(end) is int and self.now() < start <= self.now()+366*86400
                     and 5*60 <= end-start <= 120*60)
                fail(db.execute("SELECT 1 FROM availability WHERE teacher=? AND starts<=? AND ends>=?",
                                (teacher, start, end)).fetchone() is not None, 409, "Outside teacher availability")
                overlap = db.execute("SELECT 1 FROM lessons WHERE status!='cancelled' AND (teacher=? OR student=?) "
                                     "AND starts<? AND ends>?", (teacher, student['id'], end, start)).fetchone()
                fail(overlap is None, 409, "Schedule conflict")
                target = secrets.token_hex(16)
                db.execute("INSERT INTO lessons(id,student,teacher,starts,ends,status) VALUES(?,?,?,?,?,'booked')",
                           (target, student['id'], teacher, start, end))
                self.audit(db, aid, action, target)
                return {'id': target}
            if action in ('lessons', 'student_export', 'student_delete'):
                student = self.student(db, actor, data.get('student'))
                if action != 'lessons':
                    fail(student['guardian'] == aid, 403, "Guardian required")
                if action == 'student_delete':
                    fail(data.get('confirm') is True)
                    db.execute("DELETE FROM students WHERE id=?", (student['id'],))
                    self.audit(db, aid, action, student['id'])
                    return {'ok': True}
                sql, args = "SELECT * FROM lessons WHERE student=?", [student['id']]
                if role == 'teacher':
                    sql += " AND teacher=?"
                    args.append(aid)
                lessons = [dict(r) for r in db.execute(sql+" ORDER BY starts", args)]
                result = {'lessons': lessons}
                if action == 'student_export':
                    result['student'] = dict(student)
                    result['plans'] = [dict(r) for r in db.execute(
                        "SELECT * FROM revision_plans WHERE student=? ORDER BY due", (student['id'],))]
                    result['observations'] = [dict(r) for r in db.execute(
                        "SELECT o.* FROM observations o JOIN revision_plans p ON p.id=o.plan WHERE p.student=?",
                        (student['id'],))]
                    result['complaints'] = [dict(r) for r in db.execute(
                        "SELECT c.* FROM complaints c JOIN lessons l ON l.id=c.lesson WHERE l.student=?", (student['id'],))]
                return result
            if action in ('lesson_update', 'complaint_create'):
                lesson = self.lesson(db, actor, data.get('lesson'))
                if action == 'lesson_update':
                    status = data.get('status')
                    fail(status in ('completed', 'cancelled'))
                    fail(lesson['status'] == 'booked', 409, "Lesson already closed")
                    if status == 'completed':
                        fail(role == 'teacher' and lesson['teacher'] == aid, 403, "Assigned teacher required")
                        fail(self.now() >= lesson['ends'], 409, "Lesson has not ended")
                    note = text(data.get('note', ''), 500, 0)
                    db.execute("UPDATE lessons SET status=?,note=? WHERE id=?", (status, note, lesson['id']))
                    self.audit(db, aid, action, lesson['id'])
                    return {'ok': True}
                target = secrets.token_hex(16)
                db.execute("INSERT INTO complaints(id,lesson,reporter,note) VALUES(?,?,?,?)",
                           (target, lesson['id'], aid, text(data.get('note'), 500)))
                self.audit(db, aid, action, target)
                return {'id': target}
            if action == 'plan_create':
                student = self.student(db, actor, data.get('student'))
                fail(role == 'teacher', 403, "Assigned teacher required")
                chapter, first, last, due = (data.get(k) for k in ('chapter', 'first_ayah', 'last_ayah', 'due'))
                fail(all(type(v) is int for v in (chapter, first, last, due)) and 1 <= chapter <= 114)
                fail(1 <= first <= last <= self.ayah_counts[chapter-1]
                     and self.now() <= due <= self.now()+366*86400)
                target = secrets.token_hex(16)
                db.execute("INSERT INTO revision_plans VALUES(?,?,?,?,?,?,?,?)",
                           (target, student['id'], aid, chapter, first, last, due,
                            text(data.get('instruction', ''), 500, 0)))
                self.audit(db, aid, action, target)
                return {'id': target}
            if action == 'plans':
                student = self.student(db, actor, data.get('student'))
                sql, args = "SELECT * FROM revision_plans WHERE student=?", [student['id']]
                if role == 'teacher':
                    sql += " AND teacher=?"
                    args.append(aid)
                rows = [dict(r) for r in db.execute(sql+" ORDER BY due", args)]
                for plan in rows:
                    plan['observations'] = [dict(r) for r in db.execute(
                        "SELECT kind,result,note,at FROM observations WHERE plan=? ORDER BY at,id LIMIT 100", (plan['id'],))]
                return {'plans': rows}
            if action == 'plan_observe':
                plan = db.execute("SELECT * FROM revision_plans WHERE id=?", (text(data.get('plan'), 32),)).fetchone()
                fail(plan is not None, 404, "Record unavailable")
                student = self.student(db, actor, plan['student'])
                fail(role == 'guardian' and student['guardian'] == aid or
                     role == 'teacher' and plan['teacher'] == aid, 404, "Record unavailable")
                fail(data.get('result') in ('recalled', 'needs_review'))
                fail(db.execute("SELECT count(*) FROM observations WHERE plan=?", (plan['id'],)).fetchone()[0] < 100, 409,
                     "Observation limit reached")
                target = secrets.token_hex(16)
                kind = 'teacher_observation' if role == 'teacher' else 'guardian_self_report'
                db.execute("INSERT INTO observations VALUES(?,?,?,?,?,?,?)",
                           (target, plan['id'], aid, kind, data['result'],
                            text(data.get('note', ''), 500, 0), self.now()))
                self.audit(db, aid, action, target)
                return {'id': target, 'kind': kind}
            if action == 'complaints':
                if role == 'admin':
                    rows = db.execute("SELECT * FROM complaints ORDER BY id")
                else:
                    rows = db.execute("SELECT * FROM complaints WHERE reporter=? ORDER BY id", (aid,))
                return {'complaints': [dict(r) for r in rows]}
            if action == 'complaint_resolve':
                fail(role == 'admin', 403, "Administrator required")
                target = text(data.get('complaint'), 32)
                fail(db.execute("SELECT 1 FROM complaints WHERE id=?", (target,)).fetchone(), 404)
                db.execute("UPDATE complaints SET status='resolved',resolution=? WHERE id=?",
                           (text(data.get('resolution'), 500), target))
                self.audit(db, aid, action, target)
                return {'ok': True}
            if action == 'account_disable':
                fail(role == 'admin', 403, "Administrator required")
                target = text(data.get('account'), 32)
                row = db.execute("SELECT * FROM accounts WHERE id=?", (target,)).fetchone()
                fail(row and row['role'] != 'admin', 400)
                db.execute("UPDATE accounts SET active=0 WHERE id=?", (target,))
                db.execute("DELETE FROM sessions WHERE account=?", (target,))
                db.execute("UPDATE teachers SET status='suspended' WHERE account=?", (target,))
                db.execute("UPDATE lessons SET status='cancelled' WHERE (teacher=? OR student IN "
                           "(SELECT id FROM students WHERE guardian=?)) AND status='booked'", (target, target))
                if row['role'] == 'guardian':
                    db.execute("DELETE FROM enrollments WHERE student IN (SELECT id FROM students WHERE guardian=?)", (target,))
                self.audit(db, aid, action, target)
                return {'ok': True}
            if action == 'audit':
                fail(role == 'admin', 403, "Administrator required")
                return {'events': [dict(r) for r in db.execute("SELECT * FROM audit ORDER BY id DESC LIMIT 200")]}
            raise AcademyError(404, "Operation unavailable")
