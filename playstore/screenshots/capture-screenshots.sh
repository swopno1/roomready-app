#!/usr/bin/env bash
# Pull the 6 Play Store screenshots from a connected demo device.
# Usage: ./capture-screenshots.sh   (run from playstore/screenshots/)
set -euo pipefail

OUT="$(dirname "$0")"
shot() {
  local n="$1" name="$2" setup="$3"
  echo ""
  echo "=== Shot $n: $name ==="
  echo "Setup: $setup"
  read -r -p "Press Enter when ready… "
  adb exec-out screencap -p > "$OUT/raw-$n-$name.png"
  echo "saved raw-$n-$name.png"
}

adb start-server
adb wait-for-device

shot 1 home "Home tab, synced, day in progress (some Dirty, some Ready)"
shot 2 activity-offline "AIRPLANE MODE ON (status bar icon visible) → Activity tab showing pending + today's offline work"
shot 3 rooms "Airplane OFF → Rooms tab, filter All, mixed states visible"
shot 4 checklist "Room detail mid-cleaning, 3-4 steps checked, progress bar visible"
shot 5 inspect "SUPERVISOR session → Inspect screen with evidence photo visible"
shot 6 profile-bn "Profile tab → switch to বাংলা → identity card visible"

echo ""
echo "Done. Frame them with: python3 ../frame-shot.py"
