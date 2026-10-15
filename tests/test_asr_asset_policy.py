import hashlib
import unittest
from pathlib import Path
from tools.prepare_whisper_model import SHA256, SIZE, URL

ROOT = Path(__file__).resolve().parents[1]

class AsrAssetPolicyTests(unittest.TestCase):
    def test_model_is_pinned_and_runtime_sources_do_not_download(self):
        self.assertEqual(77691713,SIZE)
        self.assertEqual('be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21',SHA256)
        self.assertIn('5359861c739e955e79d9a303bcbc70fb988958b1',URL)
        source=(ROOT/'app/src/main/java/com/nexvary/hifzai/learning/LocalWhisperModel.kt').read_text()
        self.assertIn(SHA256,source)
        self.assertNotIn('https://',source)
        cmake=(ROOT/'app/src/main/cpp/CMakeLists.txt').read_text()
        self.assertIn('set(WHISPER_CURL OFF',cmake)
        self.assertIn('set(GGML_RPC OFF',cmake)
        self.assertIn('URL_HASH SHA256=645445da884df6dbd52b30f82d7be7c01d1767c938714e477e786b88d529e9f4',cmake)

    def test_model_cannot_feed_scores_or_canonical_quran(self):
        source=(ROOT/'app/src/main/java/com/nexvary/hifzai/learning/LocalTranscription.kt').read_text()
        for forbidden in ('ProgressStore(', 'saveSession(', 'HifzScheduler', 'QuranCorpus.parse', 'createSpeechRecognizer'):
            self.assertNotIn(forbidden,source)
        for filename in ('Whisper-model-MIT.txt','whisper-cpp-MIT.txt'):
            self.assertIn('MIT License',(ROOT/'app/src/main/assets/licenses'/filename).read_text())
        raw=(ROOT/'app/src/main/assets/quran/uthmani-v1.1.txt').read_bytes()
        self.assertEqual('6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f',hashlib.sha256(raw).hexdigest())
