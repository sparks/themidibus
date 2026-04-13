/*
 * Compares the Apple-native javax.sound.midi back-end against CoreMIDI4J
 * on the current macOS + JDK combination for all common MIDI message types.
 * Pairs each back-end with scripts/midi-test.swift (a pure-CoreMIDI helper,
 * known working) on the opposite end, one direction at a time. Reveals
 * asymmetric bugs that a simple loopback can't distinguish — e.g.
 * Apple-native receives SysEx fine but silently drops outbound SysEx.
 *
 * Used to answer "do we still need the CoreMIDI4J dep?" Run this whenever
 * you upgrade macOS or the Processing-bundled JDK to verify the answer
 * hasn't changed.
 *
 * Invoked by scripts/compare-midi-backends.sh. Requires IAC Driver online
 * (run scripts/setup-iac.sh first if not) and scripts/midi-test.swift
 * present with `swift` on PATH.
 */

import javax.sound.midi.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompareMidiBackends {

    static final String SWIFT_SCRIPT = "scripts/midi-test.swift";

    // Parallel arrays — order matters for display.
    static final String[] MSG_TYPES  = {
        "noteon", "noteoff", "cc", "progchange",
        "pitchbend", "chanpressure", "polypressure", "sysex"
    };
    static final String[] MSG_LABELS = {
        "Note On", "Note Off", "CC", "Prog Change",
        "Pitch Bend", "Chan Pressure", "Poly Pressure", "SysEx"
    };

    static class Result {
        String label;
        MidiDevice.Info inInfo;
        MidiDevice.Info outInfo;
        String[] recv = new String[MSG_TYPES.length]; // per-type status
        String[] send = new String[MSG_TYPES.length];

        Result() {
            for (int i = 0; i < MSG_TYPES.length; i++) {
                recv[i] = "skip";
                send[i] = "skip";
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Result apple = new Result();
        apple.label = "Apple native (com.sun.media.sound)";
        Result cmj = new Result();
        cmj.label = "CoreMIDI4J (uk.co.xfactorylibrarians.*)";

        // Walk MidiSystem.getMidiDeviceInfo() once; classify each IAC endpoint
        // by the implementing class.
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice d = MidiSystem.getMidiDevice(info);
                String cls = d.getClass().getName();
                String name = info.getName();
                boolean isIAC = name.contains("IAC") || name.startsWith("Bus ");
                if (!isIAC) continue;

                if (cls.startsWith("com.sun.media.sound.Midi")) {
                    if (d.getMaxTransmitters() != 0 && apple.inInfo == null) apple.inInfo = info;
                    if (d.getMaxReceivers() != 0 && apple.outInfo == null) apple.outInfo = info;
                } else if (cls.startsWith("uk.co.xfactorylibrarians.coremidi4j")) {
                    if (d.getMaxTransmitters() != 0 && cmj.inInfo == null) cmj.inInfo = info;
                    if (d.getMaxReceivers() != 0 && cmj.outInfo == null) cmj.outInfo = info;
                }
            } catch (Exception ignore) {}
        }

        if (!Paths.get(SWIFT_SCRIPT).toFile().exists()) {
            System.out.println("error: " + SWIFT_SCRIPT + " not found.");
            System.exit(1);
        }

        System.out.println("Testing Apple-native...");
        runDirectionIsolation(apple);
        System.out.println("Testing CoreMIDI4J...");
        runDirectionIsolation(cmj);

        // --- Print results ---
        System.out.println();
        System.out.println("=== themidibus back-end comparison ===");
        System.out.println();
        System.out.println("Direction-isolated MIDI message tests: each back-end is paired with");
        System.out.println(SWIFT_SCRIPT + " (pure CoreMIDI) on the opposite end,");
        System.out.println("one direction at a time.");

        printBackendTable(apple);
        printBackendTable(cmj);

        // --- Verdict ---
        System.out.println();

        boolean cmjAllOk = true;
        for (int i = 0; i < MSG_TYPES.length; i++) {
            if (!"received".equals(cmj.recv[i]) || !"received".equals(cmj.send[i])) {
                cmjAllOk = false;
                break;
            }
        }

        if (!cmjAllOk) {
            System.out.println("WARNING: CoreMIDI4J did not fully deliver all message types.");
            System.out.println("         Check IAC setup or CoreMIDI4J version.");
            System.out.flush();
            System.exit(0);
        }

        // Count Apple-native failures by category.
        boolean appleShortRecvOk = true, appleShortSendOk = true;
        boolean appleSysexRecvOk = "received".equals(apple.recv[MSG_TYPES.length - 1]);
        boolean appleSysexSendOk = "received".equals(apple.send[MSG_TYPES.length - 1]);

        for (int i = 0; i < MSG_TYPES.length - 1; i++) { // skip sysex
            if (!"received".equals(apple.recv[i])) appleShortRecvOk = false;
            if (!"received".equals(apple.send[i])) appleShortSendOk = false;
        }

        if (apple.inInfo == null || apple.outInfo == null) {
            System.out.println("Apple-native IAC endpoints not found — cannot assess.");
        } else if (appleShortRecvOk && appleShortSendOk && appleSysexRecvOk && appleSysexSendOk) {
            System.out.println("Verdict: Apple-native now handles ALL message types in BOTH directions.");
            System.out.println("         The CoreMIDI4J dependency may no longer be needed — investigate");
            System.out.println("         whether themidibus can drop it.");
        } else if (appleShortRecvOk && appleShortSendOk && appleSysexRecvOk && !appleSysexSendOk) {
            System.out.println("Verdict: CoreMIDI4J is still required for SENDING SysEx.");
            System.out.println("         Apple-native handles all channel messages correctly and receives");
            System.out.println("         SysEx, but silently drops outbound SysEx.");
        } else if (!appleShortRecvOk || !appleShortSendOk) {
            System.out.println("Verdict: Apple-native has channel-message failures — this is a NEW finding.");
            System.out.println("         CoreMIDI4J is required. Investigate which message types broke.");
        } else {
            System.out.println("Verdict: CoreMIDI4J is still required. Apple-native SysEx results:");
            System.out.println("         recv=" + (appleSysexRecvOk ? "works" : "BROKEN") +
                             ", send=" + (appleSysexSendOk ? "works" : "BROKEN"));
        }

        System.out.flush();
        System.exit(0);
    }

    static void printBackendTable(Result r) {
        System.out.println();
        System.out.println(r.label + ":");
        if (r.inInfo == null || r.outInfo == null) {
            System.out.println("  (IAC endpoints not found — skipped)");
            return;
        }
        System.out.printf("  %-16s  %-11s  %-11s%n", "Message type", "recv", "send");
        System.out.printf("  %-16s  %-11s  %-11s%n", "------------", "----", "----");
        for (int i = 0; i < MSG_TYPES.length; i++) {
            System.out.printf("  %-16s  %-11s  %-11s%n", MSG_LABELS[i], r.recv[i], r.send[i]);
        }
    }

    // ----------------------------------------------------------------
    // Direction isolation: pair this back-end with Swift/CoreMIDI on
    // the opposite end, one direction at a time.
    // ----------------------------------------------------------------

    static void runDirectionIsolation(Result r) {
        if (r.inInfo == null || r.outInfo == null) return;
        testReceiveAll(r);
        testSendAll(r);
    }

    /**
     * Receive test: open this back-end's IAC input, have Swift send each
     * message type, check which ones arrive.
     */
    static void testReceiveAll(Result r) {
        MidiDevice in_ = null;
        Transmitter tx = null;
        try {
            in_ = MidiSystem.getMidiDevice(r.inInfo);
            if (!in_.isOpen()) in_.open();

            AtomicBoolean[] seen = new AtomicBoolean[MSG_TYPES.length];
            for (int i = 0; i < seen.length; i++) seen[i] = new AtomicBoolean(false);

            tx = in_.getTransmitter();
            tx.setReceiver(new Receiver() {
                @Override public void send(MidiMessage m, long ts) {
                    String type = classifyMessage(m);
                    for (int i = 0; i < MSG_TYPES.length; i++) {
                        if (MSG_TYPES[i].equals(type)) {
                            seen[i].set(true);
                            break;
                        }
                    }
                }
                @Override public void close() {}
            });
            Thread.sleep(100);

            for (int i = 0; i < MSG_TYPES.length; i++) {
                Process p = new ProcessBuilder("swift", SWIFT_SCRIPT, "send", MSG_TYPES[i])
                        .redirectErrorStream(true).start();
                boolean exited = p.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    p.destroyForcibly();
                    r.recv[i] = "probe-err";
                    continue;
                }
                if (p.exitValue() != 0) {
                    r.recv[i] = "probe-err";
                    continue;
                }
                Thread.sleep(300);
                r.recv[i] = seen[i].get() ? "received" : "DROPPED";
            }
        } catch (Exception e) {
            for (int i = 0; i < MSG_TYPES.length; i++) {
                if ("skip".equals(r.recv[i])) r.recv[i] = "err";
            }
        } finally {
            try { if (tx != null) tx.close(); } catch (Exception ignore) {}
            try { if (in_ != null && in_.isOpen()) in_.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * Send test: for each message type, launch Swift in listen mode,
     * send the message through this back-end's IAC output, check if
     * Swift received it.
     */
    static void testSendAll(Result r) {
        MidiDevice out = null;
        Receiver outRx = null;
        try {
            out = MidiSystem.getMidiDevice(r.outInfo);
            if (!out.isOpen()) out.open();
            outRx = out.getReceiver();

            for (int i = 0; i < MSG_TYPES.length; i++) {
                Process listen = null;
                try {
                    listen = new ProcessBuilder("swift", SWIFT_SCRIPT, "listen", MSG_TYPES[i])
                            .redirectErrorStream(true).start();
                    BufferedReader br = new BufferedReader(new InputStreamReader(listen.getInputStream()));

                    boolean ready = false;
                    long deadline = System.currentTimeMillis() + 5000;
                    while (System.currentTimeMillis() < deadline) {
                        if (!br.ready()) {
                            if (!listen.isAlive()) break;
                            Thread.sleep(20);
                            continue;
                        }
                        String line = br.readLine();
                        if (line == null) break;
                        if ("READY".equals(line)) { ready = true; break; }
                    }
                    if (!ready) {
                        r.send[i] = "probe-err";
                        continue;
                    }
                    Thread.sleep(50);

                    MidiMessage msg = createJavaMessage(MSG_TYPES[i]);
                    outRx.send(msg, -1);

                    boolean exited = listen.waitFor(5, TimeUnit.SECONDS);
                    while (br.ready()) { if (br.readLine() == null) break; }
                    if (!exited) {
                        r.send[i] = "probe-err";
                    } else {
                        r.send[i] = listen.exitValue() == 0 ? "received" : "DROPPED";
                    }
                } finally {
                    if (listen != null && listen.isAlive()) listen.destroyForcibly();
                }
            }
        } catch (Exception e) {
            for (int i = 0; i < MSG_TYPES.length; i++) {
                if ("skip".equals(r.send[i])) r.send[i] = "err";
            }
        } finally {
            try { if (outRx != null) outRx.close(); } catch (Exception ignore) {}
            try { if (out != null && out.isOpen()) out.close(); } catch (Exception ignore) {}
        }
    }

    // ----------------------------------------------------------------
    // Message helpers
    // ----------------------------------------------------------------

    static String classifyMessage(MidiMessage m) {
        if (m instanceof SysexMessage) return "sysex";
        if (m instanceof ShortMessage) {
            switch (((ShortMessage) m).getCommand()) {
                case ShortMessage.NOTE_ON:          return "noteon";
                case ShortMessage.NOTE_OFF:         return "noteoff";
                case ShortMessage.CONTROL_CHANGE:   return "cc";
                case ShortMessage.PROGRAM_CHANGE:   return "progchange";
                case ShortMessage.PITCH_BEND:       return "pitchbend";
                case ShortMessage.CHANNEL_PRESSURE: return "chanpressure";
                case 0xA0:                          return "polypressure";
            }
        }
        return "unknown";
    }

    static MidiMessage createJavaMessage(String type) throws Exception {
        switch (type) {
            case "noteon":       return new ShortMessage(ShortMessage.NOTE_ON, 0, 64, 100);
            case "noteoff":      return new ShortMessage(ShortMessage.NOTE_OFF, 0, 64, 0);
            case "cc":           return new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 1, 64);
            case "progchange":   return new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 42, 0);
            case "pitchbend":    return new ShortMessage(ShortMessage.PITCH_BEND, 0, 0, 64);
            case "chanpressure": return new ShortMessage(ShortMessage.CHANNEL_PRESSURE, 0, 100, 0);
            case "polypressure": return new ShortMessage(0xA0, 0, 64, 100);
            case "sysex": {
                SysexMessage sx = new SysexMessage();
                sx.setMessage(new byte[] { (byte)0xF0, 0x7D, 0x01, 0x02, 0x03, (byte)0xF7 }, 6);
                return sx;
            }
            default: throw new IllegalArgumentException("unknown type: " + type);
        }
    }
}
