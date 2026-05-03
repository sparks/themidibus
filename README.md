<img src="http://smallbutdigital.com/media/projects/themidibus/themidibus.png" alt="The MidiBus" align="right" width="200">

[The MidiBus](http://smallbutdigital.com/projects/themidibus/) is a MIDI library for [Processing](http://www.processing.org/) which provides a fast and easy way to send and receive MIDI data. <!--more-->The MidiBus is desgined primarily for real time MIDI applications. It's focused on MIDI I/O and keeps the frills to a minimum; currently it has no built in sequencer, file read/write, MIDI recording/playback.

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

# Caveats, SysEx on macOS

Apple's native Java MIDI implementation on macOS silently drops outbound SysEx messages (`>= 0xF0`). The MidiBus bundles [CoreMIDI4J](https://github.com/DerekCook/CoreMidi4J) to work around this — no extra setup is needed. SysEx send and receive both work out of the box.

If you need to bypass CoreMIDI4J for any reason, you can call `MidiBus.bypassCoreMidi4J(true)`, but be aware that outbound SysEx will be silently dropped. Receiving SysEx still works in bypass mode. See `MAC-JAVA-ISSUES.md` for the full technical details.

# Building from source, etc

See [DEVELOPER-NOTES.md](./DEVELOPER-NOTES.md)