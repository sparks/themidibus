import themidibus.*; //Import the library

MidiBus busA; // The first MidiBus
MidiBus busB; // The second MidiBus

void setup() {
	size(400,400);
	background(0);
	
	MidiBus.list(); // List all available Midi devices. This will show each device's index and name.
	
	String[][] list_of_devices = MidiBus.returnList(); //This is a different way of listing the available Midi devices.

	println(); 
	println("Available Midi Devices (from returnList()):"); 
	println("-----------------------"); 

	for(int i = 0;i < list_of_devices.length;i++) { 
	   println("[" + i + "] \"" + list_of_devices[i][0] + "\" [" + list_of_devices[i][1] + "]"); //It gives you the index (i), name (list_of_devices[i][0]) and type (list_of_devices[i][1]) of each device in a 2D array for easy manipulation.
	}
	
	busA = new MidiBus(this, "IncomingA", "OutgoingA", "busA"); // Create a first new MidiBus attached to the IncommingA Midi input device and the OutgoingA Midi output device. We will name it busA.
	busB = new MidiBus(this, "IncomingB", "OutgoingB", "busB"); // Create a second new MidiBus attached to the IncommingB Midi input device and the OutgoingB Midi output device. We will name it busB.

	busA.addOutput("OutgoingC"); // Add a new output device to busA called OutgoingC
	busB.addInput("IncomingC"); // Add a new input device to busB called IncommingC
}

void draw() {
	int channel = 0;
	int pitch = 64;
	int velocity = 127;
	
	busA.sendNoteOn(channel, pitch, velocity); // Send a noteOn to OutgoingA and OutgoingC through busA
	delay(200);
	busB.sendNoteOn(channel, pitch, velocity); // Send a noteOn to OutgoingB through busB
	delay(100);
	busA.sendNoteOff(channel, pitch, velocity); // Send a noteOff to OutgoingA and OutgoingC through busA
	busB.sendNoteOff(channel, pitch, velocity); // Send a noteOff to OutgoingB through busB
	
	int number = 0;
	int value = 90;
	
	busA.sendControllerChange(channel, number, value); // Send a controllerChange to OutgoingA and OutgoingC through busA
	busB.sendControllerChange(channel, number+10, value+10); // Send a controllerChange to OutgoingB through busB
	delay(2000);
}

void noteOn(int channel, int pitch, int velocity, String bus_name) {
	println();
	println("Note On:");
	println("--------");
	println("Channel:"+channel);
	println("Pitch:"+pitch);
	println("Velocity:"+velocity);
	println("Recieved on Bus:"+bus_name);
	if(bus_name == "busA") {
		println("This came from IncomingA");
	} else if(bus_name == "busB") {
		println("This came from IncomingB or IncomingC");
	}
}

void noteOff(int channel, int pitch, int velocity, String bus_name) {
	println();
	println("Note Off:");
	println("--------");
	println("Channel:"+channel);
	println("Pitch:"+pitch);
	println("Velocity:"+velocity);
	println("Recieved on Bus:"+bus_name);
	if(bus_name == "busA") {
		println("This came from IncomingA");
	} else if(bus_name == "busB") {
		println("This came from IncomingB or IncomingC");
	}
}

void controllerChange(int channel, int number, int value, String bus_name) {
	println();
	println("Controller Change:");
	println("--------");
	println("Channel:"+channel);
	println("Number:"+number);
	println("Value:"+value);
	println("Recieved on Bus:"+bus_name);
	if(bus_name == "busA") {
		println("This came from IncomingA");
	} else if(bus_name == "busB") {
		println("This came from IncomingB or IncomingC");
	}
}