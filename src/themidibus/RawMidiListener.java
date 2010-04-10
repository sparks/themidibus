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

/**
 * A RawMidiListener can be notified of incomming MIDI messages in raw form, usually by a MidiBus object which it is connect to. Typically it would analyse and react to incomming MIDI messages in some useful way.
 *
 * @version 004
 * @author Severin Smith
 * @see MidiListener
 * @see SimpleMidiListener
 * @see StandardMidiListener
 * @see MidiBus
*/
public interface RawMidiListener extends MidiListener {
	/**
	 * Objects notifying this RawMidiListener of a new MIDI message call this method and pass the raw message to it.
	 * 
	 * @param data the data bytes that make up the MIDI message
	*/
	public void rawMidiMessage(byte[] data);
}