The MidiBus is a MIDI library for [Processing](http://www.processing.org/) which provides a fast and easy way to send and receive MIDI data. <!--more-->The MidiBus is desgined primarily for real time MIDI applications. It's focused on MIDI I/O and keeps the frills to a minimum; currently it has no built in sequencer, file read/write, MIDI recording/playback.

# Installation

The best way to install the MidiBus is via the [Processing Library Manager](http://wiki.processing.org/w/How_to_Install_a_Contributed_Library). This way Processing will automatically notify you when a new version is available.

If you want to install the MidiBus manually you can get the latest stable build including binaries, examples and documentation [via direct downloadd](http://smallbutdigital.com/releases/themidibus/themidibus-latest.zip).

# Resources and Documentation

* [GitHub page](http://github.com/sparks/themidibus)
* [The MidiBus Online Javadocs](http://www.smallbutdigital.com/docs/themidibus/themidibus/package-summary.html)

# A Few Quick Notes

Before you get started, it is important to understand that the MidiBus offers little functionality that isn't available from Java's native [javax.sound.midi package](http://docs.oracle.com/javase/6/docs/api/javax/sound/midi/package-summary.html). Anyone interested in working with MIDI in Java should take the time to read the documentation for the [javax.sound.midi package](http://docs.oracle.com/javase/6/docs/api/javax/sound/midi/package-summary.html). It offers a more full featured and flexible alternative to this package but it is also more complicated to use. In addition, it is also worthwhile to skim [the "official" Java Tutorial for the javax.sound.* packages](http://docs.oracle.com/javase/tutorial/sound/index.html).

# Getting Started

The MidiBus is very straight forwards to use. A good place to start is the included Basic.pde example. From there you can look at the JavaDocs either [online](http://www.smallbutdigital.com/docs/themidibus/themidibus/package-summary.html) or bundled with your library in the "reference" subdirectory. The JavaDocs are a comprehensive reference of all the MidiBus' available functionality. There are also a few advanced examples which can (hopefully) help answer most common questions: how to work with multiple input/output devices, how to send uncommon MIDI messages and how to receive uncommon MIDI messages.

Please do not hesitate to contact me with any questions, comments or bug reports.

# Caveats, Problems with SysEx, Alternate MIDI for java

The Apple MIDI subsystem has a number of problems. Most notably it doesn't seem to support MIDI messages with a status byte `>= 0xF0` such as SysEx messages. You can use [MMJ](http://www.humatic.de/htools/mmj.htm) as an alternate subsystem. To do so, download mmj and add both `mmj.jar` and `libmmj.jnilib` to the midibus `library` subdirectory. You will also need to disable timestamps in your MidiBus instance otherwise MMJ won't work properly. You can do this by calling `mybus.sendTimestamp(false)`
