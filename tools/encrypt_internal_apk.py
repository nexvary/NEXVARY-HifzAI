"""Host-only authenticated transport. No plaintext APK is uploaded from a public repo."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path


def encrypt(apk: Path, certificate: Path, directory: Path):
    if not apk.is_file() or not certificate.is_file():
        raise ValueError('Input file or public recipient certificate missing')
    # A public certificate is sufficient. This script never needs a private key.
    check = subprocess.run(['openssl','x509','-in',str(certificate),'-noout','-checkend','0'],capture_output=True)
    if check.returncode:
        raise ValueError('Recipient certificate invalid or expired')
    directory.mkdir(parents=True,exist_ok=True)
    output = directory/'internal-test.apk.cms'
    if output.exists(): raise ValueError('Refusing to overwrite existing encrypted build')
    subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-gcm','-in',str(apk),
        '-outform','DER','-out',str(output),'-recip',str(certificate),
        '-keyopt','rsa_padding_mode:oaep','-keyopt','rsa_oaep_md:sha256'],check=True,capture_output=True)
    cert = subprocess.check_output(['openssl','x509','-in',str(certificate),'-outform','DER'])
    report = {'purpose':'Internal development test only; not a scholarly submission or public release',
        'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),
        'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'apk_bytes':apk.stat().st_size,
        'encrypted_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),
        'public_recipient_certificate_sha256':hashlib.sha256(cert).hexdigest(),
        'encryption':'OpenSSL CMS AES-256-GCM and RSA-OAEP-SHA256',
        'scientific_review_ready':False,'public_release_allowed':False,'committee_contacted':False}
    (directory/'CANDIDATE.json').write_text(json.dumps(report,indent=2)+'\n')
    return output,report


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--apk',required=True,type=Path);p.add_argument('--certificate',required=True,type=Path)
    p.add_argument('--output',required=True,type=Path);a=p.parse_args()
    _,report=encrypt(a.apk,a.certificate,a.output)
    print(json.dumps({'source_commit':report['source_commit'],'encrypted_candidate_created':True,
                      'public_release_allowed':False}))

if __name__=='__main__':main()
