# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

[The MidiBus](http://smallbutdigital.com/projects/themidibus/) is a minimal MIDI library for [Processing](http://www.processing.org/) that wraps `javax.sound.midi` with a simpler, Processing-friendly API. It is a library (distributed as a JAR + examples + javadoc zip), not an application — there is no no runtime entry point. It is distributed inside processing via the library manager.

## Build commands (Ant)

The build system is Ant (`build.xml`). There is no Maven/Gradle, no test runner, and no linter configured.

- `ant jar` — compile `src/` to `bin/` and produce `library/themidibus.jar` (the distributable JAR).
- `ant main` — default target; same as `jar` followed by `clean` (removes `bin/`).
- `ant doc` — generate Javadoc into `reference/`.
- `ant zip` — build jar + doc and package the full release as `themidibus.zip` (suitable for Processing Library Manager upload).
- `ant test` — build jar, compile `test/` to `bin-test/`, run the headless smoke test, delete `bin-test/`. See "Testing" below.
- `ant clean` — delete `bin/`.

### Build dependencies and paths (important)

`build.xml` needs two paths from a local Processing install, supplied via environment variables. **There are no hardcoded fallbacks** — the build aborts at initialization with a clear `<fail>` message if either is unset.

- `$PROCESSING_CORE_DIR` → `processing.core.dir`: Processing's `core/library/` directory, which holds `core.jar` (`processing.core.PApplet`) for the compile classpath.
- `$PROCESSING_JAVAC` → `processing.javac`: the `javac` executable inside Processing's bundled JDK. Compilation is pinned to this JDK, not the system default, because Processing sketches must be compiled against the same JDK Processing itself runs.

Both abort conditions use top-level `<fail>` tasks with `<condition><not><isset .../></not></condition>` — top-level Ant tasks run during project initialization, so any target (`jar`, `doc`, `zip`, `clean`) triggers the check. See the top of `build.xml`.

If a build fails:
- "PROCESSING_CORE_DIR is not set" / "PROCESSING_JAVAC is not set" → the user hasn't exported the env vars; point them at README.md's "Building from source" section.
- `javac` errors about a missing executable → the JDK folder name (e.g. `jdk-17.0.14+7`) has changed because Processing was updated; `$PROCESSING_JAVAC` needs to be re-set to the new folder name.
- `core.jar` not found on classpath → `$PROCESSING_CORE_DIR` points somewhere that no longer has `core/library/core.jar`.

Historical note: earlier versions of `build.xml` had two properties, `core.dir` and `core-lib.dir`, both pointing at the same path and feeding two redundant `<fileset>` entries. These were consolidated into a single `processing.core.dir` at the same time the env-var mechanism was introduced.

The classpath also pulls every JAR under `./lib/`. Currently this is `coremidi4j-1.6.jar` (provides `uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider`, used to enumerate MIDI devices on macOS with SysEx support — see "macOS / CoreMIDI4J" below). The JAR must be present for compilation. Note that `lib/` is not ignored, `library/` is which is used in making the final build and zip.

The `javac` task uses `sourcepath=""` and `<exclude name="**/PApplet.java"/>`. The excluded `src/themidibus/PApplet.java` is a **documentation-only stub** (see `src/themidibus/PApplet.java`) that exists solely so Javadoc can describe the callback methods (`noteOn`, `noteOff`, `controllerChange`, `rawMidi`, `midiMessage`) that users override on their Processing sketch. At compile time the real `processing.core.PApplet` from `core.jar` is used instead. Do not remove the exclude, and do not add real logic to the stub.

## Architecture

### The callback model — why reflection is everywhere

The library's central design is that a Processing sketch (a `PApplet` subclass) can define methods like `noteOn(int, int, int)` or `noteOn(Note)` or `noteOn(int, int, int, long, String)`, and the MidiBus will invoke whichever overloads exist — *without the user subclassing or implementing an interface*. This matches Processing's "just define `draw()`" idiom.

This is implemented in `MidiBus.registerParent(Object)` (`src/themidibus/MidiBus.java:1208`), which uses `Class.getMethod(...)` reflection to probe the parent for every supported overload and caches the resulting `java.lang.reflect.Method` references in fields named `method_note_on`, `method_note_on_with_bus_name`, `method_note_on_wcla` (with-class-argument), etc. When MIDI arrives, `notifyParent(...)` (line 1067) invokes whichever method references are non-null. This is why the `parent` constructor argument is typed `Object`, not `PApplet` — it is only duck-typed via reflection, so the library works outside Processing too.

**Implication for changes:** adding a new callback overload requires adding (1) the field, (2) the lookup in `registerParent`, (3) the invoke in `notifyParent`, (4) a documentation stub in `src/themidibus/PApplet.java`, and (5) the corresponding declaration + counter on `TestParent` inside `test/themidibus/MidiBusTest.java`. All five must stay in sync — the test suite's Layer 2 will silently fail to exercise a new overload if step 5 is skipped.

### MidiBus is two buses in one

A `MidiBus` instance aggregates an arbitrary set of input devices and an arbitrary set of output devices. Outgoing messages are fanned out to **all** attached outputs; incoming messages from **all** attached inputs are merged. You cannot tell which input a received message came from, nor target a specific output. For independent device sets, users instantiate multiple `MidiBus` objects and disambiguate in callbacks via the `bus_name` parameter (the `..._with_bus_name` overloads exist for this).

Device attachment state lives in two `Vector<...>` fields, `input_devices` and `output_devices`, holding private inner classes `InputDeviceContainer` / `OutputDeviceContainer` (bottom of `MidiBus.java`). Each input container owns a `Transmitter` wired to an `MReceiver` (also a private inner class, `MidiBus.java:1720`) which normalizes `NOTE_ON` with velocity 0 into `NOTE_OFF` before dispatching to listeners and parent.

### Device enumeration (static, cached)

`MidiBus` keeps a static `available_devices` array populated by `findMidiDevices()` (`MidiBus.java:1552`), which calls `CoreMidiDeviceProvider.getMidiDeviceInfo()` from CoreMIDI4J rather than `MidiSystem.getMidiDeviceInfo()` directly. This cache is populated lazily on first access and can be refreshed by calling `MidiBus.findMidiDevices()` again. `list()`, `availableInputs()`, `availableOutputs()`, and `unavailableDevices()` all read from this cache. This is primarily done because the midi implementation for java on mac is very brittle and buggy. Often calling it multiple times will cause undefined behaviours or problems.

### Listener interface hierarchy

Alongside the reflection-based parent callbacks, the library also offers a conventional listener API via `addMidiListener(MidiListener)`. The marker interface `MidiListener` is sub-typed by:

- `RawMidiListener` — `rawMidi(byte[])`
- `StandardMidiListener` — `midiMessage(MidiMessage)`
- `SimpleMidiListener` — `noteOn`/`noteOff`/`controllerChange` with primitive args
- `ObjectMidiListener` — same as Simple but with `Note` / `ControlChange` value objects

`notifyListeners()` (`MidiBus.java:1022`) walks the listener vector and dispatches based on `instanceof`. `Note` and `ControlChange` are plain value classes in the same package.

### macOS / CoreMIDI4J

The Apple-native Java MIDI subsystem historically has not supported SysEx and messages with status byte ≥ `0xF0`. Also more recently it sometimes just doesn't work at all. This library works around that by depending on CoreMIDI4J (`lib/coremidi4j-1.6.jar`) for device enumeration. The older workaround (MMJ) is still mentioned in README.md and in `package-info.java`, and `sendTimestamps(false)` is the compatibility hook for that case — do not remove it even though CoreMIDI4J is now the default path.

## Release artifacts

`library/themidibus.jar` is the built JAR; it is gitignored but produced by `ant jar`. `library.properties` (read by Processing's Library Manager) holds the version — bump both `version` (int) and `prettyVersion` (string) there for releases, and add a `CHANGELOG.txt` entry. The `ant zip` target produces the file users download via the "themidibus-latest.zip" URL referenced in README.md.

## Testing

`ant test` runs a headless smoke suite in `test/themidibus/MidiBusTest.java` that mirrors the operations in all four `examples/*.pde` sketches. The test lives in package `themidibus` so it can call the package-private dispatch entry points `notifyListeners` (`src/themidibus/MidiBus.java:1022`) and `notifyParent` (`src/themidibus/MidiBus.java:1067`) directly — this is how Layers 2–4 exercise the reflection/listener dispatch layer without real MIDI hardware.

Seven layers:
1. Value classes (`Note`, `ControlChange`) — pure.
2. Reflection callback dispatch via `notifyParent` — uses the fat `TestParent` that declares every callback overload `registerParent` probes for, with per-overload counters.
3. Listener dispatch via `notifyListeners` — covers the full `MidiListener` subinterface hierarchy (which none of the examples exercise).
4. Multi-bus isolation with distinct `bus_name`s.
5. Device enumeration — static methods.
6. Real send pipeline against Gervill (Java Sound Synthesizer). Skipped if Gervill isn't present.
7. IAC Driver round-trip loopback. Skipped if IAC isn't visible OR if IAC is visible but not forwarding (the "Device is online" checkbox is unchecked). Layer 7 is the only test that covers the velocity-0 `NOTE_ON` → `NOTE_OFF` rewrite inside `MReceiver.send` at `src/themidibus/MidiBus.java:1732`, since `MReceiver` is a private inner class and that rewrite can only be reached via a real device reception path.

Layers that can't run are reported as `SKIP` in the summary, not failures — `ant test` still exits 0 if every runnable layer passes. The overall suite uses a tiny inline assertion helper (no JUnit) and prints a per-layer `N/M` pass count plus a list of failures.

### IAC Driver setup (for Layer 7)

`scripts/setup-iac.sh` opens Audio MIDI Setup and walks the user through enabling the IAC Driver. It does not GUI-script the config — Audio MIDI Setup has no usable AppleScript dictionary for MIDI Studio and GUI scripting the icon grid is brittle across macOS versions. The script is a humane wrapper: open app, print steps, wait on Enter, re-probe.

`scripts/check-iac.sh` is the probe the setup script uses — it reruns `MidiBusTest --check-iac`, which does a live loopback round-trip (send NOTE_ON through IAC, wait up to 300ms for the callback). Name presence alone is not enough because IAC can be visible but offline. Use `check-iac.sh` directly if you want to verify your setup without running the full suite.

**Do not replace IAC with CoreMIDI4J virtual endpoints.** CoreMIDI4J is deliberately used minimally (only for `CoreMidiDeviceProvider.getMidiDeviceInfo()` in `MidiBus.findMidiDevices`); the project treats it as a forced dependency with the smallest possible surface area. The loopback comes from user-configured IAC, not from `CoreMidiSource`/`CoreMidiDestination`.
