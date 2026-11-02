import copy
import datetime as dt
import json
import unittest
from tools.evaluate_recitation import alignment, evaluate, canonical_references


class RecitationEvaluationTests(unittest.TestCase):
    # Explicitly synthetic non-Quran linguistic fixtures. No audio or real participants.
    def fixture(self):
        return {'model_id': 'synthetic-unit-fixture', 'riwayah': 'hafs-an-asim', 'samples': [{
            'sample_id': 'fixture-1', 'speaker_id': 'synthetic-speaker-1', 'split': 'evaluation',
            'region': 'EG', 'verse': 'fixture', 'consent': True, 'consent_record': 'synthetic-fixture',
            'rights_cleared': True, 'rights_record': 'synthetic-fixture',
            'consent_expires_on': '2099-01-01', 'human_transcript': 'نص تعليمي للاختبار',
            'hypothesis': 'نص للاختبار', 'confidence': 0.99, 'expert_correct': True
        }]}

    def run_fixture(self, data):
        return evaluate(data, {'fixture': 'نص تعليمي للاختبار'}, dt.date(2026, 11, 2))

    def test_edit_operations_and_repeated_words(self):
        self.assertEqual(1, alignment('ألف باء جيم', 'ألف جيم')['omitted'])
        self.assertEqual(1, alignment('ألف باء', 'ألف جديد باء')['inserted'])
        self.assertEqual(1, alignment('ألف باء', 'ألف جيم')['substituted'])
        self.assertEqual(1, alignment('ألف ألف باء', 'ألف باء')['distance'])
        self.assertEqual(0, alignment('نَصّ تَعْلِيمِيّ', 'نص تعليمي')['distance'])

    def test_metrics_and_private_output(self):
        report = self.run_fixture(self.fixture())
        row = report['groups']['all']
        self.assertAlmostEqual(1/3, row['wer'])
        self.assertEqual(1, row['false_alarm_rate'])
        self.assertIsNone(row['missed_error_rate'])
        self.assertFalse(report['automatic_grading_enabled'])
        encoded = json.dumps(report)
        for private in ('synthetic-speaker-1', 'نص للاختبار', 'consent_record', 'fixture-1'):
            self.assertNotIn(private, encoded)

    def test_human_transcript_is_asr_reference_not_intended_ayah(self):
        data = self.fixture()
        data['samples'][0].update(human_transcript='نص للاختبار', expert_correct=False)
        row = self.run_fixture(data)['groups']['all']
        self.assertEqual(0, row['wer'])  # Recognizer heard the erroneous recitation accurately.
        self.assertEqual(0, row['missed_error_rate'])
        data['samples'][0]['hypothesis'] = 'نص تعليمي للاختبار'
        self.assertEqual(1, self.run_fixture(data)['groups']['all']['missed_error_rate'])

    def test_abstention_not_counted_as_correct_or_missed(self):
        data = self.fixture()
        for hypothesis, confidence in (('نص للاختبار', 0.2), ('', 1.0)):
            data['samples'][0].update(hypothesis=hypothesis, confidence=confidence)
            row = self.run_fixture(data)['groups']['all']
            self.assertEqual(1, row['abstained'])
            self.assertEqual(0, row['coverage'])
            self.assertIsNone(row['false_alarm_rate'])

    def test_consent_rights_and_expiry_required(self):
        for key, bad in [('consent', False), ('consent', 1), ('rights_cleared', False),
                         ('consent_record', ''), ('rights_record', ''), ('consent_expires_on', '2026-01-01')]:
            data = self.fixture()
            data['samples'][0][key] = bad
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.run_fixture(data)

    def test_duplicate_samples_speaker_leakage_wrong_riwayah(self):
        data = self.fixture()
        data['samples'].append(copy.deepcopy(data['samples'][0]))
        with self.assertRaises(ValueError):
            self.run_fixture(data)
        data['samples'][1].update(sample_id='fixture-2', split='calibration')
        with self.assertRaises(ValueError):
            self.run_fixture(data)
        data = self.fixture()
        data['riwayah'] = 'warsh'
        with self.assertRaises(ValueError):
            self.run_fixture(data)

    def test_invalid_confidence_unknown_reference_and_oversize_rejected(self):
        for key, bad in [('confidence', float('nan')), ('confidence', True), ('confidence', 1.1),
                         ('verse', 'unknown'), ('expert_correct', 'yes'), ('hypothesis', 'word '*513),
                         ('human_transcript', ''), ('hypothesis', 'x'*20_001)]:
            data = self.fixture()
            data['samples'][0][key] = bad
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.run_fixture(data)

    def test_real_source_references_have_original_boundaries(self):
        refs = canonical_references()
        self.assertEqual(6236, len(refs))
        self.assertTrue(refs['1:1'])
        self.assertTrue(refs['114:6'])
        self.assertNotEqual(refs['1:1'], refs['2:1'])
