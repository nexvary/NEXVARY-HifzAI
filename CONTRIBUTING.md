# Contributing to NEXVARY HifzAI

Thank you for contributing. The project's public code is licensed under [MIT](LICENSE); third-party Quran texts, recordings, translations, images, fonts and teacher identities are separate and must not be added without clear rights.

## Ground rules
1. Open an issue describing the need, design, risk and tests; refer to [the execution plan](docs/WORK_PLAN_SINGLE_REVIEW.md).
2. Keep PRs scoped, preserve Arabic RTL and existing features, include reproducible tests and before/after screenshots for UI changes.
3. Quranic original text is immutable and may only be imported from a licensed, independently verified canonical source. Never generate or “fix” verses with AI.
4. Keep all third-party assets out of PRs unless their origin, license, attribution and redistribution rights are recorded in [the content register](docs/THIRD_PARTY_CONTENT_REGISTER.md).
5. Do not enable speculative automatic tajweed verdicts or make claims of scholarly approval.
6. The **one final scholarly committee submission** is controlled by the project owner. No individual contributor should contact that committee on the project's behalf.
7. Do not commit credentials, personally identifying data, child recordings, production logs or private teacher documents.
8. PRs should state the validation evidence and known limitations. CI success is necessary, not sufficient for correctness and safety.
9. By contributing, you represent that you have the right to grant your original submitted code under the repository's MIT license and that you follow all obligations for third-party components.

## Development workflow
Use branch `dev/foundation` for ongoing work. Make a topic branch and open a PR. For current checks, inspect `.github/workflows/android.yml`; tests include Kotlin/JVM tests, Python corpus-tool tests and debug APK build. A public Release build is deliberately blocked pending scholarly/content/security/legal review.

This repository currently contains a prototype, **not** an approved full Mushaf or automatic tajweed service.
