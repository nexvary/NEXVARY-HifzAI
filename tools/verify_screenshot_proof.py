"""Reject error messages disguised as PNG files; retain exact source/image digests."""
import hashlib
import json
import struct
import subprocess
import sys
import zlib
from pathlib import Path

PAGES = 'home reader today exercises chapters search practice recall similar insights settings privacy sources teachers help about reciters'.split()

def verify_png(path):
    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(f'{path.name}: invalid PNG signature')
    offset, chunks, size, complete = 8, [], None, False
    while offset < len(data):
        length = struct.unpack('>I', data[offset:offset+4])[0]
        kind = data[offset+4:offset+8]
        payload = data[offset+8:offset+8+length]
        crc = struct.unpack('>I', data[offset+8+length:offset+12+length])[0]
        if zlib.crc32(kind + payload) != crc:
            raise ValueError('PNG checksum failed')
        if kind == b'IHDR': size = struct.unpack('>II', payload[:8])
        if kind == b'IDAT': chunks.append(payload)
        if kind == b'IEND': complete = True
        offset += 12 + length
    if not complete or size is None or min(size) < 200:
        raise ValueError('Incomplete or too small screenshot')
    pixels = zlib.decompress(b''.join(chunks))
    if len(pixels) < 10000 or len(set(pixels)) < 16:
        raise ValueError('Empty screenshot')
    return {'file': path.name, 'width': size[0], 'height': size[1],
            'sha256': hashlib.sha256(data).hexdigest()}


def main():
    directory = Path(sys.argv[1])
    files = [verify_png(directory / f'hifz-{page}.png') for page in PAGES]
    source = subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
    apk = Path('app/build/outputs/apk/debug/app-debug.apk')
    report = {'source_commit':source,'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
              'origin':'Compose captureToImage during Android instrumented tests; no generated mockups',
              'screenshots':files,'visual_scholarly_acceptance':False}
    (directory / 'PROOF_MANIFEST.json').write_text(json.dumps(report,indent=2)+'\n')
    print(f'Validated {len(files)} real PNG containers; human visual review remains required')

if __name__ == '__main__': main()
