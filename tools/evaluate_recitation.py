"""Offline lexical ASR research evaluator. No collection, network, audio or grading enablement.

Only evaluate existing consented, independently transcribed research data. Synthetic
unit fixtures are not a real recitation evaluation or a model calibration record.
"""
import argparse
import datetime as dt
import hashlib
import json
import math
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / 'app/src/main/assets/quran/uthmani-v1.1.txt'
CORPUS_SHA256 = '6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f'
IDENTIFIER = re.compile(r'[a-zA-Z0-9_-]{1,64}\Z')


def normalize(text):
    return re.sub(r'\s+', ' ', re.sub('[\u064B-\u065F\u0670\u06D6-\u06ED\u0640]', '', text)
                  .translate(str.maketrans('ٱأإآى', 'ااااي'))).strip()


def alignment(expected, heard):
    a, b = normalize(expected).split(), normalize(heard).split()
    if len(a) > 512 or len(b) > 512:
        raise ValueError('Word limit exceeded')
    d = [[0]*(len(b)+1) for _ in range(len(a)+1)]
    for i in range(len(a)+1):
        d[i][0] = i
    for j in range(len(b)+1):
        d[0][j] = j
    for i in range(1, len(a)+1):
        for j in range(1, len(b)+1):
            d[i][j] = min(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]+(a[i-1] != b[j-1]))
    omitted = inserted = substituted = 0
    i, j = len(a), len(b)
    while i or j:
        if i and j and a[i-1] == b[j-1] and d[i][j] == d[i-1][j-1]:
            i, j = i-1, j-1
        elif i and j and d[i][j] == d[i-1][j-1]+1:
            substituted += 1
            i, j = i-1, j-1
        elif i and d[i][j] == d[i-1][j]+1:
            omitted += 1
            i -= 1
        else:
            inserted += 1
            j -= 1
    return {'reference_words': len(a), 'omitted': omitted, 'inserted': inserted,
            'substituted': substituted, 'distance': d[-1][-1]}


def character_alignment(expected, heard):
    """Normalized Unicode code-point CER, excluding whitespace, using linear memory.

    This is not grapheme, diacritic, pronunciation or tajweed evaluation.
    """
    a = ''.join(normalize(expected).split())
    b = ''.join(normalize(heard).split())
    if len(a) > 4096 or len(b) > 4096:
        raise ValueError('Character limit exceeded')
    previous = list(range(len(b) + 1))
    for i, first in enumerate(a, 1):
        current = [i]
        for j, second in enumerate(b, 1):
            current.append(min(previous[j] + 1, current[-1] + 1,
                               previous[j - 1] + (first != second)))
        previous = current
    return {'reference_characters': len(a), 'character_distance': previous[-1]}


def canonical_references():
    raw = CORPUS.read_bytes()
    if hashlib.sha256(raw).hexdigest() != CORPUS_SHA256:
        raise ValueError('Canonical source integrity failed')
    refs = {}
    for line in raw.decode('utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        chapter, ayah, text = line.split('|', 2)
        # Consistent with QuranCorpus.presentation: opening basmala is separate.
        if ayah == '1' and chapter not in ('1', '9'):
            text = text.split(maxsplit=4)[4]
        refs[f'{chapter}:{ayah}'] = text
    return refs


def evaluate(dataset, references, today, threshold=0.9):
    if not math.isfinite(threshold) or not 0 <= threshold <= 1:
        raise ValueError('Invalid threshold')
    samples = dataset.get('samples')
    if (not isinstance(samples, list) or not 1 <= len(samples) <= 10_000 or
            not isinstance(dataset.get('model_id'), str) or not dataset['model_id'].strip() or
            dataset.get('riwayah') != 'hafs-an-asim'):
        raise ValueError('Model, riwayah and 1–10000 samples required')
    seen = set()
    speaker_splits = {}
    aggregate = defaultdict(lambda: defaultdict(int))
    for sample in samples:
        for key in ('sample_id', 'speaker_id'):
            if not isinstance(sample.get(key), str) or not IDENTIFIER.fullmatch(sample[key]):
                raise ValueError('Pseudonymous identifiers required')
        if sample['sample_id'] in seen:
            raise ValueError('Duplicate sample')
        seen.add(sample['sample_id'])
        if (sample.get('consent') is not True or sample.get('rights_cleared') is not True or
                not isinstance(sample.get('consent_record'), str) or not sample['consent_record'].strip() or
                not isinstance(sample.get('rights_record'), str) or not sample['rights_record'].strip()):
            raise ValueError('Consent and rights records required')
        if sample.get('consent_revoked', False) is not False or sample.get('rights_revoked', False) is not False:
            raise ValueError('Revoked consent or rights; exclude sample before evaluation')
        if dt.date.fromisoformat(sample['consent_expires_on']) < today:
            raise ValueError('Expired consent; exclude/revoke sample before evaluation')
        if sample.get('split') not in ('calibration', 'evaluation'):
            raise ValueError('Explicit split required')
        speaker = sample['speaker_id']
        if speaker in speaker_splits and speaker_splits[speaker] != sample['split']:
            raise ValueError('Speaker leakage between calibration and evaluation')
        speaker_splits[speaker] = sample['split']
        if sample.get('region') not in ('EG', 'SA', 'OTHER'):
            raise ValueError('Explicit broad region required')
        if type(sample.get('expert_correct')) is not bool:
            raise ValueError('Independent human lexical annotation required')
        # ASR quality is measured against a HUMAN transcript of what was spoken,
        # not against the intended ayah (which may have been recited incorrectly).
        for key in ('human_transcript', 'hypothesis'):
            if not isinstance(sample.get(key), str) or len(sample[key]) > 20_000:
                raise ValueError('Bounded transcript strings required')
        if not normalize(sample['human_transcript']):
            raise ValueError('Human transcript cannot be empty')
        if sample.get('verse') not in references:
            raise ValueError('Unknown canonical verse')
        confidence = sample.get('confidence')
        if type(confidence) not in (int, float) or not math.isfinite(confidence) or not 0 <= confidence <= 1:
            raise ValueError('Confidence must be finite within 0–1')
        asr = alignment(sample['human_transcript'], sample['hypothesis'])
        asr.update(character_alignment(sample['human_transcript'], sample['hypothesis']))
        lexical = alignment(references[sample['verse']], sample['hypothesis'])
        for group in ('all', 'region:'+sample['region'], 'verse:'+sample['verse'], 'split:'+sample['split']):
            row = aggregate[group]
            row['samples'] += 1
            for key, value in asr.items():
                row[key] += value
            if confidence < threshold or not normalize(sample['hypothesis']):
                row['abstained'] += 1
                continue
            row['compared'] += 1
            correct = sample['expert_correct']
            row['human_correct' if correct else 'human_incorrect'] += 1
            row['false_alarm'] += int(correct and lexical['distance'] > 0)
            row['missed_error'] += int(not correct and lexical['distance'] == 0)
    def ratio(n, d):
        return n/d if d else None
    for row in aggregate.values():
        row['wer'] = ratio(row['distance'], row['reference_words'])
        row['cer'] = ratio(row['character_distance'], row['reference_characters'])
        row['coverage'] = ratio(row['compared'], row['samples'])
        row['false_alarm_rate'] = ratio(row['false_alarm'], row['human_correct'])
        row['missed_error_rate'] = ratio(row['missed_error'], row['human_incorrect'])
    return {'schema_version': 2, 'model_id': dataset['model_id'], 'riwayah': dataset['riwayah'],
            'threshold_for_analysis_only': threshold, 'groups': dict(aggregate),
            'automatic_grading_enabled': False,
            'limitations': 'Lexical research only; no tajweed/makharij validation, calibration approval or causal learning claim. Group sizes may be unrepresentative. No audio/transcripts/identifiers in output.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('dataset', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--threshold', type=float, default=0.9)
    args = parser.parse_args()
    if args.dataset.stat().st_size > 20_000_000:
        raise ValueError('Dataset too large')
    raw = args.dataset.read_bytes()
    report = evaluate(json.loads(raw), canonical_references(), dt.date.today(), args.threshold)
    report['input_sha256'] = hashlib.sha256(raw).hexdigest()
    report['canonical_sha256'] = CORPUS_SHA256
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False)+'\n')


if __name__ == '__main__':
    main()
