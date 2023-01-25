#!/bin/sh
set -e

su -c "sh /init_db.sh" -m postgres

cd app/build/classes/java/main && rmiregistry &
sleep 1

./gradlew run --args="-j $1"
