# Noor Al-Basirah — inclusive accessibility within NEXVARY HifzAI

Status: approved scope, implementation pending verification. Base: dev/foundation.

## Product contract
- One Android application, shared immutable Quran corpus and memorization engine.
- Visual and audio-first experiences are interchangeable at any time from an accessible settings control; never lock a user into a first-launch choice.
- Preserve existing memorization sessions, local progress, RTL navigation, and privacy defaults.
- No separate application, cloud requirement, or claim of being the world's first.

## Implementation sequence
1. Audit existing Compose navigation, Quran data layer, session persistence, audio services, and instrumentation tests.
2. Add a persisted `AccessibilityExperience` preference (`STANDARD`, `AUDIO_FIRST`), with accessible switch from home and settings. Preference changes must not reset current ayah, session, or progress.
3. Make all controls semantically labeled in Arabic, focusable, reachable by TalkBack and keyboard/switch access, with clear focus order, minimum touch targets, and large-text scaling. Never rely on color or gesture alone.
4. Build audio-first Quran navigation: choose surah/ayah, play/pause/repeat, navigate previous/next, announce status and errors. Provide visible and screen-reader-operable alternatives to voice commands.
5. Add optional offline-capable voice command adapter with explicit microphone consent and capability detection; never promise offline recognition where unsupported.
6. Support compatible Braille displays through Android accessibility infrastructure; validate with real hardware rather than claiming universal support.
7. Use only cleared recitations with traceable licensing; if unavailable, disable playback with an accessible explanation, never a fake play action.
8. Evaluate memorization feedback with blind and low-vision testers; do not label experimental ASR as tajwid verification.

## Acceptance gates
- Automated Compose UI tests for mode switching, persistence, focus labels, RTL, font scaling and offline navigation.
- Manual TalkBack walkthrough on Android 15 and at least one older supported Android version, including screen locked/resumed scenarios where relevant.
- Testing with blind and low-vision users; record observed failures and remediation.
- Corpus integrity and licensing checks remain mandatory; no public release until existing review gates pass.

## Non-goals for first increment
- Replacing the existing UI wholesale; online accounts; unverified automatic tajwid grading; dependence on proprietary cloud speech APIs.
