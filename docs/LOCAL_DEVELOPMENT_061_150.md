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

- Settings offer balanced/new-only/review-only planning, 0–20 new verses and 1–100
  reviews per session. The planner respects future due dates and empty budgets;
  learners can still select any verse manually. These are session limits, not a
  cross-session daily consumption counter.
- Progress shows current and previous 7-day summaries and per-practice-mode counts.
  These use retained self-reported observations (last 30 per verse). Legacy records
  have unknown practice mode; they are not assigned an invented mode. Recent failures
  can suggest a gentler drill without changing the scheduling policy.
- Favorite reciter selection is a preference; it does not unlock missing audio.
- Local teacher notebook supports reference-validated single-surah assignments,
  completion/reopening, lesson rescheduling and note editing. Per-student deletion
  cascades to assignments. Nothing is sent to a teacher/student.
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
UI tests wait for asynchronous canonical verification rather than sleeping.

Match the PR's exact commit to Android CI/device reports before claiming Android
validation. No local Android SDK/Gradle was available in the editing container. No
new physical-device, real speech, usability or human editorial results are claimed.

## Remaining dependencies

No source code can substitute for recording permissions, consented real speech,
independent ASR/learning evaluation, teacher identity/qualification verification,
Quran display review, narration-specific editorial review or committee decisions.
Background/headset reciter playback, gapless sequences, alternate narrations,
encrypted opt-in retained recordings, tafsir content and the connected academy are
not delivered by this increment. All public-release guards remain active.

See [the ASR/study protocol](RECITATION_EVALUATION_PROTOCOL.md) and
[the 90-row implementation matrix](PHASES_061_150_STATUS.md).
