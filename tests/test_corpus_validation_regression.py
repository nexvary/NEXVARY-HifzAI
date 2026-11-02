"""Regression tests for corpus structure and fail-closed behavior.

Synthetic verses only. Passing these tests does NOT authenticate Quran text.
"""
import unittest

from tools.validate_corpus import CorpusError, validate


class CorpusValidationRegressionTests(unittest.TestCase):
    def setUp(self):
        self.payload = {
            "source": "synthetic-test-only",
            "license": "test fixture",
            "source_url": "https://example.org/synthetic",
            "verses": [{"surah": 1, "ayah": 1, "text": "اختبار"}],
        }

    def test_partial_fixture_is_rejected_in_release_mode(self):
        with self.assertRaises(CorpusError):
            validate(self.payload)

    def test_partial_fixture_is_allowed_only_when_explicit(self):
        self.assertEqual(validate(self.payload, require_full=False), 1)

    def test_duplicate_verse_rejected(self):
        self.payload["verses"].append(dict(self.payload["verses"][0]))
        with self.assertRaisesRegex(CorpusError, "Duplicate"):
            validate(self.payload, require_full=False)

    def test_boolean_verse_number_rejected(self):
        self.payload["verses"][0]["ayah"] = True
        with self.assertRaises(CorpusError):
            validate(self.payload, require_full=False)

    def test_out_of_range_verse_rejected(self):
        self.payload["verses"][0]["ayah"] = 8
        with self.assertRaises(CorpusError):
            validate(self.payload, require_full=False)

    def test_html_injection_rejected(self):
        self.payload["verses"][0]["text"] = "اختبار<script>alert(1)</script>"
        with self.assertRaises(CorpusError):
            validate(self.payload, require_full=False)

    def test_non_https_provenance_rejected(self):
        self.payload["source_url"] = "http://example.org/synthetic"
        with self.assertRaises(CorpusError):
            validate(self.payload, require_full=False)

    def test_whitespace_mutation_rejected(self):
        self.payload["verses"][0]["text"] = " اختبار"
        with self.assertRaises(CorpusError):
            validate(self.payload, require_full=False)

    def test_non_arabic_verse_rejected(self):
        self.payload["verses"][0]["text"] = "placeholder"
        with self.assertRaises(CorpusError):
            validate(self.payload, require_full=False)


if __name__ == "__main__":
    unittest.main()
