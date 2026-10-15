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
for page in home reader today exercises chapters search practice recall similar insights settings privacy sources teachers help about reciters partitions range private-audio; do
  adb exec-out run-as com.nexvary.hifzai cat "files/hifz-$page.png" > "ui-proof/hifz-$page.png"
done
python3 tools/verify_screenshot_proof.py ui-proof

# Repeat UI paths at large system font. Keep normal and scaled evidence separate.
adb shell settings put system font_scale 1.5
adb shell am instrument -w -e class com.nexvary.hifzai.HifzUiSmokeTest,com.nexvary.hifzai.PrivateAudioUiTest com.nexvary.hifzai.test/androidx.test.runner.AndroidJUnitRunner | tee ui-proof/instrumentation-large-font.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)' ui-proof/instrumentation-large-font.txt
mkdir -p ui-proof/large-font
for page in home reader today exercises chapters search practice recall similar insights settings privacy sources teachers help about reciters partitions range private-audio; do
  adb exec-out run-as com.nexvary.hifzai cat "files/hifz-$page.png" > "ui-proof/large-font/hifz-$page.png"
done
python3 tools/verify_screenshot_proof.py ui-proof/large-font
adb shell settings put system font_scale 1.0
