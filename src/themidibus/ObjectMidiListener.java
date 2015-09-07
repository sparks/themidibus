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
 * A ObjectMidiListener can be notified of incomming NoteOn, NoteOff and ControllerChange events via {@link Note} and {@link ControlChange} objects, usually by a MidiBus object which it is connected to. Typically it would analyse and react to incomming messages in some useful way.
 *
 * @version 008
 * @author Severin Smith, Marc Koderer
 * @see MidiListener
 * @see RawMidiListener
 * @see SimpleMidiListener
 * @see StandardMidiListener
 * @see Note
 * @see ControlChange
 * @see MidiBus
*/

public interface ObjectMidiListener extends MidiListener {
	
	/**
	 * Objects notifying this ObjectMidiListener of a new NoteOn events call this method.
	 * 
	 * @param note the note object associated with this event
	*/
	public void noteOn(Note note);
	
	/**
	 * Objects notifying this ObjectMidiListener of a new NoteOff events call this method.
	 * 
	 * @param note the note object associated with this event
	*/
	public void noteOff(Note note);
	
	/**
	 * Objects notifying this ObjectMidiListener of a new ControllerChange events call this method.
	 * 
	 * @param change the ControlChange object associated with this event
	*/
	public void controllerChange(ControlChange change);
	
}