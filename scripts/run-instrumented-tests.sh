#!/bin/bash
echo =============================================
echo Starting ADB Daemon
echo =============================================
adb start-server
echo =============================================
echo "$ANDROID_AVD_HOME, $ANDROID_SDK_HOME/avd and $HOME/.android/avd"
echo =============================================
emulator @test -no-window -no-audio -wipe-data &
sleep 60
gradle -version
echo =============================================
echo Running instrumented tests
echo =============================================
gradle common:connectedLocalDebugAndroidTest -i -Psugar=true

