"""Run actual tests and retain a source-bound JSON result, including failures."""
import hashlib
import json
import subprocess
import sys
import time
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))


class EvidenceResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.cases = []

    def addSuccess(self, test):
        super().addSuccess(test)
        self.cases.append({'test': test.id(), 'status': 'passed'})

    def addFailure(self, test, error):
        super().addFailure(test, error)
        self.cases.append({'test': test.id(), 'status': 'failed'})

    def addError(self, test, error):
        super().addError(test, error)
        self.cases.append({'test': test.id(), 'status': 'error'})

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        self.cases.append({'test': test.id(), 'status': 'skipped', 'reason': reason})


def main():
    suite = unittest.defaultTestLoader.discover(str(ROOT / 'tests'))
    started = time.monotonic()
    result = unittest.TextTestRunner(verbosity=2, resultclass=EvidenceResult).run(suite)
    files = sorted(list((ROOT/'academy').glob('*.py')) + list((ROOT/'tests').glob('*.py')) +
                   list((ROOT/'tools').glob('*.py')))
    report = {
        'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        'working_tree_dirty': bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip()),
        'python': sys.version.split()[0], 'duration_seconds': round(time.monotonic()-started, 3),
        'tests_run': result.testsRun, 'failures': len(result.failures), 'errors': len(result.errors),
        'skipped': len(result.skipped), 'successful': result.wasSuccessful(), 'cases': result.cases,
        'source_sha256': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in files},
        'scope': 'Technical tests with synthetic academy records; not legal, child-safety or scholarly approval'
    }
    output = ROOT/'build/python/test-results.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, ensure_ascii=False)+'\n')
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    sys.exit(main())
