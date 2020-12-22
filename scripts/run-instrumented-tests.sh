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
gradle common:testDebugUnitTest -i
echo =============================================
echo Running instrumented tests -i
echo =============================================
gradle common:connectedDebugAndroidTest -i
echo =============================================================
echo Running gradle clean to remove files now owned by docker user
echo =============================================================
gradle clean -i
echo =============================================================
echo Dumping environment variables of docker container
echo =============================================================
env
