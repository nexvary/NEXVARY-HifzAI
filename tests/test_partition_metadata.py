import hashlib
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ASSET = Path(__file__).resolve().parents[1] / 'app/src/main/assets/quran/metadata-v1.0.xml'
class PartitionMetadataTests(unittest.TestCase):
    def test_official_file_is_unchanged_and_attributed(self):
        data = ASSET.read_bytes()
        self.assertEqual('8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a', hashlib.sha256(data).hexdigest())
        root = ET.fromstring(data)
        self.assertEqual('cc-by',root.attrib['license'])
        self.assertIn('Tanzil.info',root.attrib['copyright'])
        self.assertEqual('1.0',root.attrib['version'])
    def test_page_and_juz_start_indices_are_complete(self):
        root = ET.fromstring(ASSET.read_bytes())
        for container, child, count in [('pages','page',604),('juzs','juz',30)]:
            nodes = root.find(container).findall(child)
            self.assertEqual(list(range(1,count+1)),[int(n.attrib['index']) for n in nodes])
            self.assertEqual(('1','1'), (nodes[0].attrib['sura'],nodes[0].attrib['aya']))
