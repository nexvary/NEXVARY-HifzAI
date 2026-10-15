import subprocess
import tempfile
import unittest
from pathlib import Path
from tools.encrypt_internal_apk import encrypt


class InternalTransportTests(unittest.TestCase):
    def test_authenticated_round_trip_and_tamper_rejection(self):
        with tempfile.TemporaryDirectory() as temp:
            p=Path(temp);key=p/'key.pem';cert=p/'cert.pem';plain=p/'fixture.bin'
            subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-sha256','-nodes','-days','1',
                '-subj','/CN=synthetic transport test','-keyout',str(key),'-out',str(cert)],check=True,capture_output=True)
            plain.write_bytes(b'Synthetic binary transport fixture, not an APK or Quran.\x00\xff')
            encrypted,report=encrypt(plain,cert,p/'encrypted')
            result=p/'decoded.bin'
            command=['openssl','cms','-decrypt','-binary','-inform','DER','-in',str(encrypted),
                '-recip',str(cert),'-inkey',str(key),'-out',str(result)]
            subprocess.run(command,check=True,capture_output=True)
            self.assertEqual(plain.read_bytes(),result.read_bytes())
            self.assertFalse(report['public_release_allowed']);self.assertFalse(report['scientific_review_ready'])
            data=bytearray(encrypted.read_bytes());data[-1]^=1;encrypted.write_bytes(data)
            self.assertNotEqual(0,subprocess.run(command,capture_output=True).returncode)
            self.assertEqual({'internal-test.apk.cms','CANDIDATE.json'},{f.name for f in (p/'encrypted').iterdir()})
    def test_missing_certificate_fails_before_output(self):
        with tempfile.TemporaryDirectory() as temp:
            p=Path(temp);f=p/'fixture';f.write_bytes(b'synthetic')
            with self.assertRaises(ValueError):encrypt(f,p/'missing.pem',p/'output')
            self.assertFalse((p/'output').exists())
