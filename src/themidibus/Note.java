package themidibus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The Note class represents a pressed or released key.
 * <p>
 * Function {@link themidibus.SimpleMidiListener#noteOn(Note)} and
 * {@link themidibus.SimpleMidiListener#noteOff(Note)} will create such an
 * object.
 * 
 * @version 006
 * @author Marc Koderer
 * @see MidiBus
 * @see SimpleMidiListener
 */

public class Note {

	private static final Map<Integer, String> PitchMap = Collections
			.unmodifiableMap(new HashMap<Integer, String>() {
				private static final long serialVersionUID = 1L;
				{
					put(0, "C");
					put(1, "C#");
					put(2, "D");
					put(3, "D#");
					put(4, "E");
					put(5, "F");
					put(6, "F#");
					put(7, "G");
					put(8, "G#");
					put(9, "A");
					put(10, "A#");
					put(11, "B");
				}
			});

	String note;
	int pitch;
	int octave;
	int strength;
	long ticks;

	/**
	 * Constructs a Note object
	 * 
	 */
	public Note() {
	}

	/**
	 * Constructs a Note object
	 * 
	 * @param pitch
	 *            The key number
	 * @param strength
	 *            The strength the key was pressed
	 * @param ticks
	 *            Time ticks
	 */
	public Note(int pitch, int strength, long ticks) {
		setPitch(pitch);
		this.strength = strength;
		this.ticks = ticks;
	}

	/***
	 * Set pitch and calculates the note and octave
	 * 
	 * @param pitch
	 */
	public void setPitch(int pitch) {
		this.note = PitchMap.get(pitch % 12);
		this.octave = pitch / 12;
		this.pitch = pitch;
	}

	/***
	 * 
	 * @return The string representation of the note
	 */
	public String getNote() {
		return note;
	}

	/**
	 * 
	 * @return the pitch
	 */
	public int getPitch() {
		return pitch;
	}

	/**
	 * 
	 * @return The Octave of the pressed/released key
	 */
	public int getOctave() {
		return octave;
	}

	/**
	 * 
	 * @return The strength (max. 127)
	 */
	public int getStrength() {
		return strength;
	}

	/**
	 * Sets the strength
	 * 
	 * @param strength
	 */
	public void setStrength(int strength) {
		this.strength = strength;
	}

	/**
	 * 
	 * @return the time ticks of the action
	 */
	public long getTicks() {
		return ticks;
	}

	/**
	 * 
	 * @param ticks
	 *            sets the time ticks
	 */
	public void setTicks(long ticks) {
		this.ticks = ticks;
	}

	/**
	 * Returns the string representation of the note and the octave Like: C#4
	 */
	@Override
	public String toString() {
		return note + octave;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((note == null) ? 0 : note.hashCode());
		result = prime * result + octave;
		return result;
	}

	/**
	 * Can be used to compare note objects
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Note other = (Note) obj;
		if (note == null) {
			if (other.note != null)
				return false;
		} else if (!note.equals(other.note))
			return false;
		if (octave != other.octave)
			return false;
		return true;
	}

	/**
	 * Can be used to compare Strings with notes: 
	 * <p>note.equals("C#") or 
	 * note.equals("C#4);
	 * are both valid.
	 * 
	 * @param string The string value
	 * @return true/false
	 */
	public boolean equals(String string) {
		if (string == null)
			return false;
		if (this.toString().equals(string)) {
			return true;
		} else if (this.note.equals(string)) {
			return true;
		}
		return false;
	}

}
