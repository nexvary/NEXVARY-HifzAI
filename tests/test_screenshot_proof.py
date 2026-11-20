import tempfile
import unittest
from pathlib import Path
from tools.verify_screenshot_proof import verify_png

class ScreenshotProofTests(unittest.TestCase):
    def test_run_as_error_cannot_be_presented_as_screenshot(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / 'fake.png'
            path.write_bytes(b'run-as: unknown package: com.nexvary.hifzai\n')
            with self.assertRaisesRegex(ValueError, 'signature'):
                verify_png(path)
