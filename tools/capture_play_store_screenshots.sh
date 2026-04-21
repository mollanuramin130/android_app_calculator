#!/usr/bin/env bash
# Capture 8 in-app screenshots on a connected device/emulator for Google Play.
# Requires: adb, debug build installed (./gradlew :app:installDebug).
#
# IMPORTANT: Screen lock (PIN / fingerprint) must be OFF for the capture session, or the
# phone must stay unlocked — otherwise every PNG will look the same (lock / credential UI).
# Optional: Developer options → Stay awake while charging + USB connected.
set -euo pipefail
export PATH="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools:$PATH"
PKG="com.nuramin.sunsetcoralcalculator"
MAIN="com.nuramin.calculator.MainActivity"
AI="com.nuramin.sunsetcoralcalculator.ai.ui.AISmartActivity"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${ROOT}/play_store_screenshots"
mkdir -p "$OUT"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Add Android SDK platform-tools to PATH." >&2
  exit 1
fi
if ! adb devices | grep -qE '^\S+\s+device$'; then
  echo "No device in 'adb devices'. Connect a phone or start an emulator." >&2
  exit 1
fi

# Wake display and dismiss keyguard (adjust swipe if your lock pattern differs).
wake_and_unlock() {
  adb shell input keyevent 224
  sleep 0.35
  adb shell input swipe 400 1800 400 400 280
  sleep 0.55
}

shot() {
  local file="$1"
  shift
  adb shell am force-stop "$PKG" || true
  sleep 0.35
  wake_and_unlock
  adb shell am start -n "$PKG/$MAIN" --ez skip_auto_review true "$@" >/dev/null
  sleep 2.2
  wake_and_unlock
  adb exec-out screencap -p > "$file"
  echo "Wrote $file"
}

shot_ai() {
  local file="$1"
  adb shell am force-stop "$PKG" || true
  sleep 0.35
  wake_and_unlock
  adb shell am start -n "$PKG/$AI" >/dev/null
  sleep 2.2
  wake_and_unlock
  adb exec-out screencap -p > "$file"
  echo "Wrote $file"
}

echo "Capturing into $OUT (portrait; device native resolution)…"
echo "Tip: disable aggressive battery kill for this app; PIN locks need manual unlock or edit wake_and_unlock in this script."

shot "$OUT/01_basic_calculator.png"
shot "$OUT/02_scientific_calculator.png" --es open_panel scientific
shot "$OUT/03_emi_calculator.png" --es open_panel emi
shot "$OUT/04_currency_converter.png" --es open_panel currency
shot "$OUT/05_bmi_calculator.png" --es open_panel bmi
shot "$OUT/06_date_calculator.png" --es open_panel date_calc
shot "$OUT/07_math_speed_game.png" --es open_panel math_speed_game
shot_ai "$OUT/08_ai_smart.png"

adb shell am force-stop "$PKG" || true
echo "Done. Upload PNGs from: $OUT"
