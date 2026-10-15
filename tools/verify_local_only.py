"""Enforce the owner's device-local Android scope, including merged APK permissions."""
import argparse
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FORBIDDEN = {'android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE',
             'android.permission.ACCESS_WIFI_STATE', 'android.permission.CHANGE_WIFI_STATE',
             'android.permission.BLUETOOTH_CONNECT', 'android.permission.BLUETOOTH_SCAN',
             'android.permission.BLUETOOTH', 'android.permission.BLUETOOTH_ADMIN',
             'android.permission.NEARBY_WIFI_DEVICES'}
ANDROID = '{http://schemas.android.com/apk/res/android}'


def check_permissions(permissions):
    rejected = FORBIDDEN.intersection(permissions)
    if rejected:
        raise ValueError('Device-local policy rejects network permissions: '+', '.join(sorted(rejected)))


def source_permissions(root=ROOT):
    permissions = set()
    for path in (root/'app/src').rglob('AndroidManifest.xml'):
        manifest = ET.parse(path).getroot()
        for child in manifest:
            if child.tag.startswith('uses-permission'):
                permissions.add(child.attrib.get(ANDROID+'name', ''))
    check_permissions(permissions)
    return permissions


def apk_permissions(output):
    if 'com.nexvary.hifzai' not in output:
        raise ValueError('APK permission report is missing the expected package')
    permissions = set(re.findall(r"uses-permission(?:-sdk-\d+)?:\s*(?:name=)?'([^']+)'", output))
    if 'android.permission.RECORD_AUDIO' not in permissions:
        raise ValueError('Unexpected APK permission output; do not claim offline verification')
    check_permissions(permissions)
    return permissions


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--aapt2', type=Path)
    args = parser.parse_args()
    report = {'scope': 'device-local Android; no account/server/audio upload required',
              'source_permissions': sorted(source_permissions()),
              'source_commit': subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()}
    if bool(args.apk) != bool(args.aapt2):
        parser.error('--apk and --aapt2 must be supplied together')
    if args.apk:
        output = subprocess.check_output([str(args.aapt2), 'dump', 'permissions', str(args.apk)], text=True)
        report['apk_permissions'] = sorted(apk_permissions(output))
        report['apk_sha256'] = hashlib.sha256(args.apk.read_bytes()).hexdigest()
        report['apk_size_bytes'] = args.apk.stat().st_size
    directory = ROOT/'build/local-only'
    directory.mkdir(parents=True, exist_ok=True)
    (directory/'permissions.json').write_text(json.dumps(report,indent=2)+'\n')
    print('Local-only permissions verified'+(' on the built APK' if args.apk else ' in source manifests'))


if __name__ == '__main__':
    main()
