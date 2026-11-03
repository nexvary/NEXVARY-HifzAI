# Canonical recording references and local files

`RecitationCatalog` binds a recording ID to one existing ayah in the unchanged
canonical corpus. Entries include a rights record, exact byte length and a reference
review record. Unknown ayat/readers, duplicate IDs and unsupported narrations are
rejected. Pending reference reviews, revoked/expired rights and missing offline
permission are excluded from availability and sequence selection.

The reference and rights fields must come from a **reviewed register**. The code
checks their structure and recorded flags; it cannot authenticate a human signature,
recognize which ayah an audio file contains, establish permission or certify tajweed.
No external reciter audio or actual review/permission document is added here.

An explicit sequence must use one reciter and narration and consecutive canonical
references. Surah boundaries use corpus ordering, not arithmetic on ayah IDs. Missing,
repeated, reversed, skipped or mixed-reciter selections fail. The catalogue plans
the sequence; `LocalRecitationQueue` now executes an explicit bound selection.

`bind(id, file, today)` runs on an IO worker and checks present-day eligibility,
exact length and SHA-256. Metadata does not supply a file path or download URL to
open. `LicensedRecitationPlayer.open(boundAsset)` rechecks length, expiry and digest
before MediaPlayer validates/opens the bytes. Mutations after binding fail closed.
The original raw-file adapter is retained for existing internal test compatibility;
it is not a reader-facing streaming/download feature.

Tests use explicitly synthetic metadata and mathematically generated tones, never
recordings attributed to real reciters. Android instrumentation exercises actual
mapped playback/repetition and rejects post-binding mutations and expired records.
These checks are not audio-content accuracy, real permissions or scholarly approval.

## Foreground sequence adapter

`LocalRecitationQueue` accepts only catalogue-matching bound records in consecutive
canonical order, including surah boundaries. Callers construct it and issue commands
on the main thread with the owning screen's `Lifecycle`. File hashing, MediaPlayer
preparation, rights checks and transitions run on its serial worker looper. Each
item is opened/validated again immediately before use. Extra repeats (0–20) and
speed (0.5–1.5) apply to each item; the last item ends the selection without looping.
Snapshots identify the current recording/ayah, completed items and failures.

Preparation alone does not start playback. Pause, audio-focus loss and noisy-output
events pause the whole queue until an explicit play request. The low-level player's
monotonic interruption count also covers the interval between clips. Replacing or
clearing a selection and lifecycle stop invalidate queued work; stopped selections
are cleared and do not reappear on return. Destroy/close removes the observer,
closes the player and shuts down the worker. Commands/cleanup are asynchronous;
callers observe snapshots rather than treating enqueueing as completed playback.
Failures clear the decoder and stop the selection before later items can play.

The transition has polling/preparation gaps and is **not gapless**. This adapter is
not wired into a reader-facing audio library because no approved recordings are
bundled. There is no background service, headset media-button control, persistent
queue, download, network permission or permanent private-voice archive. It must use
a visible screen lifecycle, not a service/process lifecycle, when integrated.

Three added Android methods exercise actual generated WAV playback, per-item
repetition/speed across a surah boundary, pause/focus/noisy-output/background cleanup,
explicit resume, invalid/expired selection and mutation of a later file after binding.
The noisy-output check invokes the actual registered receiver through a test context
wrapper. It is simulated receiver delivery, not an OS/hardware unplug event; apps
cannot send Android's protected system broadcast. Physical-headset checks remain pending.

An actual approved catalogue, user-facing import/library screen, cleared recordings,
timings, background/headset service and gapless playback remain outstanding. The
Android app keeps its existing offline-only manifest and temporary private-voice
retention policy.


## Finite whole-range repetition and live speed

`load(..., extraRangeRepeats = n)` now traverses the selected canonical range
`n + 1` times (0–20 extra passes), independently of the existing per-ayah repeats.
A three-ayah selection with one extra range repeat is A,B,C,A,B,C. With one extra
per-ayah repeat as well, it is A,A,B,B,C,C,A,A,B,B,C,C. The default remains one
range pass. A bounded cursor tracks up to 2,000 distinct items and 21 passes
without allocating repeated copies of the bound file list. `completedItems`
counts fully finished per-ayah cycles across passes; `rangePass` and `rangePasses`
make the selected finite work explicit. Completed selections never restart on Play.

Every wrap goes through the same worker `prepare` path and rechecks file bytes,
reference and current rights before opening the first ayah again. A replaced,
expired or otherwise invalid file stops the queue. Clear/lifecycle stop resets
both traversal and counters. This does not add gapless playback or a background
service.

`setPlaybackSpeed(0.5f..1.5f)` edits a loaded queue on its worker without seeking,
autoplay or unpausing. The speed carries across subsequent ayat and range passes.
NaN, infinity and out-of-range values fail before issuing a command. Paused and
ready players apply the requested speed on explicit Play, using the existing
low-level no-autoplay behavior.

Five JVM tests cover exact range order, finite single-ayah and maximum-size work,
input bounds and terminal behavior. Three Android methods use generated WAV tones
to exercise combined repeats, mutation before wrap and ready/playing/paused speed
changes. Matching CI is required; fixtures are not approved Quran recordings.
The reader audio library UI, licensed content, background/notification/headset
controls and physical-device interruption tests remain outstanding.


Validation update: source1f97d35 passes all seven CI jobs,105 JVM tests and
49 normal+21 large-font methods in each of four emulator profiles. The five
range-cursor JVM cases and all ten generated-tone Android playback methods pass;
no real reciter recording, hardware headset or real-speech evaluation is inferred.
Exact sources, artifact digests and prior failures remain in TEST_RESULTS.md and
`docs/evidence/2026-11-03/continuation-ci.json`.
