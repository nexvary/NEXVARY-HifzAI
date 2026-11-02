import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from tools.prepare_review_bundle import REQUIRED, readiness

class ReviewBundleGateTests(unittest.TestCase):
    @patch('tools.prepare_review_bundle.subprocess.check_output', side_effect=['abc\n', ''])
    def test_empty_manifest_cannot_be_submitted(self, _):
        with tempfile.TemporaryDirectory() as d:
            result = readiness(Path(d), {})
            self.assertFalse(result['ready_for_owner_submission_decision'])
            self.assertFalse(result['public_release_allowed'])
            self.assertFalse(result['committee_contacted'])
            self.assertEqual(len(REQUIRED) + 2, len(result['blockers']))

    @patch('tools.prepare_review_bundle.subprocess.check_output', side_effect=['abc\n', ''])
    def test_verified_label_without_evidence_is_rejected(self, _):
        gates = {name: {'status':'verified','reviewer':'Internal tester','evidence':[]} for name in REQUIRED}
        with tempfile.TemporaryDirectory() as d:
            result = readiness(Path(d), {'source_commit':'abc','gates':gates})
            self.assertFalse(result['ready_for_owner_submission_decision'])
            self.assertTrue(any('no evidence' in b for b in result['blockers']))

    @patch('tools.prepare_review_bundle.subprocess.check_output', side_effect=['abc\n', ''])
    def test_evidence_cannot_escape_repository(self, _):
        with tempfile.TemporaryDirectory() as d:
            gates = {REQUIRED[0]: {'status':'verified','reviewer':'Tester',
                'evidence':[{'path':'../secret','sha256':'a'*64}]}}
            result = readiness(Path(d), {'source_commit':'abc','gates':gates})
            self.assertTrue(any('unsafe' in b for b in result['blockers']))

    def test_dirty_or_untracked_sources_block_otherwise_verified_candidate(self):
        import hashlib
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            evidence = root / 'evidence.txt'
            evidence.write_text('synthetic technical evidence, not approval')
            digest = hashlib.sha256(evidence.read_bytes()).hexdigest()
            gates = {name: {'status': 'verified', 'reviewer': 'Synthetic test reviewer',
                'evidence': [{'path': 'evidence.txt', 'sha256': digest}]} for name in REQUIRED}
            manifest = {'source_commit': 'abc', 'gates': gates,
                'apk': {'path': 'evidence.txt', 'sha256': digest}}
            # This fixture only exercises gate logic; the file is not an APK or real approval.
            for status in ('', ' M app/source.kt\n', '?? untracked-source.kt\n', 'D  source.kt\n'):
                with self.subTest(status=status), patch(
                    'tools.prepare_review_bundle.subprocess.check_output', side_effect=['abc\n', status]):
                    result = readiness(root, manifest)
                    self.assertEqual(not bool(status), result['ready_for_owner_submission_decision'])
                    self.assertFalse(result['public_release_allowed'])
                    if status:
                        self.assertTrue(any('Working tree' in b for b in result['blockers']))

    def test_real_git_status_detects_modified_and_untracked_source(self):
        import subprocess
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            def git(*args):
                return subprocess.check_output(['git', *args], cwd=root, text=True).strip()
            git('init', '-q')
            source = root / 'source.txt'
            source.write_text('reviewed source')
            git('add', 'source.txt')
            git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid',
                'commit', '-qm', 'Synthetic gate regression fixture')
            manifest = {'source_commit': git('rev-parse', 'HEAD')}
            self.assertFalse(any('Working tree' in b for b in readiness(root, manifest)['blockers']))
            source.write_text('unreviewed modification')
            self.assertTrue(any('Working tree' in b for b in readiness(root, manifest)['blockers']))
            git('checkout', '--', 'source.txt')
            (root / 'new-source.txt').write_text('unreviewed new file')
            self.assertTrue(any('Working tree' in b for b in readiness(root, manifest)['blockers']))
