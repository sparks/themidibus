import themidibus.*; //Import the library

MidiBus myBus; // The MidiBus

void setup() {
  size(400, 400);
  background(0);

  MidiBus.list(); // List all available Midi devices.

  myBus = new MidiBus(this, -1, -1); // Create a new MidiBus with no input or output devices.
  myBus.throwErrors(true); // Enable strict error mode — failed operations will throw instead of printing warnings.

  // Try to add an input device that doesn't exist.
  try {
    myBus.addInput(99);
  } catch (RuntimeException e) {
    println("Caught error adding input: " + e.getMessage());
  }

  // Try to add an output device by a name that doesn't exist.
  try {
    myBus.addOutput("My Nonexistent Synth");
  } catch (RuntimeException e) {
    println("Caught error adding output: " + e.getMessage());
  }

  // Now add a real output device. If this fails too, we'll know about it.
  try {
    myBus.addOutput("Gervill");
    println("Successfully added output: Gervill");
  } catch (RuntimeException e) {
    println("Caught error adding output: " + e.getMessage());
  }
}

void draw() {
  // Nothing to do here — this example just demonstrates error handling.
}
