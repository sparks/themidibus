#!/usr/bin/env swift
//
// General-purpose MIDI send/listen helper for themidibus backend comparison
// and Layer 9 tests. Generalises sysex-test.swift to all common channel
// message types plus SysEx.
//
// Modes:
//   send <type>    — Send a test message of <type> to IAC Driver Bus 1 via
//                    pure CoreMIDI. Exits 0 on success, non-zero on error.
//
//   listen <type>  — Listen on IAC Driver Bus 1 via pure CoreMIDI. Prints
//                    "READY\n" when armed, waits up to 3 seconds. Exits 0
//                    if a message matching <type> arrives, 2 on timeout.
//
// Types: noteon, noteoff, cc, progchange, pitchbend, chanpressure,
//        polypressure, sysex
//
// Exit codes:
//   0 — success (message sent / received)
//   1 — IAC endpoint not found
//   2 — listen timeout (no matching message within 3s)
//   3 — CoreMIDI API error or usage error

import CoreMIDI
import Foundation

// --- Module-level state for C-style MIDIReadProc callback ---
var gotMessage = false
var targetStatusMask: UInt8 = 0
var targetIsSysex = false

let readProc: MIDIReadProc = { (pktListPtr, _, _) in
    let pktList = pktListPtr.pointee
    if pktList.numPackets == 0 { return }
    withUnsafePointer(to: pktList.packet) { packetPtr in
        let packet = packetPtr.pointee
        if packet.length == 0 { return }
        var data = packet.data
        withUnsafePointer(to: &data) { ptr in
            let bytes = UnsafeRawPointer(ptr).assumingMemoryBound(to: UInt8.self)
            let status = bytes[0]
            if targetIsSysex {
                if status == 0xF0 { gotMessage = true }
            } else {
                if (status & 0xF0) == targetStatusMask { gotMessage = true }
            }
        }
    }
}

// --- Message definitions ---

struct TestMessage {
    let statusMask: UInt8
    let isSysex: Bool
    let bytes: [UInt8]
}

let messageTypes: [String: TestMessage] = [
    "noteon":       TestMessage(statusMask: 0x90, isSysex: false, bytes: [0x90, 64, 100]),
    "noteoff":      TestMessage(statusMask: 0x80, isSysex: false, bytes: [0x80, 64, 0]),
    "cc":           TestMessage(statusMask: 0xB0, isSysex: false, bytes: [0xB0, 1, 64]),
    "progchange":   TestMessage(statusMask: 0xC0, isSysex: false, bytes: [0xC0, 42]),
    "pitchbend":    TestMessage(statusMask: 0xE0, isSysex: false, bytes: [0xE0, 0, 64]),
    "chanpressure": TestMessage(statusMask: 0xD0, isSysex: false, bytes: [0xD0, 100]),
    "polypressure": TestMessage(statusMask: 0xA0, isSysex: false, bytes: [0xA0, 64, 100]),
    "sysex":        TestMessage(statusMask: 0xF0, isSysex: true,  bytes: [0xF0, 0x7D, 0x01, 0x02, 0x03, 0xF7]),
]

// --- Helpers ---

func stringProperty(_ obj: MIDIObjectRef, _ key: CFString) -> String? {
    var ref: Unmanaged<CFString>?
    guard MIDIObjectGetStringProperty(obj, key, &ref) == noErr else { return nil }
    return ref?.takeRetainedValue() as String?
}

func deviceName(of endpoint: MIDIEndpointRef) -> String? {
    var entity: MIDIEntityRef = 0
    guard MIDIEndpointGetEntity(endpoint, &entity) == noErr, entity != 0 else { return nil }
    var device: MIDIDeviceRef = 0
    guard MIDIEntityGetDevice(entity, &device) == noErr, device != 0 else { return nil }
    return stringProperty(device, kMIDIPropertyName)
}

func findIAC(source: Bool) -> MIDIEndpointRef? {
    let n = source ? MIDIGetNumberOfSources() : MIDIGetNumberOfDestinations()
    for i in 0..<n {
        let ep = source ? MIDIGetSource(i) : MIDIGetDestination(i)
        if (deviceName(of: ep) ?? "").lowercased().contains("iac") {
            return ep
        }
    }
    return nil
}

// --- Argument parsing ---

let args = CommandLine.arguments
if args.count < 3 {
    FileHandle.standardError.write(
        "usage: midi-test.swift {send|listen} <type>\ntypes: \(messageTypes.keys.sorted().joined(separator: ", "))\n"
            .data(using: .utf8)!)
    exit(3)
}

let mode = args[1]
let typeName = args[2]

guard let msg = messageTypes[typeName] else {
    FileHandle.standardError.write("unknown type: \(typeName)\n".data(using: .utf8)!)
    exit(3)
}

// --- CoreMIDI client ---

var client: MIDIClientRef = 0
guard MIDIClientCreate("themidibus-midi-test" as CFString, nil, nil, &client) == noErr else {
    print("MIDIClientCreate failed")
    exit(3)
}

switch mode {
case "send":
    guard let dst = findIAC(source: false) else {
        print("no IAC destination")
        exit(1)
    }
    var outPort: MIDIPortRef = 0
    guard MIDIOutputPortCreate(client, "out" as CFString, &outPort) == noErr else {
        print("MIDIOutputPortCreate failed")
        exit(3)
    }
    var pktList = MIDIPacketList()
    let firstPacket = MIDIPacketListInit(&pktList)
    _ = msg.bytes.withUnsafeBufferPointer { buf in
        MIDIPacketListAdd(&pktList, MemoryLayout<MIDIPacketList>.size,
                          firstPacket, 0, msg.bytes.count, buf.baseAddress!)
    }
    guard MIDISend(outPort, dst, &pktList) == noErr else {
        print("MIDISend failed")
        exit(3)
    }
    // Give CoreMIDI a moment to flush the packet before exit.
    Thread.sleep(forTimeInterval: 0.15)
    print("sent \(typeName)")
    exit(0)

case "listen":
    guard let src = findIAC(source: true) else {
        print("no IAC source")
        exit(1)
    }
    targetStatusMask = msg.statusMask
    targetIsSysex = msg.isSysex

    var inPort: MIDIPortRef = 0
    guard MIDIInputPortCreate(client, "in" as CFString, readProc, nil, &inPort) == noErr else {
        print("MIDIInputPortCreate failed")
        exit(3)
    }
    guard MIDIPortConnectSource(inPort, src, nil) == noErr else {
        print("MIDIPortConnectSource failed")
        exit(3)
    }
    // Write directly to stdout so buffering doesn't stall the parent's
    // blocking readLine().
    FileHandle.standardOutput.write("READY\n".data(using: .utf8)!)

    let deadline = Date().addingTimeInterval(3.0)
    while !gotMessage && Date() < deadline {
        RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.01))
    }
    if gotMessage {
        print("got \(typeName)")
        exit(0)
    } else {
        print("no \(typeName) within 3s")
        exit(2)
    }

default:
    FileHandle.standardError.write("unknown mode: \(mode)\n".data(using: .utf8)!)
    exit(3)
}
