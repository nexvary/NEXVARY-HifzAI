# Islamic Content Verification Policy — NEXVARY HifzAI

**Status:** Mandatory editorial and engineering gate. This document is a policy, not a scholarly certification.

## Non-negotiable rule
No Quranic text, hadith, dhikr, dua, fiqh ruling, historical claim, children's lesson, audio, translation, caption, illustration with religious implications, or AI-generated religious explanation may be published until its source and exact wording have been verified and an appropriately qualified human reviewer has approved the specific version. Unresolved material must be withheld.

## Source and version record
Each item must have: unique content ID; content type; exact original text and language; authoritative source title, edition/version, publisher, page/volume or hadith number and accessible source reference; relevant recitation/riwayah or jurisprudential school where applicable; evidence of usage rights; editor; reviewer identity/qualification; review date; decision; checksum; and revision history.

## Quran
Preserve a licensed, identified canonical corpus byte-for-byte. Verify every character, diacritic, verse boundary and metadata against the chosen source. Validate actual screen rendering, font behavior, RTL, audio alignment and accessibility output. Derived search or hints must never mutate the canonical text. Automated matching does not substitute for scholarly examination.

## Hadith and adhkar
Record the exact wording, collection, location, grading with attribution to the grading authority, and material scholarly differences. Do not invent promised virtues, repetition counts or recommended times. Do not imply all narrations are equally established.

## Fiqh, prayer and education
Use recognized primary and scholarly references. Disclose consequential, recognized differences of opinion; do not present disputed details as universal consensus. Review illustrations, children's stories, quizzes and translations for theological and practical accuracy. Qualified reviewers must validate religious instructions.

## Audio and AI
Check reciter identity, narration, permissions, transcription and synchronization. AI may assist with indexing and flagging discrepancies but cannot issue final religious approval or independently authenticate hadith, tajwid or fatwas. Mark uncertain recognition as uncertain.

## Release workflow
Draft -> Source documented -> Independent text/asset verification -> Qualified human scholarly review -> QA on target devices -> Approved exact version -> Release.
Every modification invalidates the affected item's approval until re-reviewed. Block CI/release on missing provenance, mismatch, unapproved status, or licensing failures. Maintain corrections, withdrawal and rollback mechanisms.

## Scope and limitations
This policy does not itself certify any existing content. Review records and actual expert approval are required. No claim of zero error or guaranteed religious/legal outcome is permitted.
