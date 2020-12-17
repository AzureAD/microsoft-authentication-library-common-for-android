#!/bin/bash
emulator @test -no-window -no-audio -wipe-data -no-snapshot
adb wait-for-device
sleep 10
docker run --privileged -v "$PWD":/home/gradle/ -w /home/gradle/ authclient.azurecr.io/samples/dbi-instrumented-api30 gradle common:connectedDebugAndroidTest