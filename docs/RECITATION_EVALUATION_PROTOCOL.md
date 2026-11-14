# Local recitation evaluation protocol

This is preparation for phases 081–100, not a speech dataset, independent evaluation,
tajweed assessment or approval. No real speech samples are added by this change.
Keep private research data outside Git and do not send it through the Android app.

## Data and consent

Use a reviewed consent/rights record covering the intended research and retention.
Use random participant IDs, never names. Keep consent documents and identity linkage
in a separate access-controlled store. Do not recruit or record children without a
separately reviewed guardian/child protocol. Withdrawal requires removal from future
evaluation and deletion according to the agreed retention policy. Expired consent is
rejected. The script validates recorded fields; it cannot authenticate a signature or
prove that a consent/rights declaration is true.

Each JSON dataset has `model_id`, `riwayah: "hafs-an-asim"`, and `samples` (1–10000).
Each sample requires:

| Field | Meaning |
|---|---|
| sample_id, speaker_id | Pseudonymous ASCII identifiers, 1–64 letters/digits/underscore/hyphen |
| verse | Existing canonical `chapter:ayah` reference |
| split | `calibration` or `evaluation`; speakers cannot appear in both |
| region | `EG`, `SA`, or `OTHER`; broad self-reported sampling group, not inferred accent |
| consent, rights_cleared | Boolean true, documented outside the dataset |
| consent_record, rights_record | References to separately held records, not personal documents |
| consent_revoked, rights_revoked | Optional boolean false for backwards compatibility; true or any ambiguous value rejects the sample; declarations do not authenticate consent |
| consent_expires_on | ISO date, still valid on evaluation date |
| human_transcript | Independently transcribed words actually spoken |
| hypothesis | Model output, including empty output on a failed recognition |
| confidence | Finite numeric 0–1; meaningful calibration requires a separate study |
| expert_correct | Independent human **lexical** correctness label against the intended ayah |

Human transcription and correctness labels must be reviewed independently of model
output. Record reviewer agreement, disputed cases, equipment, environment, duration,
model revision and licensing in the private study record. Include omissions,
substitutions, insertions, repetitions, silence, noise and difficult boundaries.
Do not infer tajweed, makharij or validity of religious recitation from word matching.
Use the bundled canonical corpus as an immutable reference; opening basmala is
separate consistently with the application reader. No alternate riwayah is accepted.

## Execution and interpretation

```
python tools/evaluate_recitation.py /private/consented-evaluation.json --output /private/evaluation-report.json --threshold 0.9
```

The numeric threshold is for analysis only, not independently calibrated permission
to enable automatic feedback. Tune only on calibration speakers; report the held-out
evaluation split separately. The tool also reports combined, per-region and per-ayah
summaries, with sample counts. Use `split:evaluation/region:EG` and
`split:evaluation/verse:chapter:ayah` rows for held-out region and verse metrics;
combined region and verse rows include calibration speakers. A group without sufficient representative samples cannot
support population claims. Seek independent statistical review of sample size,
uncertainty intervals and the final threshold before making accuracy claims.

WER uses the **human transcript of what was spoken**, not the intended ayah. Otherwise
a faithfully recognized learner mistake would be counted as an ASR failure. Alignment
uses a separate derived normalization consistent with the application's search;
it cannot validate orthography/diacritics, timing or pronunciation. WER may exceed 1.

For non-abstained outputs: false-alarm rate = lexical mismatch on human-correct
samples / human-correct samples; missed-error rate = lexical match on human-incorrect
samples / human-incorrect samples. Missing denominators produce null, never zero.
Low confidence and empty recognition count as abstentions; coverage is reported.
No transcript, sample/participant ID or consent record is written to the report.
Reports include the dataset and canonical hashes. Zero real samples means no real
performance report; unit fixtures only test the evaluator's arithmetic and guards.

Measure inference latency, peak memory, thermal/battery behavior and cancellation on
physical ARM devices independently. Existing generated-tone emulator tests are
technical smoke tests, not real recitation accuracy or physical-device benchmarks.

## Learning evaluation (111, 115, 120)

Do not tune intervals or advertise effectiveness from weekly self-reported summaries.
Use a separately consented study comparing the current policy with a defined baseline,
pre-register retention outcomes at days 1, 7 and 14, randomize where appropriate,
account for prior memorization and attrition, and publish denominators/limitations.
No participant study, efficacy estimate or scholarly sign-off is supplied here.

## Evaluator schema 2 — November 2026

Reports additionally include `reference_characters`, `character_distance` and `cer`
per group. CER is Levenshtein distance over normalized Unicode code points after
removing whitespace, divided by the human transcript character count. It uses the
same derived normalization as WER and cannot measure original Quran diacritics,
graphemes or pronunciation. Missing denominators produce null; insertions can
produce CER above 1. Empty model output is measured and still abstains from lexical
judgment. Limits: 512 words and 4096 normalized nonspace characters per transcript,
with linear-memory character alignment. Reports contain no new participant data.
Revoked consent/rights reject the input; remove those samples before rerunning.
These synthetic arithmetic tests supply no real ASR accuracy result.

## Paired baseline/candidate comparisons

Run `python tools/compare_recitation.py baseline.json candidate.json --output comparison.json`.
Both inputs use the evaluator schema above. They must contain the exact same sample
set and fixed metadata, human transcripts, expert annotations, consent and rights
records; only model identity, hypotheses and confidence may change. Sample order
may differ. Every input is independently validated, including duplicate, revocation
and speaker-split checks. Reports include input/canonical hashes and group-level
candidate-minus-baseline WER, CER, coverage and conditional error-rate differences.
Undefined denominators remain null. Inspect coverage and denominators together:
a model that abstains more cannot be called better merely because its conditional
false-alarm rate falls. These are descriptive paired differences, not significance,
calibration, participant consent verification or real-world improvement evidence.
No audio is collected; consent/rights assertions still require external review.
The evaluator and comparison commands reject output paths that refer to an input
dataset, including symbolic links and hard links. No real dataset has been
supplied or evaluated as part of this development batch.
