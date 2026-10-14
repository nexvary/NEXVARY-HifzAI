# Security policy — NEXVARY HifzAI

## Status
The current 0.5.0-dev code is an internal development prototype with no public production release. Do **not** treat a successful APK build as Quran approval, teaching certification or legal clearance.

## Reporting
Please avoid publishing exploit details or sensitive personal data in a public issue. If GitHub's **Report a vulnerability** / private advisory feature is available for this repository, use it. Otherwise contact repository maintainers through an agreed private communication channel before sharing details; no personal contact address is published by this file.

## Critical findings
Immediately stop release promotion if you discover:
- Mutation or misattribution of original Quran text, mixed riwayat presented as mistakes, or incorrect displayed ayah boundaries.
- Inaccurate automatic claims of recitation/tajweed correctness without expert validation.
- Leaked credentials, voice recordings, minor/guardian data, unauthorized teacher-student access.
- Unlicensed audio, images, translations or code redistributed by the project.
- A bypass of the release approval process.

Do not post secrets or personal data in GitHub issues, logs or pull requests. Prefer minimal reproductions with synthetic data. Report scope, observed behavior, device/build version, severity and safe reproduction steps privately.

## Release and patches
Maintain PR review, CI and evidence retention, and follow `docs/RELEASE_GATES.md` and `docs/WORK_PLAN_SINGLE_REVIEW.md`. A patch affecting Quran text must pass independent source checking and the project's content-release process, not just code tests.

## Local data scope
Private preferences hold self-reported recall, a bookmark, a non-audio session and an optional local teaching notebook with aliases and consent acknowledgments. Cache holds only temporary microphone audio. No network permission or telemetry SDK is present. Explicit Android 12 data extraction rules exclude cloud backup and device transfer; older backup rules exclude all domains. Device compromise/root remains outside these storage guarantees. The notebook does not authenticate separate people or qualify teachers; a remote academy requires a separately reviewed authorization service.

## Dependency scan scope — 2026-10-14
93 resolved debugRuntimeClasspath Maven coordinates were queried against OSV querybatch. No vulnerability records were returned, and no pagination was pending. See docs/DEPENDENCY_AUDIT_2026-10-14.json for exact versions and timestamp. This does not audit build plugins, operating-system code, unknown vulnerabilities, or replace an independent security review.
