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
 * The Note class represents a pressed or released key.
 * 
 * @version 008
 * @author Severin Smith, Marc Koderer
 * @see MidiBus
 * @see ObjectMidiListener
 * @see ControlChange
*/

public class Note {

	static String[] pitchMap = new String[] {
		"C",
		"C#",
		"D",
		"D#",
		"E",
		"F",
		"F#",
		"G",
		"G#",
		"A",
		"A#",
		"B"
	};

	public int channel;
	public int pitch;
	public int velocity;

	public long ticks;

	public long timestamp;
	public String bus_name;

	/**
	 * Constructs a Note object
	 * 
	 * @param channel the channel of the Note
	 * @param pitch the pitch of the Note
	 * @param velocity the velocity of the Note
	*/
	public Note(int channel, int pitch, int velocity) {
		this(channel, pitch, velocity, 0, -1, null);
	}

	/**
	 * Constructs a Note object
	 * 
	 * @param channel the channel of the Note
	 * @param pitch the pitch of the Note
	 * @param velocity the velocity of the Note
	 * @param ticks the length in ticks of the Note
	*/
	public Note(int channel, int pitch, int velocity, int ticks) {
		this(channel, pitch, velocity, ticks, -1, null);
	}

	/**
	 * Constructs a Note object
	 * 
	 * @param channel the channel of the Note
	 * @param pitch the pitch of the Note
	 * @param velocity the velocity of the Note
	 * @param timestamp the timestamp of the Note
	 * @param bus_name the name of MidiBus associated with the Note 
	*/
	public Note(int channel, int pitch, int velocity, long timestamp, String bus_name) {
		this(channel, pitch, velocity, 0, timestamp, bus_name);
	}

	/**
	 * Constructs a Note object
	 * 
	 * @param channel the channel of the Note
	 * @param pitch the pitch of the Note
	 * @param velocity the velocity of the Note
	 * @param ticks the length in ticks of the Note
	 * @param timestamp the timestamp of the Note
	 * @param bus_name the name of MidiBus associated with the Note 
	*/
	public Note(int channel, int pitch, int velocity, int ticks, long timestamp, String bus_name) {
		this.channel = channel;
		this.pitch = pitch;
		this.velocity = velocity;

		this.ticks = ticks;

		this.timestamp = timestamp;
		this.bus_name = bus_name;
	}

	/**
	 * Set channel of the Note
	 * 
	 * @param channel the channel to set
	*/
	public void setChannel(int channel) {
		this.channel = channel;
	}

	/**
	 * Return the channel of the Note
	 * 
	 * @return the channel
	*/
	public int channel() {
		return channel;
	}

	/**
	 * Set pitch of the Note
	 * 
	 * @param pitch the pitch to set
	*/
	public void setPitch(int pitch) {
		this.pitch = pitch;
	}

	/**
	 * Return the pitch of the Note
	 * 
	 * @return the pitch
	*/
	public int pitch() {
		return pitch;
	}

	/**
	 * Return the pitch of the Note relative to C. Range is 0-12.
	 * 
	 * @return the relative pitch
	*/
	public int relativePitch() {
		return pitch;
	}

	/**
	 * Return the octave of the Note. Octaves are divided by the note C
	 * 
	 * @return the octave
	*/
	public int octave() {
		return pitch/12;
	}

	/**
	 * Return the name of the note, e.g. "C" or "G#".
	 *
	 * @return the note name
	*/
	public String name() {
		return pitchMap[pitch%12];
	}

	/**
	 * Set velocity of the Note
	 * 
	 * @param velocity the velocity to set
	*/
	public void setVelocity(int velocity) {
		this.velocity = velocity;
	}

	/**
	 * Return the velocity of the Note
	 * 
	 * @return the velocity
	*/
	public int velocity() {
		return velocity;
	}

	/**
	 * Set length in ticks of the Note
	 * 
	 * @param ticks the ticks value to set
	*/
	public void setTicks(int ticks) {
		this.ticks = ticks;
	}

	/**
	 * Return the length in ticks of the Note
	 * 
	 * @return the ticks length
	*/
	public long ticks() {
		return ticks;
	}

	/**
	 * Returns a string in the format [Note Name, c:channel, p:pitch, v:velocity, t:ticks, ts:timestamp, b:bus_name] e.g "[C, c:0, p:65, v:123, t:0, ts:1234, b:bus123]". If ticks, timestamp or bus_name isn't set, it is omitted.
	 *
	 * @return the string representation
	*/
	public String toString() {
		String result = "[" + name() + ", c:" + channel + ", p:" + pitch + ", v:" + velocity;
		if(ticks != 0) result += ", t:" + ticks;
		if(timestamp != -1) result += ", ts:" + timestamp;
		if(bus_name != null) result += ", b:" + bus_name;
		result += "]";

		return result;
	}

	/**
	 * Check if all fields are equal.
	 *
	 * @return true if both objects can be considered to be equals
	*/
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj == null) return false;

		if(getClass() != obj.getClass()) return false;

		Note other = (Note)obj;

		if(other.channel != this.channel) return false;
		if(other.pitch != this.pitch) return false;
		if(other.velocity != this.velocity) return false;

		if(other.ticks != this.ticks) return false;

		if(other.timestamp != this.timestamp) return false;
		if(other.bus_name != this.bus_name) return false;

		return true;
	}

}
