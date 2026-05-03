# Building from source

The MidiBus is built with [Ant](https://ant.apache.org/). The build classpath pulls `processing.core.PApplet` out of your local [Processing](http://www.processing.org/) install, and compilation uses the `javac` that ships inside Processing's bundled JDK.

Two environment variables are **required** — the build aborts with a clear error if either is unset:

- `PROCESSING_CORE_DIR` — path to Processing's `core/library/` directory (contains `core.jar`).
- `PROCESSING_JAVAC` — path to the `javac` binary inside Processing's bundled JDK.

Set these once in your shell config. Examples below assume a default macOS Processing 4 install; adjust the paths for your Processing version and install location.

## fish

```fish
set -Ux PROCESSING_CORE_DIR /Applications/Processing.app/Contents/app/resources/core/library/
set -Ux PROCESSING_JAVAC /Applications/Processing.app/Contents/app/resources/jdk-17.0.14+7/Contents/Home/bin/javac
```

`set -Ux` makes the vars universal and exported — they persist across fish sessions without editing `config.fish`. Alternatively, add `set -gx` versions to `~/.config/fish/config.fish` if you prefer dotfile-tracked config.

## bash / zsh

```sh
export PROCESSING_CORE_DIR=/Applications/Processing.app/Contents/app/resources/core/library/
export PROCESSING_JAVAC=/Applications/Processing.app/Contents/app/resources/jdk-17.0.14+7/Contents/Home/bin/javac
```

Put these in `~/.bashrc` / `~/.zshrc` to persist.

## Building

Once the env vars are set:

- `ant jar` — compile and produce `library/themidibus.jar`.
- `ant zip` — build jar + Javadoc and package the full release as `themidibus.zip` (the file uploaded to the Processing Library Manager).
- `ant clean` — delete `bin/`.

Heads up: the JDK folder name in `PROCESSING_JAVAC` (e.g. `jdk-17.0.14+7`) bakes in a specific Processing version. When Processing updates, that folder name changes and the build breaks with "executable not found" — re-run `set -Ux PROCESSING_JAVAC <new path>` with the updated folder name.

# Deploying

`scripts/deploy.sh` builds a release and uploads it to S3. It requires the AWS profile name in an environment variable:

```fish
set -Ux MIDIBUS_SBD_AWS_PROFILE smallbutdigital
```

Then from the project root:

```
./scripts/deploy.sh
```

The script reads the version from `library.properties`, checks that the version doesn't already exist on S3, runs `ant clean && ant zip`, and uploads both the versioned (`-010`) and `-latest` files.

# Testing

A headless smoke test suite exercises the same operations as the example sketches without needing Processing. Run it with:

```
ant test
```

## What gets tested

The suite is organized into ten layers (see `test/themidibus/MidiBusTest.java`). Layers 1–5 run anywhere, with no hardware or setup:

1. **Value classes** — `Note` / `ControlChange` constructors, accessors, setters.
2. **Reflection callback dispatch** — verifies `MidiBus.registerParent` caches all the overloads and `notifyParent` dispatches to every non-null one (noteOn/noteOff/CC in plain, `_with_bus_name`, and `Note`/`ControlChange`-object forms, plus `rawMidi` and `midiMessage`).
3. **Listener dispatch** — the `SimpleMidiListener`, `ObjectMidiListener`, `RawMidiListener`, `StandardMidiListener` hierarchy.
4. **Multi-bus isolation** — two buses with distinct `bus_name`s dispatch independently and carry the right name in `_with_bus_name` callbacks.
5. **Device enumeration** — the static `list()` / `availableInputs()` / `availableOutputs()` / `unavailableDevices()` methods don't crash and return sane values. Also prints the detected devices for diagnostics.

Layer 6 verifies the full send pipeline against the Java Sound Synthesizer (Gervill), which ships with every JDK. It mirrors the send sequences in `Basic.pde`, `BasicWithClasses.pde`, and `AdvancedMIDIMessageIO.pde`. If Gervill is not present, the layer is skipped cleanly.

Layer 7 verifies the full receive pipeline — and the `NOTE_ON`-with-velocity-0 → `NOTE_OFF` rewrite in `MReceiver` — by looping MIDI back through macOS's IAC Driver. This is the only layer that needs user setup (see below). Two preconditions must be met:

1. The CoreMIDI-level IAC loopback is working, as verified by `scripts/iac-probe.swift` (a pure CoreMIDI program, no Java involved — see "Why a Swift probe" below).
2. Java MIDI can actually receive from the IAC endpoint. Java MIDI is notoriously flaky on macOS; CoreMIDI-level routing can be working fine while the Java-side receive layer (via CoreMIDI4J) silently drops messages.

If either precondition fails, Layer 7 is skipped with a precise diagnostic that tells you which one. The overall run still passes.

Layers that cannot run are reported as `SKIP` in the summary, not as failures.

### Why a Swift probe?

The skip-or-run decision for Layer 7 must be orthogonal to Java MIDI. If we used Java to ask "is IAC ready?", a false negative would hide a legitimate setup issue ("IAC is fine, your Java layer is broken") and a false positive would flood the summary with assertion failures that are really Java flakiness. `scripts/iac-probe.swift` is ~100 lines of Swift + CoreMIDI that opens IAC via the native API, sends a NOTE_ON, and verifies loopback — completely independent of Java. The test suite shells out to it to decide whether Layer 7 is runnable at all.

## IAC Driver setup (for Layer 7)

Layer 7 uses the macOS IAC Driver as a MIDI loopback. Run:

```
./scripts/setup-iac.sh
```

The script opens Audio MIDI Setup and walks you through enabling the driver. If you'd rather do it by hand:

1. Open `/Applications/Utilities/Audio MIDI Setup.app`
2. In the menu bar, choose **Window → Show MIDI Studio** (⌘2)
3. Double-click **IAC Driver**
4. Check **"Device is online"**
5. Make sure at least one port exists under "Ports" — the default "Bus 1" is fine, you don't need to rename it
6. Click **Apply** and close the window

To verify setup without running the full suite:

```
./scripts/check-iac.sh
```

`check-iac.sh` is a one-line wrapper around `scripts/iac-probe.swift`, which opens IAC directly through CoreMIDI (no Java) and does a live loopback round-trip. Exit codes: 0 = IAC is ready, 1 = no IAC device found, 2 = IAC visible but "Device is online" unchecked, 3 = CoreMIDI API error. Requires Xcode Command Line Tools (`xcode-select --install`) for `swift` to be available.

## Verifying whether CoreMIDI4J is still needed

`themidibus` bundles `lib/coremidi4j-1.6.jar` because Apple's native `javax.sound.midi` implementation has historically dropped SysEx messages on macOS. If you want to check whether that's still true on your current macOS + JDK combination (e.g. after an upgrade), run:

```
./scripts/compare-midi-backends.sh
```

This tests all common MIDI message types in both send and receive directions through both back-ends, printing a per-type results table and a verdict. Requires IAC Driver online (run `./scripts/setup-iac.sh` first). Expected output as of macOS 14 / JDK 17: Apple native handles all channel messages correctly but silently drops outbound SysEx; CoreMIDI4J delivers everything — CoreMIDI4J stays.

The test suite's Layer 9 (`test/themidibus/MidiBusTest.java`) pairs Apple-native with a Swift/CoreMIDI helper (`scripts/midi-test.swift`) to isolate which direction is broken for each message type. Current finding: Apple-native **receives** SysEx fine but **sends** are silently dropped — so sketches that only need to read inbound SysEx can safely use `MidiBus.bypassCoreMidi4J(true)`, while any sketch that needs to push SysEx to hardware requires CoreMIDI4J.


# macOS Java MIDI Issues Analysis

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
