#!/bin/bash
echo =============================================
echo Starting ADB Daemon
echo =============================================
adb start-server
emulator @test -no-window -no-audio -wipe-data &
sleep 30
gradle -version
echo =============================================
echo Running unit and instrumented tests
echo =============================================
gradle common:testDebugUnitTest common:connectedDebugAndroidTest -i

