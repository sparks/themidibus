/**
 * Copyright (c) 2008 Severin Smith

 * This file is part of a library called themidibus - http://www.smallbutdigital.com/themidibus.php.

 * themidibus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * themidibus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with themidibus.  If not, see <http://www.gnu.org/licenses/>.
*/

package themidibus;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Formatter;
import processing.core.PApplet;
import java.lang.reflect.Method;

/**
 * The MidiBus class provides simplified access to installed MIDI system resources, including devices such as synthesizers, sequencers, and MIDI input and output ports. The class is designed specifically to be used in a <a target="_blank" href="http://www.processing.org">Processing</a> sketch, although it could just as easily be used within any standard java program.
* <p>
* It is important to understand that themidibus offers very little functionality that isn't available from the <a target="_blank" href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html">javax.sound.midi</a> package. What it does offer - <i>in the spirit of Processing's easy to use sketch/prototyping style</i> - is a clean and simple way to access the major features of the <a target="_blank" href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html">javax.sound.midi</a> package with added integration and support for <a target="_blank" href="http://www.processing.org">Processing</a>, most notably in the form of support for noteOn(), noteOff() and controllerChange() methods to handle inbound midi within the {@link themidibus.PApplet}.
*<p>
* Anyone trying to build a complex and full featured MIDI application should take the time to read the documentation for java's native MIDI support package, <a target="_blank" href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html">javax.sound.midi</a>, because it offers a more full feature and flexible alternative to this package, although it does do so at the cost of a some added complexity. In addition, it may be worthwhile to skim <a href="http://java.sun.com/docs/books/tutorial/sound/index.html">the "official" Java Tutorial</a> for the javax.sound.* packages.
 * <p>
 * <b style="color:red;">Note to Processing users:</b> The current version of Processing (Processing 0135) doesn't allows java 1.5.0 (Java SE5) syntax when compiling within the IDE, but it does do the actual compiling using java 1.5.0 (Java SE5) meaning that many interfaces from the <a target="_blank" href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html">javax.sound.midi</a> package cannot be implemented in Processing sketches. [<i>see <a target="_blank" href="http://dev.processing.org/bugs/show_bug.cgi?id=598">Processing Bug 598</a></i>]
 * <h4>Typical Implementation, Simple</h4>
 * <p>
 * A typical simple Processing MIDI application would begin by invoking the static method {@link #list()} to learn what devices are available. Then using that information a new MidiBus object would be instantiated with with the desired MIDI input and/or output devices. The Processing sketch could then send midi via MidiBus's outgoing methods such as {@link #sendNoteOn(int channel, int pitch, int velocity)}, {@link #sendNoteOff(int channel, int pitch, int velocity)} and {@link #sendControllerChange(int channel, int number, int value)} and receive midi via the PApplet methods this package provides support for such as {@link PApplet#noteOn(int channel, int pitch, int velocity)}, {@link PApplet#noteOff(int channel, int pitch, int velocity)} and {@link PApplet#controllerChange(int channel, int number, int value)}.
 * <h4>Typical Implementation, Advanced</h4>
 * <p>
 * If you wish to build more complex Processing MIDI applications you can add more input and output devices to any given instance of MidiBus via the addInput() and addOutput() methods. However it is important to understand that each MidiBus object acts like 2 MIDI buses, one for input and one for output. This means, that by design, outgoing MIDI messages are sent to <i>all</i> output devices connected to a given instance of MidiBus without discrimination, and incomming messages from <i>all</i> input devices connected to a given instance of MidiBus are <i>merged</i> upon reception without discriminating. In practice, this means that, by design, you cannot tell which of the devices connected to a given instance of MidiBus sent a particular message, nor can you send a MIDI message to one particular device connected to that object. Instead, for independant reception/transmission to different <i>sets</i> of MIDI devices, you can instantiate more than one MidiBus object inside your Processing sketch. Each instance of MidiBus will only send MIDI messages to output devices which are connected to it and inbound MIDI messages arriving at each MidiBus can be diferentiated using the the {@link PApplet} methods with the bus_name parameter.
 * <p>
 * Although the bus system used by MidiBus is very simple, powerful and efficient, it may not immediately make sense. Possibly useful and/or confusing explanations as well as examples  <a href="http://www.smallbutdigital.com/themidibus.php">can be found online</a>. Please take the time to check them out.
 *
 * @version 003
 * @author Severin Smith
 * @see PApplet
 * @see MidiListener
 * @see RawMidiListener
 * @see StandardMidiListener
 * @see SimpleMidiListener
 * @see javax.sound.midi
*/

public class MidiBus {
	
	String bus_name;
	
	Vector<MidiDevice> in_devices;
	Vector<Receiver> in_receivers;
	Vector<Transmitter> in_transmitters;
	
	Vector<MidiDevice> out_devices;
	Vector<Receiver> out_receivers;
	
	Vector<MidiListener> listeners;
	Vector<RawMidiListener> raw_listeners;
	Vector<SimpleMidiListener> simple_listeners;
	Vector<StandardMidiListener> standard_listeners;
	
	PApplet parent;
	
	Method eventMethod_noteOn, eventMethod_noteOff, eventMethod_controllerChange, eventMethod_rawMidi, eventMethod_midiMessage;
	Method eventMethod_noteOn_withBusName, eventMethod_noteOff_withBusName, eventMethod_controllerChange_withBusName, eventMethod_rawMidi_withBusName, eventMethod_midiMessage_withBusName;
	
	/* -- Constructors -- */
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet. This is the simplest constructor available. No input or output MIDI devices will be opened. The new MidiBus's bus_name will be generated automatically.
	 *
	 * @see #addInput(int in_device_num)
	 * @see #addInput(int in_device_name)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(int out_device_name)
	 * @param parent the Processing PApplet to which this MidiBus is attached
	*/
	public MidiBus(PApplet parent) {
		init(parent,-1,-1);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet and opens the MIDI input and output devices specified by the indexes in_device_num and out_device_num. A value of -1 can be passed to in_device_num if no input MIDI device is to be opened, or to out_device_num if no output MIDI device is to be opened. The new MidiBus's bus_name will be generated automatically.
	 *
	 * @see #addInput(int in_device_num)
	 * @see #addInput(int in_device_name)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(int out_device_name)
	 * @see #list()
	 * @param parent the Processing PApplet to which this MidiBus is attached
	 * @param in_device_num the index of the MIDI input device to be opened
	 * @param out_device_num the index of the MIDI output device to be opened
	*/
	public MidiBus(PApplet parent, int in_device_num, int out_device_num) {		
		init(parent, in_device_num, out_device_num);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet with the specified bus_name and opens the MIDI input and output devices specified by the indexes in_device_num and out_device_num. A value of -1 can be passed to in_device_num if no input MIDI device is to be opened, or to out_device_num if no output MIDI device is to be opened.
	 *
	 * @see #addInput(int in_device_num)
	 * @see #addInput(int in_device_name)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(int out_device_name)
	 * @see #list()
	 * @param parent the Processing PApplet to which this MidiBus is attached
	 * @param in_device_num the index of the MIDI input device to be opened
	 * @param out_device_num the index of the MIDI output device to be opened
	 * @param bus_name the String which which identifies this MidiBus
	*/
	public MidiBus(PApplet parent, int in_device_num, int out_device_num, String bus_name) {		
		init(parent, in_device_num, out_device_num, bus_name);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet with the specified bus_name. No input or output MIDI devices will be opened.
	 *
	 * @see #addInput(int in_device_num)
	 * @see #addInput(int in_device_name)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(int out_device_name)
	 * @param parent the Processing PApplet to which this MidiBus is attached
	 * @param bus_name the String which which identifies this MidiBus
	*/
	public MidiBus(PApplet parent, String bus_name) {
		init(parent,-1,-1, bus_name);
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet and opens the MIDI input and output devices specified by the names in_device_name and out_device_name. An empty String can be passed to in_device_num if no input MIDI device is to be opened, or to out_device_num if no output MIDI device is to be opened. The new MidiBus's bus_name will be generated automatically.
	 * <p>
	 * If two or more MIDI inputs have the same name, whichever appears first when {@link #list()} is called will be added, simlarly for two or more MIDI outputs with the same name. It is not a problem if the MIDI input and output being added have the same name. If this behavior is problematic use {@link #MidiBus(PApplet parent, int in_device_num, int out_device_num)} instead.
	 *
	 * @see #addInput(int in_device_num)
	 * @see #addInput(int in_device_name)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(int out_device_name)
	 * @see #list()
	 * @param parent the Processing PApplet to which this MidiBus is attached
	 * @param in_device_name the name of the MIDI input device to be opened
	 * @param out_device_name the name of the MIDI output device to be opened
	*/
	public MidiBus(PApplet parent, String in_device_name, String out_device_name) {
		init(parent, inputDeviceNameToNumber(in_device_name), outputDeviceNameToNumber(out_device_name));
	}
	
	/**
	 * Constructs a new MidiBus attached to the specified PApplet with the specified bus_name and opens the MIDI input and output devices specified by the names in_device_num and out_device_num. An empty String can be passed to in_device_num if no input MIDI device is to be opened, or to out_device_num if no output MIDI device is to be opened.
	 * <p>
	 * If two or more MIDI inputs have the same name, whichever appears first when {@link #list()} is called will be added, simlarly for two or more MIDI outputs with the same name. It is not a problem if the MIDI input and output being added have the same name. If this behavior is problematic use {@link #MidiBus(PApplet parent, int in_device_num, int out_device_num, String bus_name)} instead.
	 *
	 * @see #addInput(int in_device_num)
	 * @see #addInput(int in_device_name)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(int out_device_name)
	 * @see #list()
	 * @param parent the Processing PApplet to which this MidiBus is attached
	 * @param in_device_name the name of the MIDI input device to be opened
	 * @param out_device_name the name of the MIDI output device to be opened
	 * @param bus_name the String which which identifies this MidiBus
	*/
	public MidiBus(PApplet parent, String in_device_name, String out_device_name, String bus_name) {
		init(parent, inputDeviceNameToNumber(in_device_name), outputDeviceNameToNumber(out_device_name), bus_name);
	}
	
	/* -- Constructor Functions -- */
	
	/**
	 * Creates a new (hopefully/probably) unique bus_name value for new MidiBus objects that weren't given one and then calls the regular init() function. 
	 * N.B. If two MidiBus object were to have the same name, this would be bad, but not fatal, so there's no point in spending too much time worrying about it
	*/
	private void init(PApplet parent, int in_device_num, int out_device_num) {
		String id = new Formatter().format("%08d", System.currentTimeMillis()%100000000).toString();
		init(parent, in_device_num, out_device_num, "MidiBus_"+id);
	}
	
	/**
	 * Perfoms the initialisation of new MidiBus objects, is private for a reason, and is only ever called within the constructors. This method exists only for the purpose of cleaner and easier to maintain code.
	*/
	private void init(PApplet parent, int in_device_num, int out_device_num, String bus_name) {
		this.parent = parent;
	
		parent.registerDispose(this);
		
		try {
			eventMethod_noteOn = parent.getClass().getMethod("noteOn", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
	
		try {
			eventMethod_noteOff = parent.getClass().getMethod("noteOff", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}

		try {
			eventMethod_controllerChange = parent.getClass().getMethod("controllerChange", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			eventMethod_rawMidi = parent.getClass().getMethod("rawMidi", new Class[] { byte[].class });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			eventMethod_midiMessage = parent.getClass().getMethod("midiMessage", new Class[] { MidiMessage.class });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
	
		try {
			eventMethod_noteOn_withBusName = parent.getClass().getMethod("noteOn", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
	
		try {
			eventMethod_noteOff_withBusName = parent.getClass().getMethod("noteOff", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}

		try {
			eventMethod_controllerChange_withBusName = parent.getClass().getMethod("controllerChange", new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			eventMethod_rawMidi_withBusName = parent.getClass().getMethod("rawMidi", new Class[] { byte[].class, String.class });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		try {
			eventMethod_midiMessage_withBusName = parent.getClass().getMethod("midiMessage", new Class[] { MidiMessage.class, String.class });
		} catch (Exception e) {
			// no such method, or an error.. which is fine, just ignore
		}
		
		/* -- */
		
		this.bus_name = bus_name;
	
		/* -- */
		
		in_devices = new Vector<MidiDevice>();
		in_receivers = new Vector<Receiver>();
		in_transmitters = new Vector<Transmitter>();
		
		out_devices = new Vector<MidiDevice>();
		out_receivers = new Vector<Receiver>();
		
		listeners = new Vector<MidiListener>();
		raw_listeners = new Vector<RawMidiListener>();
		simple_listeners = new Vector<SimpleMidiListener>();
		standard_listeners = new Vector<StandardMidiListener>();
	
		/* -- */
						
		if(in_device_num != -1) {
			addInput(in_device_num);
		}
	
		if(out_device_num != -1) {
    		addOutput(out_device_num);
		}
	}

	/* -- Receiver/Transmitter/Device Handling -- */
	
	/**
	 * Adds a new inbound MIDI device specified by the index in_device_num. If the MIDI input device has already been added, it will not be added again. All incomming messages from MIDI devices connected to a given MidiBus are merged indiscriminately and cannot be differentiated. More than one MidiBus should be used if it is necessary to differentiate messages from multiple devices. For more information visit <a href="http://www.smallbutdigital.com/themidibus.php">http://www.smallbutdigital.com/themidibus.php</a>.
	 *
	 * @param in_device_num the index of the MIDI input device to be opened
	 * @return true if and only if the input device was successfully added
	 * @see #addInput(String in_device_name)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(String out_device_name)
	 * @see #list()
	*/
	public boolean addInput(int in_device_num) {
		if(in_device_num == -1) return false;
		
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
	
		if(in_device_num >= available_devices.length || in_device_num < 0) {
			System.err.println("\nMidiBus Warning: The chosen input device numbered ["+in_device_num+"] was not added because it doesn't exist");
			return false;
		} else {
	  		try {
				MidiDevice in_device = MidiSystem.getMidiDevice(available_devices[in_device_num]);
				if(in_device.getMaxTransmitters() == 0) {
					System.err.println("\nMidiBus Warning: The chosen input device ["+in_device_num+"] \""+available_devices[in_device_num].getName()+"\" was not added because it is output only");
					return false;
				} else {
					for(MidiDevice device : in_devices) {
						if(in_device.getDeviceInfo() == device.getDeviceInfo()) return false;
					}
				
					in_device.open();
				
					MReceiver receiver = new MReceiver();
					Transmitter transmitter = in_device.getTransmitter();
					transmitter.setReceiver(receiver);

					in_transmitters.add(transmitter);
					in_receivers.add(receiver);
					in_devices.add(in_device);
				}
				return true;
			} catch (MidiUnavailableException e) {
				System.err.println("\nMidiBus Warning: The chosen input device ["+in_device_num+"] \""+available_devices[in_device_num].getName()+"\" was not added because a MidiUnavailableException was thrown and caught");
				return false;
			}
		}
	}
	
	/**
	 * Adds a new inbound MIDI device specified by the name in_device_name. If the MIDI input device has already been added, it will not be added again. All incomming messages from MIDI devices connected to a given MidiBus are merged indiscriminately and cannot be differentiated. More than one MidiBus should be used if it is necessary to differentiate messages from multiple devices. For more information visit <a href="http://www.smallbutdigital.com/themidibus.php">http://www.smallbutdigital.com/themidibus.php</a>.
	 * <p>
	 * If two or more MIDI inputs have the same name, whichever appears first when {@link #list()} is called will be added. It does not matter if there are MIDI outputs with the same name as the MIDI input being added. If this behavior is problematic use {@link #addInput(int in_device_num)} instead.
	 *
	 * @param in_device_name the name of the MIDI input device to be opened
	 * @return true if and only if the input device was successfully added
	 * @see #addInput(int in_device_num)
	 * @see #addOutput(int out_device_num)
	 * @see #addOutput(String out_device_name)
	 * @see #list()
	*/
	public boolean addInput(String in_device_name) {
		return addInput(inputDeviceNameToNumber(in_device_name));	
	}
	
	/**
	 * Adds a new outbound MIDI device specified by the index out_device_num. If the MIDI output device has already been added, it will not be added again. All outgoing messages sent from a MidiBus are sent indiscriminately to all connected MIDI output device. More than one MidiBus should be used if it is necessary to send distinct messages to various MIDI outputs. For more information visit <a href="http://www.smallbutdigital.com/themidibus.php">http://www.smallbutdigital.com/themidibus.php</a>.
	 *
	 * @param out_device_num the index of the MIDI output device to be opened
	 * @return true if and only if the output device was successfully added
	 * @see #addOutput(String out_device_name)
	 * @see #addInput(String in_device_num)
	 * @see #addInput(String in_device_name)
	 * @see #list()
	*/
	public boolean addOutput(int out_device_num) {
		if(out_device_num == -1) return false;
		
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
		
		if(out_device_num >= available_devices.length || out_device_num < 0) {
			System.err.println("\nMidiBus Warning: The chosen output device numbered ["+out_device_num+"] was not added because it doesn't exist");
			return false;
		} else {
			try {
				MidiDevice out_device = MidiSystem.getMidiDevice(available_devices[out_device_num]);
				if(out_device.getMaxReceivers() == 0) {
					System.err.println("\nMidiBus Warning: The chosen output device ["+out_device_num+"] \""+available_devices[out_device_num].getName()+"\" was not added because it is input only");
					return false;
				} else {
					for(MidiDevice device : out_devices) {
						if(out_device.getDeviceInfo() == device.getDeviceInfo()) return false;
					}
					out_device.open();

					out_receivers.add(out_device.getReceiver());
					out_devices.add(out_device);
				}
				return true;
			} catch (MidiUnavailableException e) {
				System.err.println("\nMidiBus Warning: The chosen output device ["+out_device_num+"] \""+available_devices[out_device_num].getName()+"\" was not added because a MidiUnavailableException was thrown and caught");
				return false;
			}
		}
	}
	
	/**
	 * Adds a new outbound MIDI device specified by the name out_device_name. If the MIDI output device has already been added, it will not be added again. All outgoing messages sent from a MidiBus are sent indiscriminately to all connected MIDI output device. More than one MidiBus should be used if it is necessary to send distinct messages to various MIDI outputs. For more information visit <a href="http://www.smallbutdigital.com/themidibus.php">http://www.smallbutdigital.com/themidibus.php</a>.
	 * <p>
	 * If two or more MIDI outputs have the same name, whichever appears first when {@link #list()} is called will be added. It does not matter if there are MIDI inputs with the same name as the MIDI output being added. If this behavior is problematic use {@link #addOutput(int out_device_num)} instead.
	 *
	 * @param out_device_name the name of the MIDI output device to be opened
	 * @return true if and only if the output device was successfully added
	 * @see #addOutput(int out_device_num)
	 * @see #addInput(String in_device_num)
	 * @see #addInput(String in_device_name)
	 * @see #list()
	*/
	public boolean addOutput(String out_device_name) {
		return addOutput(outputDeviceNameToNumber(out_device_name));	
	}
	
	/**
	 * Closes, clears and disposes of all input related MidiDevices, Transmitters and Receivers.
	 *
	 * @see #clearOutputs()
	 * @see #clearAll()
	 * @see #close()
	 * @see #stop()
	 * @see #dispose()
	*/
	public void clearInputs() {
		for(Transmitter transmitter : in_transmitters) {
			transmitter.close();
		}
		
		for(MidiDevice device : in_devices) {
			try {
				device.close();
			} catch(Exception e) {
				System.err.println("\nMidiBus Warning: Mystery error during clearInputs()");
			}
		}
		
		in_transmitters.clear();
		in_receivers.clear();
		in_devices.clear();
	}
	
	/**
	 * Closes, clears and disposes of all output related MidiDevices and Receivers.
	 *
	 * @see #clearInputs()
	 * @see #clearAll()
	 * @see #close()
	 * @see #stop()
	 * @see #dispose()
	*/
	public void clearOutputs() {
		for(Receiver receiver : out_receivers) {
			receiver.close();
		}
		
		for(MidiDevice device : out_devices) {
			try {
				device.close();
			} catch(Exception e) {
				System.err.println("\nMidiBus Warning: Mystery error during clearOutputs()");
			}
		}
		
		out_receivers.clear();
		out_devices.clear();
	}
	
	/**
	 * Closes, clears and disposes of all input and output related MidiDevices, Transmitters and Receivers.
	 *
	 * @see #clearInputs()
	 * @see #clearOutputs()
	 * @see #close()
	 * @see #stop()
	 * @see #dispose()
	*/
	public void clearAll() {
		clearInputs();
		clearOutputs();
	}
	
	/* -- Midi Out -- */
	
	/**
	 * Sends a MIDI message that takes no data bytes.
	 *
	 * @param status the MIDI status byte
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
			message.setMessage(constrain(status, 0, 255));
			for(Receiver receiver : out_receivers) {
				receiver.send(message, -1);
			}
		} catch(InvalidMidiDataException e) {
			System.err.println("\nMidiBus Warning: Message not sent, InvalidMidiDataException thrown");
		}
	}
	
	/**
	 * Sends a MIDI message that takes only one data byte. If the message does not take data, the data byte is ignored.
	 *
	 * @param status the status byte to be sent
	 * @param data data byte
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
	 * @param status the status byte to be sent
	 * @param data1 the first data byte
	 * @param data2 the second data byte
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
			message.setMessage(constrain(status, 0, 255), constrain(data1, 0, 127), constrain(data2, 0, 127));
			for(Receiver receiver : out_receivers) {
				receiver.send(message, -1);
			}
		} catch(InvalidMidiDataException e) {
			System.err.println("\nMidiBus Warning: Message not sent, InvalidMidiDataException thrown");
		}
	}
	
	/**
	 * Sends a channel message which takes up to two data bytes. If the message only takes one data byte, the second data byte is ignored; if the message does not take any data bytes, both data bytes are ignored.
	 *
	 * @param command the MIDI command represented by this message
	 * @param channel the channel associated with the message
	 * @param data1 the first data byte
	 * @param data2 the second data byte
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(MidiMessage message)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendMessage(int command, int channel, int data1, int data2) {
		ShortMessage message = new ShortMessage();
		try {
			message.setMessage(constrain(command, 0, 255), constrain(channel, 0, 15), constrain(data1, 0, 127), constrain(data2, 0, 127));
			for(Receiver receiver : out_receivers) {
				receiver.send(message, -1);
			}
		} catch(InvalidMidiDataException e) {
			System.err.println("\nMidiBus Warning: Message not sent, InvalidMidiDataException thrown");
		}
	}
	
	/**
	 * Sends a MidiMessage object.
	 *
	 * @param message the MidiMessage
	 * @see #sendMessage(int status)
	 * @see #sendMessage(int status, int data)
	 * @see #sendMessage(int status, int data1, int data2)
	 * @see #sendMessage(int command, int channel, int data1, int data2)
	 * @see #sendNoteOn(int channel, int pitch, int velocity)
	 * @see #sendNoteOff(int channel, int pitch, int velocity)
	 * @see #sendControllerChange(int channel, int number, int value)
	*/
	public void sendMessage(MidiMessage message) {
		for(Receiver receiver : out_receivers) {
			receiver.send(message,-1);
		}
	}
	
	/**
	 * Sends a NoteOn message to a channel with the specified pitch and velocity.
	 *
	 * @param channel the channel associated with the message
	 * @param pitch the pitch associated with the message
	 * @param velocity the velocity associated with the message
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
			for(Receiver receiver : out_receivers) {
				receiver.send(message, -1);
			}
		} catch(InvalidMidiDataException e) {
			System.err.println("\nMidiBus Warning: Message not sent, InvalidMidiDataException thrown");
		}
	}
	
	/**
	 * Sends a NoteOff message to a channel with the specified pitch and velocity.
	 *
	 * @param channel the channel associated with the message
	 * @param pitch the pitch associated with the message
	 * @param velocity the velocity associated with the message
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
			for(Receiver receiver : out_receivers) {
				receiver.send(message, -1);
			}
		} catch(InvalidMidiDataException e) {
			System.err.println("\nMidiBus Warning: Message not sent, InvalidMidiDataException thrown");
		}
	}
	
	/**
	 * Sends a ControllerChange message to a channel with the specified number and value.
	 *
	 * @param channel the channel associated with the message
	 * @param number the number associated with the message
	 * @param value the value associated with the message
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
			for(Receiver receiver : out_receivers) {
				receiver.send(message, -1);
			}
		} catch(InvalidMidiDataException e) {
			System.err.println("\nMidiBus Warning: Message not sent, InvalidMidiDataException thrown");
		}
	}
	
	/* -- Midi In -- */
	
	/**
	 * Notifies all types of listeners of a new MIDI message from one of the MIDI input devices.
	 *
	 * @param message the new inbound MidiMessage
	*/
	void notifyListeners(MidiMessage message) {
		byte[] data = message.getMessage();
		
		/* -- RawMidiListener -- */
		
		for(RawMidiListener raw_listener : raw_listeners) {
 			raw_listener.rawMidiMessage(data);
		}
		
		/* -- SimpleMidiListener -- */
			
		if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_ON) {
			for(SimpleMidiListener simple_listener : simple_listeners) {
				simple_listener.noteOn((int)(data[0] & 0x0F),(int)(data[1] & 0xFF),(int)(data[2] & 0xFF));
			}
		} else if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_OFF) {
			for(SimpleMidiListener simple_listener : simple_listeners) {
				simple_listener.noteOff((int)(data[0] & 0x0F),(int)(data[1] & 0xFF),(int)(data[2] & 0xFF));
			}
		} else if((int)((byte)data[0] & 0xF0) == ShortMessage.CONTROL_CHANGE) {
			for(SimpleMidiListener simple_listener : simple_listeners) {
				simple_listener.controllerChange((int)(data[0] & 0x0F),(int)(data[1] & 0xFF),(int)(data[2] & 0xFF));
			}
		}
		
		/* -- StandardMidiListener -- */
		
		for(StandardMidiListener standard_listener : standard_listeners) {
			standard_listener.midiMessage(message);
		}
	}
	
	/**
	 * Notifies any of the supported methods implemented inside the PApplet parent of a new MIDI message from one of the MIDI input devices.
	 *
	 * @param message the new inbound MidiMessage
	*/
	void notifyPApplet(MidiMessage message) {	
		byte[] data = message.getMessage();

		if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_ON) {
			if(eventMethod_noteOn != null) {
				try {
					eventMethod_noteOn.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF) });
				} catch (Exception e) {
					System.err.println("\nMidiBus Warning: Disabling noteOn() because an unkown exception was thrown and caught");
					e.printStackTrace();
					eventMethod_noteOn = null;
				}
			}
			if(eventMethod_noteOn_withBusName != null) {
				try {
					eventMethod_noteOn_withBusName.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF), bus_name });
				} catch (Exception e) {
					System.err.println("\nMidiBus Warning: Disabling noteOn() with bus_name because an unkown exception was thrown and caught");
					e.printStackTrace();
					eventMethod_noteOn_withBusName = null;
				}
			}
		} else if((int)((byte)data[0] & 0xF0) == ShortMessage.NOTE_OFF) {
			if(eventMethod_noteOff != null) {
				try {
					eventMethod_noteOff.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF) });
				} catch (Exception e) {
					System.err.println("\nMidiBus Warning: Disabling noteOff() because an unkown exception was thrown and caught");
					e.printStackTrace();
					eventMethod_noteOff = null;
				}
			}
			if(eventMethod_noteOff_withBusName != null) {
				try {
					eventMethod_noteOff_withBusName.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF), bus_name });
				} catch (Exception e) {
					System.err.println("\nMidiBus Warning: Disabling noteOff() with bus_name because an unkown exception was thrown and caught");
					e.printStackTrace();
					eventMethod_noteOff_withBusName = null;
				}
			}
		} else if((int)((byte)data[0] & 0xF0) == ShortMessage.CONTROL_CHANGE) {
			if(eventMethod_controllerChange != null) {
				try {
					eventMethod_controllerChange.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF) });
				} catch (Exception e) {
					System.err.println("\nMidiBus Warning: Disabling controllerChange() because an unkown exception was thrown and caught");
					e.printStackTrace();
					eventMethod_controllerChange = null;
				}
			}
			if(eventMethod_controllerChange_withBusName != null) {
				try {
					eventMethod_controllerChange_withBusName.invoke(parent, new Object[] { (int)(data[0] & 0x0F), (int)(data[1] & 0xFF), (int)(data[2] & 0xFF), bus_name });
				} catch (Exception e) {
					System.err.println("\nMidiBus Warning: Disabling controllerChange() with bus_name because an unkown exception was thrown and caught");
					e.printStackTrace();
					eventMethod_controllerChange_withBusName = null;
				}
			}
		}
		
		if(eventMethod_rawMidi != null) {
			try {
				eventMethod_rawMidi.invoke(parent, new Object[] { data });
			} catch (Exception e) {
				System.err.println("\nMidiBus Warning: Disabling rawMidi() because an unkown exception was thrown and caught");
				e.printStackTrace();
				eventMethod_rawMidi = null;
			}
		}
		if(eventMethod_rawMidi_withBusName != null) {
			try {
				eventMethod_rawMidi_withBusName.invoke(parent, new Object[] { data, bus_name });
			} catch (Exception e) {
				System.err.println("\nMidiBus Warning: Disabling rawMidi() with bus_name because an unkown exception was thrown and caught");
				e.printStackTrace();
				eventMethod_rawMidi_withBusName = null;
			}
		}
		
		if(eventMethod_midiMessage != null) {
			try {
				eventMethod_midiMessage.invoke(parent, new Object[] { message });
			} catch (Exception e) {
				System.err.println("\nMidiBus Warning: Disabling midiMessage() because an unkown exception was thrown and caught");
				e.printStackTrace();
				eventMethod_midiMessage = null;
			}
		}
		if(eventMethod_midiMessage_withBusName != null) {
			try {
				eventMethod_midiMessage_withBusName.invoke(parent, new Object[] { message });
			} catch (Exception e) {
				System.err.println("\nMidiBus Warning: Disabling midiMessage() with bus_name because an unkown exception was thrown and caught");
				e.printStackTrace();
				eventMethod_midiMessage_withBusName = null;
			}
		}
		
	}
	
	/* -- Listener Handling -- */
	
	/**
	 * 	Adds a listener who will be notified each time a new MIDI message is received from a MIDI input device. If the listener has already been added, it will not be added again.
	 *
	 * @param listener the listener to add
	 * @return true if and only the listener was sucessfully added
	 * @see #removeMidiListener(E listener)
	*/
	public boolean addMidiListener(MidiListener listener) {
		for(MidiListener current : listeners) if(current == listener) return false;
		
		listeners.add(listener);
		
		if(listener instanceof RawMidiListener) raw_listeners.add((RawMidiListener)listener);
		else if(listener instanceof SimpleMidiListener) simple_listeners.add((SimpleMidiListener)listener);
		else if(listener instanceof StandardMidiListener)  standard_listeners.add((StandardMidiListener)listener);
		
		return true;
	}
	
	/**
	 * 	Removes a given listener.
	 *
	 * @param listener the listener to remove
	 * @return true if and only the listener was sucessfully removed
	 * @see #addMidiListener(E listener)
	*/
	public boolean removeMidiListener(MidiListener listener) {
		for(MidiListener current : listeners) {
			if(current == listener) {
				listeners.remove(listener);

				if(listener instanceof RawMidiListener) raw_listeners.remove((RawMidiListener)listener);
				else if(listener instanceof SimpleMidiListener) simple_listeners.remove((SimpleMidiListener)listener);
				else if(listener instanceof StandardMidiListener)  standard_listeners.remove((StandardMidiListener)listener);
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
	 * @return the name of this MidiBus
	 * @see #setBusName(String bus_name)
	*/
	public String getBusName() {
		return bus_name;
	}
	
	/**
	 * Changes the name of this MidiBus.
	 *
	 * @param bus_name the new name of this MidiBus
	 * @see #getBusName()
	*/
	public void setBusName(String bus_name) {
		this.bus_name = bus_name;
	}
	
	/**
	 * Queries java's MidiSystem class to find the index any input MIDI device who's name matches device_name. If an empty string is passed -1 is automatically returned. If no matching input MIDI device is found -1 is returned.
	 * <p>
	 * If two or more MIDI inputs have the same name, whichever appears first when {@link #list()} is called will be returned.
	 *
	 * @param device_name the name to search for
	 * @see outputDeviceNameToNumber(String device_name)
	*/
	public int inputDeviceNameToNumber(String device_name) {
		if(!device_name.equals("")) {
			MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
			
				for(int i = 0;i < available_devices.length;i++) {
					try {
						if(available_devices[i].getName().equals(device_name) && MidiSystem.getMidiDevice(available_devices[i]).getMaxReceivers() == 0) return i;
					} catch (MidiUnavailableException e) {
						System.err.println("\nMidiBus Warning: device ["+i+"] \""+available_devices[i].getName()+"\" could not be gotten during inputDeviceNameToNumber(), MidiUnavailableException thrown");
					}
				}
				
			
			System.err.println("MidiBus Warning: No input MIDI devices named: \""+device_name+"\" were found");		
		}
		return -1;
	}
	
	/**
	 * Queries java's MidiSystem class to find the index any output MIDI device who's name matches device_name. If an empty string is passed -1 is automatically returned. If no matching output MIDI device is found -1 is returned.
	 * <p>
	 * If two or more MIDI outputs have the same name, whichever appears first when {@link #list()} is called will be returned.
	 *
	 * @param device_name the name to search for
	 * @see inputDeviceNameToNumber(String device_name)
	*/
	public int outputDeviceNameToNumber(String device_name) {
		if(!device_name.equals("")) {
			MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
			
				for(int i = 0;i < available_devices.length;i++) {
					try {
						if(available_devices[i].getName().equals(device_name) && MidiSystem.getMidiDevice(available_devices[i]).getMaxTransmitters() == 0) return i;
					} catch (MidiUnavailableException e) {
						System.err.println("\nMidiBus Warning: device ["+i+"] \""+available_devices[i].getName()+"\" could not be gotten during outputDeviceNameToNumber(), MidiUnavailableException thrown");
					}
				}
		
			
			System.err.println("MidiBus Warning: No output MIDI devices named: \""+device_name+"\" were found");		
		}
		return -1;
	}
	
	/* -- Object -- */
	
	/**
	 *
	 */
	public String toString() {
		String output = "MidiBus: "+bus_name+" [";
		output += in_devices.size()+" input(s), ";
		output += out_devices.size()+" output(s), ";
		output += listeners.size()+" listener(s)]";
		return output;
	}
	
	/**
	 *
	 */
	public boolean equals(MidiBus midibus) {
		if(this.getBusName() != midibus.getBusName()) return false;
		if(this.in_devices != midibus.in_devices) return false;
		if(this.out_devices != midibus.out_devices) return false;
		if(this.listeners != midibus.listeners) return false;
		return true;
	}
	
	/**
	 *
	 */
	public MidiBus clone() {
		MidiBus clone = new MidiBus(parent, -1, -1, bus_name);
		
		for(MidiDevice device : in_devices) {
			clone.addInput(device.getDeviceInfo().getName());
		}
		
		for(MidiDevice device : out_devices) {
			clone.addOutput(device.getDeviceInfo().getName());
		}
		
		for(MidiListener listener : listeners) {
			clone.addMidiListener(listener);
		}
		
		return clone;
	}
	
	/**
	 *
	 */
	public int hashCode() {
		return bus_name.hashCode()+in_devices.hashCode()+out_devices.hashCode()+listeners.hashCode();
	}
	
	/**
	 * Override the finalize() method from java.lang.Object
	 *
	*/
	protected void finalize() {
		close();
		parent.unregisterDispose(this);
	}
	
	/* -- Shutting Down -- */
	
	/**
	 * Closes this MidiBus and all connections it has with other MIDI devices. This method exists as per standard javax.sound.midi syntax. It is functionaly equivalent to stop() and dispose().
	 *
	 * @see #stop()
	 * @see #dispose()
	*/
	public void close() {		
		clearAll();
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
	
	/* -- Static Methods -- */
	
	/**
	 * Lists the name and index of all MidiDevices available and indicates if they are inputs, outputs or both.
	 *
	 * @see #returnList()
	*/
	static public void list() {
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
		MidiDevice device;
				
		System.out.println("\nAvailable Midi Devices:");
		System.out.println("-----------------------");
		
		for(int i = 0;i < available_devices.length;i++) {
			try {
				device = MidiSystem.getMidiDevice(available_devices[i]);
				if (device.getMaxReceivers() == 0) {
					System.out.println("["+i+"] \""+available_devices[i].getName()+"\" [Input]");
				} else if (device.getMaxTransmitters() == 0) {
					System.out.println("["+i+"] \""+available_devices[i].getName()+"\" [Output]");
				} else {
					System.out.println("["+i+"] \""+available_devices[i].getName()+"\" [Input/Output]");
				}
			} catch (MidiUnavailableException e) {
				System.err.println("\nMidiBus Warning: device ["+i+"] \""+available_devices[i].getName()+"\" could not be gotten during list(), MidiUnavailableException thrown");
			}
		}
	}
	
	/**
	 * Returns a 2D array containing the names and types of each available MidiDevice according to their index. Suppose the returned array is <code>String[][]</code> device_names. Then <code>device_names[i][0]</code> contains the name of the device with the index <code>i</code> and <code>device_names[i][1]</code> contains the type of the device with the index <code>i</code> (either "Input", "Output" or "Input/Output").
	 *
	 * @return the 2D array of MidiDevice names and types.
	 * @see #list()
	*/
	static public String[][] returnList() {
		MidiDevice.Info[] available_devices = MidiSystem.getMidiDeviceInfo();
		MidiDevice device;
		
		String[][] device_names = new String[available_devices.length][2];
		
		for(int i = 0;i < available_devices.length;i++) {
			try {
				device_names[i][0] = available_devices[i].getName();
				device = MidiSystem.getMidiDevice(available_devices[i]);
				if (device.getMaxReceivers() == 0) {
					device_names[i][1] = "Input";
				} else if (device.getMaxTransmitters() == 0) {
					device_names[i][1] = "Output";
				} else {
					device_names[i][1] = "Input/Output";
				}
			} catch (MidiUnavailableException e) {
				System.err.println("\nMidiBus Warning: device ["+i+"] \""+available_devices[i].getName()+"\" could not be gotten during returnList(), MidiUnavailableException thrown");
			}
		}
		
		return device_names;
	}
	
	/* -- Nested Classes -- */
	
	private class MReceiver implements Receiver {
		
		MReceiver() {
			
		}
		
		public void close() {
			out_receivers.remove(this);
		}
		
		public void send(MidiMessage message, long timeStamp) {
			
			if(message.getStatus() == ShortMessage.NOTE_ON && message.getMessage()[2] == 0) {
				try {
					ShortMessage tmp_message = (ShortMessage)message;
					tmp_message.setMessage(ShortMessage.NOTE_OFF, tmp_message.getData1(), tmp_message.getData2());
					message = tmp_message;
				} catch (Exception e) {
					System.err.println("\nMidiBus Warning: Mystery error during noteOn (0 velocity) to noteOff conversion");
				}
			}
			
			notifyListeners(message);
			notifyPApplet(message);
		}
		
	}
}