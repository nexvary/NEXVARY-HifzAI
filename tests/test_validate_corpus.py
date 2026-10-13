import copy
import unittest
from tools.validate_corpus import CorpusError, VERSE_COUNTS, validate

class CorpusValidationTests(unittest.TestCase):
    def setUp(self):
        self.payload = {
            "source": "offline-fixture-not-for-publication",
            "license": "test-only",
            "source_url": "https://example.org/test",
            "verses": [
                {"surah": 1, "ayah": 1, "text": "بِسْمِ اللَّهِ"},
                {"surah": 1, "ayah": 2, "text": "الْحَمْدُ لِلَّهِ"},
            ]
        }

    def test_total_counts(self):
        self.assertEqual(len(VERSE_COUNTS), 114)
        self.assertEqual(sum(VERSE_COUNTS), 6236)

    def test_partial_fixture_is_structurally_valid(self):
        self.assertEqual(validate(self.payload, require_full=False), 2)

    def test_demo_rejected_as_complete_corpus(self):
        with self.assertRaises(CorpusError):
            validate(self.payload)

    def test_duplicate_ayah_rejected(self):
        bad = copy.deepcopy(self.payload)
        bad["verses"][1]["ayah"] = 1
        with self.assertRaisesRegex(CorpusError, "Duplicate"):
            validate(bad, require_full=False)

    def test_bad_ayah_index_rejected(self):
        bad = copy.deepcopy(self.payload)
        bad["verses"][1]["ayah"] = 800
        with self.assertRaisesRegex(CorpusError, "Invalid ayah"):
            validate(bad, require_full=False)

    def test_html_and_non_arabic_rejected(self):
        for bad_text in ["<b>اللَّهِ</b>", "hello", ""]:
            bad = copy.deepcopy(self.payload)
            bad["verses"][0]["text"] = bad_text
            with self.assertRaises(CorpusError):
                validate(bad, require_full=False)

    def test_untrusted_plain_http_attribution_rejected(self):
        bad = copy.deepcopy(self.payload)
        bad["source_url"] = "http://example.org"
        with self.assertRaises(CorpusError):
            validate(bad, require_full=False)

if __name__ == "__main__":
    unittest.main()
