/**
 * Copyright (c) 2009 Severin Smith
 *
 * This file is part of a library called The MidiBus (themidibus) - http://www.smallbutdigital.com/themidibus.php.
 *
 * The MidiBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The MidiBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the MidiBus. If not, see <http://www.gnu.org/licenses/>.
*/

package themidibus;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Formatter;

import processing.core.PApplet;
import java.lang.reflect.Method;

/**
 * The MidiBus class provides a simple way to send and receive MIDI within Processing sketches.
 * <p>
 * <h4>Typical Implementation, Simple</h4>
 * <p>
 * A typical simple Processing MIDI application would begin by invoking the static method {@link #list()} to learn what devices are available. Then using that information a new MidiBus object would be instantiated with with the desired MIDI input and/or output devices. The Processing sketch could then send midi via MidiBus's outgoing methods such as {@link #sendNoteOn(int channel, int pitch, int velocity)}, {@link #sendNoteOff(int channel, int pitch, int velocity)} and {@link #sendControllerChange(int channel, int number, int value)} and receive midi via the PApplet methods this package provides support for such as {@link PApplet#noteOn(int channel, int pitch, int velocity)}, {@link PApplet#noteOff(int channel, int pitch, int velocity)} and {@link PApplet#controllerChange(int channel, int number, int value)}.
 * <h4>Typical Implementation, Advanced</h4>
 * <p>
 * If you wish to build more complex Processing MIDI applications you can add more input and output devices to any given instance of MidiBus via the addInput() and addOutput() methods. However it is important to understand that each MidiBus object acts like 2 MIDI buses, one for input and one for output. This means, that by design, outgoing MIDI messages are sent to <i>all</i> output devices connected to a given instance of MidiBus, and incomming messages from <i>all</i> input devices connected to a given instance of MidiBus are <i>merged</i> upon reception. In practice, this means that, by design, you cannot tell which of the devices connected to a given instance of MidiBus sent a particular message, nor can you send a MIDI message to one particular device connected to that object. Instead, for independent reception/transmission to different <i>sets</i> of MIDI devices, you can instantiate more than one MidiBus object inside your Processing sketch. Each instance of MidiBus will only send MIDI messages to output devices which are connected to it and inbound MIDI messages arriving at each MidiBus can be diferentiated using the the {@link PApplet} methods with the bus_name parameter.
 *
 * @version 004
 * @author Severin Smith
 * @see PApplet
 * @see MidiListener
 * @see RawMidiListener
 * @see StandardMidiListener
 * @see SimpleMidiListener
*/
public class MidiBus {
	
	enum OperatingSystem { MAC, WIN, NIX, OTHER }
	
	static OperatingSystem current_os = 
		(System.getProperty("os.name").toLowerCase().indexOf( "win" ) >= 0) ?
		OperatingSystem.WIN :
		(System.getProperty("os.name").toLowerCase().indexOf( "mac" ) >= 0) ?
		OperatingSystem.MAC :
		(System.getProperty("os.name").toLowerCase().indexOf( "nix" ) >= 0 || System.getProperty("os.name").toLowerCase().indexOf( "nux" ) >= 0) ?
		OperatingSystem.NIX :
		OperatingSystem.OTHER; //Yes I am aware this is ugly, but I want it to be static
	
	String bus_name;
		
	Vector<InputDeviceContainer> input_devices;
	Vector<OutputDeviceContainer> output_devices;

	Vector<MidiListener> listeners;
	
	PApplet parent;
		
	Method method_note_on, method_note_off, method_controller_change, method_raw_midi, method_midi_message;
	Method method_note_on_with_bus_name, method_note_off_with_bus_name, method_controller_change_with_bus_name, method_raw_midi_with_bus_name, method_midi_message_with_bus_name;
	
	/* -- Constructors -- */
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet. No input or output MIDI devices will be opened. The new MidiBus's bus_name will be generated automatically.
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent) {
		init(parent);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet and opens the MIDI input and output devices specified by the indexes in_device_num and out_device_num. A value of -1 can be passed to in_device_num if no input MIDI device is to be opened, or to out_device_num if no output MIDI device is to be opened. The new MidiBus's bus_name will be generated automatically.
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_num the index of the MIDI input device to be opened.
	 * @param out_device_num the index of the MIDI output device to be opened.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, int in_device_num, int out_device_num) {		
		init(parent);
		addInput(in_device_num);
		addOutput(out_device_num);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet with the specified bus_name and opens the MIDI input and output devices specified by the indexes in_device_num and out_device_num. A value of -1 can be passed to in_device_num if no input MIDI device is to be opened, or to out_device_num if no output MIDI device is to be opened.
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_num the index of the MIDI input device to be opened.
	 * @param out_device_num the index of the MIDI output device to be opened.
	 * @param bus_name the String which which identifies this MidiBus.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, int in_device_num, int out_device_num, String bus_name) {		
		init(parent, bus_name);
		addInput(in_device_num);
		addOutput(out_device_num);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet with the specified bus_name. No input or output MIDI devices will be opened.
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param bus_name the String which which identifies this MidiBus.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, String bus_name) {
		init(parent, bus_name);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet and opens the MIDI input and output devices specified by the names in_device_name and out_device_name. An empty String can be passed to in_device_name if no input MIDI device is to be opened, or to out_device_name if no output MIDI device is to be opened. The new MidiBus's bus_name will be generated automatically.
	 * <p>
	 * If two or more MIDI inputs have the same name, whichever appears first when {@link #list()} is called will be added, simlarly for two or more MIDI outputs with the same name. If this behavior is problematic use {@link #MidiBus(PApplet parent, int in_device_num, int out_device_num)} instead.
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_name the name of the MIDI input device to be opened.
	 * @param out_device_name the name of the MIDI output device to be opened.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, String in_device_name, String out_device_name) {
		init(parent);
		addInput(in_device_name);
		addOutput(out_device_name);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet with the specified bus_name and opens the MIDI input and output devices specified by the names out_device_name and out_device_name. An empty String can be passed to in_device_name if no input MIDI device is to be opened, or to out_device_name if no output MIDI device is to be opened.
	 * <p>
	 * If two or more MIDI inputs have the same name, whichever appears first when {@link #list()} is called will be added, simlarly for two or more MIDI outputs with the same name. If this behavior is problematic use {@link #MidiBus(PApplet parent, int in_device_num, int out_device_num, String bus_name)} instead.
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_name the name of the MIDI input device to be opened.
	 * @param out_device_name the name of the MIDI output device to be opened.
	 * @param bus_name the String which which identifies this MidiBus.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, String in_device_name, String out_device_name, String bus_name) {
		init(parent, bus_name);
		addInput(in_device_name);
		addOutput(out_device_name);
	}

	/* -- Yet even more delicious constructor flavors -- */
	
	/**
	 * More flavors of constructor, similar to the others, but with mixed arguments
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_num the name of the MIDI input device to be opened.
	 * @param out_device_name the name of the MIDI output device to be opened.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, int in_device_num, String out_device_name) {
		init(parent);
		addInput(in_device_num);
		addOutput(out_device_name);
	}
	
	/**
	 * More flavors of constructor, similar to the others, but with mixed arguments
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_name the name of the MIDI input device to be opened.
	 * @param out_device_num the name of the MIDI output device to be opened.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, String in_device_name, int out_device_num) {
		init(parent);
		addInput(in_device_name);
		addOutput(out_device_num);
	}
	
	/**
	 * More flavors of constructor, similar to the others, but with mixed arguments
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_num the name of the MIDI input device to be opened.
	 * @param out_device_name the name of the MIDI output device to be opened.
	 * @param bus_name the String which which identifies this MidiBus.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, int in_device_num, String out_device_name, String bus_name) {
		init(parent, bus_name);
		addInput(in_device_num);
		addOutput(out_device_name);
	}
	
	/**
	 * More flavors of constructor, similar to the others, but with mixed arguments
	 *
	 * @param parent the Processing PApplet to which this MidiBus is attached.
	 * @param in_device_name the name of the MIDI input device to be opened.
	 * @param out_device_num the name of the MIDI output device to be opened.
	 * @param bus_name the String which which identifies this MidiBus.
	 * @see #addInput(int device_num)
	 * @see #addInput(String device_name)
	 * @see #addOutput(int device_num)
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public MidiBus(PApplet parent, String in_device_name, int out_device_num, String bus_name) {
		init(parent, bus_name);
		addInput(in_device_name);
		addOutput(out_device_num);
	}
	
	/* -- Constructor Functions -- */
	
	/**
	 * Creates a new (hopefully/probably) unique bus_name value for new MidiBus objects that weren't given one and then calls the regular init() function. 
	 * If two MidiBus object were to have the same name, this would be bad, but not fatal, so there's no point in spending too much time worrying about it.
	*/
	private void init(PApplet parent) {
		String id = new Formatter().format("%08d", System.currentTimeMillis()%100000000).toString();
		init(parent, "MidiBus_"+id);
	}
	
	/**
	 * Perfoms the initialisation of new MidiBus objects, is private for a reason, and is only ever called within the constructors. This method exists only for the purpose of cleaner and easier to maintain code.
	*/
	private void init(PApplet parent, String bus_name) {

		this.parent = parent;
	
		if(parent != null) parent.registerDispose(this);
		
		try {
			method_note_on = parent.getClass().getMethod("noteOn", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
	
		try {
			method_note_off = parent.getClass().getMethod("noteOff", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}

		try {
			method_controller_change = parent.getClass().getMethod("controllerChange", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			method_raw_midi = parent.getClass().getMethod("rawMidi", new Class[] { byte[].class });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			method_midi_message = parent.getClass().getMethod("midiMessage", new Class[] { MidiMessage.class });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
	
		try {
			method_note_on_with_bus_name = parent.getClass().getMethod("noteOn", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
	
		try {
			method_note_off_with_bus_name = parent.getClass().getMethod("noteOff", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}

		try {
			method_controller_change_with_bus_name = parent.getClass().getMethod("controllerChange", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			method_raw_midi_with_bus_name = parent.getClass().getMethod("rawMidi", new Class[] { byte[].class, String.class });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			method_midi_message_with_bus_name = parent.getClass().getMethod("midiMessage", new Class[] { MidiMessage.class, String.class });
		} catch(Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		/* -- */
		
		this.bus_name = bus_name;
	
		/* -- */
		
		input_devices = new Vector<InputDeviceContainer>();
		output_devices = new Vector<OutputDeviceContainer>();
		
		listeners = new Vector<MidiListener>();
	}

	/* -- Input/Output Handling -- */
	
	/**
	 * Returns the names of all the attached input devices.
	 *
	 * @return the names of the attached inputs.
	 * @see #attachedOutputs()
	*/
	public String[] attachedInputs() {
		MidiDevice.Info[] devices_info = attachedInputsMidiDeviceInfo();
		String[] devices = new String[devices_info.length];
		
		for(int i = 0;i < devices_info.length;i++) {
			devices[i] = devices_info[i].getName();
		}
		
		return devices;
	}
	
	/**
	 * Returns the names of all the attached output devices.
	 *
	 * @return the names of the attached outputs.
	 * @see #attachedInputs()
	*/
	public String[] attachedOutputs() {
		MidiDevice.Info[] devices_info = attachedOutputsMidiDeviceInfo();
		String[] devices = new String[devices_info.length];
		
		for(int i = 0;i < devices_info.length;i++) {
			devices[i] = devices_info[i].getName();
		}
		
		return devices;
	}
	
	/**
	 * Returns the MidiDevice.Info of all the attached input devices.
	 *
	 * @return the MidiDevice.Info of the attached inputs.
	*/
	MidiDevice.Info[] attachedInputsMidiDeviceInfo() {
		MidiDevice.Info[] devices = new MidiDevice.Info[input_devices.size()];
	
		for(int i = 0;i < input_devices.size();i++) {
			devices[i] = input_devices.get(i).info;
		}
		
		return devices;
	}
	
	/**
	 * Returns the MidiDevice.Info of all the attached output devices.
	 *
	 * @return the MidiDevice.Info of the attached outputs.
	*/
	MidiDevice.Info[] attachedOutputsMidiDeviceInfo() {
		MidiDevice.Info[] devices = new MidiDevice.Info[output_devices.size()];
	
		for(int i = 0;i < output_devices.size();i++) {
			devices[i] = output_devices.get(i).info;
		}
		
		return devices;
	}
	
	/**
	 * Adds a new MIDI input device specified by the index device_num. If the MIDI input device has already been added, it will not be added again.
	 *
	 * @param device_num the index of the MIDI input device to be added.
	 * @return true if and only if the input device was successfully added.
	 * @see #addInput(String device_name)
	 * @see #list()
	*/
	public boolean addInput(int device_num) {
		if(device_num == -1) return false;

		MidiDevice.Info[] devices = availableInputsMidiDeviceInfo();
		
		if(device_num >= devices.length || device_num < 0) {
			System.err.println("\nThe MidiBus Warning: The chosen input device numbered ["+device_num+"] was not added because it doesn't exist");
			return false;
		}
		
		return addInput(devices[device_num]);
	}
	
	
	/**
	 * Removes the MIDI input device specified by the index device_num.
	 *
	 * @param device_num the index of the MIDI input device to be removed.
	 * @return true if and only if the input device was successfully removed.
	 * @see #removeInput(String device_name)
	 * @see #attachedInputs()
	*/
	public synchronized boolean removeInput(int device_num) {
		try {
			InputDeviceContainer container = input_devices.get(device_num);
	
			input_devices.remove(container);
		
			container.transmitter.close();
			container.receiver.close();
		
			return true;
		} catch(ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
	
	/**
	 * Adds a new MIDI input device specified by the name device_name. If the MIDI input device has already been added, it will not be added again.
	 * <p>
	 * If two or more MIDI inputs have the same name, whichever appears first when {@link #list()} is called will be added. If this behavior is problematic use {@link #addInput(int device_num)} instead.
	 *
	 * @param device_name the name of the MIDI input device to be added.
	 * @return true if and only if the input device was successfully added.
	 * @see #addInput(int device_num)
	 * @see #list()
	*/
	public boolean addInput(String device_name) {
		if(device_name.equals("")) return false;
		
		MidiDevice.Info[] devices = availableInputsMidiDeviceInfo();
		
		for(int i = 0;i < devices.length;i++) {
			if(devices[i].getName().equals(device_name)) return addInput(devices[i]);
		}
		
		System.err.println("\nThe MidiBus Warning: No available input MIDI devices named: \""+device_name+"\" were found");
		return false;
	}
	
	/**
	 * Removes the MIDI input device specified by the name device_name.
	 * <p>
	 * If two or more attached MIDI inputs have the same name, whichever appears first when {@link #attachedInputs()} is called will be removed. If this behavior is problematic use {@link #removeInput(int device_num)} instead.
	 *
	 * @param device_name the name of the MIDI input device to be removed.
	 * @return true if and only if the input device was successfully removed.
	 * @see #removeInput(int device_num)
	 * @see #attachedInputs()
	*/
	public synchronized boolean removeInput(String device_name) {
		for(InputDeviceContainer container : input_devices) {
			if(container.info.getName().equals(device_name)) {
				input_devices.remove(container);
		
				container.transmitter.close();
				container.receiver.close();

				return true;
			}
		}
		return false;
	}
	
	/**
	 * Adds a new MIDI input device specified by the MidiDevice.Info device_info. If the MIDI input device has already been added, it will not be added again.
	 *
	 * @param device_info the MidiDevice.Info of the MIDI input device to be added.
	 * @return true if and only if the input device was successfully added.
	*/
	synchronized boolean addInput(MidiDevice.Info device_info) {
		try {
			MidiDevice new_device = MidiSystem.getMidiDevice(device_info);
		
			if(new_device.getMaxTransmitters() == 0) {
				System.err.println("\nThe MidiBus Warning: The chosen input device \""+device_info.getName()+"\" was not added because it is output only");
				return false;
			}
			
			for(InputDeviceContainer container : input_devices) {
				if(device_info.getName().equals(container.info.getName())) return false;
			}

			if(!new_device.isOpen()) new_device.open();

			MReceiver receiver = new MReceiver();
			Transmitter transmitter = new_device.getTransmitter();
			transmitter.setReceiver(receiver);
			
			InputDeviceContainer new_container = new InputDeviceContainer(new_device);
			new_container.transmitter = transmitter;
			new_container.receiver = receiver;
			
			input_devices.add(new_container);
			
			return true;
		} catch(MidiUnavailableException e) {
			System.err.println("\nThe MidiBus Warning: The chosen input device \""+device_info.getName()+"\" was not added because it is unavailable");
			return false;
		}
	}
	
	/**
	 * Adds a new MIDI output device specified by the index device_num. If the MIDI output device has already been added, it will not be added again.
	 *
	 * @param device_num the index of the MIDI output device to be added.
	 * @return true if and only if the output device was successfully added.
	 * @see #addOutput(String device_name)
	 * @see #list()
	*/
	public boolean addOutput(int device_num) {
		if(device_num == -1) return false;

		MidiDevice.Info[] devices = availableOutputsMidiDeviceInfo();
		
		if(device_num >= devices.length || device_num < 0) {
			System.err.println("\nThe MidiBus Warning: The chosen output device numbered ["+device_num+"] was not added because it doesn't exist");
			return false;
		}
		
		return addOutput(devices[device_num]);		
	}
	
	/**
	 * Removes the MIDI output device specified by the index device_num.
	 *
	 * @param device_num the index of the MIDI output device to be removed.
	 * @return true if and only if the output device was successfully removed.
	 * @see #removeInput(String device_name)
	 * @see #attachedOutputs()
	*/
	public synchronized boolean removeOutput(int device_num) {
		try {
			OutputDeviceContainer container = output_devices.get(device_num);
	
			output_devices.remove(container);
		
			container.receiver.close();
		
			return true;
		} catch(ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
	
	/**
	 * Adds a new MIDI output device specified by the name device_name. If the MIDI output device has already been added, it will not be added again.
	 * <p>
	 * If two or more MIDI outputs have the same name, whichever appears first when {@link #list()} is called will be added. If this behavior is problematic use {@link #addOutput(int device_num)} instead.
	 *
	 * @param device_name the name of the MIDI output device to be added.
	 * @return true if and only if the output device was successfully added.
	 * @see #addOutput(int device_num)
	 * @see #list()
	*/
	public boolean addOutput(String device_name) {
		if(device_name.equals("")) return false;
		
		MidiDevice.Info[] devices = availableOutputsMidiDeviceInfo();
		
		for(int i = 0;i < devices.length;i++) {
			if(devices[i].getName().equals(device_name)) return addOutput(devices[i]);
		}
		
		System.err.println("\nThe MidiBus Warning: No available input MIDI devices named: \""+device_name+"\" were found");
		return false;	
	}
	
	/**
	 * Removes the MIDI output device specified by the name device_name.
	 * <p>
	 * If two or more attached MIDI outputs have the same name, whichever appears first when {@link #attachedOutputs()} is called will be removed. If this behavior is problematic use {@link #removeOutput(int device_num)} instead.
	 *
	 * @param device_name the name of the MIDI output device to be removed.
	 * @return true if and only if the output device was successfully removed.
	 * @see #removeOutput(int device_num)
	 * @see #attachedOutputs()
	*/
	public synchronized boolean removeOutput(String device_name) {
		for(OutputDeviceContainer container : output_devices) {
			if(container.info.getName().equals(device_name)) {
				output_devices.remove(container);
			
				container.receiver.close();

				return true;
			}
		}
		return false;
	}
	
	/**
	 * Adds a new MIDI output device specified by the MidiDevice.Info device_info. If the MIDI output device has already been added, it will not be added again.
	 *
	 * @param device_info the MidiDevice.Info of the MIDI output device to be added.
	 * @return true if and only if the input device was successfully added.
	*/
	synchronized boolean addOutput(MidiDevice.Info device_info) {
		try {
			MidiDevice new_device = MidiSystem.getMidiDevice(device_info);
		
			if(new_device.getMaxReceivers() == 0) {
				System.err.println("\nThe MidiBus Warning: The chosen output device \""+device_info.getName()+"\" was not added because it is input only");
				return false;
			}
			
			for(OutputDeviceContainer container : output_devices) {
				if(device_info.getName().equals(container.info.getName())) return false;
			}

			if(!new_device.isOpen()) new_device.open();
			
			OutputDeviceContainer new_container = new OutputDeviceContainer(new_device);
			new_container.receiver = new_device.getReceiver();
						
			output_devices.add(new_container);
			
			return true;
		} catch(MidiUnavailableException e) {
			System.err.println("\nThe MidiBus Warning: The chosen output device \""+device_info.getName()+"\" was not added because it is unavailable");
			return false;
		}
	}
		
	/**
	 * Closes, clears and disposes of all input related Transmitters and Receivers.
	 *
	 * @see #clearOutputs()
	 * @see #clearAll()
	*/
	public synchronized void clearInputs() {
		//We are purposefully not closing devices here, because in some cases that will be slow, and we might want later
		//Also it's broken on MAC
		try{
			for(InputDeviceContainer container : input_devices) {
				container.transmitter.close();
				container.receiver.close();
			}
		} catch(Exception e) {
			System.err.println("The MidiBus Warning: Unexpected error during clearInputs()");
		}
		
		input_devices.clear();
	}
	
	/**
	 * Closes, clears and disposes of all output related Receivers.
	 *
	 * @see #clearInputs()
	 * @see #clearAll()
	*/
	public synchronized void clearOutputs() {
		//We are purposefully not closing devices here, because in some cases that will be slow, and we might want later
		//Also it's broken on MAC
		try{
			for(OutputDeviceContainer container : output_devices) {
				container.receiver.close();
			}	
		} catch(Exception e) {
			System.err.println("The MidiBus Warning: Unexpected error during clearOutputs()");
		}
		
		output_devices.clear();
	}
	
	/**
	 * Closes, clears and disposes of all input and output related Transmitters and Receivers.
	 *
	 * @see #clearInputs()
	 * @see #clearOutputs()
	*/
	public void clearAll() {
		clearInputs();
		clearOutputs();
	}
	
	/**
	 * Closes all MidiDevices, should only be called when closing the application, will interrupt all MIDI I/O. Call publicly from stop(), close() or dispose()
	 *
	 * @see #clearOutputs()
	 * @see #clearInputs()
	 * @see #clearAll()
	 * @see #stop()
	 * @see #clear()
	 * @see #dispose()
	*/
	void closeAllMidiDevices() {
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
		MidiDevice device;
		
		for(int i = 0;i < available_devices.length;i++) {
			try {
				device = MidiSystem.getMidiDevice(available_devices[i]);
				//Closing input devices on mac seems to be broken in the new native Java MIDI subsystem
				//Now is hangs instead of throwing a null pointer, yay!
				if(device.isOpen() && !(current_os == OperatingSystem.MAC && device.getMaxTransmitters() != 0)) device.close();
			} catch(MidiUnavailableException e) {
				//Device wasn't available, which is fine since we wanted to close it anyways
			}
		}		
		
	}
	
	/* -- MIDI Out -- */
	
	/**
	 * Sends a MIDI message with an unspecified number of bytes. The first byte should be always be the status byte. If the message is a Meta message of a System Exclusive message it can have more than 3 byte, otherwise all extra bytes will be dropped.
	 *
	 * @param data the bytes of the MIDI message.
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendMessage(byte[] data) {
		if((int)((byte)data[0] & 0xFF) == MetaMessage.META) {
				MetaMessage message = new MetaMessage();
				try {
					byte[] payload = new byte[data.length-2];
					System.arraycopy(data, 2, payload, 0, data.length-2);
					message.setMessage((int)((byte)data[1] & 0xFF), payload, data.length-2);
					sendMessage(data);
				} catch(InvalidMidiDataException e) {
					System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
				}
			} else if((int)((byte)data[0] & 0xFF) == SysexMessage.SYSTEM_EXCLUSIVE || (int)((byte)data[0] & 0xFF) == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
				SysexMessage message = new SysexMessage();
				try {
					message.setMessage(data, data.length);
					sendMessage(message);
				} catch(InvalidMidiDataException e) {
					System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
				}
			} else {
				ShortMessage message = new ShortMessage();
				try {
					message.setMessage((int)((byte)data[0] & 0xFF), (int)((byte)data[1] & 0xFF), (int)((byte)data[2] & 0xFF));
					sendMessage(message);
				} catch(InvalidMidiDataException e) {
					System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
				}
			}
	}
	
	/**
	 * Sends a MIDI message that takes no data bytes.
	 *
	 * @param status the status byte
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendMessage(int status) {
		ShortMessage message = new ShortMessage();
		try {
			message.setMessage(status);
			sendMessage(message);
		} catch(InvalidMidiDataException e) {
			System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
		}
	}
	
	/**
	 * Sends a MIDI message that takes only one data byte. If the message does not take data, the data byte is ignored.
	 *
	 * @param status the status byte
	 * @param data the data byte
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendMessage(int status, int data) {
		sendMessage(status, data, 0);
	}
	
	/**
	 * Sends a MIDI message that takes one or two data bytes. If the message takes only one data byte, the second data byte is ignored; if the message does not take any data bytes, both data bytes are ignored.
	 *
	 * @param status the status byte.
	 * @param data1 the first data byte.
	 * @param data2 the second data byte.
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendMessage(int status, int data1, int data2) {
		ShortMessage message = new ShortMessage();
		try {
			message.setMessage(status, data1, data2);
			sendMessage(message);
		} catch(InvalidMidiDataException e) {
			System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
		}
	}
	
	/**
	 * Sends a channel message which takes up to two data bytes. If the message only takes one data byte, the second data byte is ignored; if the message does not take any data bytes, both data bytes are ignored.
	 *
	 * @param command the MIDI command represented by this message.
	 * @param channel the channel associated with the message.
	 * @param data1 the first data byte.
	 * @param data2 the second data byte.
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendMessage(int command, int channel, int data1, int data2) {
		ShortMessage message = new ShortMessage();
		try {
			message.setMessage(command, channel, data1, data2);
			sendMessage(message);
		} catch(InvalidMidiDataException e) {
			System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
		}
	}
	
	/**
	 * Sends a MidiMessage object.
	 *
	 * @param message the MidiMessage.
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public synchronized void sendMessage(MidiMessage message) {
		for(OutputDeviceContainer container : output_devices) {
			container.receiver.send(message,-1);
		}
	}
	
	/**
	 * Sends a NoteOn message to a channel with the specified pitch and velocity.
	 *
	 * @param channel the channel associated with the message.
	 * @param pitch the pitch associated with the message.
	 * @param velocity the velocity associated with the message.
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendNoteOn(int channel, int pitch, int velocity) {
		ShortMessage message = new ShortMessage();
		try {
			message.setMessage(ShortMessage.NOTE_ON, constrain(channel,0,15), constrain(pitch,0,127), constrain(velocity,0,127));
			sendMessage(message);
		} catch(InvalidMidiDataException e) {
			System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
		}
	}
	
	/**
	 * Sends a NoteOff message to a channel with the specified pitch and velocity.
	 *
	 * @param channel the channel associated with the message.
	 * @param pitch the pitch associated with the message.
	 * @param velocity the velocity associated with the message.
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendNoteOff(int channel, int pitch, int velocity) {
		ShortMessage message = new ShortMessage();
		try {
			message.setMessage(ShortMessage.NOTE_OFF, constrain(channel,0,15), constrain(pitch,0,127), constrain(velocity,0,127));
			sendMessage(message);
		} catch(InvalidMidiDataException e) {
			System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
		}
	}
	
	/**
	 * Sends a ControllerChange message to a channel with the specified number and value.
	 *
	 * @param channel the channel associated with the message.
	 * @param number the number associated with the message.
	 * @param value the value associated with the message.
	 * @see #sendMessage(byte[] data)
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	*/
	public void sendControllerChange(int channel, int number, int value) {
		ShortMessage message = new ShortMessage();
		try {
			message.setMessage(ShortMessage.CONTROL_CHANGE, constrain(channel,0,15), constrain(number,0,127), constrain(value,0,127));
			sendMessage(message);
		} catch(InvalidMidiDataException e) {
			System.err.println("\nThe MidiBus Warning: Message not sent, invalid MIDI data");
		}
	}
	
	/* -- MIDI In -- */
	
	/**
	 * Notifies all types of listeners of a new MIDI message from one of the MIDI input devices.
	 *
	 * @param message the new inbound MidiMessage.
	*/
	void notifyListeners(MidiMessage message) {
		byte[] data = message.getMessage();
		
		for(MidiListener listener : listeners) {
		
			/* -- RawMidiListener -- */
		
			if(listener instanceof RawMidiListener) ((RawMidiListener)listener).rawMidiMessage(data);
		
			/* -- SimpleMidiListener -- */
			
			if(listener instanceof SimpleMidiListener) {
				if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_ON) {
					((SimpleMidiListener)listener).noteOn((int)(data[0] & 0x0F),(int)(data[1] & 0xFF),(int)(data[2] & 0xFF));
				} else if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_OFF) {
					((SimpleMidiListener)listener).noteOff((int)(data[0] & 0x0F),(int)(data[1] & 0xFF),(int)(data[2] & 0xFF));
				} else if((int)((byte)data[0] & 0xF0) == ShortMessage.CONTROL_CHANGE) {
					((SimpleMidiListener)listener).controllerChange((int)(data[0] & 0x0F),(int)(data[1] & 0xFF),(int)(data[2] & 0xFF));
				}
			}
		
			/* -- StandardMidiListener -- */
		
			if(listener instanceof StandardMidiListener) ((StandardMidiListener)listener).midiMessage(message);
			
		}
	}
	
	/**
	 * Notifies any of the supported methods implemented inside the PApplet parent of a new MIDI message from one of the MIDI input devices.
	 *
	 * @param message the new inbound MidiMessage.
	*/
	void notifyPApplet(MidiMessage message) {	
		byte[] data = message.getMessage();

		if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_ON) {
			if(method_note_on != null) {
				try {
					method_note_on.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF) });
				} catch(Exception e) {
					System.err.println("\nThe MidiBus Warning: Disabling noteOn(int channel, int pitch, int velocity) because an unkown exception was thrown and caught");
					e.printStackTrace();
					method_note_on = null;
				}
			}
			if(method_note_on_with_bus_name != null) {
				try {
					method_note_on_with_bus_name.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF), bus_name });
				} catch(Exception e) {
					System.err.println("\nThe MidiBus Warning: Disabling noteOn(int channel, int pitch, int velocity, String bus_name) with bus_name because an unkown exception was thrown and caught");
					e.printStackTrace();
					method_note_on_with_bus_name = null;
				}
			}
		} else if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_OFF) {
			if(method_note_off != null) {
				try {
					method_note_off.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF) });
				} catch(Exception e) {
					System.err.println("\nThe MidiBus Warning: Disabling noteOff(int channel, int pitch, int velocity) because an unkown exception was thrown and caught");
					e.printStackTrace();
					method_note_off = null;
				}
			}
			if(method_note_off_with_bus_name != null) {
				try {
					method_note_off_with_bus_name.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF), bus_name });
				} catch(Exception e) {
					System.err.println("\nThe MidiBus Warning: Disabling noteOff(int channel, int pitch, int velocity, String bus_name) with bus_name because an unkown exception was thrown and caught");
					e.printStackTrace();
					method_note_off_with_bus_name = null;
				}
			}
		} else if((int)((byte)data[0] & 0xF0) == ShortMessage.CONTROL_CHANGE) {
			if(method_controller_change != null) {
				try {
					method_controller_change.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF) });
				} catch(Exception e) {
					System.err.println("\nThe MidiBus Warning: Disabling controllerChange(int channel, int number, int value) because an unkown exception was thrown and caught");
					e.printStackTrace();
					method_controller_change = null;
				}
			}
			if(method_controller_change_with_bus_name != null) {
				try {
					method_controller_change_with_bus_name.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF), bus_name });
				} catch(Exception e) {
					System.err.println("\nThe MidiBus Warning: Disabling controllerChange(int channel, int number, int value, String bus_name) with bus_name because an unkown exception was thrown and caught");
					e.printStackTrace();
					method_controller_change_with_bus_name = null;
				}
			}
		}
		
		if(method_raw_midi != null) {
			try {
				method_raw_midi.invoke(parent, new Object[] { data });
			} catch(Exception e) {
				System.err.println("\nThe MidiBus Warning: Disabling rawMidi(byte[] data) because an unkown exception was thrown and caught");
				e.printStackTrace();
				method_raw_midi = null;
			}
		}
		if(method_raw_midi_with_bus_name != null) {
			try {
				method_raw_midi_with_bus_name.invoke(parent, new Object[] { data, bus_name });
			} catch(Exception e) {
				System.err.println("\nThe MidiBus Warning: Disabling rawMidi(byte[] data, String bus_name) with bus_name because an unkown exception was thrown and caught");
				e.printStackTrace();
				method_raw_midi_with_bus_name = null;
			}
		}
		
		if(method_midi_message != null) {
			try {
				method_midi_message.invoke(parent, new Object[] { message });
			} catch(Exception e) {
				System.err.println("\nThe MidiBus Warning: Disabling midiMessage(MidiMessage message) because an unkown exception was thrown and caught");
				e.printStackTrace();
				method_midi_message = null;
			}
		}
		if(method_midi_message_with_bus_name != null) {
			try {
				method_midi_message_with_bus_name.invoke(parent, new Object[] { message, bus_name });
			} catch(Exception e) {
				System.err.println("\nThe MidiBus Warning: Disabling midiMessage(MidiMessage message, String bus_name) with bus_name because an unkown exception was thrown and caught");
				e.printStackTrace();
				method_midi_message_with_bus_name = null;
			}
		}
		
	}
	
	/* -- Listener Handling -- */
	
	/**
	 * 	Adds a listener who will be notified each time a new MIDI message is received from a MIDI input device. If the listener has already been added, it will not be added again.
	 *
	 * @param listener the listener to add.
	 * @return true if and only the listener was sucessfully added.
	 * @see #removeMidiListener(MidiListener listener)
	*/
	public boolean addMidiListener(MidiListener listener) {
		for(MidiListener current : listeners) if(current == listener) return false;
		
		listeners.add(listener);
				
		return true;
	}
	
	/**
	 * Removes a given listener.
	 *
	 * @param listener the listener to remove.
	 * @return true if and only the listener was sucessfully removed.
	 * @see #addMidiListener(MidiListener listener)
	*/
	public boolean removeMidiListener(MidiListener listener) {
		for(MidiListener current : listeners) {
			if(current == listener) {
				listeners.remove(listener);
				return true;
			}
		}
		return false;
	}
	
	
	/* -- Utilites -- */
	
	/**
	 * It's just convient ... move along...
	*/
	int constrain(int value, int min, int max) {
		if(value > max) value = max;
		if(value < min) value = min;
		return value;
	}
	
	/**
	 * Returns the name of this MidiBus.
	 *
	 * @return the name of this MidiBus.
	 * @see #setBusName(String bus_name)
	*/
	public String getBusName() {
		return bus_name;
	}
	
	/**
	 * Changes the name of this MidiBus.
	 *
	 * @param bus_name the new name of this MidiBus.
	 * @see #getBusName()
	*/
	public void setBusName(String bus_name) {
		this.bus_name = bus_name;
	}
	
	/* -- Object -- */
	
	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	public String toString() {
		String output = "MidiBus: "+bus_name+" [";
		output += input_devices.size()+" input(s), ";
		output += output_devices.size()+" output(s), ";
		output += listeners.size()+" listener(s)]";
		return output;
	}
	
	/**
	 * Indicates whether some other object is "equal to" this one.
	 *
	 * @param obj the reference object with which to compare.
	 * @return if this object is the same as the obj argument; false otherwise.
	 */
	public boolean equals(Object obj) {
		if(obj instanceof MidiBus) {
			MidiBus midibus = (MidiBus)obj;
			if(!this.getBusName().equals(midibus.getBusName())) return false;
			if(!this.input_devices.equals(midibus.input_devices)) return false;
			if(!this.output_devices.equals(midibus.output_devices)) return false;
			if(!this.listeners.equals(midibus.listeners)) return false;
			return true;
		}
		return false;
	}
	
	/**
	 * Creates and returns a copy of this object.
	 *
	 * @return a clone of this instance.
	 */
	public MidiBus clone() {
		MidiBus clone = new MidiBus(parent, bus_name);
		
		for(InputDeviceContainer container : input_devices) {
			clone.addInput(container.info);
		}
		
		for(OutputDeviceContainer container : output_devices) {
			clone.addOutput(container.info);
		}
		
		for(MidiListener listener : listeners) {
			clone.addMidiListener(listener);
		}
		
		return clone;
	}
	
	/**
	 * Returns a hash code value for the object.
	 *
	 * @return a hash code value for this object.
	 */
	public int hashCode() {
		return bus_name.hashCode()+input_devices.hashCode()+output_devices.hashCode()+listeners.hashCode();
	}
	
	/**
	 * Override the finalize() method from java.lang.Object.
	 *
	*/
	protected void finalize() {
		close();
		if(parent != null) parent.unregisterDispose(this);
	}
	
	/* -- Shutting Down -- */
	
	/**
	 * Closes this MidiBus and all connections it has with other MIDI devices. This method exists as per standard javax.sound.midi syntax. It is functionaly equivalent to stop() and dispose().
	 *
	 * @see #stop()
	 * @see #dispose()
	*/
	public void close() {		
		closeAllMidiDevices();
	}
		
	/**
	 * Closes this MidiBus and all connections it has with other MIDI devices. This method exit as per standard Processing syntax for users who are doing their sketch cleanup themselves using the stop() function. It is functionaly equivalent to close() and dispose().
	 *
	 * @see #close()
	 * @see #dispose()
	*/
	public void stop() {
		close();
	}
	
	/**
	 * Closes this MidiBus and all connections it has with other MIDI devices. This method exit as per standard Processing library syntax and is called automatically whenever the parent applet shuts down. It is functionaly equivalent to close() and stop().
	 *
	 * @see #close()
	 * @see #stop()
	*/
	public void dispose() {
		close();
	}
	
	/* -- Static methods -- */
	
	/**
	 * List all installed MIDI devices. The index, name and type (input/output/unavailable) of each devices will be indicated. 
	 *
	 * @see #availableInputs()
	 * @see #availableOutputs()
	 * @see #unavailableDevices()
	*/
	static public void list() {
		String[] available_inputs = availableInputs();
		String[] available_outputs = availableOutputs();
		String[] unavailable = unavailableDevices();
		
		if(available_inputs.length == 0 && available_outputs.length == 0 && unavailable.length == 0) return;
		
		System.out.println("\nAvailable MIDI Devices:");
		if(available_inputs.length != 0) {
			System.out.println("----------Input----------");
			for(int i = 0;i < available_inputs.length;i++) System.out.println("["+i+"] \""+available_inputs[i]+"\"");
		}
		if(available_outputs.length != 0) {
			System.out.println("----------Output----------");
			for(int i = 0;i < available_outputs.length;i++) System.out.println("["+i+"] \""+available_outputs[i]+"\"");
		}
		if(unavailable.length != 0) {
			System.out.println("----------Unavailable----------");
			for(int i = 0;i < unavailable.length;i++) System.out.println("["+i+"] \""+unavailable[i]+"\"");
		}
	}
	
	/**
	 * Returns the names of all the available input devices.
	 *
	 * @return the names of the available inputs.
	 * @see #list()
	 * @see #availableOutputs()
	 * @see #unavailableDevices()
	*/
	static public String[] availableInputs() {
		MidiDevice.Info[] devices_info = availableInputsMidiDeviceInfo();
		String[] devices = new String[devices_info.length];
		
		for(int i = 0;i < devices_info.length;i++) {
			devices[i] = devices_info[i].getName();
		}
		
		return devices;
	}
	
	/**
	 * Returns the names of all the available output devices.
	 *
	 * @return the names of the available outputs.
	 * @see #list()
	 * @see #availableInputs()
	 * @see #unavailableDevices()
	*/
	static public String[] availableOutputs() {
		MidiDevice.Info[] devices_info = availableOutputsMidiDeviceInfo();
		String[] devices = new String[devices_info.length];
		
		for(int i = 0;i < devices_info.length;i++) {
			devices[i] = devices_info[i].getName();
		}
		
		return devices;
	}
	
	/**
	 * Returns the names of all the unavailable devices.
	 *
	 * @return the names of the unavailable devices.
	 * @see #list()
	 * @see #availableInputs()
	 * @see #availableOutputs()
	*/
	static public String[] unavailableDevices() {
		MidiDevice.Info[] devices_info = unavailableMidiDeviceInfo();
		String[] devices = new String[devices_info.length];
		
		for(int i = 0;i < devices_info.length;i++) {
			devices[i] = devices_info[i].getName();
		}
		
		return devices;
	}
	
	/**
	 * Returns the MidiDevice.Info of all the available input devices.
	 *
	 * @return the MidiDevice.Info of the available inputs.
	*/
	static MidiDevice.Info[] availableInputsMidiDeviceInfo() {
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
		MidiDevice device;
		
		Vector<MidiDevice.Info> devices_list = new Vector<MidiDevice.Info>();
		
		for(int i = 0;i < available_devices.length;i++) {
			try {
				device = MidiSystem.getMidiDevice(available_devices[i]);
				//This open close checks to make sure the announced device is truely available
				//There are many reports on Windows that some devices lie about their availability
				//(For instance the Microsoft GS Wavetable Synth)
				//But in theory I guess this could happen on any OS, so I'll just do it all the time.
				if(!device.isOpen()) {
					device.open();
					//Closing input devices on mac seems to be broken in the new native Java MIDI subsystem
					//Now is hangs instead of throwing a null pointer, yay!
					if(current_os != OperatingSystem.MAC) device.close();
				}
				if (device.getMaxTransmitters() != 0) devices_list.add(available_devices[i]);
			} catch(MidiUnavailableException e) {
				//Device was unavailable which is fine, we only care about available inputs
			}
		}
		
		MidiDevice.Info[] devices = new MidiDevice.Info[devices_list.size()];
		
		devices_list.toArray(devices);
		
		return devices;
	}
	
	/**
	 * Returns the MidiDevice.Info of all the available output devices.
	 *
	 * @return the MidiDevice.Info of the available output.
	*/
	static MidiDevice.Info[] availableOutputsMidiDeviceInfo() {
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
		MidiDevice device;
		
		Vector<MidiDevice.Info> devices_list = new Vector<MidiDevice.Info>();
		
		for(int i = 0;i < available_devices.length;i++) {
			try {
				device = MidiSystem.getMidiDevice(available_devices[i]);
				//This open close checks to make sure the announced device is truely available
				//There are many reports on Windows that some devices lie about their availability
				//(For instance the Microsoft GS Wavetable Synth)
				//But in theory I guess this could happen on any OS, so I'll just do it all the time.
				if(!device.isOpen()) {
					device.open();
					device.close();
				}
				if (device.getMaxReceivers() != 0) devices_list.add(available_devices[i]);
			} catch(MidiUnavailableException e) {
				//Device was unavailable which is fine, we only care about available output
			}
		}
		
		MidiDevice.Info[] devices = new MidiDevice.Info[devices_list.size()];
		
		devices_list.toArray(devices);
		
		return devices;
	}
	
	/**
	 * Returns the MidiDevice.Info of all the unavailable devices.
	 *
	 * @return the MidiDevice.Info of the unavailable devices.
	*/
	static MidiDevice.Info[] unavailableMidiDeviceInfo() {
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
		MidiDevice device;
		
		Vector<MidiDevice.Info> devices_list = new Vector<MidiDevice.Info>();
		
		for(int i = 0;i < available_devices.length;i++) {
			try {
				device = MidiSystem.getMidiDevice(available_devices[i]);
				//This open close checks to make sure the announced device is truely available
				//There are many reports on Windows that some devices lie about their availability
				//(For instance the Microsoft GS Wavetable Synth)
				//But in theory I guess this could happen on any OS, so I'll just do it all the time.
				if(!device.isOpen()) {
					device.open();
					device.close();
				}
			} catch(MidiUnavailableException e) {
				devices_list.add(available_devices[i]);
			}
		}
		
		MidiDevice.Info[] devices = new MidiDevice.Info[devices_list.size()];
		
		devices_list.toArray(devices);
		
		return devices;
	}
		
	/* -- Nested Classes -- */
	
	private class MReceiver implements Receiver {
				
		MReceiver() {

		}
		
		public void close() {

		}
		
	 	public void send(MidiMessage message, long timeStamp) {
			
			if(message.getStatus() == ShortMessage.NOTE_ON && message.getMessage()[2] == 0) {
				try {
					ShortMessage tmp_message = (ShortMessage)message;
					tmp_message.setMessage(ShortMessage.NOTE_OFF, tmp_message.getData1(), tmp_message.getData2());
					message = tmp_message;
				} catch(Exception e) {
					System.err.println("\nThe MidiBus Warning: Mystery error during noteOn (0 velocity) to noteOff conversion");
				}
			}
			
			notifyListeners(message);
			notifyPApplet(message);
		}
		
	}	
	
	private class InputDeviceContainer {
				
		MidiDevice.Info info;
		
		Transmitter transmitter;
		Receiver receiver;
		
		InputDeviceContainer(MidiDevice device) {
			this.info = device.getDeviceInfo();
		}
		
		public boolean equals(Object container) {
			if(container instanceof InputDeviceContainer && ((InputDeviceContainer)container).info.getName().equals(this.info.getName())) return true;
			else return false;
		}
		
		public int hashCode() {
			return info.getName().hashCode();
		}
		
	}
	
	private class OutputDeviceContainer {
			
		MidiDevice.Info info;
		
		Receiver receiver;
		
		OutputDeviceContainer(MidiDevice device) {
			this.info = device.getDeviceInfo();
		}
		
		public boolean equals(Object container) {
			if(container instanceof OutputDeviceContainer && ((OutputDeviceContainer)container).info.getName().equals(this.info.getName())) return true;
			else return false;
		}
		
		public int hashCode() {
			return info.getName().hashCode();
		}
		
	}
	
}