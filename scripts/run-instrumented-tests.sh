#!/bin/bash
echo =============================================
echo Starting ADB Daemon
echo =============================================
adb start-server
echo =============================================
avdmanager list avd
echo =============================================
emulator @test -no-window -no-audio -wipe-data &
sleep 30
gradle -version
echo =============================================
echo Running instrumented tests
echo =============================================
gradle common:connectedLocalDebugAndroidTest -i -Psugar=true

