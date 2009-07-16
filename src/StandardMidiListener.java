/**
 * Copyright (c) 2009 Severin Smith

 * This file is part of a library called The MidiBus (themidibus) - http://www.smallbutdigital.com/themidibus.php.

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

import javax.sound.midi.MidiMessage;

/**
 * A StandardMidiListener can be notified of incomming MIDI messages in MidiMessage form, usually by a MidiBus object which it is connected to. Typically it would analyse and react to incomming MIDI messages in some useful way.
 *
 * @version 004
 * @author Severin Smith
 * @see MidiListener
 * @see RawMidiListener
 * @see SimpleMidiListener
 * @see MidiBus
 * @see javax.sound.midi.MidiMessage
*/
public interface StandardMidiListener extends MidiListener {
	/**
	 * Objects notifying this StandardMidiListener of a new MIDI message call this method and pass the MidiMessage
	 * 
	 * @param message the MidiMessage received
	*/
	public void midiMessage(MidiMessage message);
}