#!/bin/bash
#
# Guided setup for macOS IAC Driver, used by Layer 7 of the test suite.
# Opens Audio MIDI Setup, prints manual steps, waits, then re-probes.

set -e

cd "$(dirname "$0")/.."

echo "Checking current IAC state..."
if ./scripts/check-iac.sh >/dev/null 2>&1; then
    ./scripts/check-iac.sh
    echo
    echo "IAC Driver is already configured. You're good to run 'ant test'."
    exit 0
fi

cat <<'EOF'

=== IAC Driver setup required ===

The MidiBus test suite uses macOS's IAC Driver as a MIDI loopback for
round-trip tests (Layer 7). Audio MIDI Setup will now open. Please:

  1. In the menu bar, choose:  Window > Show MIDI Studio   (Cmd-2)
  2. In the MIDI Studio window, double-click "IAC Driver".
  3. In the properties sheet that appears:
       a. Check "Device is online"
       b. Under "Ports", make sure at least one port exists
          (default "Bus 1" is fine - you don't need to rename it)
  4. Click "Apply" and close the window.
  5. Return to this terminal and press Enter.

EOF

open -a "Audio MIDI Setup" || {
    echo "Could not launch Audio MIDI Setup. Open it manually from /Applications/Utilities/"
}

read -r -p "Press Enter when done..."

echo
echo "Re-probing..."
if ./scripts/check-iac.sh; then
    echo
    echo "IAC Driver is now configured. You can run 'ant test'."
    exit 0
else
    echo
    echo "IAC Driver is still not detected."
    echo "Double-check step 3a ('Device is online' must be checked) and try again."
    exit 1
fi
