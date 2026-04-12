/*
 * Compares the Apple-native javax.sound.midi back-end against CoreMIDI4J
 * on the current macOS + JDK combination by opening an IAC Driver loopback
 * through each and testing whether short messages and SysEx round-trip.
 *
 * Used to answer "do we still need the CoreMIDI4J dep?" — Apple-native
 * historically drops SysEx silently while CoreMIDI4J delivers it. Run
 * this whenever you upgrade macOS or the Processing-bundled JDK to verify
 * the answer hasn't changed.
 *
 * Invoked by scripts/compare-midi-backends.sh. Requires IAC Driver online
 * (run scripts/setup-iac.sh first if not).
 */

import javax.sound.midi.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CompareMidiBackends {

    static class Result {
        String label;
        MidiDevice.Info inInfo;
        MidiDevice.Info outInfo;
        String shortResult = "skip";
        String sysexResult = "skip";
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

        runStack(apple);
        runStack(cmj);

        System.out.println();
        System.out.println("=== themidibus back-end comparison ===");
        System.out.println();
        System.out.println("Testing IAC Driver loopback through javax.sound.midi.");
        System.out.println();
        System.out.printf("%-42s  %-9s  %-9s%n", "Back-end", "short", "sysex");
        System.out.printf("%-42s  %-9s  %-9s%n",
                "------------------------------------------",
                "---------", "---------");
        printRow(apple);
        printRow(cmj);
        System.out.println();

        // Verdict line — fail loudly if the answer has changed.
        boolean appleShort = "received".equals(apple.shortResult);
        boolean appleSysex = "received".equals(apple.sysexResult);
        boolean cmjShort   = "received".equals(cmj.shortResult);
        boolean cmjSysex   = "received".equals(cmj.sysexResult);

        if (!cmjShort || !cmjSysex) {
            System.out.println("WARNING: CoreMIDI4J did not fully deliver. Check IAC setup or CoreMIDI4J version.");
        } else if (!appleSysex) {
            System.out.println("Verdict: CoreMIDI4J is still required. Apple native drops SysEx silently.");
        } else if (appleShort && appleSysex) {
            System.out.println("Verdict: Apple native now handles both short and SysEx messages.");
            System.out.println("         The CoreMIDI4J dependency may no longer be needed - consider dropping it.");
        }

        System.out.flush();
        // MIDI threads may be non-daemon; force exit so the script terminates.
        System.exit(0);
    }

    static void printRow(Result r) {
        if (r.inInfo == null || r.outInfo == null) {
            System.out.printf("%-42s  %-9s  %-9s%n", r.label, "skip", "skip");
            System.out.println("  (" + r.note + ")");
        } else {
            System.out.printf("%-42s  %-9s  %-9s%n", r.label, r.shortResult, r.sysexResult);
        }
    }

    static void runStack(Result r) {
        if (r.inInfo == null || r.outInfo == null) return;

        MidiDevice in = null, out = null;
        try {
            in = MidiSystem.getMidiDevice(r.inInfo);
            out = MidiSystem.getMidiDevice(r.outInfo);
            if (!in.isOpen()) in.open();
            if (!out.isOpen()) out.open();

            AtomicInteger shortCount = new AtomicInteger();
            AtomicInteger sysexCount = new AtomicInteger();
            Object lock = new Object();

            Transmitter tx = in.getTransmitter();
            tx.setReceiver(new Receiver() {
                @Override public void send(MidiMessage m, long ts) {
                    if (m instanceof SysexMessage) sysexCount.incrementAndGet();
                    else shortCount.incrementAndGet();
                    synchronized (lock) { lock.notifyAll(); }
                }
                @Override public void close() {}
            });
            Receiver outRx = out.getReceiver();

            Thread.sleep(100); // let the stack settle

            // Short message (NOTE_ON).
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(ShortMessage.NOTE_ON, 0, 64, 100);
            outRx.send(noteOn, -1);
            awaitLatched(lock, shortCount, 1000);
            r.shortResult = shortCount.get() > 0 ? "received" : "DROPPED";

            // SysEx message.
            SysexMessage sysex = new SysexMessage();
            byte[] payload = { (byte)0xF0, 0x01, 0x02, 0x03, 0x04, (byte)0xF7 };
            sysex.setMessage(payload, payload.length);
            outRx.send(sysex, -1);
            awaitLatched(lock, sysexCount, 1000);
            r.sysexResult = sysexCount.get() > 0 ? "received" : "DROPPED";

            tx.close();
            outRx.close();
        } catch (Exception e) {
            r.note = "exception: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            r.shortResult = "err";
            r.sysexResult = "err";
        } finally {
            try { if (in != null && in.isOpen()) in.close(); } catch (Exception ignore) {}
            try { if (out != null && out.isOpen()) out.close(); } catch (Exception ignore) {}
        }
    }

    static void awaitLatched(Object lock, AtomicInteger counter, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (lock) {
            while (counter.get() == 0 && System.currentTimeMillis() < deadline) {
                lock.wait(Math.max(1, deadline - System.currentTimeMillis()));
            }
        }
    }
}
