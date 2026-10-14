#!/usr/bin/env bash
set -euo pipefail
mkdir -p ui-proof
# Run instrumentation explicitly so AGP cannot uninstall the app before PNG extraction.
gradle :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.nexvary.hifzai.test/androidx.test.runner.AndroidJUnitRunner | tee ui-proof/instrumentation.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)' ui-proof/instrumentation.txt
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=' ui-proof/instrumentation.txt; then exit 1; fi
for page in home reader today exercises chapters search practice recall similar insights settings privacy sources teachers help about reciters; do
  adb exec-out run-as com.nexvary.hifzai cat "files/hifz-$page.png" > "ui-proof/hifz-$page.png"
done
python3 tools/verify_screenshot_proof.py ui-proof
