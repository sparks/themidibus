#!/bin/bash

echo
echo Zipping Script for themidibus library
echo -------------------------------
cd `dirname $0`

echo Setting up ...

if ! cd ../..
	then
	echo Failed directory switching
	exit 1
fi

echo Zipping ...

if ! zip -rq themidibus themidibus/examples/ themidibus/library/ themidibus/reference/ themidibus/src/ themidibus/INSTALL.txt -x \*.DS_Store
	then
	echo Zipping failed
	exit 1
fi

echo Done!
echo
exit 0
