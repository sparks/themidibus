#!/bin/bash
#
# Runs scripts/CompareMidiBackends.java to verify whether the CoreMIDI4J
# dependency is still needed on the current macOS + JDK combination.
# Uses the Processing-bundled JDK (via $PROCESSING_JAVAC) to match what
# Processing actually runs. Requires IAC Driver online — if not set up,
# run scripts/setup-iac.sh first.

set -e

cd "$(dirname "$0")/.."

JAVAC="${PROCESSING_JAVAC:-}"
if [[ -z "$JAVAC" ]]; then
    echo "error: PROCESSING_JAVAC is not set. See README.md 'Building from source'."
    exit 1
fi
if [[ ! -x "$JAVAC" ]]; then
    echo "error: PROCESSING_JAVAC=$JAVAC is not executable."
    exit 1
fi
JAVA="$(dirname "$JAVAC")/java"

if [[ ! -f lib/coremidi4j-1.6.jar ]]; then
    echo "error: lib/coremidi4j-1.6.jar not found."
    exit 1
fi

TMPDIR="$(mktemp -d -t themidibus-backend-check)"
trap "rm -rf $TMPDIR" EXIT

"$JAVAC" -d "$TMPDIR" scripts/CompareMidiBackends.java
exec "$JAVA" -cp "$TMPDIR:lib/*" CompareMidiBackends
