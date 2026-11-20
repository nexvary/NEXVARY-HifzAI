import struct
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from verify_native_asr_apk import elf_load_alignments


def elf(wide, machine, alignment):
    data=bytearray(128); data[:6]=b'\x7fELF'+bytes((2 if wide else 1,1))
    struct.pack_into('<HH',data,16,3,machine)
    struct.pack_into('<Q' if wide else '<I',data,32 if wide else 28,64)
    struct.pack_into('<HH',data,54 if wide else 42,56 if wide else 32,1)
    struct.pack_into('<I',data,64,1)
    struct.pack_into('<Q' if wide else '<I',data,64+(48 if wide else 28),alignment)
    return data


class NativeElfPolicyTest(unittest.TestCase):
    def test_accepts_32_and_64_bit_load_segments(self):
        self.assertEqual([16384],elf_load_alignments(elf(False,40,16384),40))
        self.assertEqual([65536],elf_load_alignments(elf(True,183,65536),183))
    def test_rejects_old_page_alignment_and_wrong_abi(self):
        for data,machine in [(elf(True,183,4096),183),(elf(True,62,16384),183)]:
            with self.assertRaises(ValueError): elf_load_alignments(data,machine)
    def test_rejects_corrupt_and_truncated_headers(self):
        for data in (b'not a library',elf(True,62,16384)[:80]):
            with self.assertRaises(ValueError): elf_load_alignments(data,62)
