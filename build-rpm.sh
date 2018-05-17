#!/bin/bash

if [ -z $1 ]
	then
		echo "usage: build-rpm.sh version"
		exit 1
fi

cp /media/sf_daten/Programme/Menard/rpm/menard-$1.tar.gz /usr/src/packages/SOURCES/menard-$1.tar.gz
cp /media/sf_daten/Programme/Menard/rpm/menard.specs /usr/src/packages/SPECS/menard.specs
cd /usr/src/packages/
rpmbuild -ba SPECS/menard.specs
