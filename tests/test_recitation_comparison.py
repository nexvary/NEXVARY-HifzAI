import copy
import datetime as dt
import json
import unittest
from tools.compare_recitation import compare


class PairedRecitationComparisonTests(unittest.TestCase):
    def fixture(self):
        # Synthetic linguistic fixtures; no real participants or recitation audio.
        return {'model_id': 'baseline-fixture', 'riwayah': 'hafs-an-asim', 'samples': [{
            'sample_id': 'private-id', 'speaker_id': 'private-speaker', 'split': 'evaluation',
            'region': 'EG', 'verse': 'fixture', 'consent': True, 'consent_record': 'private-consent',
            'rights_cleared': True, 'rights_record': 'private-rights', 'consent_expires_on': '2099-01-01',
            'human_transcript': 'نص تعليمي', 'hypothesis': 'نص', 'confidence': 0.99, 'expert_correct': True
        }]}

    def run_comparison(self, a, b):
        return compare(a, b, {'fixture': 'نص تعليمي'}, dt.date(2026, 11, 3))

    def test_paired_delta_and_private_aggregate_output(self):
        a = self.fixture()
        b = copy.deepcopy(a)
        b['model_id'] = 'candidate-fixture'
        b['samples'][0]['hypothesis'] = 'نص تعليمي'
        report = self.run_comparison(a, b)
        delta = report['groups']['all']['candidate_minus_baseline']
        self.assertEqual(-0.5, delta['wer'])
        self.assertLess(delta['cer'], 0)
        self.assertEqual(-1, delta['false_alarm_rate'])
        self.assertFalse(report['automatic_grading_enabled'])
        for private in ('private-id', 'private-speaker', 'private-consent', 'private-rights', 'نص تعليمي'):
            self.assertNotIn(private, json.dumps(report, ensure_ascii=False))

    def test_abstentions_have_separate_coverage_and_undefined_rate_delta(self):
        a = self.fixture()
        b = copy.deepcopy(a)
        b['samples'][0]['confidence'] = 0.1
        row = self.run_comparison(a, b)['groups']['all']
        self.assertEqual(-1, row['candidate_minus_baseline']['coverage'])
        self.assertIsNone(row['candidate_minus_baseline']['false_alarm_rate'])
        self.assertEqual(1, row['candidate']['abstained'])

    def test_different_references_participants_permissions_or_sample_sets_rejected(self):
        a = self.fixture()
        for field, value in [('human_transcript', 'نص مختلف'), ('speaker_id', 'another'),
                             ('sample_id', 'another'), ('expert_correct', False), ('region', 'SA'),
                             ('split', 'calibration'), ('consent_record', 'another'), ('rights_record', 'another'),
                             ('consent_revoked', True)]:
            b = copy.deepcopy(a)
            b['samples'][0][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.run_comparison(a, b)
        b = copy.deepcopy(a)
        b['dataset_version'] = 'different'
        with self.assertRaises(ValueError):
            self.run_comparison(a, b)

    def test_duplicate_validation_and_order_independence(self):
        a = self.fixture()
        a['samples'].append(dict(a['samples'][0], sample_id='second'))
        b = copy.deepcopy(a)
        b['samples'].reverse()
        self.assertEqual(2, self.run_comparison(a, b)['groups']['all']['samples'])
        b['samples'].append(copy.deepcopy(b['samples'][0]))
        with self.assertRaises(ValueError):
            self.run_comparison(a, b)
