#!/bin/bash
adb start-server
emulator @test -no-window -no-audio -wipe-data -no-snapshot
adb wait-for-device
sleep 10
gradle -version
gradle common:connectedDebugAndroidTest
