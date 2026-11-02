# Development increment covering the 061–150 backlog

The owner's request for the next 90 phases is interpreted as **061–150 inclusive**
after the existing phase-60 topic. PR #9 itself is documentation-only and does not
prove phases 001–060 complete. The base remains dev/foundation at
`8496f42e38884126895e3d0786c70852b42394cf`. See Issue #10.

This increment implements specific local capabilities across this range. It does
**not** complete 90 phases or change the 14 pending final acceptance gates. The
historical phase ledger is not an acceptance certificate. The per-phase table below
records actual scope and outstanding work. Connected academy requirements remain
outside the owner-approved local Android scope; the optional existing academy
service is preserved, not deployed or connected to Android.

## New behavior

- [The local notebook agenda](LOCAL_NOTEBOOK_AGENDA.md) combines lessons and assignments
  with day-based pending/upcoming/past/completed/all filters and student selection.
  Five records per page keep the agenda bounded; edits, completion and confirmed
  deletion reuse existing storage and preserve unrelated records. Selection/filter
  survive Activity recreation; page position resets when results/data change.
  Date refresh uses the phone day and the explicit refresh action. Malformed/orphaned
  records are counted without deletion or invented dates. Personal learning progress
  is never changed by notebook completion. No booking/notification/identity verification
  or teacher-confirmed learning claim is added.

- Further continuation: local goals now subtract distinct self-assessed new/review
  verses already recorded on the same phone calendar day, across sessions/surahs.
  Retrying the same verse does not charge another slot. Explicit manual practice can
  exceed the proposal; failed positions assessed today have a separate optional retry
  list. Counters persist/reload, reset on the next recorded day and clear with user
  data. Legacy history has no invented first-study classifications; daily counts
  begin with newly classified assessments in this increment.
- Home, Today and "Start today's session" now share one plan: due reviews from
  the entire corpus, then new study from the explicitly selected chapter. Previously
  Today listed global reviews while Start silently planned only the selected chapter.
  Guided navigation resolves canonical chapter/ayah addresses directly, including
  transitions between short and long chapters; it never clamps an index into the old
  chapter. The new-study chapter is pinned in the session while reviews move to other
  chapters and survives recreation/Home resume. Legacy guided sessions default their
  anchor to their saved chapter; invalid anchors are rejected. Completed sessions
  return to the chosen new-study chapter and clear the saved session.
- "Start today's session" follows the current mode and remaining daily proposal,
  returning to Today when exhausted. That guided mode is persisted
  across recreation/resume. Direct verse/tab selection is manual practice and remains
  available beyond the proposal. Failed attempts can be retried immediately without
  charging a second distinct verse. Assessment progress, history, day counts and the
  optional return anchor are saved in one preferences editor transaction.
- [Canonical audio catalogue contracts](LOCAL_AUDIO_CATALOG.md) add real-ayah/reader
  validation, exact-byte binding and explicit contiguous sequence selection. The
  mapped player revalidates bytes and rights before decoding. A foreground queue
  adapter now runs explicit consecutive selections with per-item repeats/speed,
  worker-thread preparation, interruption pause and lifecycle cleanup. Real Android
  generated-tone tests exercise sequence/stop/failure paths. No approved external
  recordings, reader audio-library UI, gapless transitions or background service is claimed.

- Continuation: deletion generations now survive Activity recreation, preventing an
  unused pre-deletion saved-state generation from restoring an old recall attempt.
  The UI regression recreates the Activity both before and after deletion.
- Phase 118: an explicitly enabled local return suggestion appears on Home after a
  week without recorded practice, measured from enablement/last practice/postponement.
  It has no alarms, notifications or networking. Disabling erases its anchor date;
  training while disabled does not create one. Data deletion resets its consent.

- Settings offer balanced/new-only/review-only planning, 0–20 new verses and 1–100
  reviews per day. The planner respects future due dates and empty budgets;
  learners can still select any verse manually. These are proposals based on distinct
  graded verses, not hard limits on access, reading time or a claim of mastery.
- Progress shows current and previous 7-day summaries and per-practice-mode counts.
  These use retained self-reported observations (last 30 per verse). Legacy records
  have unknown practice mode; they are not assigned an invented mode. Recent failures
  can suggest a gentler drill without changing the scheduling policy.
- Favorite reciter selection is a preference; it does not unlock missing audio.
- Local teacher notebook supports reference-validated single-surah assignments,
  completion/reopening, lesson rescheduling and note editing. Assignments can be
  edited or individually deleted after confirmation. Changing an assignment's
  verses reopens it; moving only its date preserves completion. Invalid references,
  dates and missing IDs leave stored assignments unchanged. Individual lesson
  deletion removes only that lesson and its linked problem notes in one preferences
  transaction; other lessons, students and assignments remain. Both deletion dialogs
  offer cancellation. Per-student deletion still cascades to assignments.
  Nothing is sent to a teacher/student or changes the learner's memorization progress.
- Data deletion rebuilds Compose state, clearing in-memory session/settings/favorites
  as well as persisted data. Both data stores are attempted even if one fails.
  Quran asset loading/hash checks run on IO with explicit loading/error screens.
- Experimental lexical alignment distinguishes omissions, insertions and substitutions
  without changing canonical text or enabling voice grades. A bounded offline evaluator
  checks recorded consent/rights, expiry and speaker separation, calculates WER against
  human transcripts, and reports abstention/false alarms/missed errors by region/ayah/split.
- Rights policy rejects expired/revoked records and malformed/credential-bearing URLs.
  The licensed adapter checks expiry on playback and while open. A bounded local
  audio-store helper checks digests, quotas and cleanup. It is **not wired into a
  user import/download screen**, and no external audio or permission documents are added.

## Validation

`python tools/run_python_tests.py` includes the new evaluator tests alongside all
existing Python checks. JVM tests cover plan budgets/date windows/recent advice,
alignment/rights expiry, audio import quotas/digests and failure cleanup. Instrumentation
adds persistence/migration, notebook assignment/cascade and actual UI reset tests.
Notebook regressions also exercise invalid edits without mutation, range/date
completion rules, individual deletion isolation, cancellation and Activity reload.
The notebook UI paths run at normal and large font on the same device matrix.
The two instrumentation invocations use separate app processes before the host
changes system font scale. A separate live-font regression changes system font
with the app open, waits for a new Activity, and verifies the saved recall stage,
assistance and plan metadata in both directions. This retains coverage for real
configuration changes while isolating runner teardown between test invocations.
Failure handling preserves both instrumentation logs and full device logcat without
turning a failed run green. An API 26 scaled-run teardown previously crashed in
Compose SlotWriter disposal; the same internal stack later recurred during a normal
API 26 invocation. Process isolation alone therefore did not resolve the issue.
The source-verification wrapper now renders a separate `VerifiedAppContent` child
only after successful verification, rather than placing the full remembered screen
state after early loading/error returns. A four-cycle regression preserves an
assisted recall attempt through recreation, deletes it, and recreates again, in
both font-scale rounds. This is a lifecycle-focused mitigation under test, not a
proven diagnosis or a universal Compose-runtime stability claim; failed evidence is retained.
UI tests wait for asynchronous canonical verification rather than sleeping.

Local checks after the continuation: **56 Python tests**
pass, with no failures/errors/skips. A standalone pinned Kotlin 2.0.21/JUnit 4.13.2
run passes **92 JVM tests** (all repository JVM tests, using compile-only Android API
signatures and host JSON; no Android storage/UI runtime assertion). This
does not replace the full Gradle/Lint/Android build or instrumentation matrix.

The first PR CI confirms the original 53 Python tests and the optional web console
checks ran successfully, but artifact uploads fail because the account's Actions
artifact storage quota is exhausted. Counts/hashes are additionally emitted into
job logs/summaries; required artifact upload failures remain visible. No existing
artifacts are deleted and no gate is downgraded to make the workflow appear green.

Match the PR's exact commit to Android CI/device reports before claiming Android
validation. No local Android SDK/Gradle was available in the editing container. No
new physical-device, real speech, usability or human editorial results are claimed.

The repository is public. The existing Android workflow guards plaintext APK upload
with `github.event.repository.private == true`; source visibility does not approve
APK distribution or change the production release guard. Match current-head Actions
results before claiming evidence-upload success; earlier quota failures remain history.

## Remaining dependencies

No source code can substitute for recording permissions, consented real speech,
independent ASR/learning evaluation, teacher identity/qualification verification,
Quran display review, narration-specific editorial review or committee decisions.
Background/headset reciter playback, gapless sequences, alternate narrations,
encrypted opt-in retained recordings, tafsir content and the connected academy are
not delivered by this increment. All public-release guards remain active.

See [the ASR/study protocol](RECITATION_EVALUATION_PROTOCOL.md) and
[the 90-row implementation matrix](PHASES_061_150_STATUS.md).
