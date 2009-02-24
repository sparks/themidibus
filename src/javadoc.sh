#!/bin/bash
# This is a ghetto litte script to ease the pain making new javadocs
# It tries to eer on the side of caution, but if everything explodes ....

# IT'S NOT MY FAULT!

#Also DO NOT move this script out of the src dir, it will crash and burn if you do so (I think)
#Also if your copy of Processing is not installed in /Applications/ you might want to change line 36

echo
echo Javadocs Script for themidibus library
echo -------------------------------
cd `dirname $0`

echo Setting up ...

if ! cd ../reference
	then
	echo No ../reference directory, making it now!
	echo
	
	if ! mkdir ../reference
		then
		echo Failed to make new ../reference directory!
		echo
		exit 1
	fi
	
	else 
	cd ../src
fi

echo Making new javadocs ...
echo

if ! javadoc -classpath /Applications/Processing.app/Contents/Resources/Java/core.jar -author -version -d ../reference *.java
	then
	echo Failed to run javadoc!
	echo
	exit 1
fi

echo
echo Done!
echo
exit 0