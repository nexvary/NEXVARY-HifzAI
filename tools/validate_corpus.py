#!/usr/bin/env python3
"""Structural validation of an externally supplied canonical Quran dataset.

This cannot certify the accuracy of the Arabic text by itself.
The Quran text must be obtained from a trusted licensed source and its
checksum compared against a *separately authenticated* checksum.
"""
import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

VERSE_COUNTS = (
    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,
    112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,
    89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,
    12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,
    19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6
)
assert len(VERSE_COUNTS) == 114 and sum(VERSE_COUNTS) == 6236
ARABIC = re.compile(r"[\u0621-\u064a\u0671-\u06d3]")
HTML = re.compile(r"<[^>]*>")

class CorpusError(ValueError):
    pass

def validate(payload: object, *, require_full: bool = True) -> int:
    if not isinstance(payload, dict):
        raise CorpusError("Root object must be a dictionary")
    for field in ("source", "license", "source_url", "verses"):
        if field not in payload:
            raise CorpusError(f"Missing required field: {field}")
    if any(not isinstance(payload[field], str) or not payload[field].strip()
           for field in ("source", "license", "source_url")):
        raise CorpusError("Source, license and source_url must be nonempty strings")
    if not payload["source_url"].startswith("https://"):
        raise CorpusError("Source URL must use HTTPS")
    verses = payload["verses"]
    if not isinstance(verses, list) or not verses:
        raise CorpusError("Verses must be a nonempty array")
    seen = set()
    per_chapter: dict[int, set[int]] = {}
    for idx, verse in enumerate(verses, 1):
        if not isinstance(verse, dict):
            raise CorpusError(f"Verse {idx} is not an object")
        chapter, ayah, text = (verse.get("surah"), verse.get("ayah"), verse.get("text"))
        if type(chapter) is not int or chapter not in range(1, 115):
            raise CorpusError(f"Invalid surah in row {idx}")
        if type(ayah) is not int or not 1 <= ayah <= VERSE_COUNTS[chapter - 1]:
            raise CorpusError(f"Invalid ayah in row {idx}")
        if not isinstance(text, str) or not text or not ARABIC.search(text):
            raise CorpusError(f"Invalid Arabic verse text in row {idx}")
        if text != text.strip() or "\n" in text or HTML.search(text):
            raise CorpusError(f"Unexpected whitespace or HTML in row {idx}")
        key = (chapter, ayah)
        if key in seen:
            raise CorpusError(f"Duplicate verse {chapter}:{ayah}")
        seen.add(key)
        per_chapter.setdefault(chapter, set()).add(ayah)
    if require_full:
        if len(verses) != 6236:
            raise CorpusError(f"Expected 6236 ayat, got {len(verses)}")
        for chapter, count in enumerate(VERSE_COUNTS, 1):
            if per_chapter.get(chapter, set()) != set(range(1, count + 1)):
                raise CorpusError(f"Incomplete or invalid surah {chapter}")
    return len(verses)

def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Validate shape and checksum of Quran corpus")
    parser.add_argument("json_file", type=Path)
    parser.add_argument("--expected-sha256", help="Pinned checksum from independent trusted source")
    args = parser.parse_args(argv)
    try:
        raw = args.json_file.read_bytes()
        digest = hashlib.sha256(raw).hexdigest()
        if args.expected_sha256 and digest.lower() != args.expected_sha256.lower():
            raise CorpusError("SHA256 mismatch: supplied data is not the authenticated release")
        count = validate(json.loads(raw.decode("utf-8")), require_full=True)
    except (OSError, UnicodeError, json.JSONDecodeError, CorpusError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    print(f"Structural validation OK: {count} ayat; SHA256={digest}")
    if not args.expected_sha256:
        print("WARNING: authenticity NOT established; supply an independently trusted checksum")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
