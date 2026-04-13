# CLAUDE.md

## Project overview

[The MidiBus](http://smallbutdigital.com/projects/themidibus/) is a minimal MIDI library for [Processing](http://www.processing.org/) that wraps `javax.sound.midi` with a simpler, Processing-friendly API. It is a library (JAR + examples + javadoc), not an application. Distributed via the Processing Library Manager.

## Build (Ant)

- `ant jar` — compile and produce `library/themidibus.jar`
- `ant main` — default; jar then clean
- `ant doc` — Javadoc into `output/reference/`
- `ant zip` — jar + doc, package release into `output/themidibus.zip`
- `ant test` — build jar, run headless test suite
- `ant clean` — delete `output/` and `library/`

Requires two env vars (build aborts without them):
- `$PROCESSING_CORE_DIR` — Processing's `core/library/` dir (provides `core.jar`)
- `$PROCESSING_JAVAC` — `javac` from Processing's bundled JDK

Build classpath also includes `lib/coremidi4j-1.6.jar`. The `library/` dir (gitignored) is the distributable output; `lib/` is build-time deps.

## Key files

- `src/themidibus/PApplet.java` — **documentation-only stub**, excluded from compilation. Do not add logic or remove the exclude.
- `library.properties` — version for Processing Library Manager. Bump `version` (int) and `prettyVersion` (string) for releases.
- `scripts/deploy.sh` — builds and uploads release to S3. Requires `$MIDIBUS_SBD_AWS_PROFILE`.

## Architecture

### Reflection callback model

Sketches define methods like `noteOn(int, int, int)` and the MidiBus invokes them via reflection (no interface needed). `registerParent()` probes for all overloads; `notifyParent()` invokes non-null ones.

**Adding a callback overload requires syncing 5 places:** (1) field in MidiBus, (2) lookup in `registerParent`, (3) invoke in `notifyParent`, (4) stub in `PApplet.java`, (5) `TestParent` in `MidiBusTest.java`.

### MidiBus I/O model

Each `MidiBus` fans out sends to **all** attached outputs and merges **all** attached inputs. Use multiple `MidiBus` instances + `bus_name` overloads to distinguish devices.

### CoreMIDI4J

Only import is `CoreMidiDeviceProvider.getMidiDeviceInfo()` in `MidiBus.findMidiDevices()` — one line. Needed because Apple's Java MIDI cannot **send** SysEx (receiving works). `bypassCoreMidi4J(true)` opts out with warnings. Do not remove the warnings — they're load-bearing UX.

### Listener interfaces

`MidiListener` (marker) -> `RawMidiListener`, `StandardMidiListener`, `SimpleMidiListener`, `ObjectMidiListener`. Dispatched in `notifyListeners()`.

## Testing

`ant test` runs `test/themidibus/MidiBusTest.java`. Layers 1-5 are pure/headless. Layer 6 uses Gervill. Layers 7 and 9 use IAC Driver (run `scripts/setup-iac.sh` to enable). Layer 8 tests bypassCoreMidi4J. Layer 10 tests throwErrors. Skippable layers report SKIP, not failure.

The IAC probe is Swift (`scripts/iac-probe.swift`), not Java, to avoid false results from Java MIDI flakiness.
