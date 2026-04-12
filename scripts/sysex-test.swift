#!/usr/bin/env swift
//
// SysEx bi-directional helper for themidibus Layer 9.
//
// Modes:
//   send    - Send a single SysEx message to IAC Driver Bus 1 via pure
//             CoreMIDI. Exits 0 on success, non-zero on any CoreMIDI error
//             or if no IAC destination is found.
//
//   listen  - Listen on IAC Driver Bus 1 via pure CoreMIDI. Prints
//             "READY\n" on stdout (bypassing Swift's print() buffering so
//             the parent sees it immediately) when armed, then waits up
//             to 3 seconds for a SysEx message. Exits 0 if one arrives,
//             2 if it times out, non-zero on CoreMIDI errors.
//
// Paired with the Layer 9 Java test to isolate Apple-native javax.sound.midi
// SysEx send vs receive direction independently (Swift is the known-working
// counterpart; whichever direction fails is the half that is broken).

import CoreMIDI
import Foundation

// Module-level because a @convention(c) MIDIReadProc cannot capture state.
var gotSysex = false

let readProc: MIDIReadProc = { (pktListPtr, _, _) in
    let pktList = pktListPtr.pointee
    if pktList.numPackets == 0 { return }
    withUnsafePointer(to: pktList.packet) { packetPtr in
        let packet = packetPtr.pointee
        if packet.length == 0 { return }
        var data = packet.data
        withUnsafePointer(to: &data) { ptr in
            let bytes = UnsafeRawPointer(ptr).assumingMemoryBound(to: UInt8.self)
            if bytes[0] == 0xF0 { gotSysex = true }
        }
    }
}

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

let args = CommandLine.arguments
if args.count < 2 {
    FileHandle.standardError.write("usage: sysex-test.swift {send|listen}\n".data(using: .utf8)!)
    exit(3)
}

var client: MIDIClientRef = 0
guard MIDIClientCreate("themidibus-sysex-test" as CFString, nil, nil, &client) == noErr else {
    print("MIDIClientCreate failed")
    exit(3)
}

switch args[1] {
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
    let sysex: [UInt8] = [0xF0, 0x7D, 0x01, 0x02, 0x03, 0xF7]
    var pktList = MIDIPacketList()
    let firstPacket = MIDIPacketListInit(&pktList)
    _ = sysex.withUnsafeBufferPointer { buf in
        MIDIPacketListAdd(&pktList, MemoryLayout<MIDIPacketList>.size, firstPacket, 0, sysex.count, buf.baseAddress!)
    }
    guard MIDISend(outPort, dst, &pktList) == noErr else {
        print("MIDISend failed")
        exit(3)
    }
    // Give CoreMIDI a moment to actually flush the packet before exit.
    Thread.sleep(forTimeInterval: 0.15)
    print("sent")
    exit(0)

case "listen":
    guard let src = findIAC(source: true) else {
        print("no IAC source")
        exit(1)
    }
    var inPort: MIDIPortRef = 0
    guard MIDIInputPortCreate(client, "in" as CFString, readProc, nil, &inPort) == noErr else {
        print("MIDIInputPortCreate failed")
        exit(3)
    }
    guard MIDIPortConnectSource(inPort, src, nil) == noErr else {
        print("MIDIPortConnectSource failed")
        exit(3)
    }
    // Write directly to stdout (not print()) so buffering doesn't stall the
    // parent's blocking readLine().
    FileHandle.standardOutput.write("READY\n".data(using: .utf8)!)

    let deadline = Date().addingTimeInterval(3.0)
    while !gotSysex && Date() < deadline {
        RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.01))
    }
    if gotSysex {
        print("got sysex")
        exit(0)
    } else {
        print("no sysex within 3s")
        exit(2)
    }

default:
    FileHandle.standardError.write("unknown mode: \(args[1])\n".data(using: .utf8)!)
    exit(3)
}
