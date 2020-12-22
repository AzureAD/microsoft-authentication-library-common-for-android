#!/bin/bash
echo =============================================
echo Starting ADB Daemon
echo =============================================
adb start-server
emulator @test -no-window -no-audio -wipe-data &
sleep 30
gradle -version
echo =============================================
echo Running unit tests
echo =============================================
gradle common:testDebugUnitTest -debug
echo =============================================
echo Running instrumented tests -debug
echo =============================================
gradle common:connectedDebugAndroidTest -debug
echo =============================================================
echo Running gradle clean to remove files now owned by docker user
echo =============================================================
gradle clean -debug
echo =============================================================
echo Dumping environment variables of docker container
echo =============================================================
env
