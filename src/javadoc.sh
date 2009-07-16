#!/bin/bash

echo
echo "Javadocs Script for The MidiBus library"
echo "======================================="
cd `dirname $0`

echo "Setting up ..."

if ! cd ../reference
	then
	echo "No ../reference directory, making it now!"
	echo
	
	if ! mkdir ../reference
		then
		echo "Failed to make new ../reference directory!"
		echo
		exit 1
	fi
	
	else 
	cd ../src
fi

echo "Making new javadocs ..."
echo

if ! javadoc -classpath /Applications/Processing.app/Contents/Resources/Java/core.jar -author -version -d ../reference *.java
	then
	echo "Failed to run javadoc!"
	echo
	exit 1
fi

echo
echo "Done!"
echo
exit 0