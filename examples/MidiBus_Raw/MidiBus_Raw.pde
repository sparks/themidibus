import themidibus.*; //Import the library
import javax.sound.midi.MidiMessage; //Import the MidiMessage class http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/MidiMessage.html

MidiBus myBus; // The MidiBus

void setup() {
	size(400,400);
	background(0);
	
	MidiBus.list(); // List all available Midi devices on STDOUT. This will show each device's index and name.
	myBus = new MidiBus(this, "IncomingDeviceName", "OutgoingDeviceName"); // Create a new MidiBus object
}

void draw() {
	int channel = 0;
	int pitch = 64;
	int velocity = 127;
	
	myBus.sendNoteOn(channel, pitch, velocity); // Send a Midi noteOn
	delay(200);
	myBus.sendNoteOff(channel, pitch, velocity); // Send a Midi nodeOff
	delay(100);

	//Or for something different we could send a custom Midi message ...
	
	int status_byte = 0xC0; // For instance let us send aftertouch
	int channel_byte = 0; // On channel 0 again
	int first_byte = 64; // The same note;
	int second_byte = 80; // But with less velocity
	
	myBus.sendMessage(status_byte, channel_byte, first_byte, second_byte);
	delay(2000);
}

// Notice all bytes below are converted to integeres using the following system:
// int i = (int)(byte & 0xFF) 
// This properly convertes an unsigned byte (MIDI uses unsigned bytes) to a signed int
// Because java only supports signed bytes, you will get incorrect values if you don't do so

void rawMidi(byte[] data) { // You can also use rawMidi(byte[] data, String bus_name)
	// Receive some raw data
	// data[0] will be the status byte
	// data[1] and data[2] will contain the parameter of the message (e.g. pitch and volume for noteOn noteOff)
	println();
	println("Raw Midi Data:");
	println("--------");
	println("Status Byte/MIDI Command:"+(int)(data[0] & 0xFF));
	// N.B. In some cases (noteOn, noteOff, controllerChange, etc) the first half of the status byte is the command and the second half if the channel
	// In these cases (data[0] & 0xF0) gives you the command and (data[0] & 0x0F) gives you the channel
	if(data.length > 1) println("Param 1: "+(int)(data[1] & 0xFF));
	if(data.length > 2) println("Param 2: "+(int)(data[2] & 0xFF));
}

void midiMessage(MidiMessage message) { // You can also use midiMessage(MidiMessage message, String bus_name)
	// Receive a MidiMessage
	// MidiMessage is an abstract class, the actual passed object will be either javax.sound.midi.MetaMessage, javax.sound.midi.ShortMessage, javax.sound.midi.SysexMessage.
	// Check it out here http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html
	println();
	println("MidiMessage Data:");
	println("--------");
	println("Status Byte/MIDI Command:"+message.getStatus());
	if(message.getMessage().length > 1) println("Param 1: "+(int)(message.getMessage()[1] & 0xFF));
	if(message.getMessage().length > 2) println("Param 2: "+(int)(message.getMessage()[2] & 0xFF));
}