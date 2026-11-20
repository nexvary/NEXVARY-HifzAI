"""BUILD-HOST ONLY. Download a pinned MIT model; the Android app never downloads it."""
import hashlib
import json
import os
import shutil
import urllib.request
from pathlib import Path

SHA256 = 'be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21'
SIZE = 77691713
REVISION = '5359861c739e955e79d9a303bcbc70fb988958b1'
URL = f'https://huggingface.co/ggerganov/whisper.cpp/resolve/{REVISION}/ggml-tiny.bin'
ROOT = Path(__file__).resolve().parents[1]


def main():
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise ValueError('Android SDK path required to preserve native-runtime license notices')
    notice = Path(sdk)/'ndk/28.2.13676358/NOTICE'
    if not notice.is_file():
        raise ValueError('Pinned NDK license notice missing')
    notice_output = ROOT/'app/build/generated/asr-assets/licenses/Android-NDK-NOTICE.txt'
    notice_output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(notice, notice_output)
    output = ROOT/'app/build/generated/asr-assets/asr/ggml-tiny.bin'
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.is_file() and output.stat().st_size == SIZE and hashlib.sha256(output.read_bytes()).hexdigest() == SHA256:
        print('Verified pinned local model asset')
        return
    temporary = output.with_suffix('.part')
    digest, size = hashlib.sha256(), 0
    try:
        with urllib.request.urlopen(URL, timeout=60) as response, temporary.open('wb') as target:
            while True:
                block = response.read(1024*1024)
                if not block:
                    break
                size += len(block)
                if size > SIZE:
                    raise ValueError('Oversized upstream model')
                digest.update(block)
                target.write(block)
        if size != SIZE or digest.hexdigest() != SHA256:
            raise ValueError('Upstream model integrity failed')
        os.replace(temporary, output)
    finally:
        temporary.unlink(missing_ok=True)
    print(json.dumps({'model_sha256': SHA256, 'bytes': size, 'upstream_revision': REVISION}))


if __name__ == '__main__':
    main()
