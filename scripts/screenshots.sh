#!/usr/bin/env bash
# Captures README screenshots of the phone preview on a running landscape emulator (a tablet profile).
# Android Auto itself (the Desktop Head Unit) can't run in CI, so the sample's preview activity draws
# the same autokit-core model as car-like cards. Taps can't be timed reliably through adb, so the
# preview opens each screen from the `scene` extra. Every capture is checked for the expected text,
# for landscape and for a blank image.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

# The text each scene must show; the capture fails without it.
expected_text() {
  case "$1" in
    list) echo "The Old Lighthouse" ;;
    grid) echo "Viewpoints" ;;
    pane) echo "Lookout Point" ;;
    navigation) echo "In 300 m, turn right onto Coast Road" ;;
  esac
}

install_sample
lock_landscape
for scene in list grid pane navigation; do
  fresh_launch --es scene "$scene"
  capture "$scene" "$(expected_text "$scene")"
done
