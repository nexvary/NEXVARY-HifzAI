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
summaries, with sample counts. Do not interpret the combined/calibration rows as
held-out performance. A group without sufficient representative samples cannot
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
