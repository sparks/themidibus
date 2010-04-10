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
 * This is a placeholder interface which makes it easier to program and manipulate objects which implement subinterfaces of MidiListener like {@link RawMidiListener}, {@link SimpleMidiListener} or {@link StandardMidiListener}. It makes it easier to create lists and arrays of such object and allows for a standard method {@link MidiBus#addMidiListener(MidiListener listener)} to add any type of listener to a MidiBus object.
 *
 * @version 004
 * @author Severin Smith
 * @see RawMidiListener
 * @see SimpleMidiListener
 * @see StandardMidiListener
 * @see MidiBus
*/
public interface MidiListener {

}