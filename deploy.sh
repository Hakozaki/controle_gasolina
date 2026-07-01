#!/bin/bash
set -x 

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/controle-gasolina-debug.apk