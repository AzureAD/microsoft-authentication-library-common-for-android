#!/bin/bash
echo =============================================
echo Starting ADB Daemon
echo =============================================
adb start-server
echo =============================================
emulator @test -no-window -no-audio -wipe-data &
sleep 100
gradle -version
echo =============================================
echo Running instrumented tests
echo =============================================
gradle common:connectedLocalDebugAndroidTest -i -Psugar=true

