#!/bin/bash

name=${1}.xml
adb shell "su -c cat /data/data/etienned.lecteuropus/files/card.xml" > cards/$name
adb shell "su -c cat /data/data/etienned.lecteuropus/files/expected.xml" > expected/$name
