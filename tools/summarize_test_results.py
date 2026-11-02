"""Retain source-bound test counts in logs/job summary even if artifact storage is full.

This does not replace artifact upload gates or claim screenshot/human approval.
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


def junit_result(path):
    raw = path.read_bytes()
    root = ET.fromstring(raw)
    suites = [root] if root.tag == 'testsuite' else root.findall('.//testsuite')
    if not suites:
        raise ValueError('No JUnit test suites')
    counts = dict(tests=0, failures=0, errors=0, skipped=0)
    for suite in suites:
        cases = suite.findall('testcase')
        if int(suite.get('tests', '-1')) != len(cases):
            raise ValueError('JUnit declared count differs from actual cases')
        counts['tests'] += len(cases)
        for key, tag in (('failures', 'failure'), ('errors', 'error'), ('skipped', 'skipped')):
            observed = sum(case.find(tag) is not None for case in cases)
            if int(suite.get(key, '0')) != observed:
                raise ValueError('JUnit result counters inconsistent')
            counts[key] += observed
    return {'file': str(path), 'sha256': hashlib.sha256(raw).hexdigest(), **counts}


def instrumentation_result(path):
    raw = path.read_bytes()
    text = raw.decode('utf-8')
    successes = re.findall(r'^OK \(([1-9][0-9]*) tests?\)', text, re.MULTILINE)
    if len(successes) != 1 or re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=', text):
        raise ValueError('Instrumentation did not finish successfully')
    return {'file': str(path), 'sha256': hashlib.sha256(raw).hexdigest(),
            'tests': int(successes[0]), 'failures': 0, 'errors': 0, 'skipped': 0}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('paths', type=Path, nargs='+')
    parser.add_argument('--instrumentation', action='store_true')
    args = parser.parse_args()
    files = []
    for path in args.paths:
        if args.instrumentation:
            files.append(path)
        elif path.is_dir():
            files.extend(sorted(path.rglob('TEST-*.xml')))
        else:
            files.append(path)
    if not files:
        raise ValueError('No test result files found')
    files = list(dict.fromkeys(files))
    results = [(instrumentation_result if args.instrumentation else junit_result)(p) for p in files]
    report = {'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
              'format': 'instrumentation' if args.instrumentation else 'junit', 'files': results,
              'totals': {key: sum(row[key] for row in results) for key in ('tests', 'failures', 'errors', 'skipped')},
              'scope': 'Technical result counts only; artifact preservation and human review remain separate gates'}
    encoded = json.dumps(report, indent=2)
    print(encoded)
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as stream:
            stream.write('## Source-bound test results\n\n```json\n'+encoded+'\n```\n')
    return int(report['totals']['tests'] == 0 or report['totals']['failures'] > 0 or report['totals']['errors'] > 0)


if __name__ == '__main__':
    raise SystemExit(main())
