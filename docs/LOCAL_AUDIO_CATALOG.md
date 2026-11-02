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
repeated, reversed, skipped or mixed-reciter selections fail. This is a sequence
**planner**, not an automatic or gapless playback queue/background service.

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

An actual approved catalogue, user-facing import/library screen, cleared recordings,
timings, background/headset service and gapless playback remain outstanding. The
Android app keeps its existing offline-only manifest and temporary private-voice
retention policy.
