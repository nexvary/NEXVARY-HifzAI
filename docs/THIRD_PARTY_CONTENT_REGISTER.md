# سجل حقوق المصادر والأصول — Third-party Content Register

This ledger does not grant rights. Records must be supported by upstream documentation or an explicit authorization.

| Asset / dataset | Version / origin | Owner | License / permission proof | SHA256 / provenance | Status |
|---|---|---|---|---|---|
| Full Quran canonical corpus | Tanzil Uthmani v1.1, source `dotquran/corpus/src/resources/uthmani.txt` | Tanzil Project | CC BY 3.0, verbatim no changes; preserve original notice and tanzil.net link | exact Git blob `9813920ae5afce9e50d2ac44562457e29bb4bfca` | **Imported; scholarly/display review pending** |
| Egypt/Saudi recitation audio | No production assets bundled | TBD | Not granted to this project | TBD | **BLOCKED** |
| Tafsir / translations | None bundled | TBD | TBD | TBD | **BLOCKED** |
| Third-party Quran font/imagery | None approved | TBD | TBD | TBD | **BLOCKED** |
| Demo Surah Al-Ikhlas fixture | 4 prototype verses | Reference: Quran.com/112 | Not a full approved edition | Unit test fixture only | **DEMO ONLY** |

Before any row is marked approved, provide the **precise asset and version**, verified source, license obligations for storage/redistribution/alterations, lawful usage scope, original text accuracy where appropriate, integrity hash and review date. Reference any separate rights file in `NOTICE.md`.

The repository's MIT license governs only code to which the project can grant MIT rights. It does not override a content owner or provider's rights.

## Verified font addition 2026-10-14
- Amiri Quran: upstream https://github.com/aliftype/amiri, commit d3d0c5cb6fe7196cb4a58608fc5c8ecd2e6e80fa; OFL-1.1; unmodified font and full license bundled. SHA-256 in NOTICE.md. Unicode coverage checked; Android visual inspection pending.
- Direct official Tanzil byte comparison: docs/OFFICIAL_CORPUS_COMPARISON.json. Full bytes match, without changing the existing asset. This is technical verification, not committee approval.

## Structural metadata
Tanzil official metadata 1.0, unmodified quran-data.xml, copyright and cc-by label preserved verbatim. Source and SHA-256 in NOTICE.md and QuranPartitions.kt. Used for 30 juz and 604 Madani page boundaries, not an assertion of identical printed-page layout. The source states cc-by without a version; clarification is a recorded final rights-review blocker.
