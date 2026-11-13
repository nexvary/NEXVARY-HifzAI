#!/usr/bin/env bash
set -euo pipefail
mkdir -p ui-proof
failure_diagnostics() {
  local result=$?
  if [ "$result" -ne 0 ]; then
    adb logcat -d -v threadtime > ui-proof/logcat-failure.txt || true
    tail -n 180 ui-proof/logcat-failure.txt || true
    for suffix in txt png; do
      target="ui-proof/hifz-export-picker-failure.$suffix"
      if adb exec-out run-as com.nexvary.hifzai cat "files/hifz-export-picker-failure.$suffix" > "$target.tmp"; then
        mv "$target.tmp" "$target"
      else
        rm -f "$target.tmp"
      fi
    done
    adb exec-out run-as com.nexvary.hifzai cat files/hifz-navigation-before.png > ui-proof/hifz-navigation-failure.png || true
  fi
  exit "$result"
}
trap failure_diagnostics EXIT
# Run instrumentation explicitly so AGP cannot uninstall the app before PNG extraction.
gradle :app:assembleDebug :app:assembleDebugAndroidTest -PhifzTestAbi=x86_64 --no-daemon --stacktrace
python3 tools/verify_native_asr_apk.py app/build/outputs/apk/debug/app-debug.apk --abi x86_64
cp build/native-asr/packaging.json ui-proof/native-packaging.json
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.nexvary.hifzai
adb shell settings put system font_scale 1.0

# A focused 2x-font pass verifies full navigation text rather than accepting a
# screenshot container as evidence that labels fit. Keep its failure log too.
adb shell am force-stop com.nexvary.hifzai
adb shell settings put system font_scale 2.0
adb shell am instrument -w -e class com.nexvary.hifzai.AppearanceUiTest#everyNavigationLabelFitsAndAllDestinationsRemainReachable com.nexvary.hifzai.test/androidx.test.runner.AndroidJUnitRunner | tee ui-proof/instrumentation-navigation-2x.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)' ui-proof/instrumentation-navigation-2x.txt
adb exec-out run-as com.nexvary.hifzai cat files/hifz-navigation.png > ui-proof/hifz-navigation-2x.png
mkdir -p ui-proof/navigation-2x
cp ui-proof/hifz-navigation-2x.png ui-proof/navigation-2x/hifz-navigation.png
python3 tools/verify_screenshot_proof.py ui-proof/navigation-2x --pages navigation
adb shell settings put system font_scale 1.0
adb shell am instrument -w com.nexvary.hifzai.test/androidx.test.runner.AndroidJUnitRunner | tee ui-proof/instrumentation.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)' ui-proof/instrumentation.txt
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=' ui-proof/instrumentation.txt; then exit 1; fi
for page in home reader today exercises chapters search practice recall similar insights settings privacy sources teachers help about reciters recitation-range partitions range private-audio asr asr-microphone agenda agenda-filters saved-places appearance-settings reader-light reader-dark navigation notebook-date-picker notebook-time-picker library-playback library-paused; do
  adb exec-out run-as com.nexvary.hifzai cat "files/hifz-$page.png" > "ui-proof/hifz-$page.png"
done
adb exec-out run-as com.nexvary.hifzai cat files/hifz-native-asr.json > ui-proof/native-asr-technical.json
adb exec-out run-as com.nexvary.hifzai cat files/hifz-asr-microphone.json > ui-proof/asr-microphone-privacy.json
python3 tools/verify_screenshot_proof.py ui-proof

# Repeat UI paths at large system font. Keep normal and scaled evidence separate.
# A new instrumentation invocation owns a fresh process. Do not change global
# configuration while the previous runner's Compose windows are being disposed.
adb shell am force-stop com.nexvary.hifzai
adb shell settings put system font_scale 1.5
adb shell am instrument -w -e class com.nexvary.hifzai.HifzUiSmokeTest,com.nexvary.hifzai.PrivateAudioUiTest,com.nexvary.hifzai.LocalTranscriptionUiTest,com.nexvary.hifzai.LearningDevelopmentUiTest,com.nexvary.hifzai.TeacherNotebookUiTest,com.nexvary.hifzai.AppearanceUiTest,com.nexvary.hifzai.RecitationLibraryUiTest com.nexvary.hifzai.test/androidx.test.runner.AndroidJUnitRunner | tee ui-proof/instrumentation-large-font.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)' ui-proof/instrumentation-large-font.txt
mkdir -p ui-proof/large-font
for page in home reader today exercises chapters search practice recall similar insights settings privacy sources teachers help about reciters recitation-range partitions range private-audio asr asr-microphone agenda agenda-filters saved-places appearance-settings reader-light reader-dark navigation notebook-date-picker notebook-time-picker library-playback library-paused; do
  adb exec-out run-as com.nexvary.hifzai cat "files/hifz-$page.png" > "ui-proof/large-font/hifz-$page.png"
done
adb exec-out run-as com.nexvary.hifzai cat files/hifz-asr-microphone.json > ui-proof/large-font/asr-microphone-privacy.json
python3 tools/verify_screenshot_proof.py ui-proof/large-font
adb shell settings put system font_scale 1.0
