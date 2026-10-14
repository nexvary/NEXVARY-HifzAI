"""Fail-closed verification of vendored Tanzil Uthmani v1.1 Quran asset.

This pins the exact Git blob of a verbatim third-party copy, not scholarly
approval. Source: dotquran/corpus src/resources/uthmani.txt (Git object
9813920ae5afce9e50d2ac44562457e29bb4bfca).
"""
from pathlib import Path
import hashlib
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/quran/uthmani-v1.1.txt"
METADATA = ROOT / "app/src/main/assets/quran/surah-metadata.txt"
PINNED_SHA256 = "6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f"
PINNED_GIT_SHA1 = "9813920ae5afce9e50d2ac44562457e29bb4bfca"

class EmbeddedQuranIntegrityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = ASSET.read_bytes()
        cls.text = cls.raw.decode("utf-8")
        cls.meta = METADATA.read_text(encoding="utf-8")

    def test_verbatim_git_blob_identity(self):
        prefix = b"blob " + str(len(self.raw)).encode() + b"\0"
        self.assertEqual(hashlib.sha1(prefix + self.raw).hexdigest(), PINNED_GIT_SHA1)

    def test_sha256_independent_digest(self):
        self.assertEqual(hashlib.sha256(self.raw).hexdigest(), PINNED_SHA256)

    def test_license_and_source_version_preserved(self):
        self.assertIn("Tanzil Quran Text (Uthmani, Version 1.1)", self.text)
        self.assertIn("License: Creative Commons Attribution 3.0", self.text)
        self.assertIn("CHANGING IT IS NOT ALLOWED", self.text)
        self.assertIn("tanzil.net", self.text)

    def test_all_surahs_and_ayat_count_and_order(self):
        lines = [line for line in self.text.splitlines()
                 if line and not line.startswith("#")]
        self.assertEqual(len(lines), 6236)
        names = [line.split("|", 2) for line in self.meta.splitlines() if line]
        self.assertEqual(len(names), 114)
        self.assertEqual(sum(int(row[2]) for row in names), 6236)
        last = {i: 0 for i in range(1, 115)}
        for line in lines:
            parts = line.split("|", 2)
            self.assertEqual(len(parts), 3)
            surah, ayah = int(parts[0]), int(parts[1])
            self.assertTrue(parts[2].strip())
            self.assertIn(surah, last)
            self.assertEqual(ayah, last[surah] + 1)
            last[surah] = ayah
        for chapter, title, count in names:
            chapter_number = int(chapter)
            self.assertEqual(last[chapter_number], int(count))
            self.assertTrue(title.strip())
        self.assertEqual(lines[0].split("|", 2)[:2], ["1", "1"])
        self.assertEqual(lines[-1].split("|", 2)[:2], ["114", "6"])

    def test_source_is_not_generated_or_silently_normalized(self):
        self.assertIn("2|1|", self.text)
        self.assertIn("9|1|", self.text)
        self.assertNotIn("\ufffd", self.text)
