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
