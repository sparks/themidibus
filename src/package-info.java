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

/**
 * This package provides simplified ways to access and use to installed MIDI system resources and is designed specifically to be used in a <a target="_blank" href="http://www.processing.org">Processing</a> sketch, although it could just as easily be used within any standard java program.
 * <p>
 * <b style="color:red;">IMPORTANT</b>: Mac users must install a <a href="http://www.humatic.de/htools/mmj.htm">mmj, <i>a Mac OS X universal binary java Midi subsystem</i></a> - or an equivalent third party Midi subsystem - because apple does not provide build-in Midi support for java. Mmj is recommended because it works, it's free and it's almost open source (CC licence), although there is no 64 bit version yet.
 * <p>
 * It is important to understand that themidibus offers very little functionality that isn't available from the <a target="_blank" href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html">javax.sound.midi</a> package. What it does offer - <i>in the spirit of Processing's easy to use sketch/prototyping style</i> - is a clean and simple way to access the major features of the <a target="_blank" href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html">javax.sound.midi</a> package with added integration and support for <a target="_blank" href="http://www.processing.org">Processing</a>, most notably in the form of support for noteOn(), noteOff() and controllerChange() methods to handle inbound midi within the {@link themidibus.PApplet}.
 *<p>
 * Anyone trying to build a complex and full featured MIDI application should take the time to read the documentation for java's native MIDI support package, <a target="_blank" href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/package-summary.html">javax.sound.midi</a>, because it offers a more full feature and flexible alternative to this package, although it does do so at the cost of a some added complexity. In addition, it may be worthwhile to skim <a href="http://java.sun.com/docs/books/tutorial/sound/index.html">the "official" Java Tutorial</a> for the javax.sound.* packages.
 * <p>
 * As with all things Processing, getting basic core functionallity of themidibus up and running in a sketch is a matter of only a few line. The {@link themidibus.MidiBus} class provides everything needed to recieve and send MIDI inside a processing sketch, all the rest is fluff, padding and extras. To get to the short and sweet and jump right into themidibus, either refer to the paragraph titled "Typical Implementation, Simple" in the description for the {@link themidibus.MidiBus} class, or go download the code examples available at <a href="http://www.smallbutdigital.com/themidibus.php">http://www.smallbutdigital.com/themidibus.php</a>.
 * <p>
 * Although it takes almost no effort to get started with this package, building more advanced applications requires a good understanding of the package's bus system. Although the bus system used by MidiBus is very simple, powerful and efficient, it may not immediately make sense. Possibly useful and/or confusing explanations as well as examples  <a href="http://www.smallbutdigital.com/themidibus.php">can be found online</a>. Please take the time to check them out.
 * @version 003
 * @author Severin Smith
*/

package themidibus;
