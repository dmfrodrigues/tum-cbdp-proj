#!/bin/sh
set -e

# Start MongoDB
mongod --dbpath /data/db --quiet --logpath /dev/null &

cd app/build/classes/java/main && rmiregistry &
sleep 1

./gradlew test
