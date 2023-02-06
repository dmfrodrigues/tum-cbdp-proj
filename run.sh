#!/bin/sh
set -e

if [ ! -f /var/lib/postgresql/data/pg_hba.conf ]
then
    mkdir -p /var/lib/postgresql/data
    chown postgres:postgres /var/lib/postgresql/data
    chmod 0777 /var/lib/postgresql/data
fi

su -c "sh /init_db.sh" -m postgres

cd app/build/classes/java/main && rmiregistry &
sleep 1

if [ -z "$LEADER_HOST" ]
then
    ./gradlew run
else
    ./gradlew run --args="-j $LEADER_HOST"
fi
