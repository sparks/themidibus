import themidibus.*; //Import the library
import javax.sound.midi.MidiMessage; //Import the MidiMessage classes http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/MidiMessage.html
import javax.sound.midi.SysexMessage;
import javax.sound.midi.ShortMessage;

MidiBus myBus; // The MidiBus

void setup() {
  size(400, 400);
  background(0);

  MidiBus.list(); // List all available Midi devices on STDOUT. This will show each device's index and name.
  myBus = new MidiBus(this, 0, 0); // Create a new MidiBus object

  // On mac you will need to use MMJ since Apple's MIDI subsystem doesn't properly support SysEx. 
  // However MMJ doesn't support sending timestamps so you have to turn off timestamps.
  // myBus.sendTimestamps(false);
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

  int status_byte = 0xA0; // For instance let us send aftertouch
  int channel_byte = 0; // On channel 0 again
  int first_byte = 64; // The same note;
  int second_byte = 80; // But with less velocity

  myBus.sendMessage(status_byte, channel_byte, first_byte, second_byte);

  //Or we could even send a variable length sysex message
  //IMPORTANT: On mac you will have to use the MMJ MIDI subsystem to be able to send SysexMessages. Consult README.md for more information

  myBus.sendMessage(
    new byte[] {
      (byte)0xF0, (byte)0x1, (byte)0x2, (byte)0x3, (byte)0x4, (byte)0xF7
    }
  );
  //We could also do the same thing this way ...

  try { //All the methods of SysexMessage, ShortMessage, etc, require try catch blocks
    SysexMessage message = new SysexMessage();
    message.setMessage(
      0xF0, 
      new byte[] {
        (byte)0x5, (byte)0x6, (byte)0x7, (byte)0x8, (byte)0xF7
      },
      5
    );
    myBus.sendMessage(message);
  } catch(Exception e) {

  }

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
  for (int i = 1;i < data.length;i++) {
    println("Param "+(i+1)+": "+(int)(data[i] & 0xFF));
  }
}

void midiMessage(MidiMessage message) { // You can also use midiMessage(MidiMessage message, long timestamp, String bus_name)
  // Receive a MidiMessage
  // MidiMessage is an abstract class, the actual passed object will be either javax.sound.midi.MetaMessage, javax.sound.midi.ShortMessage, javax.sound.midi.SysexMessage.
  // Check it out here http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html
  println();
  println("MidiMessage Data:");
  println("--------");
  println("Status Byte/MIDI Command:"+message.getStatus());
  for (int i = 1;i < message.getMessage().length;i++) {
    println("Param "+(i+1)+": "+(int)(message.getMessage()[i] & 0xFF));
  }
}

void delay(int time) {
  int current = millis();
  while (millis () < current+time) Thread.yield();
}
