#!/usr/bin/env bash
# Shared helpers for scripts/screenshots.sh. Expects PKG to be set by the caller's environment
# or falls back to the sample's application id from sample/build.gradle.kts.
PKG="${PKG:-$(grep -oE 'applicationId = "[^"]+"' sample/build.gradle.kts | cut -d'"' -f2)}"
ACTIVITY="${ACTIVITY:-.preview.PreviewActivity}"
OUT="docs/screenshots"
mkdir -p "$OUT"

install_sample() {
  adb install -r sample/build/outputs/apk/debug/sample-debug.apk
  # A freshly booted emulator often shows "Pixel Launcher isn't responding".
  # Hide error/ANR dialogs and give the system time to settle.
  adb shell settings put global hide_error_dialogs 1
  adb shell settings put global anr_show_background 0
  # Keep the screen on during the captures.
  adb shell svc power stayon true
  adb shell settings put system screen_off_timeout 1800000
  adb shell input keyevent KEYCODE_WAKEUP
  sleep 20
  dismiss_system_dialogs
  # Clean status bar via System UI demo mode.
  adb shell settings put global sysui_demo_allowed 1
  adb shell am broadcast -a com.android.systemui.demo -e command enter
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
  adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile show -e level 4
  adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
}

# Locks the display in landscape. A tablet is landscape at rotation 0, a phone at rotation 1
# (the preview activity also asks for landscape, which phones honor).
lock_landscape() {
  adb shell settings put system accelerometer_rotation 0
  local size width height
  size="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 || true)"
  width="${size%x*}"
  height="${size#*x}"
  if [ "${width:-0}" -ge "${height:-0}" ]; then
    adb shell settings put system user_rotation 0
  else
    adb shell settings put system user_rotation 1
  fi
  sleep 3
}

sample_in_front() {
  adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity|ResumedActivity" | grep -q "$PKG"
}

# Starts the preview with a clean state. Retries when something system-side (a dialog, the
# launcher) comes up on top instead.
fresh_launch() {
  adb shell pm clear "$PKG" > /dev/null
  adb shell input keyevent KEYCODE_WAKEUP
  for _ in 1 2 3; do
    adb shell am start -W -n "$PKG/$ACTIVITY" "$@" > /dev/null
    sleep 6
    sample_in_front && return 0
    dismiss_system_dialogs
    adb shell input keyevent KEYCODE_BACK
    adb shell input keyevent KEYCODE_WAKEUP
    sleep 3
  done
}

dismiss_system_dialogs() {
  for _ in 1 2 3; do
    if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
      adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null
      adb shell input keyevent KEYCODE_ENTER
      sleep 2
    else
      return 0
    fi
  done
  if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
    echo "A system dialog is still on screen; refusing to capture a broken screenshot." >&2
    exit 1
  fi
}

# Fails when the sample is not the resumed activity or the expected text is missing from the screen.
# A failing UI dump (it happens on busy emulators) only warns; the image checks below still run.
expect_on_screen() {
  local text="$1"
  if ! sample_in_front; then
    echo "The sample app is not in front; refusing to capture." >&2
    adb shell dumpsys activity activities | grep -E "ResumedActivity" >&2 || true
    exit 1
  fi
  local xml=""
  for _ in 1 2 3; do
    if adb shell uiautomator dump /sdcard/window.xml > /dev/null 2>&1; then
      xml="$(adb exec-out cat /sdcard/window.xml)"
      break
    fi
    sleep 2
  done
  if [ -z "$xml" ]; then
    echo "Warning: could not dump the UI to look for \"$text\"." >&2
  elif ! printf '%s' "$xml" | grep -qF "$text"; then
    echo "\"$text\" is not on screen; the scene did not appear." >&2
    exit 1
  fi
}

# Fails on a missing, tiny, blank, single colored or portrait PNG.
check_png() {
  python3 - "$1" <<'PY'
import struct, sys, zlib

path = sys.argv[1]
with open(path, "rb") as f:
    data = f.read()
if len(data) < 12_000 or not data.startswith(b"\x89PNG\r\n\x1a\n"):
    sys.exit(f"{path}: not a PNG or too small ({len(data)} bytes)")
width, height = struct.unpack(">II", data[16:24])
if width <= height:
    sys.exit(f"{path}: {width}x{height} is not landscape; the car preview needs a wide screen")
idat, pos = b"", 8
while pos < len(data):
    length, kind = struct.unpack(">I4s", data[pos:pos + 8])
    if kind == b"IDAT":
        idat += data[pos + 8:pos + 8 + length]
    pos += 12 + length
raw = zlib.decompress(idat)
# Filtered scanlines of a blank screen are almost all zeros; real content has many different bytes.
distinct = len(set(raw[::97]))
nonzero = sum(1 for b in raw[::211] if b) / max(1, len(raw[::211]))
# A light screen with sparse content filters to mostly zeros too, so a low non-zero share only
# counts as blank when the content also has few distinct bytes.
if width < 300 or height < 300 or distinct < 24 or (nonzero < 0.01 and distinct < 120):
    sys.exit(f"{path}: looks blank ({width}x{height}, {distinct} distinct samples, {nonzero:.3f} non-zero)")
print(f"{path}: {width}x{height}, {len(data) // 1024} KB, {distinct} distinct samples")
PY
}

capture() {
  local name="$1" expected="$2"
  dismiss_system_dialogs
  expect_on_screen "$expected"
  adb exec-out screencap -p > "$OUT/$name.png"
  if ! check_png "$OUT/$name.png"; then
    rm -f "$OUT/$name.png"
    exit 1
  fi
  echo "Captured $name"
}
