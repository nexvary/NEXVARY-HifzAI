import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from tools.prepare_review_bundle import REQUIRED, readiness

class ReviewBundleGateTests(unittest.TestCase):
    @patch('tools.prepare_review_bundle.subprocess.check_output', return_value='abc\n')
    def test_empty_manifest_cannot_be_submitted(self, _):
        with tempfile.TemporaryDirectory() as d:
            result = readiness(Path(d), {})
            self.assertFalse(result['ready_for_owner_submission_decision'])
            self.assertFalse(result['public_release_allowed'])
            self.assertFalse(result['committee_contacted'])
            self.assertEqual(len(REQUIRED) + 2, len(result['blockers']))

    @patch('tools.prepare_review_bundle.subprocess.check_output', return_value='abc\n')
    def test_verified_label_without_evidence_is_rejected(self, _):
        gates = {name: {'status':'verified','reviewer':'Internal tester','evidence':[]} for name in REQUIRED}
        with tempfile.TemporaryDirectory() as d:
            result = readiness(Path(d), {'source_commit':'abc','gates':gates})
            self.assertFalse(result['ready_for_owner_submission_decision'])
            self.assertTrue(any('no evidence' in b for b in result['blockers']))

    @patch('tools.prepare_review_bundle.subprocess.check_output', return_value='abc\n')
    def test_evidence_cannot_escape_repository(self, _):
        with tempfile.TemporaryDirectory() as d:
            gates = {REQUIRED[0]: {'status':'verified','reviewer':'Tester',
                'evidence':[{'path':'../secret','sha256':'a'*64}]}}
            result = readiness(Path(d), {'source_commit':'abc','gates':gates})
            self.assertTrue(any('unsafe' in b for b in result['blockers']))
