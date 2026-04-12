The MidiBus is a MIDI library for [Processing](http://www.processing.org/) which provides a fast and easy way to send and receive MIDI data. <!--more-->The MidiBus is desgined primarily for real time MIDI applications. It's focused on MIDI I/O and keeps the frills to a minimum; currently it has no built in sequencer, file read/write, MIDI recording/playback.

# Installation

The best way to install the MidiBus is via the [Processing Library Manager](http://wiki.processing.org/w/How_to_Install_a_Contributed_Library). This way Processing will automatically notify you when a new version is available.

If you want to install the MidiBus manually you can get the latest stable build including binaries, examples and documentation [via direct downloadd](http://www.smallbutdigital.com/releases/themidibus/themidibus-latest.zip).

# Resources and Documentation

* [GitHub page](http://github.com/sparks/themidibus)
* [The MidiBus Online Javadocs](http://www.smallbutdigital.com/docs/themidibus/themidibus/package-summary.html)

# A Few Quick Notes

Before you get started, it is important to understand that the MidiBus offers little functionality that isn't available from Java's native [javax.sound.midi package](http://docs.oracle.com/javase/6/docs/api/javax/sound/midi/package-summary.html). Anyone interested in working with MIDI in Java should take the time to read the documentation for the [javax.sound.midi package](http://docs.oracle.com/javase/6/docs/api/javax/sound/midi/package-summary.html). It offers a more full featured and flexible alternative to this package but it is also more complicated to use. In addition, it is also worthwhile to skim [the "official" Java Tutorial for the javax.sound.* packages](http://docs.oracle.com/javase/tutorial/sound/index.html).

# Getting Started

The MidiBus is very straight forwards to use. A good place to start is the included Basic.pde example. From there you can look at the JavaDocs either [online](http://www.smallbutdigital.com/docs/themidibus/themidibus/package-summary.html) or bundled with your library in the "reference" subdirectory. The JavaDocs are a comprehensive reference of all the MidiBus' available functionality. There are also a few advanced examples which can (hopefully) help answer most common questions: how to work with multiple input/output devices, how to send uncommon MIDI messages and how to receive uncommon MIDI messages.

Please do not hesitate to contact me with any questions, comments or bug reports.

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

# Testing

A headless smoke test suite exercises the same operations as the example sketches without needing Processing. Run it with:

```
ant test
```

## What gets tested

The suite is organized into seven layers (see `test/themidibus/MidiBusTest.java`). Layers 1–5 run anywhere, with no hardware or setup:

1. **Value classes** — `Note` / `ControlChange` constructors, accessors, setters.
2. **Reflection callback dispatch** — verifies `MidiBus.registerParent` caches all the overloads and `notifyParent` dispatches to every non-null one (noteOn/noteOff/CC in plain, `_with_bus_name`, and `Note`/`ControlChange`-object forms, plus `rawMidi` and `midiMessage`).
3. **Listener dispatch** — the `SimpleMidiListener`, `ObjectMidiListener`, `RawMidiListener`, `StandardMidiListener` hierarchy.
4. **Multi-bus isolation** — two buses with distinct `bus_name`s dispatch independently and carry the right name in `_with_bus_name` callbacks.
5. **Device enumeration** — the static `list()` / `availableInputs()` / `availableOutputs()` / `unavailableDevices()` methods don't crash and return sane values. Also prints the detected devices for diagnostics.

Layer 6 verifies the full send pipeline against the Java Sound Synthesizer (Gervill), which ships with every JDK. It mirrors the send sequences in `Basic.pde`, `BasicWithClasses.pde`, and `AdvancedMIDIMessageIO.pde`. If Gervill is not present, the layer is skipped cleanly.

Layer 7 verifies the full receive pipeline — and the `NOTE_ON`-with-velocity-0 → `NOTE_OFF` rewrite in `MReceiver` — by looping MIDI back through macOS's IAC Driver. This is the only layer that needs user setup (see below). If IAC is unconfigured or not routing messages, the layer is skipped cleanly and the overall run still passes.

Layers that cannot run are reported as `SKIP` in the summary, not as failures.

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

This does a live loopback probe (sends a MIDI message through IAC and waits for it to return) and exits 0 if the round-trip works, 1 if it doesn't. A printed `IAC ready: …` means you're ready for `ant test`. Name-presence alone isn't enough — if "Device is online" is unchecked, the device shows up in the Java MIDI system but won't forward messages, and the probe will tell you so.

# Caveats, Problems with SysEx, Alternate MIDI for java

The Apple MIDI subsystem has a number of problems. Most notably it doesn't seem to support MIDI messages with a status byte `>= 0xF0` such as SysEx messages. You can use [MMJ](http://www.humatic.de/htools/mmj.htm) as an alternate subsystem. To do so, download mmj and add both `mmj.jar` and `libmmj.jnilib` to the midibus `library` subdirectory. You will also need to disable timestamps in your MidiBus instance otherwise MMJ won't work properly. You can do this by calling `mybus.sendTimestamp(false)`
