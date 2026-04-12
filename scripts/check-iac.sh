#!/bin/bash
#
# Probes whether the macOS IAC Driver has at least one online port visible
# to MidiBus. Intended for humans and for setup-iac.sh.
#
# Exit codes:
#   0 - IAC ready (both input and output names containing "IAC" are present)
#   1 - not ready; run scripts/setup-iac.sh
#
# Reuses MidiBusTest.main --check-iac to avoid duplicating device probing.

set -e

cd "$(dirname "$0")/.."

if [[ ! -f library/themidibus.jar ]]; then
    echo "check-iac.sh: library/themidibus.jar not found. Run 'ant jar' first."
    exit 2
fi

if [[ -z "$PROCESSING_CORE_DIR" ]]; then
    echo "check-iac.sh: PROCESSING_CORE_DIR is not set. See README.md."
    exit 2
fi

# Compile the test class if not already built (e.g. after a clean).
# We use a stable bin-test dir kept out of version control.
mkdir -p bin-test
if [[ ! -f bin-test/themidibus/MidiBusTest.class ]] || [[ test/themidibus/MidiBusTest.java -nt bin-test/themidibus/MidiBusTest.class ]]; then
    JAVAC="${PROCESSING_JAVAC:-javac}"
    "$JAVAC" \
        -cp "library/themidibus.jar:$PROCESSING_CORE_DIR/*:lib/*" \
        -d bin-test \
        test/themidibus/MidiBusTest.java
fi

# Run with --check-iac. Java under fork inherits env vars; use java from Processing's JDK.
JAVA="$(dirname "${PROCESSING_JAVAC:-java}")/java"
if [[ ! -x "$JAVA" ]]; then
    JAVA=java
fi

exec "$JAVA" \
    -cp "library/themidibus.jar:$PROCESSING_CORE_DIR/*:lib/*:bin-test" \
    themidibus.MidiBusTest --check-iac
