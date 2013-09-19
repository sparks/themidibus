# The MidiBus

The MidiBus is a MIDI library for Processing. It provides a quick and simple way to access and interact with installed MIDI system resources. The MidiBus is aimed primarily at real time MIDI applications. The focus is on strong MIDI I/O capabilities and keeping frills to a minimum (e.g. no built in sequencer, file read/write, MIDI recording/playback).

## Download and Install (Stable)

The recommended way to install the MidiBus is the [Processing Library Manager](http://wiki.processing.org/w/How_to_Install_a_Contributed_Library). This way Processing will automatically notify you when a new version is available.

If you wish to install the MidiBus manually you can get the latest stable build including binaries and docs [via direct downloadd](http://smallbutdigital.com/releases/themidibus/themidibus-latest.zip).

## Download and Install (Latest)

The latest version can easily be cloned directly from github via

    git clone https://github.com/sparks/themidibus.git /your/processing/libraries/

### Compiling and Generating JavaDocs

If you clone the latest version you will need to compile the library and generate the JavaDocs. This requires ``javac`` and ``ant``.

To compile run ``ant`` 

To generate the JavaDocs run ``ant doc``

## About
Before you get started, it is important to understand that the MidiBus offers little functionality that isn't available from Java's native [javax.sound.midi package](http://docs.oracle.com/javase/6/docs/api/javax/sound/midi/package-summary.html). Anyone interested in working with MIDI in Java should take the time to read the documentation for the [javax.sound.midi package](http://docs.oracle.com/javase/6/docs/api/javax/sound/midi/package-summary.html). It offers a more full featured and flexible alternative to this package, although it does do so at the cost of added complexity. In addition, it may be worthwhile to skim [the "official" Java Tutorial for the javax.sound.* packages](http://docs.oracle.com/javase/tutorial/sound/index.html).

## Getting Started

The MidiBus is very straight forwards to use. A good place to start is the included Basic.pde example. From there you can look at the JavaDocs either [online](http://smallbutdigital.com/themidibus/themidibus/package-summary.html) or bundled with your library in the "reference" subdirectory. The JavaDocs are a comprehensive reference of all the MidiBus' available functionality. There are also a few advanced examples which can (hopefully) help answer the most common questions: how to work with multiple input/output devices, how to send uncommon MIDI messages and how to receive uncommon MIDI messages.

## Using Alternate MIDI Subsytem

The Apple MIDI subsystem has a number of problems. Most notably it doesn't seem to support MIDI messages with status >= 0xF0 such as SysEx messages. You can use [MMJ](http://www.humatic.de/htools/mmj.htm) as an alternate subsystem. To do so, download mmj and add both `mmj.jar` and `libmmj.jnilib` to the midibus `library` subdirectory. You must also disable timestamps in your MidiBus instance otherwise MMJ won't work properly. You can do so by calling `mybus.sendTimestamp(false)`

## Liscence

GPL3