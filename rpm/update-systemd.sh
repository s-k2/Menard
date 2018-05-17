#!/bin/bash

MANAGER_VERSION=`find deploy/bin -name "manager-*.jar" -printf '%f\n' | sort -r | head -n 1`

sed s/__MANAGER_VERSION__/$MANAGER_VERSION/g rpm/menard.service.template >deploy/systemd/menard.service