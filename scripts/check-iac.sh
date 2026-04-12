#!/bin/bash
#
# Probes whether the macOS IAC Driver is configured AND actively forwarding
# MIDI messages. Delegates to scripts/iac-probe.swift, which uses CoreMIDI
# directly — this keeps the probe orthogonal to Java MIDI (which is flaky
# and can fail to see IAC even when CoreMIDI-level routing works fine).
#
# Exit codes (from iac-probe.swift):
#   0 - IAC round-trip successful (ready for `ant test`)
#   1 - no IAC device found in CoreMIDI
#   2 - IAC found but not forwarding (Device is online probably off)
#   3 - CoreMIDI API call failed

set -e

cd "$(dirname "$0")/.."
exec swift scripts/iac-probe.swift
