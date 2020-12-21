#!/bin/bash
adb start-server
emulator @test -no-window -no-audio -wipe-data -no-snapshot &
sleep 30
gradle -version
gradle common:connectedDebugAndroidTest
