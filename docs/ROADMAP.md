# NEXVARY-HifzAI roadmap

## Phase 0 — foundational Android prototype
- [x] Android Kotlin/Compose scaffold, Arabic RTL UI
- [x] Read → progressively mask → unaided recall → honest self-score
- [x] Pure Kotlin spaced-review engine (SM-2-inspired) with unit tests
- [x] Small source-checked fixture for Surah Al-Ikhlas
- [x] Device-local progress, no microphone permissions or network upload
- [x] CI configuration for tests and APK (success must be verified)

## Phase 1 — trustworthy Quran corpus & genuine memorization
- [ ] Obtain an authenticated, licensed full Quran text asset; verify every verse and preserve version/hash
- [ ] Segmentation by word/stop point and visual display distinct from immutable canonical source
- [ ] Exercise transitions between verses and personalized similar-verses confusions
- [ ] Queue due reviews first, differentiate recall reliability vs recitation accuracy
- [ ] Reset/delete/export user data, encryption and migrations

## Phase 2 — audio and teacher review
- [ ] Secure rights for licensed recitations and streaming/offline rules
- [ ] Offline-first audio player, reciter selection and repeating intervals
- [ ] Controlled ASR experiments (dialect/recitation variability, substitutions/omissions)
- [ ] Expert validation for tajweed, makharij, and confidence thresholds
- [ ] Make AI uncertainty explicit and always provide user correction

## Phase 3 — personalized learning & cross-platform
- [ ] A/B pilot: compare retention 1, 7 and 14 days against conventional repetition
- [ ] Adaptive schedules based on evidence, not claimed scientific guarantees
- [ ] Teacher portal and verified Egyptian/Saudi reciters/educators
- [ ] Optional sync and web/Windows clients with consent and protection for minors

## Definition of Done for first Android release
A successful CI build, downloadable APK, working launch and Arabic RTL on real emulator/device, review persistence tests and regression test for canonical Quran text.
