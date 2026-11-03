"""Paired offline ASR comparison; never enables grading or claims real-world improvement.

Both inputs must contain the same consented samples and human references. Only
model_id, hypothesis and confidence may differ. Reports contain aggregate metrics,
not transcripts, consent records or participant identifiers.
"""
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path

try:
    from tools.evaluate_recitation import evaluate, canonical_references, CORPUS_SHA256
except ModuleNotFoundError:  # Direct invocation from tools/.
    from evaluate_recitation import evaluate, canonical_references, CORPUS_SHA256


METRICS = ('wer', 'cer', 'coverage', 'false_alarm_rate', 'missed_error_rate')


def compare(baseline, candidate, references, today, threshold=0.9):
    # Validate each dataset before constructing maps (duplicates must not disappear).
    before = evaluate(baseline, references, today, threshold)
    after = evaluate(candidate, references, today, threshold)
    left = {sample['sample_id']: sample for sample in baseline['samples']}
    right = {sample['sample_id']: sample for sample in candidate['samples']}
    if left.keys() != right.keys():
        raise ValueError('Paired comparison requires identical sample sets')
    # Prevent accidental comparison of different corpora/versions/conditions.
    def fixed_dataset(data):
        return {k: v for k, v in data.items() if k not in ('model_id', 'samples')}
    if fixed_dataset(baseline) != fixed_dataset(candidate):
        raise ValueError('Dataset metadata differs')
    for sample_id in left:
        def fixed_sample(sample):
            return {k: v for k, v in sample.items() if k not in ('hypothesis', 'confidence')}
        if fixed_sample(left[sample_id]) != fixed_sample(right[sample_id]):
            raise ValueError('Paired sample metadata or human reference differs')
    groups = {}
    for group in before['groups']:
        a, b = before['groups'][group], after['groups'][group]
        groups[group] = {'samples': a['samples'], 'baseline': a, 'candidate': b,
                         'candidate_minus_baseline': {
                             metric: b[metric] - a[metric] if a[metric] is not None and b[metric] is not None else None
                             for metric in METRICS}}
    return {'schema_version': 1, 'baseline_model_id': before['model_id'],
            'candidate_model_id': after['model_id'], 'groups': groups,
            'threshold_for_analysis_only': threshold, 'automatic_grading_enabled': False,
            'limitations': 'Paired descriptive lexical metrics only. Negative WER/CER deltas mean fewer edits on these inputs, not statistical significance or generalization. Error rates are conditional on each model not abstaining; compare coverage and denominators. No audio quality, timing, tajweed or learning efficacy measurement. No real-data claim from synthetic fixtures.'}


def read_dataset(path):
    if path.stat().st_size > 20_000_000:
        raise ValueError('Dataset too large')
    raw = path.read_bytes()
    return json.loads(raw), hashlib.sha256(raw).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline', type=Path)
    parser.add_argument('candidate', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--threshold', type=float, default=0.9)
    args = parser.parse_args()
    if args.output.resolve() in (args.baseline.resolve(), args.candidate.resolve()):
        raise ValueError('Output must not overwrite a dataset')
    baseline, baseline_sha = read_dataset(args.baseline)
    candidate, candidate_sha = read_dataset(args.candidate)
    report = compare(baseline, candidate, canonical_references(), dt.date.today(), args.threshold)
    report.update(baseline_input_sha256=baseline_sha, candidate_input_sha256=candidate_sha,
                  canonical_sha256=CORPUS_SHA256)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False) + '\n')


if __name__ == '__main__':
    main()
