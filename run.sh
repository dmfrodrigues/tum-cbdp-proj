#!/bin/sh
set -e

su -c "sh /init_db.sh" -m postgres

cd app/build/classes/java/main && rmiregistry &
sleep 1

if [ -z "$LEADER_HOST" ]
then
    ./gradlew run
else
    ./gradlew run --args="-j $LEADER_HOST"
fi
