# Implementation evidence — 2026-10-14 (development branch)

**Development version:** 0.5.0-dev  
**Scholarly approval:** none. **Public release:** blocked.  
**Branch:** `dev/foundation`. Draft PR #1.

## Implemented in source
- [x] Tanzil Uthmani v1.1 Quran corpus: 114 surahs, 6236 ayat, verbatim original with copyright footer.
- [x] Technical source Git blob pin: `9813920ae5afce9e50d2ac44562457e29bb4bfca`.
- [x] Source SHA-256 pin: `6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f` (subject to CI confirmation).
- [x] Runtime refusal to show Quran content when source integrity check fails.
- [x] Quran reader, 114-surah navigation, search by name/number, font sizing, 8-ayah page navigation.
- [x] Arabic RTL five-tab home/practice/reader/insights/settings with reciter directory as secondary screen.
- [x] Active-recall learning ladder, hint/reveal accounting, due-review plan, memory fingerprints and difficulty labels.
- [x] Cross-surah lexical similarity suggestions, labelled as an unverified training aid.
- [x] Optional written recall, never retained between sessions.
- [x] Optional private microphone record/play/erase for self-assessment, microphone permission requested on demand; audio must not leave device.
- [x] Native vector app icon and royal navy / gold / emerald design baseline.
- [x] Python checks for all surah and ayah indices and both corpus digests.
- [x] Compose Android emulator smoke-test code and screenshot-capture workflow added.
- [x] MIT code license, CC BY 3.0 corpus notice, independent rights register, one-committee-submission policy, Gradle release block.

## Not yet verified
- [ ] Successful CI for **the current 0.5.0-dev HEAD**, including compile, Kotlin tests, Python hash tests.
- [ ] Actual Android emulator screenshot proof artifacts from the new UI workflow.
- [ ] Physical phone and tablet UI/diacritic/line-breaking/accessibility review.
- [ ] Academic measurement of learning effectiveness / confidence calibration.
- [ ] Independent scholarly inspection of Quranic text rendering and copy source against an official release.
- [ ] Audit that audio file deletion handles lifecycle edge-cases on physical devices.

## Still to be built or legally cleared
- [ ] Licensed and correctly attributed Egyptian/Saudi reciter audio playback and repetition.
- [ ] Optional offline audio caches only when a recording's rights permit it.
- [ ] ASR and recitation pronunciation/tajweed evaluation only after expert-validated accuracy and uncertainty tests.
- [ ] Complete verified teacher academy, backend, booking, minor safety and access controls.
- [ ] Multi-device synchronization with encryption/privacy controls, if enabled.
- [ ] Full responsive design-system refinements, TalkBack, performance/energy audit.
- [ ] Professional QA for stable distribution, signed release, legal/privacy/rights reviews.
- [ ] **Single final scholarly committee submission after all gates, not before.**

## Truthfulness rule
A checked **implemented in source** checkbox means the code was committed and can be inspected; it **does not** mean the feature passed QA or is certified. No latest-build success, released APK or genuine screenshots should be claimed without a matching successful Actions run.
