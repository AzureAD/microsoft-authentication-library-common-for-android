#!/bin/bash
emulator @test -no-window -no-audio -wipe-data -no-snapshot
adb wait-for-device
sleep 10
gradle common:connectedDebugAndroidTest