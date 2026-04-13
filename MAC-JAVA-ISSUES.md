# macOS Java MIDI Issues

This is a Claude summary of the issue and workaround considering how to proceed.

## SysEx Send: Silently Dropped by Apple's javax.sound.midi

Apple's native `javax.sound.midi` implementation on macOS silently drops outbound System Exclusive messages. All channel message types (Note On/Off, CC, Program Change, Pitch Bend, Channel/Poly Pressure) work correctly in both directions. Only SysEx send is broken — SysEx receive works fine.

### Root cause

CoreMIDI separates regular MIDI from SysEx at the API level:

- **`MIDISend()`** — handles standard MIDI messages (notes, CC, etc.)
- **`MIDISendSysex()`** — dedicated function for System Exclusive, with asynchronous transmission, completion callbacks, and flow control

The JDK's native macOS bridge (`com.sun.media.sound.MidiOutDevice`) calls `MIDISend()` for all message types. `MIDISend()` filters out status bytes >= 0xF0, so SysEx is discarded before Java sees a return value. No `javax.sound.midi` API call can route around this — the drop happens in native code below the Java layer.

This is a long-standing JDK bug with no fix planned:

- [JDK-8237495](https://bugs.openjdk.org/browse/JDK-8237495) — dereferenced memory error on raw 0xF7 byte
- [JDK-8250667](https://bugs.openjdk.org/browse/JDK-8250667) — SysEx over USB gets scrambled

### Current workaround: CoreMIDI4J

[CoreMIDI4J](https://github.com/DerekCook/CoreMidi4J) (v1.6, `lib/coremidi4j-1.6.jar`) is a `javax.sound.midi` Service Provider that bypasses Apple's broken implementation entirely. It uses JNI to call CoreMIDI directly — detecting SysEx messages and routing them to `MIDISendSysex()` instead of `MIDISend()`. It acts as a drop-in replacement: same `javax.sound.midi` API, different native backend.

The `MidiBus.bypassCoreMidi4J(true)` flag falls back to Apple's native implementation with warnings. Safe for sketches that only **receive** SysEx, but outbound SysEx will be silently dropped.

### Is this bug macOS-specific?

**Yes.** The SysEx send bug is specific to Apple's native `javax.sound.midi` implementation. Other platforms:

| Platform | javax.sound.midi SysEx send | Notes |
|---|---|---|
| **macOS** | Broken — silently dropped | Apple's bridge only calls `MIDISend()`, never `MIDISendSysex()` |
| **Windows** | Mostly works | Edge-case crashes on certain SysEx patterns ([JDK-8237495](https://bugs.openjdk.org/browse/JDK-8237495)), but `winmm.dll` API itself is sound |
| **Linux** | Works | ALSA handles SysEx correctly; no known javax.sound.midi issues |

This means any fix only needs to target macOS. On Windows and Linux, `javax.sound.midi` delivers SysEx without intervention. Cross-platform compatibility for a workaround means "fixes macOS without breaking Windows/Linux."

### Can we fix this without an external library?

**No, not on JDK 17.** Any fix must cross the Java/native boundary to call `MIDISendSysex()`. The options:

| Approach | Dependencies | Status | Cross-platform |
|---|---|---|---|
| **CoreMIDI4J** (current) | 1 JAR with bundled `.dylib` | Working today | Safe — SPI gracefully returns no devices on non-macOS; Windows/Linux fall back to standard javax.sound.midi automatically |
| **Panama FFM API** (`java.lang.foreign`) | None — standard JDK API | Requires JDK 22+; Processing bundles 17 | Safe — would only invoke CoreMIDI on macOS; no native binaries to ship since the framework is on the system. Windows/Linux use javax.sound.midi as-is |
| **JNA** (Java Native Access) | Swaps coremidi4j.jar for jna.jar | Works on 17, but still an external dep | Possible but painful — JNA itself is cross-platform, but MIDI APIs differ per OS (CoreMIDI / winmm / ALSA). Would need 3 platform-specific code paths or macOS-only with fallback |
| **Bundled JNI** | ~200 lines of C, compiled `.dylib` | Removes dep but adds native build complexity | macOS `.dylib` only — since the bug is macOS-specific, no Windows/Linux binaries needed. Must handle `UnsatisfiedLinkError` gracefully on other platforms |
| **Swift subprocess** | None (`swift` always on macOS) | 100-200ms latency — unusable for real-time | macOS only — Swift is technically cross-platform but not bundled on Windows/Linux. Not viable for a library targeting all platforms |

**Panama FFM** is the most promising long-term path — it would let us call `MIDISendSysex()` directly from Java with zero external dependencies and zero shipped binaries (CoreMIDI is a system framework). Blocked until Processing upgrades past JDK 22.

### Verification

Run `scripts/compare-midi-backends.sh` to re-check the current state on your macOS + JDK combination. The script tests all common MIDI message types in both directions for both the Apple-native and CoreMIDI4J backends. If Apple ever fixes the send path (or regresses the receive path), the test suite (Layer 9) will catch it.

### Related

- [themidibus#31](https://github.com/sparks/themidibus/issues/31) — original SysEx / MMJ issue
- [CoreMidi4J#22](https://github.com/DerekCook/CoreMidi4J/issues/22) — underlying Java MIDI problems
- [CoreMidi4J#37](https://github.com/DerekCook/CoreMidi4J/issues/37) — split SysEx handling (fixed in 1.6)
- [Apple MIDISendSysex docs](https://developer.apple.com/documentation/coremidi/1495356-midisendsysex)
