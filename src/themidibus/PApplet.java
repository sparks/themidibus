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
import javax.sound.midi.MidiMessage;

/**
 * PApplet is your processing application or sketch. In it you can implement the following methods which will be called whenerever a MidiBus object attached to the PApplet, recieves a new incomming MIDI message of the appropriate type.
 * <p>
 * <i><b style="color:red;">Note:</b> This page is a dummy page for documentation of the MidiBus' extention of the regular PApplet's functionality, for the full documentation of PApplet please visits the <a target="_blank" href="http://dev.processing.org/reference/core/javadoc/processing/core/PApplet.html">Processing javadocs</a></i>
 *
 * @version 004
 * @author Severin Smith
 * @see MidiBus
 * @see MidiListener
 * @see RawMidiListener
 * @see StandardMidiListener
 * @see SimpleMidiListener
*/
public class PApplet{	
	
	/**
	 * Is passed the channel, controller number and contoller value associated with every new ContollerChange MIDI message recieved by a MidiBus attached to this applet.
	 *
	 * @param channel the channel on which the ContollerChange arrived
	 * @param number the controller number associated with the ContollerChange
	 * @param value the controller value associated with the ContollerChange
	 * @see #controllerChange(int channel, int pitch, int velocity, String bus_name)
	*/
	public void controllerChange(int channel, int number, int value) {
		
	}
	
	/**
	 * Is passed the channel, pitch and velocity associated with every new NoteOff MIDI message recieved by a MidiBus attached to this applet and the name of the MidiBus which recieved the message.
	 *
	 * @param channel the channel on which the ContollerChange arrived
	 * @param number the controller number associated with the ContollerChange
	 * @param value the controller value associated with the ContollerChange
	 * @param bus_name the name of MidiBus which recieved the ContollerChange
	 * @see #controllerChange(int channel, int pitch, int velocity)
	*/
	public void controllerChange(int channel, int number, int value, String bus_name) {
		
	}
	
	/**
	 * Is passed the raw MidiMessage associated with every new MIDI message recieved by a MidiBus attached to this applet.
	 *
	 * @param message the MidiMessage recieved
	 * @see #midiMessage(MidiMessage message, String bus_name)
	*/
	public void midiMessage(MidiMessage message) {
		
	}
	
	/**
	 * Is passed the raw MidiMessage associated with every new MIDI message recieved by a MidiBus attached to this applet and the name of the MidiBus which recieved the message.
	 *
	 * @param message the MidiMessage recieved
	 * @param bus_name the name of MidiBus which recieved the MIDI message 
	 * @see #midiMessage(MidiMessage message)
	*/
	public void midiMessage(MidiMessage message, String bus_name) {
		
	}
	
	/**
	 * Is passed the channel, pitch and velocity associated with every new NoteOff MIDI message recieved by a MidiBus attached to this applet.
	 *
	 * @param channel the channel on which the NoteOff arrived
	 * @param pitch the pitch associated with the NoteOff
	 * @param velocity the velocity associated with the NoteOff
	 * @see #noteOff(int channel, int pitch, int velocity, String bus_name)
	*/
	public void noteOff(int channel, int pitch, int velocity) {
		
	}
	
	/**
	 * Is passed the channel, pitch and velocity associated with every new NoteOff MIDI message recieved by a MidiBus attached to this applet and the name of the MidiBus which recieved the message.
	 *
	 * @param channel the channel on which the NoteOff arrived
	 * @param pitch the pitch associated with the NoteOff
	 * @param velocity the velocity associated with the NoteOff
	 * @param bus_name the name of MidiBus which recieved the NoteOff
	 * @see #noteOff(int channel, int pitch, int velocity)
	*/
	public void noteOff(int channel, int pitch, int velocity, String bus_name) {
		
	}
	
	/**
	 * Is passed the channel, pitch and velocity associated with every new NoteOn MIDI message recieved by a MidiBus attached to this applet.
	 *
	 * @param channel the channel on which the NoteOn arrived
	 * @param pitch the pitch associated with the NoteOn
	 * @param velocity the velocity associated with the NoteOn
	 * @see #noteOn(int channel, int pitch, int velocity, String bus_name)
	*/
	public void noteOn(int channel, int pitch, int velocity) {
		
	}
	
	/**
	 * Is passed the channel, pitch and velocity associated with every new NoteOn MIDI message recieved by a MidiBus attached to this applet and the name of the MidiBus which recieved the message.
	 *
	 * @param channel the channel on which the NoteOn arrived
	 * @param pitch the pitch associated with the NoteOn
	 * @param velocity the velocity associated with the NoteOn
	 * @param bus_name the name of MidiBus which recieved the NoteOn 
	 * @see #noteOn(int channel, int pitch, int velocity)
	*/
	public void noteOn(int channel, int pitch, int velocity, String bus_name) {
		
	}
	
	/**
	 * Is passed the raw data associated with every new MIDI message recieved by a MidiBus attached to this applet.
	 *
	 * @param data the raw data associated with the MIDI message
	 * @see #rawMidi(byte[] data, String bus_name)
	*/
	public void rawMidi(byte[] data) {
		
	}
	
	/**
	 * Is passed the raw data associated with every new MIDI message recieved by a MidiBus attached to this applet and the name of the MidiBus which recieved the message.
	 *
	 * @param data the raw data associated with the MIDI message
	 * @param bus_name the name of MidiBus which recieved the MIDI message 
	 * @see #rawMidi(byte[] data)
	*/
	public void rawMidi(byte[] data, String bus_name) {
		
	}
}
