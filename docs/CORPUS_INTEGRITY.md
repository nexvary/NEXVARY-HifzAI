# Canonical Quran content policy

This app must **never** generate canonical Quran verses from a language model.

1. Obtain the full text from an authorized source (e.g. Tanzil) and preserve the original orthography, punctuation and diacritics as licensed.
2. Record the precise version, provider, extraction date, licensing obligations, and a SHA-256 digest validated independently of the delivered text.
3. Convert a legitimate source distribution into a UTF-8 JSON manifest containing `source`, `license`, `source_url`, and `verses` with `surah` (int), `ayah` (int), and `text` (string).
4. Run `python tools/validate_corpus.py your-file.json --expected-sha256 <TRUSTED_HASH>`.
5. Structural validation checks 114 surahs and 6236 ayat by chapter. **It cannot prove the Arabic wording or orthography is authentic.** Expert review and separately pinned upstream hashes are mandatory.
6. Keep licensed attribution prominently visible within the app, and reproduce required notices in any distributed asset.
7. Masking, search normalization and tutoring hints must never overwrite or replace canonical text records.
8. No automatic translation of Quran/tafsir; translations must come from reviewed licensed sources.
9. The current four-verse Surah Al-Ikhlas fixture is a **prototype only**, not a validated production corpus.

Authoritative documentation:
- Tanzil text license: https://tanzil.net/docs/Text_License
- Quran Foundation: https://api-docs.quran.com/docs/quickstart/
  - Content APIs require backend secrets: never embed client_secret inside an APK.
  - Pre-live datasets currently contain surahs 1 and 2 only; full data requires approved production access.
