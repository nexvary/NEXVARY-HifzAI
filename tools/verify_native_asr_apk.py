"""Inspect the built APK, pinned model, offline notices and 16KiB native ELF packaging."""
import argparse
import hashlib
import json
import struct
import subprocess
import zipfile
from pathlib import Path
from prepare_whisper_model import SHA256, SIZE

MACHINES = {'armeabi-v7a': 40, 'arm64-v8a': 183, 'x86': 3, 'x86_64': 62}


def elf_load_alignments(data, machine):
    if len(data) < 64 or data[:4] != b'\x7fELF' or data[5] != 1 or data[4] not in (1, 2):
        raise ValueError('Invalid little-endian Android ELF')
    if struct.unpack_from('<HH', data, 16) != (3, machine):
        raise ValueError('ELF must be a shared library for the expected ABI')
    wide = data[4] == 2
    if wide != (machine in (62, 183)):
        raise ValueError("ELF class does not match its ABI")
    phoff = struct.unpack_from('<Q' if wide else '<I', data, 32 if wide else 28)[0]
    entry, count = struct.unpack_from('<HH', data, 54 if wide else 42)
    expected = 56 if wide else 32
    if entry != expected or not 1 <= count <= 128 or phoff + count*entry > len(data):
        raise ValueError('Invalid ELF program headers')
    alignments = []
    for n in range(count):
        offset = phoff+n*entry
        if struct.unpack_from('<I', data, offset)[0] == 1:
            alignment = struct.unpack_from('<Q' if wide else '<I', data, offset+(48 if wide else 28))[0]
            if alignment < 16384 or alignment & (alignment-1):
                raise ValueError('Native PT_LOAD alignment does not support 16KiB pages')
            alignments.append(alignment)
    if not alignments:
        raise ValueError('ELF has no loadable segments')
    return alignments


def verify(apk, abis):
    report = {'source_commit': subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),
              'apk_sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
              'apk_size_bytes': apk.stat().st_size, 'native_libraries': [],
              'asr_accuracy_evaluated': False}
    with zipfile.ZipFile(apk) as archive:
        model = archive.getinfo('assets/asr/ggml-tiny.bin')
        if model.file_size != SIZE:
            raise ValueError('Unexpected model size')
        digest = hashlib.sha256()
        with archive.open(model) as stream:
            for block in iter(lambda: stream.read(1024*1024), b''): digest.update(block)
        if digest.hexdigest() != SHA256:
            raise ValueError('Bundled model digest mismatch')
        report['model_sha256'] = SHA256
        for name in ('Whisper-model-MIT.txt','whisper-cpp-MIT.txt','Android-NDK-NOTICE.txt'):
            content = archive.read('assets/licenses/'+name)
            if len(content) < 500: raise ValueError('Missing or incomplete native/model license notice')
        actual = sorted(p.filename.split('/')[1] for p in archive.infolist()
                        if p.filename.startswith('lib/') and p.filename.endswith('/libhifz_whisper.so'))
        if actual != sorted(abis): raise ValueError('Unexpected native ABI coverage')
        for abi in abis:
            info = archive.getinfo(f'lib/{abi}/libhifz_whisper.so')
            try:
                alignments = elf_load_alignments(archive.read(info), MACHINES[abi])
            except ValueError as error:
                raise ValueError(f"{abi}: {error}") from error
            with apk.open('rb') as source:
                source.seek(info.header_offset)
                header = source.read(30)
            if header[:4] != b'PK\x03\x04': raise ValueError('Invalid ZIP local header')
            filename, extra = struct.unpack_from('<HH',header,26)
            offset = info.header_offset+30+filename+extra
            if info.compress_type == zipfile.ZIP_STORED and offset % 16384:
                raise ValueError('Uncompressed native library ZIP alignment is below 16KiB')
            report['native_libraries'].append({'abi':abi,'load_alignments':alignments,
                'zip_data_offset':offset,'compressed':info.compress_type != zipfile.ZIP_STORED})
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk',type=Path)
    parser.add_argument('--abi',action='append',choices=list(MACHINES))
    args = parser.parse_args()
    report = verify(args.apk,args.abi or list(MACHINES))
    output = Path('build/native-asr/packaging.json'); output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,indent=2)+'\n')
    print('Pinned offline model, notices and native ABI/16KiB packaging verified')

if __name__ == '__main__': main()
