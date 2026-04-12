/*
 * Headless smoke test suite for the MidiBus library.
 *
 * Runs in package `themidibus` so it can call the package-private
 * notifyParent() and notifyListeners() dispatch entry points directly,
 * driving the reflection/listener layer without real MIDI hardware.
 *
 * Invoked by `ant test`.
 *
 * Layer 7 (IAC loopback) uses scripts/iac-probe.swift as its precondition —
 * a pure-CoreMIDI round-trip check — so the skip/run decision is orthogonal
 * to Java MIDI (which is notoriously flaky and can fail to see IAC even
 * when CoreMIDI-level routing works).
 *
 * Exit codes:
 *   0 - all layers passed or skipped cleanly
 *   1 - at least one assertion failure or unexpected exception
 */

package themidibus;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MidiBusTest {

	// Runtime-probed device names. null means "not present, skip dependent layers".
	static String OUTPUT_SYNTH_NAME;
	static String IAC_INPUT_NAME;
	static String IAC_OUTPUT_NAME;

	// Per-layer pass/fail tracking.
	static int layerPassCount;
	static int layerFailCount;
	static int totalPassCount;
	static int totalFailCount;
	static final List<String> failures = new ArrayList<>();
	static final List<String> summary = new ArrayList<>();

	public static void main(String[] args) {
		MidiBus.findMidiDevices();
		probeDevices();

		System.out.println("=== themidibus headless test suite ===");
		System.out.println();
		System.out.println("Probed devices:");
		System.out.println("  OUTPUT_SYNTH_NAME = " + OUTPUT_SYNTH_NAME);
		System.out.println("  IAC_INPUT_NAME    = " + IAC_INPUT_NAME);
		System.out.println("  IAC_OUTPUT_NAME   = " + IAC_OUTPUT_NAME);
		System.out.println();

		runLayer("Layer 1 (value classes)",             MidiBusTest::layer1_valueClasses);
		runLayer("Layer 2 (reflection dispatch)",       MidiBusTest::layer2_reflectionDispatch);
		runLayer("Layer 3 (listener dispatch)",         MidiBusTest::layer3_listenerDispatch);
		runLayer("Layer 4 (multi-bus isolation)",       MidiBusTest::layer4_multiBusIsolation);
		runLayer("Layer 5 (device enumeration)",        MidiBusTest::layer5_deviceEnumeration);
		runLayer("Layer 6 (Gervill send pipeline)",     MidiBusTest::layer6_gervillSendPipeline);
		runLayer("Layer 7 (IAC round-trip loopback)",   MidiBusTest::layer7_iacLoopback);

		System.out.println();
		System.out.println("=== Summary ===");
		for (String line : summary) System.out.println("  " + line);
		System.out.println("  ----");
		System.out.println("  " + totalPassCount + "/" + (totalPassCount + totalFailCount) + " assertions passed");
		if (!failures.isEmpty()) {
			System.out.println();
			System.out.println("Failures:");
			for (String f : failures) System.out.println("  - " + f);
		}

		System.exit(totalFailCount == 0 ? 0 : 1);
	}

	/* -- Device probing -- */

	static void probeDevices() {
		String[] outputs = MidiBus.availableOutputs();
		for (String o : outputs) {
			if ("Gervill".equals(o) || "Java Sound Synthesizer".equals(o)) {
				OUTPUT_SYNTH_NAME = o;
				break;
			}
		}

		String[] inputs = MidiBus.availableInputs();
		for (String i : inputs) {
			if (i.toLowerCase().contains("iac")) {
				IAC_INPUT_NAME = i;
				break;
			}
		}
		for (String o : outputs) {
			if (o.toLowerCase().contains("iac")) {
				IAC_OUTPUT_NAME = o;
				break;
			}
		}
	}

	/* -- Layer runner -- */

	interface LayerFn { void run() throws Exception; }

	static void runLayer(String name, LayerFn fn) {
		layerPassCount = 0;
		layerFailCount = 0;
		System.out.println(">>> " + name);
		try {
			fn.run();
		} catch (SkipException e) {
			System.out.println("    SKIP: " + e.getMessage());
			summary.add(String.format("%-40s  SKIP  (%s)", name, e.getMessage()));
			return;
		} catch (Throwable t) {
			layerFailCount++;
			String msg = "unexpected exception: " + t.getClass().getSimpleName() + ": " + t.getMessage();
			failures.add(name + ": " + msg);
			System.out.println("    FAIL: " + msg);
			t.printStackTrace();
		}
		totalPassCount += layerPassCount;
		totalFailCount += layerFailCount;
		summary.add(String.format("%-40s  %d/%d  %s",
				name,
				layerPassCount,
				layerPassCount + layerFailCount,
				layerFailCount == 0 ? "PASS" : "FAIL"));
	}

	static class SkipException extends RuntimeException {
		SkipException(String msg) { super(msg); }
	}

	static void skip(String reason) { throw new SkipException(reason); }

	/* -- Assertions -- */

	static void assertEq(Object expected, Object actual, String what) {
		boolean eq = (expected == null) ? actual == null : expected.equals(actual);
		if (eq) {
			layerPassCount++;
		} else {
			layerFailCount++;
			String msg = what + ": expected <" + expected + ">, got <" + actual + ">";
			failures.add(msg);
			System.out.println("    FAIL: " + msg);
		}
	}

	static void assertTrue(boolean cond, String what) {
		if (cond) {
			layerPassCount++;
		} else {
			layerFailCount++;
			failures.add(what);
			System.out.println("    FAIL: " + what);
		}
	}

	static void assertArrayEq(byte[] expected, byte[] actual, String what) {
		if (expected == null && actual == null) { layerPassCount++; return; }
		if (expected == null || actual == null || expected.length != actual.length) {
			layerFailCount++;
			failures.add(what + ": array length mismatch");
			System.out.println("    FAIL: " + what + ": array length mismatch");
			return;
		}
		for (int i = 0; i < expected.length; i++) {
			if (expected[i] != actual[i]) {
				layerFailCount++;
				failures.add(what + ": byte[" + i + "] expected " + (expected[i] & 0xFF) + ", got " + (actual[i] & 0xFF));
				System.out.println("    FAIL: " + what + ": byte[" + i + "] mismatch");
				return;
			}
		}
		layerPassCount++;
	}

	/**
	 * Shell out to scripts/iac-probe.swift (pure CoreMIDI, no Java MIDI involved).
	 * Returns the probe's exit code, or -1 if `swift` is not available / the
	 * probe script can't be located.
	 */
	static int runSwiftIacProbe() {
		// Locate scripts/iac-probe.swift relative to the project root. When invoked
		// via `ant test`, the working directory is the project root.
		Path probe = Paths.get("scripts/iac-probe.swift");
		if (!probe.toFile().exists()) {
			return -1;
		}
		try {
			ProcessBuilder pb = new ProcessBuilder("swift", probe.toString());
			pb.redirectErrorStream(true);
			Process p = pb.start();
			// Drain stdout so the probe can't block on a full pipe, and surface
			// its line in the test log for context.
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = r.readLine()) != null) {
					System.out.println("    [iac-probe] " + line);
				}
			}
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return -1;
			}
			return p.exitValue();
		} catch (Exception e) {
			System.out.println("    [iac-probe] launch failed: " + e.getMessage());
			return -1;
		}
	}

	/* -- Message builders -- */

	static ShortMessage shortMsg(int command, int channel, int d1, int d2) {
		try {
			ShortMessage m = new ShortMessage();
			m.setMessage(command, channel, d1, d2);
			return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static SysexMessage sysexMsg(byte[] payload) {
		try {
			SysexMessage m = new SysexMessage();
			m.setMessage(payload, payload.length);
			return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* ========================================================= */
	/* Layer 1 - value classes                                    */
	/* ========================================================= */

	static void layer1_valueClasses() {
		// Note: 3-arg constructor
		Note n = new Note(5, 60, 100);
		assertEq(5,   n.channel(),  "Note(5,60,100).channel()");
		assertEq(60,  n.pitch(),    "Note(5,60,100).pitch()");
		assertEq(100, n.velocity(), "Note(5,60,100).velocity()");
		assertEq(0L,  n.ticks(),    "Note(5,60,100).ticks() defaults to 0");

		// Note: timestamp + bus_name constructor
		Note n2 = new Note(1, 64, 127, 42L, "busX");
		assertEq(1,      n2.channel(),  "Note(...,bus_name).channel()");
		assertEq(64,     n2.pitch(),    "Note(...,bus_name).pitch()");
		assertEq(127,    n2.velocity(), "Note(...,bus_name).velocity()");
		assertEq(42L,    n2.timestamp,  "Note(...,bus_name).timestamp");
		assertEq("busX", n2.bus_name,   "Note(...,bus_name).bus_name");

		// Note: setters round-trip
		Note n3 = new Note(0, 0, 0);
		n3.setChannel(7);
		n3.setPitch(72);
		n3.setVelocity(64);
		n3.setTicks(128);
		assertEq(7,    n3.channel(),  "Note setChannel round-trip");
		assertEq(72,   n3.pitch(),    "Note setPitch round-trip");
		assertEq(64,   n3.velocity(), "Note setVelocity round-trip");
		assertEq(128L, n3.ticks(),    "Note setTicks round-trip");

		// Note.name() / octave() — pitch 60 is middle C (C4)
		Note c4 = new Note(0, 60, 0);
		assertEq("C", c4.name(),    "Note(pitch=60).name() == \"C\"");
		assertEq(5,   c4.octave(),  "Note(pitch=60).octave() == 5"); // pitch/12 = 5

		// ControlChange: 3-arg constructor
		ControlChange cc = new ControlChange(2, 7, 110);
		assertEq(2,   cc.channel(), "ControlChange(2,7,110).channel()");
		assertEq(7,   cc.number(),  "ControlChange(2,7,110).number()");
		assertEq(110, cc.value(),   "ControlChange(2,7,110).value()");

		// ControlChange: timestamp + bus_name constructor
		ControlChange cc2 = new ControlChange(3, 10, 50, 99L, "busCC");
		assertEq(3,       cc2.channel(), "ControlChange(...,bus_name).channel()");
		assertEq(10,      cc2.number(),  "ControlChange(...,bus_name).number()");
		assertEq(50,      cc2.value(),   "ControlChange(...,bus_name).value()");
		assertEq(99L,     cc2.timestamp, "ControlChange(...,bus_name).timestamp");
		assertEq("busCC", cc2.bus_name,  "ControlChange(...,bus_name).bus_name");

		// ControlChange: setters round-trip
		ControlChange cc3 = new ControlChange(0, 0, 0);
		cc3.setChannel(8);
		cc3.setNumber(20);
		cc3.setValue(90);
		assertEq(8,  cc3.channel(), "ControlChange setChannel round-trip");
		assertEq(20, cc3.number(),  "ControlChange setNumber round-trip");
		assertEq(90, cc3.value(),   "ControlChange setValue round-trip");
	}

	/* ========================================================= */
	/* Layer 2 - reflection callback dispatch                     */
	/* ========================================================= */

	static void layer2_reflectionDispatch() throws Exception {
		TestParent parent = new TestParent();
		MidiBus bus = new MidiBus(parent, "test_bus");

		// -- NOTE_ON dispatch --
		parent.reset();
		bus.notifyParent(shortMsg(ShortMessage.NOTE_ON, 0, 64, 127), 42L);
		assertEq(1, parent.noteOnBasicCount,     "NOTE_ON: noteOn(int,int,int) fires once");
		assertEq(1, parent.noteOnWithBusCount,   "NOTE_ON: noteOn(int,int,int,long,String) fires once");
		assertEq(1, parent.noteOnObjectCount,    "NOTE_ON: noteOn(Note) fires once");
		assertEq(0, parent.noteOffBasicCount,    "NOTE_ON does not fire noteOff");
		assertEq(0, parent.ccBasicCount,         "NOTE_ON does not fire controllerChange");
		assertEq(1, parent.rawMidiCount,         "NOTE_ON: rawMidi(byte[]) fires once");
		assertEq(1, parent.rawMidiWithBusCount,  "NOTE_ON: rawMidi(byte[],long,String) fires once");
		assertEq(1, parent.midiMessageCount,     "NOTE_ON: midiMessage(MidiMessage) fires once");
		assertEq(1, parent.midiMessageWithBusCount, "NOTE_ON: midiMessage(MidiMessage,long,String) fires once");
		assertEq(0,      parent.lastNoteOnChannel,  "NOTE_ON captured channel");
		assertEq(64,     parent.lastNoteOnPitch,    "NOTE_ON captured pitch");
		assertEq(127,    parent.lastNoteOnVelocity, "NOTE_ON captured velocity");
		assertEq(42L,    parent.lastNoteOnTimestamp,"NOTE_ON captured timestamp");
		assertEq("test_bus", parent.lastNoteOnBusName, "NOTE_ON captured bus_name");
		assertTrue(parent.lastNoteOnObject != null,    "NOTE_ON Note object received");
		if (parent.lastNoteOnObject != null) {
			assertEq(64, parent.lastNoteOnObject.pitch(),  "NOTE_ON Note.pitch");
			assertEq(127, parent.lastNoteOnObject.velocity(), "NOTE_ON Note.velocity");
			assertEq("test_bus", parent.lastNoteOnObject.bus_name, "NOTE_ON Note.bus_name");
		}

		// -- NOTE_OFF dispatch --
		parent.reset();
		bus.notifyParent(shortMsg(ShortMessage.NOTE_OFF, 3, 50, 10), 7L);
		assertEq(1, parent.noteOffBasicCount,    "NOTE_OFF: noteOff(int,int,int) fires once");
		assertEq(1, parent.noteOffWithBusCount,  "NOTE_OFF: noteOff(int,int,int,long,String) fires once");
		assertEq(1, parent.noteOffObjectCount,   "NOTE_OFF: noteOff(Note) fires once");
		assertEq(0, parent.noteOnBasicCount,     "NOTE_OFF does not fire noteOn");
		assertEq(1, parent.rawMidiCount,         "NOTE_OFF: rawMidi fires once");
		assertEq(1, parent.midiMessageCount,     "NOTE_OFF: midiMessage fires once");
		assertEq(3,  parent.lastNoteOffChannel,  "NOTE_OFF captured channel");
		assertEq(50, parent.lastNoteOffPitch,    "NOTE_OFF captured pitch");
		assertEq(10, parent.lastNoteOffVelocity, "NOTE_OFF captured velocity");

		// -- CONTROL_CHANGE dispatch --
		parent.reset();
		bus.notifyParent(shortMsg(ShortMessage.CONTROL_CHANGE, 5, 7, 88), 0L);
		assertEq(1, parent.ccBasicCount,        "CC: controllerChange(int,int,int) fires once");
		assertEq(1, parent.ccWithBusCount,      "CC: controllerChange(int,int,int,long,String) fires once");
		assertEq(1, parent.ccObjectCount,       "CC: controllerChange(ControlChange) fires once");
		assertEq(0, parent.noteOnBasicCount,    "CC does not fire noteOn");
		assertEq(0, parent.noteOffBasicCount,   "CC does not fire noteOff");
		assertEq(1, parent.rawMidiCount,        "CC: rawMidi fires once");
		assertEq(1, parent.midiMessageCount,    "CC: midiMessage fires once");
		assertEq(5,  parent.lastCcChannel, "CC captured channel");
		assertEq(7,  parent.lastCcNumber,  "CC captured number");
		assertEq(88, parent.lastCcValue,   "CC captured value");

		// -- SYSEX dispatch --
		parent.reset();
		byte[] sysex = { (byte)0xF0, 0x01, 0x02, 0x03, 0x04, (byte)0xF7 };
		bus.notifyParent(sysexMsg(sysex), 0L);
		assertEq(0, parent.noteOnBasicCount,  "SysEx does not fire noteOn");
		assertEq(0, parent.noteOffBasicCount, "SysEx does not fire noteOff");
		assertEq(0, parent.ccBasicCount,      "SysEx does not fire controllerChange");
		assertEq(1, parent.rawMidiCount,      "SysEx: rawMidi fires once");
		assertEq(1, parent.midiMessageCount,  "SysEx: midiMessage fires once");
		assertArrayEq(sysex, parent.lastRawMidiData, "SysEx raw bytes captured");

		// -- Narrow parent: only noteOn(int,int,int) declared, others tolerated --
		NarrowParent narrow = new NarrowParent();
		MidiBus narrowBus = new MidiBus(narrow, "narrow_bus");
		narrowBus.notifyParent(shortMsg(ShortMessage.NOTE_ON, 0, 64, 127), 0L);
		assertEq(1, narrow.count, "Narrow parent: noteOn(int,int,int) fires with no error");
	}

	/* ========================================================= */
	/* Layer 3 - listener dispatch                                */
	/* ========================================================= */

	static void layer3_listenerDispatch() throws Exception {
		MidiBus bus = new MidiBus(null, "listener_bus");

		CountingSimpleListener simple = new CountingSimpleListener();
		CountingObjectListener object = new CountingObjectListener();
		CountingRawListener raw = new CountingRawListener();
		CountingStandardListener standard = new CountingStandardListener();
		bus.addMidiListener(simple);
		bus.addMidiListener(object);
		bus.addMidiListener(raw);
		bus.addMidiListener(standard);

		// -- NOTE_ON --
		bus.notifyListeners(shortMsg(ShortMessage.NOTE_ON, 2, 60, 100), 0L);
		assertEq(1, simple.noteOn,   "Listener NOTE_ON: SimpleMidiListener.noteOn");
		assertEq(0, simple.noteOff,  "Listener NOTE_ON: SimpleMidiListener.noteOff not fired");
		assertEq(0, simple.cc,       "Listener NOTE_ON: SimpleMidiListener.cc not fired");
		assertEq(1, object.noteOn,   "Listener NOTE_ON: ObjectMidiListener.noteOn");
		assertEq(1, raw.count,       "Listener NOTE_ON: RawMidiListener fires");
		assertEq(1, standard.count,  "Listener NOTE_ON: StandardMidiListener fires");
		assertEq(2,  simple.lastChannel,  "Listener NOTE_ON captured channel");
		assertEq(60, simple.lastD1,       "Listener NOTE_ON captured pitch");
		assertEq(100, simple.lastD2,      "Listener NOTE_ON captured velocity");

		// -- NOTE_OFF --
		simple.reset(); object.reset(); raw.reset(); standard.reset();
		bus.notifyListeners(shortMsg(ShortMessage.NOTE_OFF, 4, 45, 20), 0L);
		assertEq(0, simple.noteOn,   "Listener NOTE_OFF: noteOn not fired");
		assertEq(1, simple.noteOff,  "Listener NOTE_OFF: noteOff fires");
		assertEq(0, simple.cc,       "Listener NOTE_OFF: cc not fired");
		assertEq(1, object.noteOff,  "Listener NOTE_OFF: ObjectMidiListener.noteOff");
		assertEq(1, raw.count,       "Listener NOTE_OFF: RawMidiListener fires");
		assertEq(1, standard.count,  "Listener NOTE_OFF: StandardMidiListener fires");

		// -- CONTROL_CHANGE --
		simple.reset(); object.reset(); raw.reset(); standard.reset();
		bus.notifyListeners(shortMsg(ShortMessage.CONTROL_CHANGE, 1, 7, 64), 0L);
		assertEq(0, simple.noteOn,  "Listener CC: noteOn not fired");
		assertEq(0, simple.noteOff, "Listener CC: noteOff not fired");
		assertEq(1, simple.cc,      "Listener CC: controllerChange fires");
		assertEq(1, object.cc,      "Listener CC: ObjectMidiListener.controllerChange");
		assertEq(1, raw.count,      "Listener CC: RawMidiListener fires");
		assertEq(1, standard.count, "Listener CC: StandardMidiListener fires");

		// -- SYSEX: only raw + standard, never simple/object --
		simple.reset(); object.reset(); raw.reset(); standard.reset();
		byte[] sysex = { (byte)0xF0, 0x05, 0x06, (byte)0xF7 };
		bus.notifyListeners(sysexMsg(sysex), 0L);
		assertEq(0, simple.noteOn + simple.noteOff + simple.cc, "Listener SysEx: SimpleMidiListener silent");
		assertEq(0, object.noteOn + object.noteOff + object.cc, "Listener SysEx: ObjectMidiListener silent");
		assertEq(1, raw.count,      "Listener SysEx: RawMidiListener fires");
		assertEq(1, standard.count, "Listener SysEx: StandardMidiListener fires");
		assertArrayEq(sysex, raw.lastData, "Listener SysEx: raw bytes captured");
	}

	/* ========================================================= */
	/* Layer 4 - multi-bus isolation                              */
	/* ========================================================= */

	static void layer4_multiBusIsolation() throws Exception {
		TestParent parentA = new TestParent();
		TestParent parentB = new TestParent();
		MidiBus busA = new MidiBus(parentA, "busA");
		MidiBus busB = new MidiBus(parentB, "busB");

		busA.notifyParent(shortMsg(ShortMessage.NOTE_ON, 0, 64, 127), 0L);
		assertEq(1, parentA.noteOnBasicCount, "busA dispatch fires parentA");
		assertEq(0, parentB.noteOnBasicCount, "busA dispatch does NOT fire parentB");
		assertEq("busA", parentA.lastNoteOnBusName, "parentA captured bus_name \"busA\"");

		busB.notifyParent(shortMsg(ShortMessage.NOTE_ON, 0, 64, 127), 0L);
		assertEq(1, parentA.noteOnBasicCount, "busB dispatch did not re-fire parentA");
		assertEq(1, parentB.noteOnBasicCount, "busB dispatch fires parentB");
		assertEq("busB", parentB.lastNoteOnBusName, "parentB captured bus_name \"busB\"");
	}

	/* ========================================================= */
	/* Layer 5 - device enumeration                               */
	/* ========================================================= */

	static void layer5_deviceEnumeration() throws Exception {
		MidiBus.findMidiDevices(); // should not throw
		layerPassCount++;

		String[] inputs = MidiBus.availableInputs();
		assertTrue(inputs != null, "availableInputs() non-null");

		String[] outputs = MidiBus.availableOutputs();
		assertTrue(outputs != null, "availableOutputs() non-null");

		String[] unavailable = MidiBus.unavailableDevices();
		assertTrue(unavailable != null, "unavailableDevices() non-null");

		// list() writes to stdout; redirect to catch it, just verify no throw.
		PrintStream old = System.out;
		try {
			System.setOut(new PrintStream(new ByteArrayOutputStream()));
			MidiBus.list();
			layerPassCount++;
		} finally {
			System.setOut(old);
		}

		// Print for developer diagnostics (matches top of MultipleBuses.pde).
		System.out.println("    Inputs:       " + java.util.Arrays.toString(inputs));
		System.out.println("    Outputs:      " + java.util.Arrays.toString(outputs));
		System.out.println("    Unavailable:  " + java.util.Arrays.toString(unavailable));
	}

	/* ========================================================= */
	/* Layer 6 - Gervill send pipeline                            */
	/* ========================================================= */

	static void layer6_gervillSendPipeline() throws Exception {
		if (OUTPUT_SYNTH_NAME == null) {
			skip("no Java Sound Synthesizer found (Gervill/Java Sound Synthesizer)");
		}

		TestParent parent = new TestParent();
		MidiBus bus = new MidiBus(parent, -1, OUTPUT_SYNTH_NAME);

		assertEq(1, bus.attachedOutputs().length, "Gervill bus: one attached output");
		assertEq(0, bus.attachedInputs().length,  "Gervill bus: no attached inputs");
		assertEq(OUTPUT_SYNTH_NAME, bus.attachedOutputs()[0], "Gervill bus: attached output matches probed name");

		// Basic.pde sequence
		bus.sendNoteOn(0, 64, 127);
		bus.sendNoteOff(0, 64, 127);
		bus.sendControllerChange(0, 0, 90);
		layerPassCount++; // no exception

		// BasicWithClasses.pde sequence
		bus.sendNoteOn(new Note(0, 64, 127));
		bus.sendNoteOff(new Note(0, 64, 127));
		bus.sendControllerChange(new ControlChange(0, 0, 90));
		layerPassCount++; // no exception

		// AdvancedMIDIMessageIO.pde sequence
		bus.sendMessage(0xA0, 0, 64, 80); // aftertouch (command, channel, d1, d2)
		layerPassCount++;

		byte[] rawSysex = { (byte)0xF0, 0x01, 0x02, 0x03, 0x04, (byte)0xF7 };
		bus.sendMessage(rawSysex);
		layerPassCount++;

		SysexMessage sx = new SysexMessage();
		sx.setMessage(new byte[] { (byte)0xF0, 0x05, 0x06, 0x07, 0x08, (byte)0xF7 }, 6);
		bus.sendMessage(sx);
		layerPassCount++;

		bus.close();
		layerPassCount++;
	}

	/* ========================================================= */
	/* Layer 7 - IAC round-trip loopback                          */
	/* ========================================================= */

	static void layer7_iacLoopback() throws Exception {
		if (IAC_INPUT_NAME == null || IAC_OUTPUT_NAME == null) {
			skip("no IAC Driver port detected in Java MIDI - run scripts/setup-iac.sh");
		}

		// Precondition: ask the pure-CoreMIDI Swift probe whether IAC is actually
		// routing messages. This is orthogonal to Java MIDI; if Swift says IAC is
		// working but the Java test below fails, that's a real themidibus /
		// CoreMIDI4J bug worth reporting, not a setup problem.
		int probeExit = runSwiftIacProbe();
		if (probeExit == -1) {
			skip("iac-probe.swift not runnable - cannot verify IAC state (is `swift` on PATH?)");
		} else if (probeExit == 1) {
			skip("no IAC device found at CoreMIDI level - run scripts/setup-iac.sh");
		} else if (probeExit == 2) {
			skip("IAC visible but not forwarding messages - run scripts/setup-iac.sh ('Device is online' must be checked)");
		} else if (probeExit != 0) {
			skip("iac-probe.swift exited with status " + probeExit + " - CoreMIDI API error");
		}
		System.out.println("    Swift probe says IAC is routing; running full Java loopback test.");

		TestParent parent = new TestParent();
		MidiBus bus = new MidiBus(parent, IAC_INPUT_NAME, IAC_OUTPUT_NAME, "iac_loop");
		assertEq(1, bus.attachedInputs().length,  "IAC bus: one attached input");
		assertEq(1, bus.attachedOutputs().length, "IAC bus: one attached output");

		try {
			// Second-tier precondition: confirm Java-side receive actually works before
			// asserting anything. If the Swift probe says IAC routes but Java-side
			// receive doesn't, something is broken in themidibus or CoreMIDI4J.
			// Historically this masked the Receiver.send() wrong-timestamp bug (fixed
			// by passing -1 instead of System.currentTimeMillis()); keep the skip as
			// a safety net in case a similar latent bug resurfaces, so developers
			// get a precise diagnostic rather than a wall of failed assertions.
			parent.resetWithLatch();
			bus.sendNoteOn(0, 64, 127);
			if (!parent.latch.await(1000, TimeUnit.MILLISECONDS)) {
				bus.close();
				skip("CoreMIDI confirms IAC routes but Java receiver saw nothing within 1000ms - probable themidibus send-path or CoreMIDI4J receive-path regression");
			}
			// First message made it through. Now count it and run the full assertions.
			assertEq(1, parent.noteOnBasicCount, "IAC NOTE_ON: basic callback");
			assertEq(1, parent.noteOnWithBusCount, "IAC NOTE_ON: with_bus callback");
			assertEq(1, parent.noteOnObjectCount, "IAC NOTE_ON: Note-object callback");
			assertEq(0,   parent.lastNoteOnChannel,  "IAC NOTE_ON channel");
			assertEq(64,  parent.lastNoteOnPitch,    "IAC NOTE_ON pitch");
			assertEq(127, parent.lastNoteOnVelocity, "IAC NOTE_ON velocity");
			assertEq("iac_loop", parent.lastNoteOnBusName, "IAC NOTE_ON bus_name");

			// -- NOTE_OFF round-trip --
			parent.resetWithLatch();
			bus.sendNoteOff(0, 64, 127);
			assertTrue(parent.latch.await(1000, TimeUnit.MILLISECONDS), "IAC NOTE_OFF: callback within 1000ms");
			assertEq(1, parent.noteOffBasicCount, "IAC NOTE_OFF: basic callback");
			assertEq(1, parent.noteOffWithBusCount, "IAC NOTE_OFF: with_bus callback");
			assertEq(1, parent.noteOffObjectCount, "IAC NOTE_OFF: Note-object callback");

			// -- CC round-trip --
			parent.resetWithLatch();
			bus.sendControllerChange(0, 7, 90);
			assertTrue(parent.latch.await(1000, TimeUnit.MILLISECONDS), "IAC CC: callback within 1000ms");
			assertEq(1, parent.ccBasicCount,  "IAC CC: basic callback");
			assertEq(1, parent.ccWithBusCount, "IAC CC: with_bus callback");
			assertEq(1, parent.ccObjectCount, "IAC CC: ControlChange-object callback");

			// -- NOTE_ON velocity=0 must be rewritten to NOTE_OFF by MReceiver --
			parent.resetWithLatch();
			bus.sendNoteOn(0, 64, 0); // velocity 0
			assertTrue(parent.latch.await(1000, TimeUnit.MILLISECONDS), "IAC NOTE_ON-vel0: callback within 1000ms");
			assertEq(1, parent.noteOffBasicCount, "IAC NOTE_ON-vel0 rewritten to noteOff (MReceiver conversion)");
			assertEq(0, parent.noteOnBasicCount,  "IAC NOTE_ON-vel0 does NOT surface as noteOn");
		} finally {
			bus.close();
		}
		layerPassCount++; // close without throwing
	}

	/* ========================================================= */
	/* Support classes                                            */
	/* ========================================================= */

	/**
	 * Fat parent: declares every callback overload MidiBus.registerParent probes for.
	 * Per-overload counters plus captured-arg fields. resetWithLatch() creates a fresh
	 * CountDownLatch that the last callback (midiMessage with bus_name) counts down,
	 * used by Layer 7 to synchronize on full callback fanout completion.
	 */
	public static class TestParent {
		public int noteOnBasicCount, noteOnWithBusCount, noteOnObjectCount;
		public int noteOffBasicCount, noteOffWithBusCount, noteOffObjectCount;
		public int ccBasicCount, ccWithBusCount, ccObjectCount;
		public int rawMidiCount, rawMidiWithBusCount;
		public int midiMessageCount, midiMessageWithBusCount;

		public int lastNoteOnChannel, lastNoteOnPitch, lastNoteOnVelocity;
		public long lastNoteOnTimestamp;
		public String lastNoteOnBusName;
		public Note lastNoteOnObject;

		public int lastNoteOffChannel, lastNoteOffPitch, lastNoteOffVelocity;
		public long lastNoteOffTimestamp;
		public String lastNoteOffBusName;
		public Note lastNoteOffObject;

		public int lastCcChannel, lastCcNumber, lastCcValue;
		public long lastCcTimestamp;
		public String lastCcBusName;
		public ControlChange lastCcObject;

		public byte[] lastRawMidiData;
		public MidiMessage lastMidiMessageObject;

		public CountDownLatch latch = new CountDownLatch(0);

		public void reset() {
			noteOnBasicCount = noteOnWithBusCount = noteOnObjectCount = 0;
			noteOffBasicCount = noteOffWithBusCount = noteOffObjectCount = 0;
			ccBasicCount = ccWithBusCount = ccObjectCount = 0;
			rawMidiCount = rawMidiWithBusCount = 0;
			midiMessageCount = midiMessageWithBusCount = 0;
			lastNoteOnChannel = lastNoteOnPitch = lastNoteOnVelocity = 0;
			lastNoteOnTimestamp = 0;
			lastNoteOnBusName = null;
			lastNoteOnObject = null;
			lastNoteOffChannel = lastNoteOffPitch = lastNoteOffVelocity = 0;
			lastNoteOffTimestamp = 0;
			lastNoteOffBusName = null;
			lastNoteOffObject = null;
			lastCcChannel = lastCcNumber = lastCcValue = 0;
			lastCcTimestamp = 0;
			lastCcBusName = null;
			lastCcObject = null;
			lastRawMidiData = null;
			lastMidiMessageObject = null;
		}

		public void resetWithLatch() {
			reset();
			latch = new CountDownLatch(1);
		}

		public void noteOn(int channel, int pitch, int velocity) {
			noteOnBasicCount++;
			lastNoteOnChannel = channel;
			lastNoteOnPitch = pitch;
			lastNoteOnVelocity = velocity;
		}
		public void noteOn(int channel, int pitch, int velocity, long timestamp, String bus_name) {
			noteOnWithBusCount++;
			lastNoteOnTimestamp = timestamp;
			lastNoteOnBusName = bus_name;
		}
		public void noteOn(Note note) {
			noteOnObjectCount++;
			lastNoteOnObject = note;
		}

		public void noteOff(int channel, int pitch, int velocity) {
			noteOffBasicCount++;
			lastNoteOffChannel = channel;
			lastNoteOffPitch = pitch;
			lastNoteOffVelocity = velocity;
		}
		public void noteOff(int channel, int pitch, int velocity, long timestamp, String bus_name) {
			noteOffWithBusCount++;
			lastNoteOffTimestamp = timestamp;
			lastNoteOffBusName = bus_name;
		}
		public void noteOff(Note note) {
			noteOffObjectCount++;
			lastNoteOffObject = note;
		}

		public void controllerChange(int channel, int number, int value) {
			ccBasicCount++;
			lastCcChannel = channel;
			lastCcNumber = number;
			lastCcValue = value;
		}
		public void controllerChange(int channel, int number, int value, long timestamp, String bus_name) {
			ccWithBusCount++;
			lastCcTimestamp = timestamp;
			lastCcBusName = bus_name;
		}
		public void controllerChange(ControlChange change) {
			ccObjectCount++;
			lastCcObject = change;
		}

		public void rawMidi(byte[] data) {
			rawMidiCount++;
			lastRawMidiData = data;
		}
		public void rawMidi(byte[] data, long timestamp, String bus_name) {
			rawMidiWithBusCount++;
		}

		public void midiMessage(MidiMessage message) {
			midiMessageCount++;
			lastMidiMessageObject = message;
		}
		public void midiMessage(MidiMessage message, long timestamp, String bus_name) {
			midiMessageWithBusCount++;
			latch.countDown(); // last callback in notifyParent order
		}
	}

	/** Narrow parent: only one overload declared, to verify missing-overload tolerance. */
	public static class NarrowParent {
		public int count;
		public void noteOn(int channel, int pitch, int velocity) { count++; }
	}

	/* -- Listener stubs with counters -- */

	static class CountingSimpleListener implements SimpleMidiListener {
		int noteOn, noteOff, cc;
		int lastChannel, lastD1, lastD2;
		void reset() { noteOn = noteOff = cc = 0; }
		public void noteOn(int c, int p, int v)     { noteOn++; lastChannel = c; lastD1 = p; lastD2 = v; }
		public void noteOff(int c, int p, int v)    { noteOff++; lastChannel = c; lastD1 = p; lastD2 = v; }
		public void controllerChange(int c, int n, int v) { cc++; lastChannel = c; lastD1 = n; lastD2 = v; }
	}

	static class CountingObjectListener implements ObjectMidiListener {
		int noteOn, noteOff, cc;
		void reset() { noteOn = noteOff = cc = 0; }
		public void noteOn(Note n)         { noteOn++; }
		public void noteOff(Note n)        { noteOff++; }
		public void controllerChange(ControlChange c) { cc++; }
	}

	static class CountingRawListener implements RawMidiListener {
		int count;
		byte[] lastData;
		void reset() { count = 0; lastData = null; }
		public void rawMidiMessage(byte[] data) { count++; lastData = data; }
	}

	static class CountingStandardListener implements StandardMidiListener {
		int count;
		MidiMessage lastMessage;
		void reset() { count = 0; lastMessage = null; }
		public void midiMessage(MidiMessage message, long timeStamp) { count++; lastMessage = message; }
	}
}
