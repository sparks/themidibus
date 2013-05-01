import themidibus.*; //Import the library
import javax.sound.midi.MidiMessage; //Import the MidiMessage classes http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/MidiMessage.html
import javax.sound.midi.SysexMessage;
import javax.sound.midi.ShortMessage;

MidiBus myBus; // The MidiBus

void setup() {
  MidiBus.list(); // List all available Midi devices on STDOUT. This will show each device's index and name.
  myBus = new MidiBus(this, 0, 0); // Create a new MidiBus object
}

void draw(){}

void noteOn(Note nt){
  println ("Note pressed: " + nt.toString());
  println ("Octave: " + nt.getOctave());
  println ("Strength: " + nt.getStrength());
  
  if (nt.equals("C#")){
    println("It's a C#!!");
  }
  
  if (nt.equals("D#5")){
    println("It's a D#!!");
  }
}

  void noteOff(Note nt){
    println ("Note release: " + nt.toString());
    println ("Octave: " + nt.getOctave());
    println ("Strength: " + nt.getStrength());
  }

void delay(int time) {
  int current = millis();
  while(millis() < current+time) Thread.yield();
}