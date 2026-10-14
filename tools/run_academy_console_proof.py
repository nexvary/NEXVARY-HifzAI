"""Run browser proof against disposable SQLite and explicitly synthetic accounts."""
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import threading
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from academy.service import Academy
from academy.server import make_server
from tools.verify_screenshot_proof import verify_png


def main():
    output = ROOT/'build/academy-console-proof'
    output.mkdir(parents=True, exist_ok=True)
    # Remove stale successful evidence before a new attempt.
    for file in output.glob('*'):
        if file.is_file():
            file.unlink()
    with tempfile.TemporaryDirectory() as directory:
        academy = Academy(Path(directory)/'synthetic.sqlite')
        password = 'synthetic-console-password-2026'
        academy.bootstrap_admin('administrator', password)
        admin = academy.login('administrator', password)['token']
        for login, role in [('teacher', 'teacher'), ('guardian', 'guardian')]:
            academy.call(admin, 'account_create', {'login': login, 'password': password, 'role': role})
        server = make_server(academy)
        worker = threading.Thread(target=server.serve_forever, daemon=True)
        worker.start()
        env = os.environ.copy()
        env['HIFZ_CONSOLE_URL'] = f'http://127.0.0.1:{server.server_port}'
        env['HIFZ_CONSOLE_PROOF'] = str(output)
        try:
            result = subprocess.run(['node', str(ROOT/'tests/academy_console_smoke.cjs')],
                                    cwd=ROOT, env=env, timeout=180, check=False)
        finally:
            server.shutdown()
            server.server_close()
            worker.join()
        if result.returncode:
            return result.returncode
    report = json.loads((output/'browser-result.json').read_text())
    images = list(output.glob('*.png'))
    if not report.get('successful') or len(images) != 8:
        raise ValueError('Missing browser/image proof')
    for image in images:
        verify_png(image)
    report['source_commit'] = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    report['working_tree_dirty'] = bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip())
    report['sha256'] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in output.glob('*.png')}
    (output/'PROOF_MANIFEST.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print('Private academy console browser proof passed; synthetic records only, not Android approval.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
