# Content, third-party materials and provenance

## Code
The original NEXVARY HifzAI software source code is released under the [MIT License](LICENSE). Contributions are accepted only under the contributor's actual right to submit them, with compliance with any third-party license.

## Quran text
A separately sourced Quran text asset does **not** automatically become MIT-licensed by being placed beside MIT-licensed application code. Preserve the Quran text provider's own license, attribution, version, orthography, and independent verification requirements. The bundled full corpus is byte-identical to an official Tanzil download (see docs/OFFICIAL_CORPUS_COMPARISON.json). Demo fixtures are not used by the reader. Rendered-text scholarly acceptance remains pending.

## Recitation audio, translations, tafsir and imagery
Reciter recordings, portraits, Quran manuscripts, imagery, tafsir, translations, Arabic fonts, and teaching texts require their own lawful authorization, attribution, and redistribution permissions. A public URL or a free API alone does not grant redistribution rights.

## Open source does not mean formally certified
Publishing source code does not imply that scholars have approved an interpretation, method, recitation, or display. This repository currently lacks final scholarly committee approval and approval to make production Quran accuracy claims.

Any future contribution adding third-party materials must add the origin and license in `docs/THIRD_PARTY_CONTENT_REGISTER.md`, the exact versions and SHA-256 hashes where applicable, and reviews required by `docs/RELEASE_GATES.md`. Unverified content is rejected, not silently bundled.

## Trademark
The MIT software license does not grant an automatic right to imply endorsement by NEXVARY or any reciter, educator, or Quranic institution. Project marks and third-party identities are handled separately from code rights.

### Bundled Tanzil Uthmani corpus
Original file: `app/src/main/assets/quran/uthmani-v1.1.txt`. CC BY 3.0, Tanzil Project (Version 1.1). It remains a byte-identical copy of `dotquran/corpus/src/resources/uthmani.txt` Git blob `9813920ae5afce9e50d2ac44562457e29bb4bfca`, including its full original copyright notice and provenance. Attribution: https://tanzil.net/ . The MIT software license does not supersede Tanzil's license. Independent scholarly inspection of actual rendered text is still pending.

### Amiri Quran font
Amiri Quran, unmodified from aliftype/amiri commit d3d0c5cb6fe7196cb4a58608fc5c8ecd2e6e80fa. SIL Open Font License 1.1; copyright and full license in app/src/main/assets/licenses/AMIRI-OFL.txt. Font SHA-256: 8fa95faaf7bd18b71789f6f57312003e30c5029218a035e5baf613a0222ab82d. All Unicode codepoints in the bundled Quran are in its cmap; that does not certify rendered glyph placement.

### Tanzil structural metadata
Official unmodified quran-data.xml metadata 1.0: https://tanzil.net/res/text/metadata/quran-data.xml . Root identifies copyright (C) 2008-2009 Tanzil.info and license cc-by, without a version suffix. Preserved verbatim in quran/metadata-v1.0.xml; SHA-256 8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a. Numerical partition addresses are used without altering Quran text. License-version clarification and internal specialist boundary/display review remain part of final rights acceptance.
