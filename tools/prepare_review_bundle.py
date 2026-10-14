"""Fail-closed internal packaging. Never submits to a committee or publishes an APK."""
import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path

REQUIRED = (
    "functional_scope", "official_corpus", "rendered_quran_internal_review",
    "learning_methods_internal_review", "audio_rights", "asr_independent_evaluation",
    "academy_authorization_and_child_safety", "android_ci", "device_matrix",
    "accessibility", "security_and_dependency_review", "arabic_user_guide",
    "real_screenshots_all_pages", "internal_quality_minutes",
)


def readiness(root, manifest):
    blockers = []
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    if manifest.get("source_commit") != head:
        blockers.append("Evidence source_commit does not match HEAD")
    for name in REQUIRED:
        gate = manifest.get("gates", {}).get(name, {})
        if gate.get("status") != "verified" or not gate.get("reviewer"):
            blockers.append(f"{name}: not verified by an identified reviewer")
            continue
        evidence = gate.get("evidence", [])
        if not evidence:
            blockers.append(f"{name}: no evidence files")
        for item in evidence:
            path = (root / item.get("path", "")).resolve()
            if not path.is_relative_to(root.resolve()) or not path.is_file():
                blockers.append(f"{name}: missing/unsafe evidence path")
            elif hashlib.sha256(path.read_bytes()).hexdigest() != item.get("sha256"):
                blockers.append(f"{name}: evidence digest mismatch")
    apk = manifest.get("apk", {})
    path = (root / apk.get("path", "")).resolve()
    if not path.is_relative_to(root.resolve()) or not path.is_file():
        blockers.append("Internal candidate APK missing")
    elif hashlib.sha256(path.read_bytes()).hexdigest() != apk.get("sha256"):
        blockers.append("Internal candidate APK digest mismatch")
    return {"source_commit": head, "ready_for_owner_submission_decision": not blockers,
            "public_release_allowed": False, "committee_contacted": False, "blockers": blockers}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", default="docs/review/READINESS.json")
    parser.add_argument("--audit", action="store_true")
    parser.add_argument("--output", default="internal-scholarly-review.zip")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    manifest = json.loads((root / args.manifest).read_text())
    report = readiness(root, manifest)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if args.audit:
        return
    if not report["ready_for_owner_submission_decision"]:
        raise SystemExit("BLOCKED: review bundle cannot be marked final or submitted")
    paths = {args.manifest, manifest["apk"]["path"]}
    for gate in manifest["gates"].values():
        paths.update(item["path"] for item in gate["evidence"])
    paths.update(subprocess.check_output(["git", "ls-files"], cwd=root, text=True).splitlines())
    output = Path(args.output)
    if output.exists():
        raise SystemExit("Refusing to replace an existing review bundle")
    with zipfile.ZipFile(output, "x", compression=zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(paths):
            resolved = (root / path).resolve()
            if not resolved.is_relative_to(root) or not resolved.is_file():
                raise SystemExit("Invalid package path")
            archive.write(resolved, path)
        archive.writestr("INTERNAL_REVIEW_DECISION.json", json.dumps(report, indent=2))
    print("Internal package created; owner decision required. No submission or publication performed.")


if __name__ == "__main__":
    main()
