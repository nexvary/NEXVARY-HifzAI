# Phase 60 — Evidence-driven implementation gate

This document tracks a target milestone, **not** a claim that phases 1–60 are complete.

## Non-negotiable controls
- Public release remains blocked by Issue #2 and `blockUnapprovedPublicRelease`.
- No scholarly approval, content redistribution rights, on-device QA, or reproducibility may be inferred from green CI.
- Keep the canonical Quran corpus immutable. Derived search/masking text must never overwrite source bytes.
- Local-only scope governs; online academy and server features are deferred and must not be presented as implemented.

## Next engineering sequence
| Gate | Work | Required evidence | State |
|---|---|---|---|
| 01 | Pin branch HEAD and test baseline | commit SHA, workflow run URLs, failing test list | pending |
| 02 | Corpus provenance and integrity | independent upstream hash, 114/6236 structure, byte comparison, signed internal visual review | pending |
| 03 | Basmala and Uthmani renderer | round-trip tests, screenshots from physical devices, no modified canonical bytes | pending |
| 04 | Memorization engine | independent recall scoring, hint/reveal exclusion, spaced repetition tests | pending |
| 05 | RTL and accessibility | real phone/tablet screenshots, 320dp, large font, TalkBack, working back buttons | pending |
| 06 | Licensed recitation and local ASR | asset rights ledger, negative/uncertain ASR benchmark, permissions tests | pending |
| 07 | Child privacy and offline-only | no network permission, shared-device isolation, deletion tests | pending |
| 08 | Supply chain and QA | SBOM, dependency checks, device matrix, reproducible build comparison | pending |
| 09 | Final internal candidate | pinned signed artifact hash, changelog, Arabic manual, internal approval | pending |
| 10 | Single committee submission | owner-authorized one-time packet only after gates 01–09 | blocked |
| 11 | Public release | documented scholarly decision and legal/rights sign-offs | blocked |

## Evidence requirements
For each gate, link a commit SHA, CI run, test report, device model/API version where relevant, reviewer and date. A task is not complete merely because its implementation exists. Any unresolved Quran integrity or privacy issue blocks distribution.

## Phase milestone rule
Phase 60 is achieved only when its actual implementation and evidence are attached to a PR. This file does not close Issues #2–#8, and does not authorize a release.
