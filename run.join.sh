#!/bin/sh

cd app/build/classes/java/main && rmiregistry &
sleep 1

./gradlew run --args="-j $1"
