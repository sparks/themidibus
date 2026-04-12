/*
 * Compares the Apple-native javax.sound.midi back-end against CoreMIDI4J
 * on the current macOS + JDK combination. Pairs each back-end with
 * scripts/sysex-test.swift (a pure-CoreMIDI helper, known working) on
 * the opposite end, one direction at a time. Reveals asymmetric bugs
 * that a simple loopback can't distinguish — e.g. Apple-native receives
 * SysEx fine but silently drops outbound SysEx. This is the same probe
 * Layer 9 of the test suite runs.
 *
 * NOTE: this script used to also do a same-JVM loopback test (send and
 * receive on one back-end) but that was removed because Apple's
 * MidiInDevice/MidiOutDevice has bidirectional state leakage - whichever
 * phase of the test runs second finds the device in a broken state. The
 * direction-isolation results alone answer every question the loopback
 * answered, plus tell you which direction is broken.
 *
 * Used to answer "do we still need the CoreMIDI4J dep?" Run this whenever
 * you upgrade macOS or the Processing-bundled JDK to verify the answer
 * hasn't changed.
 *
 * Invoked by scripts/compare-midi-backends.sh. Requires IAC Driver online
 * (run scripts/setup-iac.sh first if not) and scripts/sysex-test.swift
 * present with `swift` on PATH.
 */

import javax.sound.midi.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompareMidiBackends {

    static class Result {
        String label;
        MidiDevice.Info inInfo;
        MidiDevice.Info outInfo;
        // Direction isolation (pairs this back-end with Swift/CoreMIDI).
        String dirRecvSysex = "skip";
        String dirSendSysex = "skip";
        String note = "endpoints not found";
    }

    public static void main(String[] args) throws Exception {
        Result apple = new Result();
        apple.label = "Apple native (com.sun.media.sound)";
        Result cmj = new Result();
        cmj.label = "CoreMIDI4J (uk.co.xfactorylibrarians.*)";

        // Walk MidiSystem.getMidiDeviceInfo() once; classify each IAC endpoint
        // by the implementing class. Pick the first one of each kind so the
        // comparison is 1:1 and deterministic.
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice d = MidiSystem.getMidiDevice(info);
                String cls = d.getClass().getName();
                String name = info.getName();
                // Apple native uses short names ("Bus 1"). CoreMIDI4J uses
                // the "CoreMIDI4J - IAC Driver Bus N" form. Accept either.
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

        if (!Paths.get("scripts/sysex-test.swift").toFile().exists()) {
            System.out.println("error: scripts/sysex-test.swift not found. Cannot run direction isolation.");
            System.exit(1);
        }

        runDirectionIsolation(apple);
        runDirectionIsolation(cmj);

        System.out.println();
        System.out.println("=== themidibus back-end comparison ===");
        System.out.println();
        System.out.println("Direction-isolated SysEx test: each back-end is paired with");
        System.out.println("scripts/sysex-test.swift (pure CoreMIDI) on the opposite end,");
        System.out.println("one direction at a time. Whichever half of the loopback fails");
        System.out.println("is the half under test.");
        System.out.println();
        System.out.printf("%-42s  %-11s  %-11s%n", "Back-end", "recv sysex", "send sysex");
        System.out.printf("%-42s  %-11s  %-11s%n",
                "------------------------------------------",
                "-----------", "-----------");
        printDirectionRow(apple);
        printDirectionRow(cmj);
        System.out.println();

        boolean cmjRecv  = "received".equals(cmj.dirRecvSysex);
        boolean cmjSend  = "received".equals(cmj.dirSendSysex);
        boolean appleRecv = "received".equals(apple.dirRecvSysex);
        boolean appleSend = "received".equals(apple.dirSendSysex);

        if (!cmjRecv || !cmjSend) {
            System.out.println("WARNING: CoreMIDI4J did not fully deliver SysEx (" +
                    "recv=" + cmj.dirRecvSysex + ", send=" + cmj.dirSendSysex + "). " +
                    "Check IAC setup or CoreMIDI4J version.");
        } else if (appleRecv && appleSend) {
            System.out.println("Verdict: Apple-native now handles SysEx in BOTH directions.");
            System.out.println("         The CoreMIDI4J dependency may no longer be needed - investigate");
            System.out.println("         whether themidibus can drop it. (Also verify Gervill and other");
            System.out.println("         non-IAC devices still work via MidiSystem.getMidiDeviceInfo().)");
        } else if (appleRecv && !appleSend) {
            System.out.println("Verdict: CoreMIDI4J is still required for SENDING SysEx.");
            System.out.println("         Apple-native receives SysEx correctly but silently drops");
            System.out.println("         outbound SysEx. Sketches that only read inbound SysEx can use");
            System.out.println("         MidiBus.bypassCoreMidi4J(true); sketches that push SysEx to");
            System.out.println("         hardware (patch dumps, MMC, etc.) must keep CoreMIDI4J.");
        } else if (!appleRecv && appleSend) {
            System.out.println("Verdict: Apple-native sends SysEx but does not receive it (unusual!).");
            System.out.println("         CoreMIDI4J is still required. This is a new asymmetry - update");
            System.out.println("         the bypassCoreMidi4J warning in src/themidibus/MidiBus.java.");
        } else {
            System.out.println("Verdict: CoreMIDI4J is still required. Apple-native cannot handle");
            System.out.println("         SysEx in either direction.");
        }

        System.out.flush();
        // MIDI threads may be non-daemon; force exit so the script terminates.
        System.exit(0);
    }

    static void printDirectionRow(Result r) {
        if (r.inInfo == null || r.outInfo == null) {
            System.out.printf("%-42s  %-11s  %-11s%n", r.label, "skip", "skip");
        } else {
            System.out.printf("%-42s  %-11s  %-11s%n", r.label, r.dirRecvSysex, r.dirSendSysex);
        }
    }

    /**
     * Direction-isolated SysEx test: pair this back-end with scripts/sysex-test.swift
     * on the opposite end, one direction at a time. Populates r.dirRecvSysex and
     * r.dirSendSysex. Requires IAC to actually be routing; if not, both end up "skip".
     */
    static void runDirectionIsolation(Result r) {
        if (r.inInfo == null || r.outInfo == null) return;

        // -- Receive test: Swift sends SysEx, this back-end listens. --
        MidiDevice in = null;
        Transmitter tx = null;
        try {
            in = MidiSystem.getMidiDevice(r.inInfo);
            if (!in.isOpen()) in.open();
            AtomicBoolean gotSysex = new AtomicBoolean(false);
            tx = in.getTransmitter();
            tx.setReceiver(new Receiver() {
                @Override public void send(MidiMessage m, long ts) {
                    if (m instanceof SysexMessage) gotSysex.set(true);
                }
                @Override public void close() {}
            });
            Thread.sleep(100); // settle

            Process send = new ProcessBuilder("swift", "scripts/sysex-test.swift", "send")
                    .redirectErrorStream(true).start();
            boolean exited = send.waitFor(5, TimeUnit.SECONDS);
            if (!exited || send.exitValue() != 0) {
                r.dirRecvSysex = "probe-err";
            } else {
                Thread.sleep(500); // allow CoreMIDI delivery
                r.dirRecvSysex = gotSysex.get() ? "received" : "DROPPED";
            }
        } catch (Exception e) {
            r.dirRecvSysex = "err";
        } finally {
            try { if (tx != null) tx.close(); } catch (Exception ignore) {}
            try { if (in != null && in.isOpen()) in.close(); } catch (Exception ignore) {}
        }

        // -- Send test: Swift listens, this back-end sends SysEx. --
        Process listen = null;
        MidiDevice out = null;
        Receiver outRx = null;
        try {
            listen = new ProcessBuilder("swift", "scripts/sysex-test.swift", "listen")
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
                listen.destroyForcibly();
                r.dirSendSysex = "probe-err";
                return;
            }
            Thread.sleep(50);

            out = MidiSystem.getMidiDevice(r.outInfo);
            if (!out.isOpen()) out.open();
            outRx = out.getReceiver();
            SysexMessage sx = new SysexMessage();
            sx.setMessage(new byte[] { (byte)0xF0, 0x7D, 0x01, 0x02, 0x03, (byte)0xF7 }, 6);
            outRx.send(sx, -1);

            boolean exited = listen.waitFor(5, TimeUnit.SECONDS);
            // Drain any remaining stdout so the pipe can't stall.
            while (br.ready()) { if (br.readLine() == null) break; }
            if (!exited) {
                listen.destroyForcibly();
                r.dirSendSysex = "probe-err";
            } else {
                r.dirSendSysex = listen.exitValue() == 0 ? "received" : "DROPPED";
            }
        } catch (Exception e) {
            r.dirSendSysex = "err";
        } finally {
            try { if (outRx != null) outRx.close(); } catch (Exception ignore) {}
            try { if (out != null && out.isOpen()) out.close(); } catch (Exception ignore) {}
            if (listen != null && listen.isAlive()) listen.destroyForcibly();
        }
    }

}
