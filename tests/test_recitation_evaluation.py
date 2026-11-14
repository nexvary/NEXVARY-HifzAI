import copy
import datetime as dt
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from tools.evaluate_recitation import alignment, evaluate, canonical_references, character_alignment, main


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

    def test_held_out_region_and_ayah_counts_exclude_calibration_speakers(self):
        data = self.fixture()
        sample = data['samples'][0]
        data['samples'].append(dict(sample, split='calibration', sample_id='training',
                                    speaker_id='training-speaker', hypothesis=sample['human_transcript']))
        groups = self.run_fixture(data)['groups']
        self.assertAlmostEqual(1/6, groups['region:EG']['wer'])
        self.assertAlmostEqual(1/3, groups['split:evaluation/region:EG']['wer'])
        self.assertEqual(1, groups['split:evaluation/verse:fixture']['samples'])
        self.assertEqual(0, groups['split:calibration/verse:fixture']['wer'])

    def test_cli_refuses_to_overwrite_dataset_through_any_alias(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory, 'dataset.json')
            source.write_text('private dataset', encoding='utf-8')
            symbolic = source.with_name('symbolic.json')
            symbolic.symlink_to(source)
            hard = source.with_name('hard.json')
            os.link(source, hard)
            for output in (source, symbolic, hard):
                with self.subTest(output=output), patch('sys.argv', ['evaluate_recitation', str(source), '--output', str(output)]):
                    with self.assertRaisesRegex(ValueError, 'must not overwrite'):
                        main()
                self.assertEqual('private dataset', source.read_text(encoding='utf-8'))

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

    def test_character_metric_uses_human_transcript_and_normalization(self):
        self.assertEqual({'reference_characters': 2, 'character_distance': 0},
                         character_alignment('أَ ب', 'ا ب'))
        self.assertEqual(1, character_alignment('اب', 'اج')['character_distance'])
        self.assertEqual(3, character_alignment('اب', 'ابجده')['character_distance'])
        data = self.fixture()
        data['samples'][0].update(human_transcript='اب', hypothesis='اج')
        report = self.run_fixture(data)
        for row in report['groups'].values():
            self.assertEqual(0.5, row['cer'])
            self.assertEqual(2, row['reference_characters'])
        data['samples'][0]['hypothesis'] = ''
        row = self.run_fixture(data)['groups']['all']
        self.assertEqual(1, row['cer'])
        self.assertEqual(1, row['abstained'])
        self.assertIsNone(row['false_alarm_rate'])
        with self.assertRaises(ValueError):
            character_alignment('x' * 4097, '')

    def test_revoked_or_ambiguous_consent_and_rights_are_rejected(self):
        for field in ('consent_revoked', 'rights_revoked'):
            for revoked in (True, 0, None, 'false'):
                data = self.fixture()
                data['samples'][0][field] = revoked
                with self.subTest(field=field, revoked=revoked), self.assertRaises(ValueError):
                    self.run_fixture(data)
            data = self.fixture()
            data['samples'][0][field] = False
            self.assertEqual(1, self.run_fixture(data)['groups']['all']['samples'])
