#!/usr/bin/env swift
//
// IAC Driver loopback probe. Uses CoreMIDI directly — no Java, no themidibus,
// no coremidi4j. Purpose: verify that macOS's IAC Driver is actually forwarding
// MIDI messages (not just visible in the device list) as an orthogonal
// precondition for themidibus test Layer 7. Java MIDI is known-flaky; this
// probe isolates "IAC is set up correctly" from "Java can see IAC".
//
// Exit codes:
//   0 - IAC round-trip successful
//   1 - IAC source and/or destination not found in CoreMIDI
//   2 - IAC found but no loopback within 500ms (Device is online probably off)
//   3 - CoreMIDI API call failed

import CoreMIDI
import Foundation

// Mutated from the C-style MIDIReadProc callback. Must be a module-level
// variable because @convention(c) closures cannot capture Swift state.
var received = false

let readProc: MIDIReadProc = { (pktList, _, _) in
    received = true
}

func stringProperty(_ obj: MIDIObjectRef, _ key: CFString) -> String? {
    var ref: Unmanaged<CFString>?
    guard MIDIObjectGetStringProperty(obj, key, &ref) == noErr else { return nil }
    return ref?.takeRetainedValue() as String?
}

// Walks endpoint -> entity -> device and returns the device name (e.g. "IAC Driver").
// Raw CoreMIDI endpoints only expose the port name ("Bus 1"); the "IAC Driver"
// label lives on the parent device, which is what CoreMIDI4J concatenates into
// its "CoreMIDI4J - IAC Driver Bus 1" strings.
func deviceName(of endpoint: MIDIEndpointRef) -> String? {
    var entity: MIDIEntityRef = 0
    guard MIDIEndpointGetEntity(endpoint, &entity) == noErr, entity != 0 else { return nil }
    var device: MIDIDeviceRef = 0
    guard MIDIEntityGetDevice(entity, &device) == noErr, device != 0 else { return nil }
    return stringProperty(device, kMIDIPropertyName)
}

func findIAC(isSource: Bool) -> (MIDIEndpointRef, String)? {
    let n = isSource ? MIDIGetNumberOfSources() : MIDIGetNumberOfDestinations()
    for i in 0..<n {
        let ep = isSource ? MIDIGetSource(i) : MIDIGetDestination(i)
        let portName = stringProperty(ep, kMIDIPropertyName) ?? "?"
        let devName = deviceName(of: ep) ?? ""
        if devName.lowercased().contains("iac") {
            return (ep, "\(devName) \(portName)")
        }
    }
    return nil
}

guard let (src, srcName) = findIAC(isSource: true) else {
    print("IAC source not found in CoreMIDI")
    exit(1)
}
guard let (dst, dstName) = findIAC(isSource: false) else {
    print("IAC destination not found in CoreMIDI")
    exit(1)
}

var client: MIDIClientRef = 0
var status: OSStatus = MIDIClientCreate("themidibus-iac-probe" as CFString, nil, nil, &client)
guard status == noErr else {
    print("MIDIClientCreate failed: \(status)")
    exit(3)
}

var inPort: MIDIPortRef = 0
status = MIDIInputPortCreate(client, "in" as CFString, readProc, nil, &inPort)
guard status == noErr else {
    print("MIDIInputPortCreate failed: \(status)")
    exit(3)
}

status = MIDIPortConnectSource(inPort, src, nil)
guard status == noErr else {
    print("MIDIPortConnectSource failed: \(status)")
    exit(3)
}

var outPort: MIDIPortRef = 0
status = MIDIOutputPortCreate(client, "out" as CFString, &outPort)
guard status == noErr else {
    print("MIDIOutputPortCreate failed: \(status)")
    exit(3)
}

// Build a NOTE_ON packet list. MIDIPacketList has a fixed-struct layout with
// room for a small embedded first packet; 3 bytes of NOTE_ON fits easily.
var pktList = MIDIPacketList()
let firstPacket = MIDIPacketListInit(&pktList)
let noteOn: [UInt8] = [0x90, 64, 100]
let added = noteOn.withUnsafeBufferPointer { buf in
    MIDIPacketListAdd(&pktList, MemoryLayout<MIDIPacketList>.size, firstPacket, 0, noteOn.count, buf.baseAddress!)
}
guard added != nil else {
    print("MIDIPacketListAdd failed")
    exit(3)
}

status = MIDISend(outPort, dst, &pktList)
guard status == noErr else {
    print("MIDISend failed: \(status)")
    exit(3)
}

// Spin the run loop for up to 500ms so CoreMIDI can deliver the callback.
let deadline = Date().addingTimeInterval(0.5)
while !received && Date() < deadline {
    RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.01))
}

if received {
    print("IAC round-trip OK: src=\"\(srcName)\" dst=\"\(dstName)\"")
    exit(0)
} else {
    print("IAC visible but not forwarding: src=\"\(srcName)\" dst=\"\(dstName)\" (no message received within 500ms; 'Device is online' may be unchecked)")
    exit(2)
}
