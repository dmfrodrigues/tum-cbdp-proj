#!/bin/sh
set -e

# Start MongoDB
mongod --dbpath /data/db --quiet --logpath /dev/null &

cd app/build/classes/java/main && rmiregistry &
sleep 10

if [ -z "$LEADER_HOST" ]
then
    ./gradlew run
else
    ./gradlew run --args="-j $LEADER_HOST"
fi
