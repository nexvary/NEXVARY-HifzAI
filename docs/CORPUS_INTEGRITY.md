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


## Quran Foundation API restrictions — verified 2026-10-14
The [Quran Foundation Developer Terms](https://api-docs.quran.com/legal/developer-terms/) were updated on **2026-10-11**. Before ANY QF integration:

- Client credentials stay exclusively on a private backend; do not embed them in Android.
- Do not cache QF Content beyond **one week**, unless obtained and updated via the authorized [Content Sync API](https://api-docs.quran.com/docs/tutorials/content-sync/offline-cache-patterns/) or other stated exceptions.
- A resource available through Content Sync must only be obtained for offline use through the authorized Content Sync mechanism and synced at least every **seven days when online**.
- An API response is not permission to redistribute the data or recording files as a separate bundle, public API, or dataset.
- Recitation records and chapter audio timing metadata do not imply redistribution rights in the underlying audio files.
- Do not use QF Content or User Data to train AI/ML models without written consent under the applicable API rules.
- Attribution: “Quran data provided by Quran Foundation”, as required where relevant, plus specific translator/reciter credits.
- API terms, local law, scholarly approvals and the rights of other contributors are all separate requirements.

These API restrictions do not apply by default to a separately licensed official Tanzil text asset; **each provider's terms must be evaluated independently**. No QF or Tanzil full Quran dataset is currently bundled in this repository.
