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
 * The ControlChange class represents a change in value from a controller.
 * 
 * @version 007
 * @author Severin Smith, Marc Koderer
 * @see MidiBus
 * @see ObjectMidiListener
 * @see Note
*/

public class ControlChange {

	public int channel;
	public int number;
	public int value;

	public long timestamp;
	public String bus_name;

	/**
	 * Constructs a ControlChange object
	 * 
	 * @param channel the channel of the ControlChange
	 * @param number the number of the ControlChange
	 * @param value the value of the ControlChange
	*/
	public ControlChange(int channel, int number, int value) {
		this(channel, number, value, -1, null);
	}


	/**
	 * Constructs a ControlChange object
	 * 
	 * @param channel the channel of the ControlChange
	 * @param number the number of the ControlChange
	 * @param value the value of the ControlChange
	 * @param timestamp the timestamp of the ControlChange
	 * @param bus_name the name of MidiBus associated with the ControlChange 
	*/
	public ControlChange(int channel, int number, int value, long timestamp, String bus_name) {
		this.channel = channel;
		this.number = number;
		this.value = value;

		this.timestamp = timestamp;
		this.bus_name = bus_name;
	}

	/**
	 * Set channel of the ControlChange
	 * 
	 * @param channel the channel to set
	*/
	public void setChannel(int channel) {
		this.channel = channel;
	}

	/**
	 * Return the channel of the ControlChange
	 * 
	 * @return the channel
	*/
	public int channel() {
		return channel;
	}

	/**
	 * Set number of the ControlChange
	 * 
	 * @param number the number to set
	*/
	public void setNumber(int number) {
		this.number = number;
	}

	/**
	 * Return the number of the ControlChange
	 * 
	 * @return the number
	*/
	public int number() {
		return number;
	}

	/**
	 * Set value of the ControlChange
	 * 
	 * @param value the value to set
	*/
	public void setValue(int value) {
		this.value = value;
	}

	/**
	 * Return the value of the ControlChange
	 * 
	 * @return the value
	*/
	public int value() {
		return value;
	}

	/**
	 * Returns a string in the format [c:channel, n:number, v:value, ts:timestamp, b:bus_name] e.g "[c:0, n:65, v:123, ts:1234, b:bus123]". If timestamp or bus_name isn't set, it is omitted.
	 *
	 * @return the string representation
	*/
	public String toString() {
		String result = "[c:" + channel + ", v:" + number + ", n:" + value;
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

		ControlChange other = (ControlChange)obj;

		if(other.channel != this.channel) return false;
		if(other.number != this.number) return false;
		if(other.value != this.value) return false;

		if(other.timestamp != this.timestamp) return false;
		if(other.bus_name != this.bus_name) return false;

		return true;
	}

}
